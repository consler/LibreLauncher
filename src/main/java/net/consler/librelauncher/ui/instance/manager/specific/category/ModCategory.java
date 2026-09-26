package net.consler.librelauncher.ui.instance.manager.specific.category;

import javafx.scene.Node;
import net.consler.librelauncher.ui.instance.manager.specific.ManagerFormat;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.Mod;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ModCategory extends InstanceCategory
{
    private final Map<File, Optional<Mod>> cache = new ConcurrentHashMap<>();

    @Override
    public String title()
    {
        return "Mods";
    }

    @Override
    public File directory(InstanceProfile profile)
    {
        return profile.getModsFolder().toFile();
    }

    @Override
    public String primaryText(File file)
    {
        Mod mod = load(file);
        if (mod != null && mod.getName() != null && !mod.getName().isBlank()) return mod.getName();
        return stripExtension(file.getName());
    }

    @Override
    public Node icon(File file)
    {
        Mod mod = load(file);
        if (mod != null && mod.hasIcon())
        {
            return ManagerFormat.imageIcon(mod.getIcon(), true, defaultIcon());
        }
        return super.icon(file);
    }

    @Override
    public String description(File file)
    {
        Mod mod = load(file);
        if (mod == null)
        {
            return file.getName().toLowerCase().endsWith(".jar")
                    ? "Mod jar - unable to read metadata"
                    : "Disabled or unrecognized mod file";
        }

        String authors = (mod.getAuthors() == null || mod.getAuthors().isEmpty())
                ? null : String.join(", ", mod.getAuthors());

        return ManagerFormat.joinNonBlank(" • ",
                mod.getVersion() == null ? null : "v" + mod.getVersion(),
                mod.getLoaderType(),
                authors);
    }

    private Mod load(File file)
    {
        if (!file.getName().toLowerCase().endsWith(".jar")) return null;

        return cache.computeIfAbsent(file, f ->
        {
            try
            {
                return Optional.of(new Mod(f));
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
        return "\u2699";
    }
}