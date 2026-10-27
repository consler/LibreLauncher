package net.consler.librelauncher.ui.category.home;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static net.consler.librelauncher.util.SystemUtils.isRunningAndClean;

public class Home
{
    private final Main main;
    public JButton playButton;
    public JPanel panel;
    public JComboBox<String> instances;

    public Home(Main main)
    {
        this.main = main;
    }

    public void show()
    {
        if (panel != null)
        {
            main.showPage("home");
            return;
        }

        panel = new RoundCornerPanel(main.client.getBackground());
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        playButton = new JButton("Play");
        playButton.setPreferredSize(new Dimension(200, 60));
        playButton.setMaximumSize(new Dimension(200, 60));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setBackground(main.client.getBackground().darker());
        playButton.setForeground(Color.WHITE);
        playButton.setFocusable(false);
        playButton.setFont(main.themeManager.comfortaaBold.deriveFont(18f));

        instances = new JComboBox<>();
        instances.setPreferredSize(new Dimension(200, 40));
        instances.setMaximumSize(new Dimension(180, 40));
        instances.setAlignmentX(Component.CENTER_ALIGNMENT);
        instances.setBackground(main.client.getBackground().darker());
        instances.setForeground(Color.WHITE);
        instances.setFocusable(false);
        instances.setBorder(BorderFactory.createEmptyBorder());
        instances.putClientProperty("FlatLaf.style",
            "buttonBackground: #00000000; " +
            "buttonSeparatorColor: #00000000; " +
            "buttonArrowColor: #00000000; " +
            "buttonHoverArrowColor: #00000000; " +
            "buttonPressedArrowColor: #00000000;"
        );
        instances.addActionListener(l -> playButton.setEnabled(!isRunningAndClean(String.valueOf(instances.getSelectedItem()))));

        new InstancesConfig().getInstanceList().forEach(instances::addItem);

        playButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    Launcher.launch((String) instances.getSelectedItem());
                }
                catch (Exception ex)
                {
                    new ErrorDialog(main.client, ex, "launching");
                }
            }
        });

        panel.add(Box.createVerticalGlue());
        panel.add(playButton);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(instances);
        panel.add(Box.createVerticalGlue());

        main.pageContainer.add(panel, "home");
        main.showPage("home");
    }

    public void populate()
    {
        instances.removeAllItems();
        new InstancesConfig().getInstanceList().forEach(instances::addItem);
    }
}