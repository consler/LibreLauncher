package net.consler.librelauncher;

import com.formdev.flatlaf.FlatDarkLaf;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.home.Home;
import net.consler.librelauncher.ui.Sidebar;
import net.consler.librelauncher.ui.Titlebar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

public class Main
{
    public static JFrame client;

    static void main(String[] args)
    {
        ThemeManager.init();
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() ->
        {
            client = new JFrame("LibreLauncher");
            client.setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("icon.png")));
            client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.setSize(800, 500);
            client.setLocationRelativeTo(null);
            client.setLayout(new BorderLayout());
            client.setUndecorated(true);

            client.getContentPane().setBackground(client.getBackground().darker());

            client.setShape(new RoundRectangle2D.Double(0, 0, 800, 500, 20, 20));
            client.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    client.setShape(new RoundRectangle2D.Double(0, 0, client.getWidth(), client.getHeight(), 20, 20));
                }
            });

            Titlebar.show(client);
            Sidebar.show();

            Home.show();

            client.setVisible(true);
        });
    }
}