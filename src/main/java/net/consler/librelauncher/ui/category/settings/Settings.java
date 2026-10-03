package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;

public class Settings
{
    public static JPanel panel;

    public static void show()
    {
        panel = new RoundCornerPanel();

        JButton test = new JButton("Test");
        panel.add(test);

        Main.client.add(panel);
    }

    public static void hide()
    {
        if (panel != null) Main.client.remove(panel);
    }
}