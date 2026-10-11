package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;

public class InstanceManager
{
    public static JPanel panel;
    public static void show()
    {
        if (panel != null)
        {
            Main.showPage("instancemanager");
            return;
        }

        panel = new RoundCornerPanel();
        panel.setLayout(new BorderLayout());

        SidePanel.show();
        InstancePanel.show();

        Main.pageContainer.add(panel, "instancemanager");
        Main.showPage("instancemanager");
    }
}
