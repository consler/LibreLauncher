package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.Mod;
import net.consler.librelauncherlib.instance.ModReadException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class InstanceMods
{
    private final Main main;
    private JPanel listPanel;
    private SwingWorker<Void, ModEntry> modLoader;

    public JPanel panel;

    public InstanceMods(Main main)
    {
        this.main = main;
    }

    public JPanel createPanel(InstanceProfile instance)
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Settings.createHeader("Mods"));
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
                if(!e.isPopupTrigger()) return;

                JPopupMenu popupMenu = new JPopupMenu();

                JMenuItem addMods = new JMenuItem("Add Mods");
                addMods.addActionListener(ev -> new ModDownloadDialog(main, instance, InstanceMods.this).open());
                popupMenu.add(addMods);

                JMenuItem openFolder = new JMenuItem("Open Folder");
                openFolder.addActionListener(ev ->
                {
                    try
                    {
                        SystemUtils.openDirectory(instance.getModsFolder().toFile());
                    }
                    catch (IOException ex)
                    {
                        new ErrorDialog(main.client, ex, "opening mods folder");
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
        if (modLoader != null) modLoader.cancel(true);
        listPanel.removeAll();
        listPanel.revalidate();
        listPanel.repaint();

        modLoader = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground() throws IOException
            {
                if (!Files.isDirectory(instance.getModsFolder())) return null;

                try (Stream<Path> paths = Files.list(instance.getModsFolder()))
                {
                    paths.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().endsWith(".jar"))
                            .forEach(modFile ->
                            {
                                if (isCancelled()) return;

                                try
                                {
                                    Mod mod = new Mod(modFile.toFile());
                                    BufferedImage image = mod.getIcon();
                                    BufferedImage scaledImage = null;
                                    if (image != null)
                                    {
                                        scaledImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
                                        Graphics2D graphics = scaledImage.createGraphics();
                                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                                        graphics.drawImage(image, 0, 0, 50, 50, null);
                                        graphics.dispose();
                                    }
                                    publish(new ModEntry(modFile, mod.getName(), scaledImage));
                                }
                                catch (ModReadException ignore)
                                {
                                }
                            });
                }
                return null;
            }

            @Override
            protected void process(List<ModEntry> entries)
            {
                if (modLoader != this || isCancelled()) return;

                for (ModEntry entry : entries)
                {
                    JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    rowPanel.setPreferredSize(new Dimension(500, 60));
                    rowPanel.setMaximumSize(new Dimension(500, 60));
                    rowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    rowPanel.setBackground(panel.getBackground().darker());

                    rowPanel.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mousePressed(MouseEvent e)
                        {
                            if (!e.isPopupTrigger()) return;

                            JPopupMenu popupMenu = new JPopupMenu();

                            JMenuItem delete = new JMenuItem("Delete");
                            delete.addActionListener(ev ->
                            {
                                entry.path().toFile().delete();
                                populate(instance);
                            });
                            popupMenu.add(delete);

                            popupMenu.show(rowPanel, e.getX(), e.getY());
                        }
                    });

                    if (entry.icon() != null) rowPanel.add(new JLabel(new ImageIcon(entry.icon())));
                    rowPanel.add(new JLabel(entry.name()));

                    listPanel.add(rowPanel);
                    listPanel.add(Box.createVerticalStrut(5));
                }

                listPanel.revalidate();
                listPanel.repaint();
            }

            @Override
            protected void done()
            {
                if (modLoader != this || isCancelled()) return;

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
                    new ErrorDialog(main.client, new Exception(e.getCause()), "listing mods");
                }
                catch (CancellationException ignore)
                {
                }
            }
        };
        modLoader.execute();
    }

    private record ModEntry(Path path, String name, Image icon) {}
}
