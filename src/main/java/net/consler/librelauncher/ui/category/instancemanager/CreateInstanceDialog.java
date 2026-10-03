package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.launcher.Download;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.Titlebar;
import net.consler.librelauncherlib.versions.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Objects;

public class CreateInstanceDialog
{
    public static JDialog dialog;

    public static JTextField nameField = new JTextField(36);

    public static JComboBox<String> versionChoice = new JComboBox<>();
    public static JCheckBox filterReleases = new JCheckBox("Releases", true);
    public static JCheckBox filterSnapshots = new JCheckBox("Snapshots");
    public static JCheckBox filterBetas = new JCheckBox("Betas");
    public static JCheckBox filterAlphas = new JCheckBox("Alphas");

    public static JRadioButton loaderVanilla = new JRadioButton("Vanilla", true);
    public static JRadioButton loaderFabric = new JRadioButton("Fabric");
    public static JRadioButton loaderForge = new JRadioButton("Forge");
    public static JRadioButton loaderNeoforge = new JRadioButton("NeoForge");
    public static JRadioButton loaderQuilt = new JRadioButton("Quilt");
    public static ButtonGroup loaders;
    public static JComboBox<String> modLoaderVersionChoice = new JComboBox<>();

    public static JProgressBar progressBar = new JProgressBar(0, 100);

    public static JButton cancelButton = new JButton("Cancel");
    public static JButton createButton = new JButton("Create Instance");

    public static void open()
    {
        dialog = new JDialog(Main.client, "Create New Instance", true);
        dialog.setUndecorated(true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        nameField.setText("");
        versionChoice.setSelectedItem(null);
        modLoaderVersionChoice.setSelectedItem(null);
        modLoaderVersionChoice.setEnabled(true);
        loaderVanilla.setSelected(true);
        progressBar.setValue(0);
        progressBar.setVisible(false);

        putVersions();
        setupLoaderButtons();

        nameField.putClientProperty("JTextField.placeholderText", "Enter instance name...");
        versionChoice.putClientProperty("JComboBox.placeholderText", "Select Version...");
        modLoaderVersionChoice.putClientProperty("JComboBox.placeholderText", "Select Loader Version...");
        cancelButton.putClientProperty("JButton.buttonType", "borderless");

        for (JComponent c : new JComponent[]{nameField, versionChoice, modLoaderVersionChoice, createButton, cancelButton})
        {
            c.putClientProperty("JComponent.minimumHeight", 44);
        }

        for(JCheckBox versionFilter : new JCheckBox[]{filterReleases, filterSnapshots, filterBetas, filterAlphas})
        {
            versionFilter.addActionListener(e -> putVersions());
        }

        versionChoice.addActionListener(e -> putLoaderVersions());

        createButton.putClientProperty("JComponent.minimumWidth", 150);
        progressBar.setPreferredSize(new Dimension(100, 8));

        cancelButton.addActionListener(e -> dialog.dispose());
        createButton.addActionListener(e -> Download.createInstance());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(cancelButton);
        buttons.add(createButton);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(28, 32, 28, 32));
        addRow(form, "Instance Name", nameField);
        addRow(form, "Minecraft Version", row(versionChoice, filterReleases, filterSnapshots, filterBetas, filterAlphas));
        addRow(form, "Mod Loader", row(loaderVanilla, loaderFabric, loaderForge, loaderNeoforge, loaderQuilt));
        addRow(form, "Loader Version", modLoaderVersionChoice);
        addRow(form, null, progressBar);
        addRow(form, null, buttons);

        applyFont(form, ThemeManager.comfortaaBold.deriveFont(14f));
        versionChoice.setPreferredSize(new Dimension(260, versionChoice.getPreferredSize().height));
        versionChoice.setMaximumSize(versionChoice.getPreferredSize());

        dialog.add(Titlebar.create(dialog, "Create New Instance", false, dialog::dispose), BorderLayout.NORTH);
        dialog.add(form, BorderLayout.CENTER);
        dialog.getRootPane().setDefaultButton(createButton);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.pack();
        dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 20, 20));
        dialog.setLocationRelativeTo(Main.client);
        dialog.setVisible(true);
    }

    private static void addRow(JPanel panel, String text, JComponent comp)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        if (text != null)
        {
            JLabel label = new JLabel(text);
            label.setForeground(UIManager.getColor("Label.disabledForeground"));
            c.insets = new Insets(0, 0, 8, 0);
            panel.add(label, c);
        }

        c.insets = new Insets(0, 0, 22, 0);
        panel.add(comp, c);
    }

    private static JPanel row(JComponent... items)
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < items.length; i++)
        {
            if (i > 0) p.add(Box.createHorizontalStrut(18));
            items[i].setAlignmentY(Component.CENTER_ALIGNMENT);
            p.add(items[i]);
        }
        p.add(Box.createHorizontalGlue());
        return p;
    }

    private static void applyFont(Container container, Font font)
    {
        for (Component c : container.getComponents())
        {
            c.setFont(font);
            if (c instanceof Container child) applyFont(child, font);
        }
    }

    private static void putVersions()
    {
        new Thread(() ->
        {
            versionChoice.removeAllItems();
            List<String> versions = VanillaVersions.getVersionsFiltered(filterReleases.isSelected(), filterSnapshots.isSelected(), filterBetas.isSelected(), filterAlphas.isSelected());
            for(String version : versions)
            {
                SwingUtilities.invokeLater(() -> versionChoice.addItem(version));
            }
        }).start();
    }

    private static void putLoaderVersions()
    {
        String loader = getActiveLoader();

        if(versionChoice.getSelectedItem() == null) return;
        if(loader.equals("Vanilla")) return;

        SwingUtilities.invokeLater(() -> modLoaderVersionChoice.removeAllItems());

        switch(loader)
        {
            case "Fabric" ->
            {
                List<String> versions = FabricVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());

                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
            case "Forge" ->
            {
                List<String> versions = ForgeVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());
                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
            case "NeoForge" ->
            {
                List<String> versions = NeoforgeVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());
                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
            case "Quilt" ->
            {
                List<String> versions = QuiltVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());
                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
        }
    }

    private static void setupLoaderButtons()
    {
        loaders = new ButtonGroup();
        for (JRadioButton loaderButton : new JRadioButton[]{loaderVanilla, loaderFabric, loaderForge, loaderNeoforge, loaderQuilt})
        {
            loaders.add(loaderButton);

            modLoaderVersionChoice.setEnabled(!loaderButton.getText().equals("Vanilla"));

            loaderButton.addActionListener(e ->
            {
                modLoaderVersionChoice.removeAllItems();
                new Thread(CreateInstanceDialog::putLoaderVersions).start();
            });

        }
    }

    private static String getActiveLoader()
    {
        if(loaderVanilla.isSelected()) return "Vanilla";
        if(loaderFabric.isSelected()) return "Fabric";
        if(loaderForge.isSelected()) return "Forge";
        if(loaderNeoforge.isSelected()) return "NeoForge";
        if(loaderQuilt.isSelected()) return "Quilt";
        throw new RuntimeException("Couldn't find an active mod loader button");
    }
}