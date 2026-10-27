package net.consler.librelauncher.ui.category.settings;
import javax.swing.*;
import java.awt.*;
import net.consler.librelauncher.settings.SettingsSaver;

public class AppearanceSettings
{
    private final SettingsSaver settingsSaver;
    public JPanel panel;

    public AppearanceSettings(SettingsSaver settingsSaver)
    {
        this.settingsSaver = settingsSaver;
    }

    public JPanel createPanel()
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Settings.createHeader("Appearance Settings"));

        JLabel themeLabel = new JLabel("Theme");
        themeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(themeLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        JComboBox<String> themeBox = new JComboBox<>(new String[]{"Dark", "Light", "Darcula", "Intellij"});
        themeBox.setMaximumSize(new Dimension(250, 32));
        themeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeBox.setSelectedItem(settingsSaver.get("theme", "Dark"));
        themeBox.addActionListener(e ->
        {
            settingsSaver.set("theme", (String) themeBox.getSelectedItem());
            settingsSaver.save();
        });
        panel.add(themeBox);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JCheckBox showSidebar = new JCheckBox("Show Side Panel");
        showSidebar.setSelected(Boolean.parseBoolean(settingsSaver.get("show_side_panel", "false")));
        showSidebar.addActionListener(e ->
        {
            settingsSaver.set("show_side_panel", String.valueOf(showSidebar.isSelected()));
            settingsSaver.save();
        });
        panel.add(showSidebar);

        return panel;
    }
}