package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.instanceeditor.download.ModDownloadDialog;
import net.consler.librelauncher.ui.category.instanceeditor.download.ModpackDownloadDialog;
import net.consler.librelauncher.ui.category.instanceeditor.download.ModrinthUpdater;
import net.consler.librelauncher.ui.category.instanceeditor.download.ResourcePackDownloadDialog;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.Instance;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SidePanel
{
    private final Main main;
    private final String type;
    private final Instance instance;

    public JPanel panel;

    public SidePanel(Main main, String type, Instance instance)
    {
        this.main = main;
        this.type = type;
        this.instance = instance;
    }

    public JPanel show()
    {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(140, 0));
        panel.setMaximumSize(new Dimension(140, Integer.MAX_VALUE));

        panel.add(Box.createVerticalStrut(5));

        if(type.equals("Mod"))
        {
            JButton addModButton = createButton("Add mods");
            addModButton.addActionListener(e -> new ModDownloadDialog(main, instance).open());
            panel.add(addModButton);

            panel.add(Box.createVerticalStrut(5));

            JButton addModpackButton = createButton("Add modpack");
            addModpackButton.addActionListener(e -> new ModpackDownloadDialog(main, instance).open());
            panel.add(addModpackButton);

            panel.add(Box.createVerticalStrut(5));

            JButton updateButton = createButton("Update mods");
            updateButton.addActionListener(e -> new ModrinthUpdater(main, instance, InstanceMods.getInstance().mods).open());
            panel.add(updateButton);

            panel.add(Box.createVerticalStrut(5));
        }
        else if (type.equals("Resource Pack"))
        {
            JButton addPacksButton = createButton("Add packs");
            addPacksButton.addActionListener(e -> new ResourcePackDownloadDialog(main, instance).open());
            panel.add(addPacksButton);

            panel.add(Box.createVerticalStrut(5));
        }

        JButton folderButton = createButton("Open folder");
        folderButton.addActionListener(e ->
        {
            try
            {
                File dir;
                switch (type)
                {
                    case "Mod" -> dir = instance.getModsFolder().toFile();
                    case "Resource Pack" -> dir = instance.getResourcePackFolder().toFile();
                    case "World" -> dir = instance.getSavesFolder().toFile();
                    default -> throw new IllegalArgumentException("Unknown type: " + type);
                }
                SystemUtils.openDirectory(dir);
            }
            catch (Exception ex)
            {
                new ErrorDialog(main.client, ex, "opening folder");
                throw new RuntimeException(ex);
            }
        });
        panel.add(folderButton);

        return panel;
    }

    private JButton createButton(String name)
    {
        JButton button = new JButton(name);
        button.setBorderPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(130, 30));
        button.setMaximumSize(new Dimension(130, 30));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        button.setFont(main.themeManager.comfortaaBold);

        return button;
    }
}
