package net.consler.librelauncher;

import com.formdev.flatlaf.FlatDarkLaf;
import net.consler.librelauncher.ui.Home;
import net.consler.librelauncher.ui.Sidebar;
import net.consler.librelauncher.ui.Titlebar;

import javax.swing.*;
import java.awt.*;

public class Main
{
    public static JFrame client;

    static void main(String[] args)
    {
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

            Titlebar.show();
            Sidebar.show();

            Home.show();

            client.setVisible(true);
        });
    }
}