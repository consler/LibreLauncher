package net.consler.librelauncher.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.category.Home;
import net.consler.librelauncher.ui.category.InstanceManager;
import net.consler.librelauncher.ui.category.Settings;

import javax.swing.*;
import java.awt.*;

public class Sidebar
{
    public static String currentCategory = "home";

    public static JPanel sidebar;
    public static JButton homeButton;
    public static JButton instanceManagerButton;
    public static JButton settingsButton;

    public static void show()
    {
        sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(32, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Main.client.getBackground().darker());
        sidebar.add(Box.createVerticalGlue());

        homeButton = createButton("client/home.svg");
        homeButton.addActionListener(e -> setUICategory("home"));
        sidebar.add(homeButton);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        instanceManagerButton = createButton("client/instance.svg");
        instanceManagerButton.addActionListener(e -> setUICategory("instancemanager"));
        sidebar.add(instanceManagerButton);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        settingsButton = createButton("client/settings.svg");
        settingsButton.addActionListener(e -> setUICategory("settings"));
        sidebar.add(settingsButton);

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(Box.createRigidArea(new Dimension(0, 32)));

        Main.client.add(sidebar, BorderLayout.WEST);
    }
    private static JButton createButton(String path)
    {
        FlatSVGIcon icon = new FlatSVGIcon(Main.class.getResource(path)).derive(24, 24);
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter(color ->  new Color(150, 150, 150));
        icon.setColorFilter(filter);

        JButton button = new JButton();
        button.setIcon(icon);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(40, 40));
        button.setMaximumSize(new Dimension(40, 40));
        button.setBackground(Main.client.getBackground().darker());
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setVerticalTextPosition(AbstractButton.CENTER);
        button.setHorizontalTextPosition(AbstractButton.CENTER);

        return button;
    }

    public static void setUICategory(String category)
    {
        if(category.equals(currentCategory)) return;

        switch (category)
        {
            case "home" ->
            {
                Settings.hide();
                InstanceManager.hide();
                Home.show();
            }
            case "instancemanager" ->
            {
                Settings.hide();
                Home.hide();
                InstanceManager.show();
            }
            case "settings" ->
            {
                Home.hide();
                InstanceManager.hide();
                Settings.show();
            }
        }

        Main.client.revalidate();
        Main.client.repaint();

        currentCategory = category;
    }
}