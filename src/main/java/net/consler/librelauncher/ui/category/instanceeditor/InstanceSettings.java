package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.settings.AutoDetectJavaDialog;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.util.JavaVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InstanceSettings
{
    public JPanel panel;

    private final InstancesConfig instancesConfig;

    public InstanceSettings(Main main, InstancesConfig instancesConfig)
    {
        this.instancesConfig = instancesConfig;
    }

    public JPanel createPanel(Instance instance)
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
        ramField.setText(instancesConfig.getProperty(instance.getName(), "ram", "2048"));
        ramField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ramField.setAlignmentX(Component.LEFT_ALIGNMENT);

        ThemeManager.silentField(ramField);

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
        javaPathField.setText(instancesConfig.getProperty(instance.getName(), "java_path", JavaVersion.current().executable().toString()));
        javaPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        javaPathField.setAlignmentX(Component.LEFT_ALIGNMENT);
        ThemeManager.silentField(javaPathField);

        javaPathField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                instancesConfig.saveProperty(instance.getName(), "java_path", javaPathField.getText());
                instancesConfig.save();
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
                    instancesConfig.saveProperty(instance.getName(), "java_path", javaPathField.getText());
                    instancesConfig.save();
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
