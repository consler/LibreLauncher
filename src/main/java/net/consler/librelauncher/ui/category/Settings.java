package net.consler.librelauncher.ui.category;

import net.consler.librelauncher.Main;

import javax.swing.*;

public class Settings
{
    public static JPanel panel;

    public static void show()
    {
        panel = new UICategory();

        JButton test = new JButton("Test");
        panel.add(test);

        Main.client.add(panel);
    }

    public static void hide()
    {
        if (panel != null) Main.client.remove(panel);
    }
}