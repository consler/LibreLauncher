package net.consler.librelauncher.ui.category.settings;
import javax.swing.*;
import java.awt.*;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.instancemanager.InstanceManager;

public class AppearanceSettings
{
    private final Main main;
    private final SettingsSaver settingsSaver;
    public JPanel panel;

    public AppearanceSettings(Main main, SettingsSaver settingsSaver)
    {
        this.main = main;
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

        JComboBox<String> themeBox = new JComboBox<>(ThemeManager.themeLabels());
        themeBox.setMaximumSize(new Dimension(250, 32));
        themeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeBox.setSelectedItem(main.themeManager.getTheme().label);
        themeBox.addActionListener(e ->
        {
            settingsSaver.set("theme", (String) themeBox.getSelectedItem());
            settingsSaver.save();
            main.themeManager.setTheme();
        });
        panel.add(themeBox);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JCheckBox roundWindowCorners = new JCheckBox("Round Window Corners");
        roundWindowCorners.setSelected(Boolean.parseBoolean(settingsSaver.get("round_window_corners", "true")));
        roundWindowCorners.addActionListener(e ->
        {
            settingsSaver.set("round_window_corners", String.valueOf(roundWindowCorners.isSelected()));
            settingsSaver.save();
            main.setRoundWindowCorners(roundWindowCorners.isSelected());
        });
        panel.add(roundWindowCorners);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JCheckBox roundedCorners = new JCheckBox("Round UI Corners");
        roundedCorners.setSelected(Boolean.parseBoolean(settingsSaver.get("rounded_corners", "true")));
        roundedCorners.addActionListener(e ->
        {
            settingsSaver.set("rounded_corners", String.valueOf(roundedCorners.isSelected()));
            settingsSaver.save();
            main.themeManager.setTheme();
        });
        panel.add(roundedCorners);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JCheckBox showSidebar = new JCheckBox("Show Side Panel");
        showSidebar.setSelected(Boolean.parseBoolean(settingsSaver.get("show_side_panel", "false")));
        showSidebar.addActionListener(e ->
        {
            settingsSaver.set("show_side_panel", String.valueOf(showSidebar.isSelected()));
            settingsSaver.save();
            InstanceManager instanceManager = InstanceManager.getInstance();
            if (instanceManager != null)
                instanceManager.updateSidePanelVisibility(showSidebar.isSelected());
        });
        panel.add(showSidebar);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JCheckBox wrapLogLines = new JCheckBox("Wrap Lines of Logs");
        wrapLogLines.setSelected(Boolean.parseBoolean(settingsSaver.get("wrap_logs_lines", "false")));
        wrapLogLines.addActionListener(e ->
        {
            settingsSaver.set("wrap_logs_lines", String.valueOf(wrapLogLines.isSelected()));
            settingsSaver.save();
            InstanceManager instanceManager = InstanceManager.getInstance();
            if (instanceManager != null)
                instanceManager.updateSidePanelVisibility(wrapLogLines.isSelected());
        });
        panel.add(wrapLogLines);

        return panel;
    }
}