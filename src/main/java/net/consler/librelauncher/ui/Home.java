package net.consler.librelauncher.ui;

import net.consler.librelauncher.Main;

import javax.swing.*;
import java.awt.*;

public class Home
{
    public static JButton playButton;
    public static JPanel homePanel;
    public static JComboBox<String> instances;

    public static void show()
    {
        homePanel = new JPanel();
        homePanel.setLayout(new BoxLayout(homePanel, BoxLayout.Y_AXIS));

        playButton = new JButton("Play");
        playButton.setPreferredSize(new Dimension(200, 60));
        playButton.setMaximumSize(new Dimension(200, 60));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setOpaque(true);
        playButton.setBackground(Main.client.getBackground().darker());
        playButton.setForeground(Color.WHITE);
        playButton.setFocusable(false);
        playButton.setBorderPainted(false);
        playButton.setFont(new Font("Arial", Font.BOLD, 16));

        instances = new JComboBox<>();
        instances.setPreferredSize(new Dimension(200, 40));
        instances.setMaximumSize(new Dimension(180, 40));
        instances.setAlignmentX(Component.CENTER_ALIGNMENT);
        instances.setOpaque(true);
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

        homePanel.add(Box.createVerticalGlue());
        homePanel.add(playButton);
        homePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        homePanel.add(instances);
        homePanel.add(Box.createVerticalGlue());

        Main.client.getContentPane().add(homePanel);
    }

    public static void hide()
    {
        if (homePanel != null) Main.client.getContentPane().remove(homePanel);
    }
}