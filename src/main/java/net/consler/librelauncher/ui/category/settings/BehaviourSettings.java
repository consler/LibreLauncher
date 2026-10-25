package net.consler.librelauncher.ui.category.settings;

import javax.swing.*;
import java.awt.*;
import net.consler.librelauncher.settings.SettingsSaver;

public class BehaviourSettings
{
    private final SettingsSaver settingsSaver;
    public JPanel panel;

    public BehaviourSettings(SettingsSaver settingsSaver)
    {
        this.settingsSaver = settingsSaver;
    }

    public JPanel createPanel()
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Settings.createHeader("Behaviour Settings"));

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        JCheckBox trayCheck = new JCheckBox("Close after launch");
        trayCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        trayCheck.setSelected(Boolean.parseBoolean(settingsSaver.get("close_after_launch", "false")));
        trayCheck.addActionListener(e->
        {
            settingsSaver.set("close_after_launch", String.valueOf(trayCheck.isSelected()));
            settingsSaver.save();
        });
        panel.add(trayCheck);

        return panel;
    }
}