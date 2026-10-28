package net.consler.librelauncher.ui.category.instancemanager;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.instance.Exporter;
import net.consler.librelauncher.instance.Importer;
import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.RoundCornerPanel;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.InstanceProfile;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

import static net.consler.librelauncher.util.SystemUtils.isRunningAndClean;

public class InstancePanel
{
    private final Main main;
    private final InstanceManager instanceManager;
    private static InstancePanel instancePanelClass;

    public JPanel instancePanel;

    InstancesConfig instancesConfig;

    public InstancePanel(Main main, InstanceManager instanceManager)
    {
        this.main = main;
        this.instanceManager = instanceManager;
        instancePanelClass = this;
    }

    public void show()
    {
        instancePanel = new RoundCornerPanel(main.client.getBackground());

        instancePanel.setLayout(new BoxLayout(instancePanel, BoxLayout.Y_AXIS));
        instancePanel.setComponentPopupMenu(createPopup());

        populate();

        instancePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeManager.outlineColor(instancePanel.getBackground())),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));

        JScrollPane scrollPane = new JScrollPane(instancePanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        instanceManager.panel.add(scrollPane, BorderLayout.CENTER);
    }

    public void populate()
    {
        instancesConfig = new InstancesConfig();

        instancePanel.removeAll();

        FlatSVGIcon playIcon = new FlatSVGIcon(Main.class.getResource("icon/play.svg")).derive(16, 16);
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter(color -> new Color(63, 151, 86));
        playIcon.setColorFilter(filter);

        FlatSVGIcon stopIcon = new FlatSVGIcon(Main.class.getResource("icon/stop.svg")).derive(16, 16);
        FlatSVGIcon.ColorFilter filter2 = new FlatSVGIcon.ColorFilter(color -> new Color(255, 50, 50, 100));
        stopIcon.setColorFilter(filter2);

        FlatSVGIcon folderIcon = new FlatSVGIcon(Main.class.getResource("icon/folder.svg")).derive(16, 16);
        FlatSVGIcon.ColorFilter filter3 = new FlatSVGIcon.ColorFilter(color -> new Color(100, 100, 100));
        folderIcon.setColorFilter(filter3);

        FlatSVGIcon editIcon = new FlatSVGIcon(Main.class.getResource("icon/edit.svg")).derive(16, 16);
        FlatSVGIcon.ColorFilter filter4 = new FlatSVGIcon.ColorFilter(color -> new Color(100, 100, 100));
        editIcon.setColorFilter(filter4);

        for (String instanceName : instancesConfig.instanceCache.keySet())
        {
            JPanel rowPanel = new JPanel();
            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
            rowPanel.setPreferredSize(new Dimension(0, 30));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            rowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            rowPanel.setOpaque(false);
            rowPanel.putClientProperty("FlatLaf.style", "arc:25;");
            rowPanel.setBackground(ThemeManager.surfaceColor(instancePanel.getBackground()));
            rowPanel.setComponentPopupMenu(createInstancePopup(instanceName));

            JButton playButton = createButton();
            playButton.setIcon(isRunningAndClean(instanceName) ? stopIcon : playIcon);
            playButton.addActionListener(e ->
            {
                if(isRunningAndClean(instanceName))
                    SystemUtils.killInstance(instanceName);
                else
                    Launcher.launch(instanceName);
            });

            JButton folderButton = createButton();
            folderButton.setIcon(folderIcon);
            folderButton.addActionListener(e ->
            {
                try
                {
                    SystemUtils.openDirectory(InstancesConfig.instancesDir.resolve(instanceName).toFile());
                }
                catch (IOException ex)
                {
                    new ErrorDialog(main.client, ex, "creating UI: Open Folder menu item");
                }
            });

            JButton editButton = createButton();
            editButton.setIcon(editIcon);
            editButton.addActionListener(e -> main.instanceEditor.show(new InstanceProfile(InstancesConfig.instancesDir.resolve(instanceName))));

            JLabel nameLabel = new JLabel(instanceName);
            nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

            rowPanel.add(Box.createHorizontalStrut(10));
            rowPanel.add(playButton);
            rowPanel.add(Box.createHorizontalStrut(10));
            rowPanel.add(nameLabel);
            rowPanel.add(Box.createHorizontalGlue());
            rowPanel.add(editButton);
            rowPanel.add(folderButton);
            rowPanel.add(Box.createHorizontalStrut(10));

            instancePanel.add(Box.createVerticalStrut(5));
            instancePanel.add(rowPanel);
        }

        instancePanel.revalidate();
        instancePanel.repaint();

        main.home.populate();
    }

    private static JButton createButton()
    {
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(true);
        button.setBackground(null);
        button.setPreferredSize(new Dimension(20, 20));
        button.setMaximumSize(new Dimension(20, 20));
        button.setAlignmentY(Component.CENTER_ALIGNMENT);

        return button;
    }

    public JPopupMenu createInstancePopup(String instanceName)
    {
        JPopupMenu contextMenu = new JPopupMenu();

        if(isRunningAndClean(instanceName))
        {
            JMenuItem launch = new JMenuItem("Stop");
            launch.addActionListener(ev ->
                SystemUtils.killInstance(instanceName));
            contextMenu.add(launch);
        }
        else
        {
            JMenuItem launch = new JMenuItem("Launch");
            launch.addActionListener(ev ->
                    Launcher.launch(instanceName));
            contextMenu.add(launch);
        }

        JMenuItem edit = new JMenuItem("Edit");
        edit.addActionListener(ev -> main.instanceEditor.show(new InstanceProfile(InstancesConfig.instancesDir.resolve(instanceName))));
        contextMenu.add(edit);

        JMenuItem export = new JMenuItem("Export");
        export.addActionListener(ev ->
        {
            try
            {
                Exporter.export(InstancesConfig.instancesDir.resolve(instanceName).toFile());
            }
            catch (IOException ex)
            {
                new ErrorDialog(main.client, ex, "exporting");
                throw new RuntimeException(ex);
            }
        });
        contextMenu.add(export);

        JMenuItem folder = new JMenuItem("Open Folder");
        folder.addActionListener(ev ->
        {
            try
            {
                SystemUtils.openDirectory(InstancesConfig.instancesDir.resolve(instanceName).toFile());
            }
            catch (IOException ex)
            {
                new ErrorDialog(main.client, ex, "creating UI: Open Folder menu item");
                throw new RuntimeException(ex);
            }
        });
        contextMenu.add(folder);

        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(ev ->
        {
            SystemUtils.killInstance(instanceName);
            instancesConfig.deleteInstance(instanceName);
            populate();
        });
        contextMenu.add(delete);

        return contextMenu;
    }

    public JPopupMenu createPopup()
    {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem addInstance = new JMenuItem("Add Instance");
        addInstance.addActionListener(ev -> new CreateInstanceDialog(main, instanceManager.instancePanel).open());
        contextMenu.add(addInstance);

        JMenuItem importInstance = new JMenuItem("Import");
        importInstance.addActionListener(ev ->
        {
            try
            {
                Path gameDir = Importer.doImport(InstancesConfig.instancesDir.toFile());
                if(gameDir == null) return;

                InstanceProfile instance = new InstanceProfile(gameDir);
                InstancesConfig instancesConfig = new InstancesConfig();
                instancesConfig.saveProperty(instance.getName(), "version", instance.getVersion());
                instancesConfig.saveProperty(instance.getName(), "modloader", instance.getModloader());
                instancesConfig.saveProperty(instance.getName(), "modloader_version", instance.getLoaderVersion());
                instancesConfig.save();

                instanceManager.instancePanel.populate();
            }
            catch (IOException ex)
            {
                new ErrorDialog(main.client, ex, "importing instance");
                throw new RuntimeException(ex);
            }
        });
        contextMenu.add(importInstance);

        JMenuItem openFolder = new JMenuItem("Open Folder");
        openFolder.addActionListener(ev ->
        {
            try
            {
                SystemUtils.openDirectory(InstancesConfig.instancesDir.toFile());
            }
            catch (IOException ex)
            {
                new ErrorDialog(main.client, ex, "opening folder");
            }
        });
        contextMenu.add(openFolder);

        return contextMenu;
    }

    public static synchronized InstancePanel getInstance()
    {
        return instancePanelClass;
    }
}