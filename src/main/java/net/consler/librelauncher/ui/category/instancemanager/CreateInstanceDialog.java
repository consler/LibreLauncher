package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.launcher.Downloader;
import net.consler.librelauncherlib.versions.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Objects;

public class CreateInstanceDialog
{
    private final Main main;
    private final InstancePanel instancePanel;
    public JDialog dialog;

    public JTextField nameField = new JTextField(36);

    public JComboBox<String> versionChoice = new JComboBox<>();
    public JCheckBox filterReleases = new JCheckBox("Releases", true);
    public JCheckBox filterSnapshots = new JCheckBox("Snapshots");
    public JCheckBox filterBetas = new JCheckBox("Betas");
    public JCheckBox filterAlphas = new JCheckBox("Alphas");

    public JRadioButton loaderVanilla = new JRadioButton("Vanilla", true);
    public JRadioButton loaderFabric = new JRadioButton("Fabric");
    public JRadioButton loaderForge = new JRadioButton("Forge");
    public JRadioButton loaderNeoforge = new JRadioButton("NeoForge");
    public JRadioButton loaderQuilt = new JRadioButton("Quilt");
    public ButtonGroup loaders;
    public JComboBox<String> modLoaderVersionChoice = new JComboBox<>();

    public JProgressBar progressBar = new JProgressBar(0, 100);

    public JButton cancelButton = new JButton("Cancel");
    public JButton createButton = new JButton("Create Instance");

    public CreateInstanceDialog(Main main, InstancePanel instancePanel)
    {
        this.main = main;
        this.instancePanel = instancePanel;
    }

    public void open()
    {
        dialog = new JDialog(main.client, "Create New Instance", true);
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

        applyFont(form, main.themeManager.comfortaaBold.deriveFont(14f));
        versionChoice.setPreferredSize(new Dimension(260, versionChoice.getPreferredSize().height));
        versionChoice.setMaximumSize(versionChoice.getPreferredSize());

        dialog.add(main.titlebar.create(dialog, "Create New Instance", false, dialog::dispose), BorderLayout.NORTH);
        dialog.add(form, BorderLayout.CENTER);
        dialog.getRootPane().setDefaultButton(createButton);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        main.themeManager.silentField(nameField);

        Downloader downloader = new Downloader(this, instancePanel);

        cancelButton.addActionListener(e ->
        {
            downloader.cancelInstanceCreation();
            dialog.dispose();
        });
        createButton.addActionListener(e ->
        {
            if(nameField.getText().trim().isEmpty()) return;

            nameField.setEnabled(false);
            versionChoice.setEnabled(false);
            modLoaderVersionChoice.setEnabled(false);
            modLoaderVersionChoice.setEnabled(false);
            filterReleases.setEnabled(false);
            filterAlphas.setEnabled(false);
            filterBetas.setEnabled(false);
            filterSnapshots.setEnabled(false);
            loaderVanilla.setEnabled(false);
            loaderFabric.setEnabled(false);
            loaderForge.setEnabled(false);
            loaderQuilt.setEnabled(false);
            loaderNeoforge.setEnabled(false);
            createButton.setEnabled(false);
            progressBar.setEnabled(false);

            downloader.createInstance();
        });

        dialog.pack();
        dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 20, 20));
        dialog.setLocationRelativeTo(main.client);
        dialog.setVisible(true);
    }

    public void close()
    {
        dialog.dispose();
    }

    private void addRow(JPanel panel, String text, JComponent comp)
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

    private JPanel row(JComponent... items)
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

    private void applyFont(Container container, Font font)
    {
        for (Component c : container.getComponents())
        {
            c.setFont(font);
            if (c instanceof Container child) applyFont(child, font);
        }
    }

    public void putVersions()
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

    public void putLoaderVersions()
    {
        SwingUtilities.invokeLater(() -> modLoaderVersionChoice.removeAllItems());

        String loader = getActiveLoader();

        if(versionChoice.getSelectedItem() == null) return;
        if(loader.equals("Vanilla")) return;

        switch(loader)
        {
            case "fabric" ->
            {
                List<String> versions = FabricVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());

                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
            case "forge" ->
            {
                List<String> versions = ForgeVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());
                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
            case "neoforge" ->
            {
                List<String> versions = NeoforgeVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());
                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
            case "quilt" ->
            {
                List<String> versions = QuiltVersions.getVersionsCompatibleWith(Objects.requireNonNull(versionChoice.getSelectedItem()).toString());
                for(String version : versions)
                {
                    SwingUtilities.invokeLater(() -> modLoaderVersionChoice.addItem(version));
                }
            }
        }
    }

    private void setupLoaderButtons()
    {
        loaders = new ButtonGroup();
        for (JRadioButton loaderButton : new JRadioButton[]{loaderVanilla, loaderFabric, loaderForge, loaderNeoforge, loaderQuilt})
        {
            loaders.add(loaderButton);

            modLoaderVersionChoice.setEnabled(!loaderButton.getText().equals("Vanilla"));

            loaderButton.addActionListener(e ->
            {
                modLoaderVersionChoice.removeAllItems();
                new Thread(this::putLoaderVersions).start();
            });

        }
    }

    public String getActiveLoader()
    {
        if(loaderFabric.isSelected()) return "fabric";
        if(loaderForge.isSelected()) return "forge";
        if(loaderNeoforge.isSelected()) return "neoforge";
        if(loaderQuilt.isSelected()) return "quilt";
        return "vanilla";
    }
}