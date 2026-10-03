package net.consler.librelauncher.ui.category.instancemanager;

import net.consler.librelauncher.config.settings.InstancesConfig;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;

public class InstancePanel
{

    public static JPanel instancePanel;

    public static void show()
    {
        instancePanel = new RoundCornerPanel();
        instancePanel.setLayout(new BoxLayout(instancePanel, BoxLayout.Y_AXIS));

        populate();

        InstanceManager.panel.add(instancePanel, BorderLayout.CENTER);
    }

    public static void populate()
    {
        InstancesConfig instancesConfig = new InstancesConfig();

        for(String instanceName : instancesConfig.instanceCache.keySet())
        {
            JButton instanceButton = new JButton(instanceName);

            instanceButton.setBorderPainted(false);
            instanceButton.setPreferredSize(new Dimension(700, 30));
            instanceButton.setMaximumSize(new Dimension(700, 30));
            instanceButton.setAlignmentX(Component.CENTER_ALIGNMENT);

            instancePanel.add(instanceButton);
        }
    }
}