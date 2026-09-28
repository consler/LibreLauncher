package net.consler.librelauncher.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.consler.librelauncher.Main;

import javax.swing.*;
import java.awt.*;

public class Sidebar
{
    public static void show()
    {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(40, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.setBackground(Main.client.getBackground().darker());

        JButton homeButton = createButton("client/home.svg");
        sidebar.add(homeButton);

        JButton instanceMangerButton = createButton("client/instance.svg");
        sidebar.add(instanceMangerButton);

        sidebar.add(Box.createVerticalGlue());

        JButton settingsButton = createButton("client/settings.svg");
        sidebar.add(settingsButton);

        JButton foldersButton = createButton("client/folder.svg");
        sidebar.add(foldersButton);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

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
}