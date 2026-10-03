package net.consler.librelauncher.ui.category.settings;

import javax.swing.*;
import java.awt.*;

public class BehaviourSettings
{
    public static JPanel panel;
    public static JPanel createPanel()
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Settings.createHeader("Behaviour Settings"));

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        JCheckBox trayCheck = new JCheckBox("Close after launch");
        trayCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        trayCheck.setSelected(Boolean.parseBoolean(Settings.settingsSaver.get("close_after_launch", "false")));
        trayCheck.addActionListener(e->
        {
            Settings.settingsSaver.set("close_after_launch", String.valueOf(trayCheck.isSelected()));
            Settings.settingsSaver.save();
        });
        panel.add(trayCheck);

        return panel;
    }
}