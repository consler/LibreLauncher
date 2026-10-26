package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.World;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class InstanceWorlds
{
    private final Main main;
    private JPanel listPanel;
    private SwingWorker<Void, WorldEntry> worldLoader;

    public JPanel panel;

    public InstanceWorlds(Main main)
    {
        this.main = main;
    }

    public JPanel createPanel(InstanceProfile instance)
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Settings.createHeader("Worlds"));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane);

        panel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (!e.isPopupTrigger()) return;
                JPopupMenu popupMenu = new JPopupMenu();
                JMenuItem openFolder = new JMenuItem("Open Worlds Folder");
                openFolder.addActionListener(ev ->
                {
                    try
                    {
                        SystemUtils.openDirectory(instance.getSavesFolder().toFile());
                    }
                    catch (IOException ex)
                    {
                        new ErrorDialog(main.client, ex, "opening worlds folder");
                        throw new RuntimeException(ex);
                    }
                });
                popupMenu.add(openFolder);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        populate(instance);
        return panel;
    }

    public void populate(InstanceProfile instance)
    {
        if (worldLoader != null) worldLoader.cancel(true);
        listPanel.removeAll();
        listPanel.revalidate();
        listPanel.repaint();

        worldLoader = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground() throws IOException
            {
                Path savesFolder = instance.getSavesFolder();
                if (!Files.isDirectory(savesFolder)) return null;

                try (Stream<Path> paths = Files.list(savesFolder))
                {
                    paths.filter(Files::isDirectory)
                            .forEach(worldFolder ->
                            {
                                if (isCancelled()) return;
                                try
                                {
                                    World world = new World(worldFolder);
                                    Image icon = world.getIcon() == null ? null
                                            : world.getIcon().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                                    publish(new WorldEntry(world.getLevelName(), icon));
                                }
                                catch (IOException ignored)
                                {
                                }
                            });
                }
                return null;
            }

            @Override
            protected void process(List<WorldEntry> entries)
            {
                if (worldLoader != this || isCancelled()) return;
                for (WorldEntry entry : entries) addWorldRow(entry);
                listPanel.revalidate();
                listPanel.repaint();
            }

            @Override
            protected void done()
            {
                if (worldLoader != this || isCancelled()) return;
                try
                {
                    get();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    new ErrorDialog(main.client, new Exception(e.getCause()), "listing worlds");
                }
                catch (CancellationException ignored)
                {
                }
            }
        };
        worldLoader.execute();
    }

    private void addWorldRow(WorldEntry entry)
    {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowPanel.setPreferredSize(new Dimension(500, 60));
        rowPanel.setMaximumSize(new Dimension(500, 60));
        rowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rowPanel.setBackground(panel.getBackground().darker());

        if (entry.icon() != null) rowPanel.add(new JLabel(new ImageIcon(entry.icon())));
        else rowPanel.add(Box.createRigidArea(new Dimension(50, 50)));
        rowPanel.add(new JLabel(entry.name()));
        listPanel.add(rowPanel);
        listPanel.add(Box.createVerticalStrut(5));
    }

    private record WorldEntry(String name, Image icon) {}
}
