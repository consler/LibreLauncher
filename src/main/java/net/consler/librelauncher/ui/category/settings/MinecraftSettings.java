package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncherlib.util.SystemHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MinecraftSettings
{
    private final ThemeManager themeManager;
    private final SettingsSaver settingsSaver;
    public JPanel panel;

    public MinecraftSettings(ThemeManager themeManager, SettingsSaver settingsSaver)
    {
        this.themeManager = themeManager;
        this.settingsSaver = settingsSaver;
    }

    public JPanel createPanel()
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
        ramField.setText(settingsSaver.get("ram", "2048"));
        ramField.setMaximumSize(new Dimension(250, 32));
        ramField.setAlignmentX(Component.LEFT_ALIGNMENT);

        themeManager.silentField(ramField);

        ramField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                settingsSaver.set("ram", ramField.getText());
                settingsSaver.save();
            }
        });
        panel.add(ramField);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel javaPathLabel = new JLabel("Java Path");
        javaPathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(javaPathLabel);

        JTextField javaPathField = new JTextField();
        javaPathField.setText(settingsSaver.get("java_path", SystemHelper.getJavaBin().toString()));
        javaPathField.setMaximumSize(new Dimension(400, 32));
        javaPathField.setAlignmentX(Component.LEFT_ALIGNMENT);

        themeManager.silentField(javaPathField);

        javaPathField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                settingsSaver.set("java_path", javaPathField.getText());
                settingsSaver.save();
            }
        });
        panel.add(javaPathField);

        return panel;
    }
}