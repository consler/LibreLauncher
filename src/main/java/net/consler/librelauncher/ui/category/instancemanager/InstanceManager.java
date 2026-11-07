package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;

public class InstanceManager
{
    private final Main main;
    public JPanel panel;
    public InstancePanel instancePanel;
    private SidePanel sidePanel;

    public static InstanceManager instanceManager;

    public InstanceManager(Main main)
    {
        this.main = main;
        instanceManager = this;
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

        updateSidePanelVisibility(Boolean.parseBoolean(new SettingsSaver().get("show_side_panel", "false")));

        instancePanel = new InstancePanel(main, this);
        instancePanel.show();

        main.pageContainer.add(panel, "instancemanager");
        main.showPage("instancemanager");
    }

    public void updateSidePanelVisibility(boolean visible)
    {
        if (panel == null) return;

        if (visible && sidePanel == null)
        {
            sidePanel = new SidePanel(main, this);
            sidePanel.show();
        }
        else if (!visible && sidePanel != null)
        {
            panel.remove(sidePanel.panel);
            sidePanel = null;
            panel.revalidate();
            panel.repaint();
        }
    }

    public static InstanceManager getInstance()
    {
        return instanceManager;
    }
}
