package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.util.SystemUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SidePanel
{
    public static JPanel panel;
    public static JButton addInstanceButton;
    public static JButton instanceFolderButton;

    public static void show()
    {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(150, 0));
        panel.setMaximumSize(new Dimension(150, 0));

        panel.add(Box.createVerticalStrut(10));

        addInstanceButton = createButton("Add Instance");
        addInstanceButton.setFont(ThemeManager.comfortaaBold);
        addInstanceButton.addActionListener(e -> CreateInstanceDialog.open());
        panel.add(addInstanceButton);

        panel.add(Box.createVerticalStrut(10));

        instanceFolderButton = createButton("Folder");
        instanceFolderButton.setFont(ThemeManager.comfortaaBold);
        instanceFolderButton.addActionListener(e ->
        {
            try
            {
                SystemUtils.openDirectory(InstancesConfig.instancesDir.toFile());
            }
            catch (IOException ex)
            {
                new ErrorDialog(Main.client, ex, "opening folder");
            }
        });
        panel.add(instanceFolderButton);

        InstanceManager.panel.add(panel, BorderLayout.EAST);
    }

    private static JButton createButton(String name)
    {
        JButton button = new JButton(name);
        button.setBorderPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(120, 30));
        button.setMaximumSize(new Dimension(120, 30));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(Main.client.getBackground().darker());
        button.setFont(ThemeManager.comfortaaBold);

        return button;
    }
}
