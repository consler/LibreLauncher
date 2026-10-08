package net.consler.librelauncher.ui.category.home;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.launcher.Launch;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Home
{
    public static JButton playButton;
    public static JPanel panel;
    public static JComboBox<String> instances;

    public static void show()
    {
        panel = new RoundCornerPanel();

        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        playButton = new JButton("Play");
        playButton.setPreferredSize(new Dimension(200, 60));
        playButton.setMaximumSize(new Dimension(200, 60));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setBackground(Main.client.getBackground().darker());
        playButton.setForeground(Color.WHITE);
        playButton.setFocusable(false);
        playButton.setBorderPainted(false);
        playButton.setFont(new Font("Arial", Font.BOLD, 16));
        playButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                Launch.launch((String) instances.getSelectedItem());
            }
        });

        instances = new JComboBox<>();
        instances.setPreferredSize(new Dimension(200, 40));
        instances.setMaximumSize(new Dimension(180, 40));
        instances.setAlignmentX(Component.CENTER_ALIGNMENT);
        instances.setBackground(Main.client.getBackground().darker());
        instances.setForeground(Color.WHITE);
        instances.setFocusable(false);
        instances.setBorder(BorderFactory.createEmptyBorder());
        instances.putClientProperty("FlatLaf.style",
            "buttonBackground: #00000000; " +
            "buttonSeparatorColor: #00000000; " +
            "buttonArrowColor: #00000000; " +
            "buttonHoverArrowColor: #00000000; " +
            "buttonPressedArrowColor: #00000000;"
        );

        new InstancesConfig().getInstanceList().forEach(instances::addItem);

        panel.add(Box.createVerticalGlue());
        panel.add(playButton);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(instances);
        panel.add(Box.createVerticalGlue());

        Main.client.add(panel);
    }

    public static void hide()
    {
        if (panel != null) Main.client.remove(panel);
    }
}