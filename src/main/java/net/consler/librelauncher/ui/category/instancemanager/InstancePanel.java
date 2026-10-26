package net.consler.librelauncher.ui.category.instancemanager;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.instance.Exporter;
import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.RoundCornerPanel;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.InstanceProfile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import static net.consler.librelauncher.util.SystemUtils.isRunningAndClean;

public class InstancePanel
{
    private final Main main;
    private final InstanceManager instanceManager;

    public JPanel instancePanel;

    public InstancePanel(Main main, InstanceManager instanceManager)
    {
        this.main = main;
        this.instanceManager = instanceManager;
    }

    public void show()
    {
        instancePanel = new RoundCornerPanel(main.client.getBackground());
        instancePanel.setLayout(new BoxLayout(instancePanel, BoxLayout.Y_AXIS));

        populate();

        instancePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 255, 255, 20)),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));

        instanceManager.panel.add(instancePanel, BorderLayout.CENTER);
    }

    public void populate()
    {
        InstancesConfig instancesConfig = new InstancesConfig();

        instancePanel.removeAll();

        FlatSVGIcon playIcon = new FlatSVGIcon(Main.class.getResource("icons/play.svg")).derive(16, 16);
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter(color -> new Color(63, 151, 86));
        playIcon.setColorFilter(filter);

        FlatSVGIcon stopIcon = new FlatSVGIcon(Main.class.getResource("icons/pause.svg")).derive(16, 16);
        FlatSVGIcon.ColorFilter filter2 = new FlatSVGIcon.ColorFilter(color -> new Color(255, 50, 50, 100));
        stopIcon.setColorFilter(filter2);

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
                    if(!e.isPopupTrigger()) return;

                    JPopupMenu contextMenu = new JPopupMenu();

                    if(isRunningAndClean(instanceName))
                    {
                        JMenuItem launch = new JMenuItem("Stop");
                        launch.addActionListener(ev ->
                        {
                            SystemUtils.killInstance(instanceName);
                            populate();
                        });
                        contextMenu.add(launch);
                    }
                    else
                    {
                        JMenuItem launch = new JMenuItem("Launch");
                        launch.addActionListener(ev ->
                        {
                            Launcher.launch(instanceName);
                            populate();
                        });
                        contextMenu.add(launch);
                    }

                    JMenuItem edit = new JMenuItem("Edit");
                    edit.addActionListener(ev -> main.instanceEditor.show(new InstanceProfile(InstancesConfig.instancesDir.resolve(instanceName))));
                    contextMenu.add(edit);

                    JMenuItem export = new JMenuItem("Export");
                    export.addActionListener(ev ->
                    {
                        try
                        {
                            Exporter.export(InstancesConfig.instancesDir.resolve(instanceName).toFile());
                        }
                        catch (IOException ex)
                        {
                            new ErrorDialog(main.client, ex, "exporting");
                            throw new RuntimeException(ex);
                        }
                    });
                    contextMenu.add(export);

                    JMenuItem folder = new JMenuItem("Open Folder");
                    folder.addActionListener(ev ->
                    {
                        try
                        {
                            SystemUtils.openDirectory(InstancesConfig.instancesDir.resolve(instanceName).toFile());
                        }
                        catch (IOException ex)
                        {
                            new ErrorDialog(main.client, ex, "creating UI: Open Folder menu item");
                            throw new RuntimeException(ex);
                        }
                    });
                    contextMenu.add(folder);

                    JMenuItem delete = new JMenuItem("Delete");
                    delete.addActionListener(ev ->
                    {
                        SystemUtils.killInstance(instanceName);
                        instancesConfig.deleteInstance(instanceName);
                        populate();
                    });
                    contextMenu.add(delete);

                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });

            JButton playButton = new JButton();
            playButton.setIcon(isRunningAndClean(instanceName) ? stopIcon : playIcon);
            playButton.setBorderPainted(false);
            playButton.setFocusPainted(false);
            playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            playButton.setContentAreaFilled(true);
            playButton.setBackground(rowPanel.getBackground());
            playButton.setPreferredSize(new Dimension(20, 20));
            playButton.setHorizontalTextPosition(AbstractButton.CENTER);
            playButton.setVerticalTextPosition(AbstractButton.CENTER);

            playButton.addActionListener(e ->
            {
                if(isRunningAndClean(instanceName))
                {
                    SystemUtils.killInstance(instanceName);
                }
                else
                {
                    Launcher.launch(instanceName);
                }
            });

            JLabel nameLabel = new JLabel(instanceName);

            rowPanel.add(playButton);
            rowPanel.add(nameLabel);

            instancePanel.add(Box.createVerticalStrut(5));
            instancePanel.add(rowPanel);
        }

        instancePanel.revalidate();
        instancePanel.repaint();

        main.home.populate();
    }
}