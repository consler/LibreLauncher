package net.consler.librelauncher.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.consler.librelauncher.Main;

import javax.swing.*;
import java.awt.*;

public class Sidebar
{
    private final Main main;
    private String currentCategory = "home";

    public JPanel sidebar;
    public JButton homeButton;
    public JButton instanceManagerButton;
    public JButton settingsButton;

    public Sidebar(Main main)
    {
        this.main = main;
    }

    public void show()
    {
        sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(32, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        sidebar.add(Box.createVerticalGlue());

        homeButton = createButton("icon/home.svg");
        homeButton.addActionListener(e -> setUICategory("home"));
        sidebar.add(homeButton);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        instanceManagerButton = createButton("icon/instance.svg");
        instanceManagerButton.addActionListener(e -> setUICategory("instancemanager"));
        sidebar.add(instanceManagerButton);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        settingsButton = createButton("icon/settings.svg");
        settingsButton.addActionListener(e -> setUICategory("settings"));
        sidebar.add(settingsButton);

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(Box.createRigidArea(new Dimension(0, 32)));

        main.client.add(sidebar, BorderLayout.WEST);
    }
    private JButton createButton(String path)
    {
        FlatSVGIcon icon = new FlatSVGIcon(Main.class.getResource(path)).derive(24, 24);
        Color iconColor = UIManager.getColor("Label.disabledForeground");
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter(color -> iconColor);
        icon.setColorFilter(filter);

        JButton button = new JButton();
        button.setIcon(icon);
        button.setPreferredSize(new Dimension(40, 40));
        button.setMaximumSize(new Dimension(40, 40));
        button.setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setVerticalTextPosition(AbstractButton.CENTER);
        button.setHorizontalTextPosition(AbstractButton.CENTER);

        return button;
    }

    public void setUICategory(String category)
    {
        if (category.equals(currentCategory) && !category.equals("instancemanager")) return;

        switch (category)
        {
            case "home" -> main.home.show();
            case "instancemanager" -> main.instanceManager.show();
            case "settings" -> main.settings.show();
        }

        currentCategory = category;
    }
}