package net.consler.librelauncher.ui.category.instancemanager;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
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

        instancePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 255, 255, 20)),
            BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));

        InstanceManager.panel.add(instancePanel, BorderLayout.CENTER);
    }

    public static void populate()
    {
        InstancesConfig instancesConfig = new InstancesConfig();

        instancePanel.removeAll();

        FlatSVGIcon icon = new FlatSVGIcon(Main.class.getResource("icons/play.svg")).derive(12, 12);
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter(color ->  new Color(42, 106, 59));
        icon.setColorFilter(filter);


        for(String instanceName : instancesConfig.instanceCache.keySet())
        {
            JButton instanceButton = new JButton("  " + instanceName);

            instanceButton.setBorderPainted(false);
            instanceButton.setPreferredSize(new Dimension(700, 30));
            instanceButton.setMaximumSize(new Dimension(700, 30));
            instanceButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            instanceButton.setBackground(instancePanel.getBackground().darker());
            instanceButton.setHorizontalAlignment(SwingConstants.LEFT);
            instanceButton.setIcon(icon);

            instancePanel.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
            instancePanel.add(instanceButton);
        }

        instancePanel.revalidate();
        instancePanel.repaint();
    }
}