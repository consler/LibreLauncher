package net.consler.librelauncher.ui.category.settings;
import javax.swing.*;
import java.awt.*;

public class AppearanceSettings
{
    public static JPanel panel;
    public static JPanel createPanel()
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
        themeBox.setSelectedItem(Settings.settingsSaver.get("theme", "Dark"));
        themeBox.addActionListener(e ->
        {
            Settings.settingsSaver.set("theme", (String) themeBox.getSelectedItem());
            Settings.settingsSaver.save();
        });
        panel.add(themeBox);

        return panel;
    }
}