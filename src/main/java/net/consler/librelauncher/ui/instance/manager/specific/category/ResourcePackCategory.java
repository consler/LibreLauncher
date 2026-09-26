package net.consler.librelauncher.ui.instance.manager.specific.category;

import javafx.scene.Node;
import net.consler.librelauncher.ui.instance.manager.specific.ManagerFormat;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.ResourcePack;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ResourcePackCategory extends InstanceCategory
{
    private final Map<File, Optional<ResourcePack>> cache = new ConcurrentHashMap<>();

    @Override
    public String title()
    {
        return "Resource Packs";
    }

    @Override
    public File directory(InstanceProfile profile)
    {
        return profile.getResourcePackFolder().toFile();
    }

    @Override
    public String primaryText(File file)
    {
        ResourcePack pack = load(file);
        if (pack != null && pack.getName() != null && !pack.getName().isBlank()) return pack.getName();
        return stripExtension(file.getName());
    }

    @Override
    public Node icon(File file)
    {
        ResourcePack pack = load(file);
        if (pack != null && pack.hasIcon())
        {
            return ManagerFormat.imageIcon(pack.getIcon(), true, defaultIcon());
        }
        return super.icon(file);
    }

    @Override
    public String description(File file)
    {
        ResourcePack pack = load(file);
        if (pack == null)
        {
            return file.isDirectory() ? "Resource pack folder" : "Resource pack - unable to read metadata";
        }

        String description = pack.getDescriptionAsString();
        if (description != null) description = description.replaceAll("\\s+", " ").trim();

        return ManagerFormat.joinNonBlank(" • ", description, "format " + pack.getPackFormat());
    }

    private ResourcePack load(File file)
    {
        if (!file.getName().toLowerCase().endsWith(".zip")) return null;

        return cache.computeIfAbsent(file, f ->
        {
            try
            {
                return Optional.of(new ResourcePack(f.toPath()));
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
        return "\u25a3";
    }
}