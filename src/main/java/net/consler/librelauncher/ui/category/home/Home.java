package net.consler.librelauncher.ui.category.home;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

import static net.consler.librelauncher.util.InstanceUtils.isAuthenticating;
import static net.consler.librelauncher.util.InstanceUtils.isRunningAndClean;

public class Home
{
    private final Main main;
    public JPanel panel;
    private JButton playButton;
    private JComboBox<String> instances;
    private SettingsSaver settingsSaver;

    private boolean populating = false;

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

        settingsSaver = new SettingsSaver();

        panel = new RoundCornerPanel(main.client.getBackground());
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        playButton = new JButton("Play");
        playButton.setPreferredSize(new Dimension(200, 60));
        playButton.setMaximumSize(new Dimension(200, 60));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        playButton.setForeground(UIManager.getColor("Button.foreground"));
        playButton.setFocusable(false);
        playButton.setFont(main.themeManager.comfortaaBold.deriveFont(18f));
        playButton.addActionListener(e ->
        {
            try
            {
                Launcher.launch((String) instances.getSelectedItem());
            }
            catch (Exception ex)
            {
                new ErrorDialog(main.client, ex, "launching");
            }
        });

        instances = new JComboBox<>();
        instances.setPreferredSize(new Dimension(200, 40));
        instances.setMaximumSize(new Dimension(180, 40));
        instances.setAlignmentX(Component.CENTER_ALIGNMENT);
        instances.setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        instances.setForeground(UIManager.getColor("ComboBox.foreground"));
        instances.setFocusable(false);
        instances.setBorder(BorderFactory.createEmptyBorder());
        instances.putClientProperty("FlatLaf.style",
                "buttonBackground: #00000000; " +
                        "buttonSeparatorColor: #00000000; " +
                        "buttonArrowColor: #00000000; " +
                        "buttonHoverArrowColor: #00000000; " +
                        "buttonPressedArrowColor: #00000000;"
        );
        instances.addItemListener(e ->
        {
            if (populating) return;
            if (e.getStateChange() != ItemEvent.SELECTED) return;

            updatePlayButton();

            Object selected = instances.getSelectedItem();
            if (selected != null)
            {
                settingsSaver.set("last_chosen", selected.toString());
                settingsSaver.save();
            }
        });

        populate();

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
        String lastChosen = settingsSaver != null ? settingsSaver.get("last_chosen") : null;

        populating = true;
        try
        {
            instances.removeAllItems();
            new InstancesConfig().getInstanceList().forEach(instances::addItem);

            if (lastChosen != null)
                instances.setSelectedItem(lastChosen);
        }
        finally
        {
            populating = false;
        }

        updatePlayButton();
    }

    private void updatePlayButton()
    {
        if (playButton == null || instances == null) return;

        Object selected = instances.getSelectedItem();
        playButton.setEnabled(selected != null
                && !isRunningAndClean(selected.toString())
                && !isAuthenticating(selected.toString()));
    }
}