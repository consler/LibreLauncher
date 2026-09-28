package net.consler.librelauncher.ui;

import net.consler.librelauncher.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class Titlebar
{
    private static Point initialClick;

    public static void show()
    {
        JPanel titlebar = new JPanel(new BorderLayout());
        titlebar.setPreferredSize(new Dimension(Main.client.getWidth(), 32));
        titlebar.setBackground(Main.client.getBackground().darker());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        ImageIcon rawIcon = new ImageIcon(Objects.requireNonNull(Main.class.getResource("icon.png")));
        Image scaledImage = rawIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));
        leftPanel.add(iconLabel);

        JLabel titleLabel = new JLabel("LibreLauncher");
        titleLabel.setForeground(new Color(220, 220, 220));
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        leftPanel.add(titleLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        JButton minimizeButton = createControlButton("—", e -> Main.client.setState(Frame.ICONIFIED));
        JButton closeButton = createControlButton("✕", e -> System.exit(0));

        rightPanel.add(minimizeButton);
        rightPanel.add(closeButton);

        titlebar.add(leftPanel, BorderLayout.WEST);
        titlebar.add(rightPanel, BorderLayout.EAST);

        MouseAdapter dragAdapter = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                initialClick = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                int thisX = Main.client.getLocation().x;
                int thisY = Main.client.getLocation().y;

                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                Main.client.setLocation(thisX + xMoved, thisY + yMoved);
            }
        };

        titlebar.addMouseListener(dragAdapter);
        titlebar.addMouseMotionListener(dragAdapter);

        Main.client.add(titlebar, BorderLayout.NORTH);
    }

    private static JButton createControlButton(String text, ActionListener action)
    {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(46, 32));
        button.setBorderPainted(false);
        button.setBackground(Main.client.getBackground().darker());
        button.setFocusable(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.addActionListener(action);

        return button;
    }
}