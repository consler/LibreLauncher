package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.InstanceProfile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InstanceResourcePacks
{
    private final Main main;
    private JPanel listPanel;
    private SwingWorker<Void, ResourcePackEntry> resourcePackLoader;
    private final Map<Path, JLabel> iconLabels = new HashMap<>();

    public JPanel panel;

    public InstanceResourcePacks(Main main)
    {
        this.main = main;
    }

    public JPanel createPanel(InstanceProfile instance)
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Settings.createHeader("Resource Packs"));
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
                JMenuItem addResourcePacks = new JMenuItem("Add Resource Packs");
                addResourcePacks.addActionListener(ev ->
                        new ResourcePackDownloadDialog(main, instance, InstanceResourcePacks.this).open());
                popupMenu.add(addResourcePacks);

                JMenuItem openFolder = new JMenuItem("Open Folder");
                openFolder.addActionListener(ev ->
                {
                    try
                    {
                        SystemUtils.openDirectory(instance.getResourcePackFolder().toFile());
                    }
                    catch (IOException ex)
                    {
                        new ErrorDialog(main.client, ex, "opening resource packs folder");
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
        if (resourcePackLoader != null) resourcePackLoader.cancel(true);
        listPanel.removeAll();
        iconLabels.clear();
        listPanel.revalidate();
        listPanel.repaint();

        resourcePackLoader = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground() throws IOException
            {
                Path resourcePackFolder = instance.getResourcePackFolder();
                if (!Files.isDirectory(resourcePackFolder)) return null;

                List<Path> packFiles;
                try (Stream<Path> paths = Files.list(resourcePackFolder))
                {
                    packFiles = paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                            .sorted(Comparator.comparing(
                                    path -> path.getFileName().toString(),
                                    String.CASE_INSENSITIVE_ORDER))
                            .toList();
                }

                for (Path packFile : packFiles)
                {
                    if (isCancelled()) return null;
                    publish(new ResourcePackEntry(packFile, getPackName(packFile), null, false));
                }

                for (Path packFile : packFiles)
                {
                    if (isCancelled()) return null;
                    try
                    {
                        BufferedImage icon = readPackIcon(packFile);
                        Image scaledIcon = icon == null ? null
                                : icon.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                        publish(new ResourcePackEntry(packFile, getPackName(packFile), scaledIcon, true));
                    }
                    catch (IOException ignored)
                    {
                        publish(new ResourcePackEntry(packFile, getPackName(packFile), null, true));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<ResourcePackEntry> entries)
            {
                if (resourcePackLoader != this || isCancelled()) return;
                for (ResourcePackEntry entry : entries)
                {
                    if (entry.iconLoaded())
                    {
                        JLabel iconLabel = iconLabels.get(entry.path());
                        if (iconLabel != null && entry.icon() != null)
                            iconLabel.setIcon(new ImageIcon(entry.icon()));
                    }
                    else
                    {
                        addResourcePackRow(instance, entry);
                    }
                }
                listPanel.revalidate();
                listPanel.repaint();
            }

            @Override
            protected void done()
            {
                if (resourcePackLoader != this || isCancelled()) return;
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
                    new ErrorDialog(main.client, new Exception(e.getCause()), "listing resource packs");
                }
                catch (CancellationException ignored)
                {
                }
            }
        };
        resourcePackLoader.execute();
    }

    private void addResourcePackRow(InstanceProfile instance, ResourcePackEntry entry)
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
                    try
                    {
                        Files.delete(entry.path());
                        populate(instance);
                    }
                    catch (IOException ex)
                    {
                        new ErrorDialog(main.client, ex, "deleting resource pack");
                    }
                });
                popupMenu.add(delete);
                popupMenu.show(rowPanel, e.getX(), e.getY());
            }
        });

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(50, 50));
        iconLabels.put(entry.path(), iconLabel);
        rowPanel.add(iconLabel);
        rowPanel.add(new JLabel(entry.name()));
        listPanel.add(rowPanel);
        listPanel.add(Box.createVerticalStrut(5));
    }

    private static BufferedImage readPackIcon(Path packFile) throws IOException
    {
        try (ZipFile zipFile = new ZipFile(packFile.toFile()))
        {
            ZipEntry iconEntry = zipFile.getEntry("pack.png");
            if (iconEntry == null) return null;
            try (var input = zipFile.getInputStream(iconEntry))
            {
                return ImageIO.read(input);
            }
        }
    }

    private static String getPackName(Path packFile)
    {
        String filename = packFile.getFileName().toString();
        return filename.replaceFirst("[.][^.]+$", "");
    }

    private record ResourcePackEntry(Path path, String name, Image icon, boolean iconLoaded) {}
}
