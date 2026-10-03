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

    public static JPanel titlebar;

    public static void show(JFrame frame)
    {
        frame.add(create(frame, "LibreLauncher", true, () -> System.exit(0)), BorderLayout.NORTH);
    }

    public static JPanel create(Window window, String title, boolean minimizable, Runnable onClose)
    {
        titlebar = new JPanel(new BorderLayout());
        titlebar.setPreferredSize(new Dimension(window.getWidth(), 32));
        titlebar.setBackground(window.getBackground().darker());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        ImageIcon rawIcon = new ImageIcon(Objects.requireNonNull(Main.class.getResource("icon.png")));
        Image scaledImage = rawIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));
        leftPanel.add(iconLabel);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(220, 220, 220));
        titleLabel.setFont(ThemeManager.comfortaaBold);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        leftPanel.add(titleLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        if (minimizable && window instanceof Frame frame)
        {
            rightPanel.add(createControlButton("—", e -> frame.setState(Frame.ICONIFIED), window));
        }
        rightPanel.add(createControlButton("✕", e -> onClose.run(), window));

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
                int thisX = window.getLocation().x;
                int thisY = window.getLocation().y;

                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                window.setLocation(thisX + xMoved, thisY + yMoved);
            }
        };

        titlebar.addMouseListener(dragAdapter);
        titlebar.addMouseMotionListener(dragAdapter);

        return titlebar;
    }

    private static JButton createControlButton(String text, ActionListener action, Window window)
    {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(46, 32));
        button.setBorderPainted(false);
        button.setBackground(window.getBackground().darker());
        button.setFocusable(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.addActionListener(action);

        return button;
    }
}