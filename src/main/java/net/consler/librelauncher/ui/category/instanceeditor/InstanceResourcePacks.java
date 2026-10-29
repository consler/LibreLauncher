package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.instanceeditor.download.ResourcePackDownloadDialog;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.instance.ResourcePack;
import net.consler.librelauncherlib.instance.ResourcePackReadException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InstanceResourcePacks extends InstanceItem
{
    private SwingWorker<List<ResourcePack>, ResourcePackEntry> resourcePackLoader;
    private final Map<Path, JLabel> iconLabels = new HashMap<>();
    private volatile List<ResourcePack> resourcePacks = List.of();

    private static InstanceResourcePacks instance;

    public InstanceResourcePacks(Main main)
    {
        super(main, "Resource Packs", "Resource Pack");
        instance = this;
    }

    @Override
    public void populate(Instance instance)
    {
        resourcePacks = List.of();
        if (resourcePackLoader != null) resourcePackLoader.cancel(true);
        listPanel.removeAll();
        iconLabels.clear();
        listPanel.revalidate();
        listPanel.repaint();

        resourcePackLoader = new SwingWorker<>()
        {
            @Override
            protected List<ResourcePack> doInBackground() throws IOException
            {
                Path resourcePackFolder = instance.getResourcePackFolder();
                if (!Files.isDirectory(resourcePackFolder)) return List.of();

                List<Path> packFiles;
                try (Stream<Path> paths = Files.list(resourcePackFolder))
                {
                    packFiles = paths.filter(Files::isRegularFile)
                            .filter(path -> (
                                    path.getFileName().toString().toLowerCase().endsWith(".zip") ||
                                    path.getFileName().toString().toLowerCase().endsWith(".zip.disabled")))
                            .toList();
                }

                List<ResourcePack> loadedPacks = new ArrayList<>();
                for (Path packFile : packFiles)
                {
                    if (isCancelled()) return null;
                    try
                    {
                        loadedPacks.add(new ResourcePack(packFile));
                    }
                    catch (ResourcePackReadException ignored)
                    {
                    }
                    publish(new ResourcePackEntry(packFile, getPackName(packFile), null, false, !packFile.getFileName().toString().toLowerCase().endsWith(".zip.disabled")));
                }

                for (Path packFile : packFiles)
                {
                    if (isCancelled()) return null;
                    try
                    {
                        BufferedImage icon = readPackIcon(packFile);
                        Image scaledIcon = icon == null ?
                                null
                                : icon.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                        publish(new ResourcePackEntry(packFile, getPackName(packFile), scaledIcon, true, !packFile.getFileName().toString().toLowerCase().endsWith(".zip.disabled")));
                    }
                    catch (IOException ignored)
                    {
                        publish(new ResourcePackEntry(packFile, getPackName(packFile), null, true, !packFile.getFileName().toString().toLowerCase().endsWith(".zip.disabled")));
                    }
                }
                return loadedPacks;
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
                    resourcePacks = List.copyOf(get());
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    showListingError(e, "listing resource packs");
                }
                catch (CancellationException ignored)
                {
                }
            }
        };
        resourcePackLoader.execute();
    }

    private void addResourcePackRow(Instance instance, ResourcePackEntry entry)
    {
        JPanel rowPanel;
        if(entry.enabled())
            rowPanel = createItemRow(entry.name(), null, true);
        else
            rowPanel = createItemRow(entry.name() + " (disabled)", null, true);

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
                new ErrorDialog(main.client, ex, "deleting a resource pack");
            }
        });

        JMenuItem disable = entry.enabled() ? new JMenuItem("Disable") : new JMenuItem("Enable");
        disable.addActionListener(ev ->
        {
            if (entry.enabled())
                entry.path().toFile().renameTo(new File(entry.path().toFile().getAbsolutePath() + ".disabled"));
            else
                entry.path().toFile().renameTo(new File(entry.path().toFile().getAbsolutePath().replace(".disabled", "")));
            populate(instance);
        });

        popupMenu.add(delete);
        popupMenu.add(disable);

        rowPanel.setComponentPopupMenu(popupMenu);

        JLabel iconLabel = (JLabel) rowPanel.getComponent(0);
        iconLabels.put(entry.path(), iconLabel);
        addItemRow(rowPanel);
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

    @Override
    public JPopupMenu createPopup(Instance instance)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem addResourcePacks = new JMenuItem("Add Resource Packs");
        addResourcePacks.addActionListener(ev ->
                new ResourcePackDownloadDialog(main, instance).open());
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

        return popupMenu;
    }

    private static String getPackName(Path packFile)
    {
        String filename = packFile.getFileName().toString();
        return filename.replaceFirst("[.][^.]+$", "");
    }

    public boolean doesExist(String packName)
    {
        if (packName == null) return false;
        return resourcePacks.stream().anyMatch(pack -> pack.getName().equalsIgnoreCase(packName));
    }

    private record ResourcePackEntry(Path path, String name, Image icon, boolean iconLoaded, boolean enabled) {}

    public static InstanceResourcePacks getInstance()
    {
        return instance;
    }
}
