package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ThemeManager;

import javax.swing.*;
import java.awt.*;

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
        addInstanceButton.addActionListener(e -> CreateInstanceDialog.open());
        panel.add(addInstanceButton);

        panel.add(Box.createVerticalStrut(10));

        instanceFolderButton = createButton("Folder");
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
