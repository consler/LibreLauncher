package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.util.SystemUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SidePanel
{
    private final Main main;
    private final InstanceManager instanceManager;
    public JPanel panel;
    public JButton addInstanceButton;
    public JButton instanceFolderButton;

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

        panel.add(Box.createVerticalStrut(10));

        addInstanceButton = createButton("Add Instance");
        addInstanceButton.addActionListener(e -> new CreateInstanceDialog(main, instanceManager.instancePanel).open());
        panel.add(addInstanceButton);

        panel.add(Box.createVerticalStrut(10));

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
