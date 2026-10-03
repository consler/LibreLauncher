package net.consler.librelauncher.ui.category.settings;

import javax.swing.*;
import java.awt.*;
public class MinecraftSettings
{
    public static JPanel panel;
    public static JPanel createPanel()
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Settings.createHeader("Default Minecraft Settings"));

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel ramLabel = new JLabel("Maximum Memory Allocation (MB)");
        ramLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ramLabel);

        JTextField ramField = new JTextField();
        ramField.setText(Settings.settingsSaver.get("ram", "4096"));
        ramField.setMaximumSize(new Dimension(250, 32));
        ramField.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e)
            {
                Settings.settingsSaver.set("ram", ramField.getText());
                Settings.settingsSaver.save();
            }
        });
        panel.add(ramField);

        return panel;
    }
}