package net.consler.librelauncher.ui.category.instanceeditor;

import dev.dirs.UserDirectories;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.Instance;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

public class InstanceJarMods extends InstanceItems
{
    private static InstanceJarMods instance;
    private String search = "";

    public InstanceJarMods(Main main)
    {
        super(main, "Jar Mods", "Jar Mod");
        instance = this;
    }

    @Override
    public void populate(Instance instance, String search)
    {
        this.search = search;
        listPanel.removeAll();

        Path jarModsFolder = instance.getJarModsFolder();
        if (Files.isDirectory(jarModsFolder))
        {
            try (Stream<Path> paths = Files.list(jarModsFolder))
            {
                paths.filter(Files::isRegularFile)
                        .filter(path -> isJarMod(path) && getJarModName(path).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(path -> addJarModRow(instance, path));
            }
            catch (IOException e)
            {
                new ErrorDialog(main.client, e, "listing JAR mods");
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void addJarModRow(Instance instance, Path path)
    {
        boolean enabled = !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar.disabled");
        JPanel rowPanel = createItemRow(getJarModName(path) + (enabled ? "" : " (disabled)"), null, false);

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(event ->
        {
            try
            {
                instance.removeJarMod(path);
                populate(instance, search);
            }
            catch (IOException e)
            {
                new ErrorDialog(main.client, e, "deleting a JAR mod");
            }
        });
        popupMenu.add(delete);

        JMenuItem toggle = new JMenuItem(enabled ? "Disable" : "Enable");
        toggle.addActionListener(event ->
        {
            String fileName = path.getFileName().toString();
            Path destination = enabled
                    ? path.resolveSibling(fileName + ".disabled")
                    : path.resolveSibling(fileName.substring(0, fileName.length() - ".disabled".length()));
            try
            {
                Files.move(path, destination);
                populate(instance, search);
            }
            catch (IOException e)
            {
                new ErrorDialog(main.client, e, (enabled ? "disabling" : "enabling") + " a JAR mod");
            }
        });
        popupMenu.add(toggle);

        rowPanel.setComponentPopupMenu(popupMenu);
        addItemRow(rowPanel);
    }

    @Override
    public JPopupMenu createPopup(Instance instance)
    {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem addJarMod = new JMenuItem("Add JAR Mod");
        addJarMod.addActionListener(event -> importJarMod(main, instance));
        popupMenu.add(addJarMod);

        JMenuItem openFolder = new JMenuItem("Open Folder");
        openFolder.addActionListener(event ->
        {
            try
            {
                SystemUtils.openDirectory(instance.getJarModsFolder().toFile());
            }
            catch (IOException e)
            {
                new ErrorDialog(main.client, e, "opening JAR mods folder");
            }
        });
        popupMenu.add(openFolder);
        return popupMenu;
    }

    static void importJarMod(Main main, Instance instance)
    {
        File selectedFile = SystemUtils.importFile(new File(UserDirectories.get().downloadDir), ".jar");
        if (selectedFile == null) return;

        try
        {
            instance.addJarMod(selectedFile.toPath());
            if (InstanceJarMods.instance != null)
                InstanceJarMods.instance.populate(instance, InstanceJarMods.instance.search);
        }
        catch (IOException | IllegalArgumentException e)
        {
            new ErrorDialog(main.client, e, "adding a JAR mod");
        }
    }

    private static boolean isJarMod(Path path)
    {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".jar") || fileName.endsWith(".jar.disabled");
    }

    private static String getJarModName(Path path)
    {
        String fileName = path.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".jar.disabled"))
            return fileName.substring(0, fileName.length() - ".jar.disabled".length());
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".jar"))
            return fileName.substring(0, fileName.length() - ".jar".length());
        return fileName;
    }
}
