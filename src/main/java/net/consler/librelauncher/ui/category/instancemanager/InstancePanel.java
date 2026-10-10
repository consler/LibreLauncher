package net.consler.librelauncher.ui.category.instancemanager;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter(color -> new Color(63, 151, 86));
        icon.setColorFilter(filter);

        for (String instanceName : instancesConfig.instanceCache.keySet())
        {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            rowPanel.setPreferredSize(new Dimension(700, 30));
            rowPanel.setMaximumSize(new Dimension(700, 30));
            rowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            rowPanel.setOpaque(false);
            rowPanel.putClientProperty("FlatLaf.style", "arc:25;");
            rowPanel.setBackground(instancePanel.getBackground().darker());
            rowPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    if(e.isPopupTrigger())
                    {
                        JPopupMenu contextMenu = new JPopupMenu();

                        JMenuItem launch = new JMenuItem("Launcher");
                        launch.addActionListener(ev -> Launcher.launch(instanceName));
                        JMenuItem delete = new JMenuItem("Delete");
                        delete.addActionListener(ev ->
                        {
                            instancesConfig.deleteInstance(instanceName);
                            populate();
                        });
                        contextMenu.add(launch);
                        contextMenu.add(delete);

                        contextMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            JButton playButton = new JButton(icon);
            playButton.setBorderPainted(false);
            playButton.setFocusPainted(false);
            playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            playButton.setContentAreaFilled(true);
            playButton.setBackground(rowPanel.getBackground());
            playButton.setPreferredSize(new Dimension(28, 22));

            playButton.addActionListener(e -> Launcher.launch(instanceName));

            JLabel nameLabel = new JLabel(instanceName);

            rowPanel.add(playButton);
            rowPanel.add(nameLabel);

            instancePanel.add(Box.createVerticalStrut(5));
            instancePanel.add(rowPanel);
        }

        instancePanel.revalidate();
        instancePanel.repaint();
    }
}