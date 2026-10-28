package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.instanceeditor.download.ModDownloadDialog;
import net.consler.librelauncher.ui.category.instanceeditor.download.ModpackDownloadDialog;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.Mod;
import net.consler.librelauncherlib.instance.ModReadException;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class InstanceMods extends InstanceItem
{
    private static InstanceMods instance;
    private SwingWorker<List<Mod>, ModEntry> modLoader;
    private volatile List<Mod> mods = List.of();

    public InstanceMods(Main main)
    {
        super(main, "Mods", "Mod");
        instance = this;
    }

    @Override
    public void populate(InstanceProfile instance)
    {
        mods = List.of();
        if (modLoader != null) modLoader.cancel(true);
        listPanel.removeAll();
        listPanel.revalidate();
        listPanel.repaint();

        modLoader = new SwingWorker<>()
        {
            @Override
            protected List<Mod> doInBackground() throws IOException
            {
                List<Mod> loadedMods = new ArrayList<>();
                if (!Files.isDirectory(instance.getModsFolder())) return loadedMods;

                try (Stream<Path> paths = Files.list(instance.getModsFolder()))
                {
                    paths.filter(Files::isRegularFile)
                            .filter(p ->(
                                    p.getFileName().toString().endsWith(".jar")) ||
                                    p.getFileName().toString().endsWith(".jar.disabled"))
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
                                    loadedMods.add(mod);
                                    publish(new ModEntry(modFile, mod.getName(), scaledImage, !modFile.getFileName().toString().endsWith(".jar.disabled")));
                                }
                                catch (ModReadException ignore)
                                {
                                }
                            });
                }
                return loadedMods;
            }

            @Override
            protected void process(List<ModEntry> entries)
            {
                if (modLoader != this || isCancelled()) return;

                for (ModEntry entry : entries)
                {
                    JPanel rowPanel;
                    if(entry.enabled)
                        rowPanel = createItemRow(entry.name() , entry.icon(), false);
                    else
                        rowPanel = createItemRow(entry.name() + " (disabled)" , entry.icon(), false);

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
                            new ErrorDialog(main.client, ex, "deleting a mod");
                        }
                    });

                    JMenuItem disable = entry.enabled ? new JMenuItem("Disable") : new JMenuItem("Enable");
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

                    addItemRow(rowPanel);
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
                    mods = List.copyOf(get());
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    showListingError(e, "listing mods");
                }
                catch (CancellationException ignore)
                {
                }
            }
        };
        modLoader.execute();
    }

    @Override
    public JPopupMenu createPopup(InstanceProfile instance)
    {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem addMods = new JMenuItem("Add Mods");
        addMods.addActionListener(ev -> new ModDownloadDialog(main, instance).open());
        popupMenu.add(addMods);

        JMenuItem addModpack = new JMenuItem("Add Modpack");
        addModpack.addActionListener(ev ->
                new ModpackDownloadDialog(main, instance).open());
        popupMenu.add(addModpack);

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

        return popupMenu;
    }

    public boolean doesExist(String title)
    {
        if (title == null) return false;
        return mods.stream().anyMatch(mod -> mod.getName() != null && mod.getName().equalsIgnoreCase(title));
    }

    private record ModEntry(Path path, String name, Image icon, boolean enabled) {}

    public static InstanceMods getInstance()
    {
        return instance;
    }
}
