package net.consler.librelauncher.ui.category;

import net.consler.librelauncher.Main;

import javax.swing.*;

public class InstanceManager
{
    public static JPanel panel;
    public static void show()
    {
        panel = new UICategory();

        panel.add(new JButton("test2"));

        Main.client.add(panel);
    }

    public static void hide()
    {
        if (panel != null) Main.client.remove(panel);
    }
}
