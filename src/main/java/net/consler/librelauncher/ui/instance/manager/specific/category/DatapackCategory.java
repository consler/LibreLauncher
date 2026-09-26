package net.consler.librelauncher.ui.instance.manager.specific.category;

import javafx.scene.Node;
import net.consler.librelauncher.ui.instance.manager.specific.ManagerFormat;
import net.consler.librelauncherlib.instance.Datapack;
import net.consler.librelauncherlib.instance.InstanceProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DatapackCategory extends InstanceCategory
{
    private final Map<File, Optional<Datapack>> cache = new ConcurrentHashMap<>();

    @Override
    public String title()
    {
        return "Datapacks";
    }

    @Override
    public File directory(InstanceProfile profile)
    {
        return profile.getSavesFolder().toFile();
    }

    @Override
    public String locationLabel(InstanceProfile profile)
    {
        return new File(profile.getSavesFolder().toFile(), "*" + File.separator + "datapacks").getAbsolutePath();
    }

    @Override
    public File[] items(InstanceProfile profile)
    {
        File[] worlds = directory(profile).listFiles(File::isDirectory);
        if (worlds == null) return new File[0];

        Arrays.sort(worlds, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        List<File> packs = new ArrayList<>();
        for (File world : worlds)
        {
            File[] found = new File(world, "datapacks").listFiles();
            if (found == null) continue;

            Arrays.sort(found, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            packs.addAll(Arrays.asList(found));
        }
        return packs.toArray(new File[0]);
    }

    @Override
    public String primaryText(File file)
    {
        Datapack pack = load(file);
        if (pack != null && pack.getName() != null && !pack.getName().isBlank()) return pack.getName();
        return stripExtension(file.getName());
    }

    @Override
    public Node icon(File file)
    {
        Datapack pack = load(file);
        if (pack != null && pack.hasIcon())
        {
            return ManagerFormat.imageIcon(pack.getIcon(), true, defaultIcon());
        }
        return super.icon(file);
    }

    @Override
    public String description(File file)
    {
        String worldLabel = "World: " + worldNameOf(file);

        Datapack pack = load(file);
        if (pack == null)
        {
            return ManagerFormat.joinNonBlank(" • ", worldLabel,
                    file.isDirectory() ? "Datapack folder" : "Datapack - unable to read metadata");
        }

        String description = pack.getDescriptionAsString();
        if (description != null) description = description.replaceAll("\\s+", " ").trim();

        return ManagerFormat.joinNonBlank(" • ", worldLabel, description, "format " + pack.getPackFormat());
    }

    private String worldNameOf(File file)
    {
        File datapacksFolder = file.getParentFile();
        File world = datapacksFolder == null ? null : datapacksFolder.getParentFile();
        return world == null ? "Unknown" : world.getName();
    }

    private Datapack load(File file)
    {
        if (!file.getName().toLowerCase().endsWith(".zip")) return null;

        return cache.computeIfAbsent(file, f ->
        {
            try
            {
                return Optional.of(new Datapack(f.toPath()));
            }
            catch (Exception e)
            {
                return Optional.empty();
            }
        }).orElse(null);
    }

    @Override
    protected String defaultIcon()
    {
        return "\u25c6";
    }
}