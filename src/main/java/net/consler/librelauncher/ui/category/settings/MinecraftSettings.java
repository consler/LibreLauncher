package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncherlib.util.JavaVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MinecraftSettings
{
    private final SettingsSaver settingsSaver;
    public JPanel panel;

    public MinecraftSettings(SettingsSaver settingsSaver)
    {
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
        ramField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ramField.setAlignmentX(Component.LEFT_ALIGNMENT);

        ThemeManager.silentField(ramField);

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
        javaPathField.setText(settingsSaver.get("java_path", JavaVersion.current().executable().toString()));
        javaPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        javaPathField.setAlignmentX(Component.LEFT_ALIGNMENT);
        ThemeManager.silentField(javaPathField);

        javaPathField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                settingsSaver.set("java_path", javaPathField.getText());
                settingsSaver.save();
            }
        });

        JButton autoDetectJavaButton = new JButton("Auto Detect");
        autoDetectJavaButton.setMaximumSize(new Dimension(150, 32));
        autoDetectJavaButton.setFocusable(false);
        autoDetectJavaButton.addActionListener(e ->
        {
            new AutoDetectJavaDialog(Main.getInstance().client, javaPathField)
            {
                @Override
                public void onDone()
                {
                    settingsSaver.set("java_path", javaPathField.getText());
                    settingsSaver.save();
                }
            };
        });

        JPanel javaPathRow = new JPanel(new BorderLayout(8, 0));
        javaPathRow.setOpaque(false);
        javaPathRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        javaPathRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        javaPathRow.add(javaPathField, BorderLayout.CENTER);
        javaPathRow.add(autoDetectJavaButton, BorderLayout.EAST);

        panel.add(javaPathRow);

        return panel;
    }
}