package net.consler.librelauncher.ui.instance.manager.specific.category;

import javafx.scene.Node;
import net.consler.librelauncher.ui.instance.manager.specific.ManagerFormat;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.World;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WorldCategory extends InstanceCategory
{
    private final Map<File, Optional<World>> cache = new ConcurrentHashMap<>();

    @Override
    public String title()
    {
        return "Worlds";
    }

    @Override
    public File directory(InstanceProfile profile)
    {
        return profile.getSavesFolder().toFile();
    }

    @Override
    public String primaryText(File file)
    {
        World world = load(file);
        if (world != null)
        {
            String levelName = world.getLevelName();
            if (levelName != null && !levelName.isBlank()) return levelName;
        }
        return stripExtension(file.getName());
    }

    @Override
    public Node icon(File file)
    {
        World world = load(file);
        if (world != null && world.hasIcon())
        {
            return ManagerFormat.imageIcon(world.getIcon(), true, defaultIcon());
        }
        return super.icon(file);
    }

    @Override
    public String description(File file)
    {
        World world = load(file);
        if (world == null) return "World folder - unable to read level data";

        String gameMode = switch (world.getGameType())
        {
            case 1 -> "Creative";
            case 2 -> "Adventure";
            case 3 -> "Spectator";
            default -> "Survival";
        };
        String difficulty = world.isHardcore() ? "Hardcore" : capitalize(world.getDifficultyString());
        String modded = world.wasModded() ? " • Modded" : "";

        return ManagerFormat.joinNonBlank(" • ", world.getVersionName(), gameMode, difficulty)
                + modded + " • Last played " + ManagerFormat.date(world.getLastPlayed());
    }

    private World load(File file)
    {
        return cache.computeIfAbsent(file, f ->
        {
            try
            {
                return Optional.of(new World(f.toPath()));
            }
            catch (Exception e)
            {
                return Optional.empty();
            }
        }).orElse(null);
    }

    private static String capitalize(String value)
    {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    @Override
    protected String defaultIcon()
    {
        return "\u25c9";
    }
}