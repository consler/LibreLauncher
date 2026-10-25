package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.util.SystemHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InstanceSettings
{
    public JPanel panel;

    private final ThemeManager themeManager;
    private final InstancesConfig instancesConfig;

    public InstanceSettings(Main main, InstancesConfig instancesConfig)
    {
        this.themeManager = main.themeManager;
        this.instancesConfig = instancesConfig;
    }

    public JPanel createPanel(InstanceProfile instance)
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Settings.createHeader("Minecraft Settings"));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel ramLabel = new JLabel("Maximum Memory Allocation (MB)");
        ramLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ramLabel);

        JTextField ramField = new JTextField();
        ramField.setText(instancesConfig.getProperty(instance.getName(), "ram", "2048"));
        ramField.setMaximumSize(new Dimension(250, 32));
        ramField.setAlignmentX(Component.LEFT_ALIGNMENT);

        themeManager.silentField(ramField);

        ramField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                instancesConfig.saveProperty(instance.getName(), "ram", ramField.getText());
                instancesConfig.save();
            }
        });
        panel.add(ramField);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel javaPathLabel = new JLabel("Java Path");
        javaPathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(javaPathLabel);

        JTextField javaPathField = new JTextField();
        javaPathField.setText(instancesConfig.getProperty(instance.getName(), "java_path", SystemHelper.getJavaBin().toString()));
        javaPathField.setMaximumSize(new Dimension(400, 32));
        javaPathField.setAlignmentX(Component.LEFT_ALIGNMENT);

        themeManager.silentField(javaPathField);

        javaPathField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                instancesConfig.saveProperty(instance.getName(), "java_path", javaPathField.getText());
                instancesConfig.save();
            }
        });
        panel.add(javaPathField);

        return panel;
    }
}
