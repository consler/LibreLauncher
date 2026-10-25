package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;

public class InstanceManager
{
    private final Main main;
    public JPanel panel;
    public InstancePanel instancePanel;

    public InstanceManager(Main main)
    {
        this.main = main;
    }

    public void show()
    {
        if (panel != null)
        {
            main.showPage("instancemanager");
            return;
        }

        panel = new RoundCornerPanel(main.client.getBackground());
        panel.setLayout(new BorderLayout());

        new SidePanel(main, this).show();
        instancePanel = new InstancePanel(main, this);
        instancePanel.show();

        main.pageContainer.add(panel, "instancemanager");
        main.showPage("instancemanager");
    }
}
