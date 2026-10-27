package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.instance.Importer;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.InstanceProfile;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class SidePanel
{
    private final Main main;
    private final InstanceManager instanceManager;
    public JPanel panel;
    public JButton addInstanceButton;
    public JButton instanceFolderButton;
    public JButton importButton;

    public SidePanel(Main main, InstanceManager instanceManager)
    {
        this.main = main;
        this.instanceManager = instanceManager;
    }

    public void show()
    {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(150, 0));
        panel.setMaximumSize(new Dimension(150, 0));

        panel.add(Box.createVerticalStrut(20));

        addInstanceButton = createButton("Add Instance");
        addInstanceButton.addActionListener(e -> new CreateInstanceDialog(main, instanceManager.instancePanel).open());
        panel.add(addInstanceButton);

        panel.add(Box.createVerticalStrut(5));

        importButton = createButton("Import");
        importButton.addActionListener(e ->
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
        panel.add(importButton);

        panel.add(Box.createVerticalStrut(5));

        instanceFolderButton = createButton("Folder");
        instanceFolderButton.addActionListener(e ->
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
        panel.add(instanceFolderButton);

        instanceManager.panel.add(panel, BorderLayout.EAST);
    }

    private JButton createButton(String name)
    {
        JButton button = new JButton(name);
        button.setBorderPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(120, 30));
        button.setMaximumSize(new Dimension(120, 30));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(main.client.getBackground().darker());
        button.setFont(main.themeManager.comfortaaBold);

        return button;
    }
}
