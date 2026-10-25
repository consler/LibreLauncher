package net.consler.librelauncher.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.dirs.BaseDirectories;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class InstancesConfig
{
    public static final File instancesConfigFile = new File(SettingsSaver.configDir.toFile(), "instances.json");
    public static final Path instancesDir = Path.of(BaseDirectories.get().dataLocalDir).resolve("LibreLauncher");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
    public final Map<String, Map<String, String>> instanceCache = new HashMap<>();

    public InstancesConfig()
    {
        File parentDir = instancesConfigFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        if (!instancesConfigFile.exists())
        {
            try
            {
                instancesConfigFile.createNewFile();
                save();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to create instances.json", e);
            }
        }

        if (instancesConfigFile.isFile())
        {
            try (FileReader reader = new FileReader(instancesConfigFile))
            {
                Map<String, Map<String, String>> loadedCache = GSON.fromJson(reader, MAP_TYPE);
                if (loadedCache != null)
                {
                    instanceCache.putAll(loadedCache);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load instances.json", e);
            }
        }
    }

    public void saveProperty(String instanceId, String key, String value)
    {
        instanceCache.computeIfAbsent(instanceId, k -> new HashMap<>()).put(key, value);
    }

    public String getProperty(String instanceId, String key)
    {
        return getProperty(instanceId, key, null);
    }

    public String getProperty(String instanceId, String key, String defaultValue)
    {
        Map<String, String> instanceProps = instanceCache.get(instanceId);

        if (instanceProps == null) return defaultValue;

        String value = instanceProps.get(key);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    public void save()
    {
        Path target = instancesConfigFile.toPath();
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp))
        {
            GSON.toJson(instanceCache, MAP_TYPE, writer);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save instances.json", e);
        }
        try
        {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save instances.json", e);
        }
    }

    public ArrayList<String> getInstanceList()
    {
        return new ArrayList<>(instanceCache.keySet());
    }

    public void deleteInstance(String instanceName)
    {
        Path instancePath = instancesDir.resolve(instanceName);

        if (Files.exists(instancePath))
        {
            try (Stream<Path> walk = Files.walk(instancePath))
            {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path ->
                        {
                            try
                            {
                                Files.delete(path);
                            } catch (IOException e)
                            {
                                new ErrorDialog(Main.getInstance().client, e, "deleting instance");
                                throw new RuntimeException(e);
                            }
                        });
            }
            catch (IOException e)
            {
                new ErrorDialog(Main.getInstance().client, e, "deleting instance");
                throw new RuntimeException(e);
            }
        }

        instanceCache.remove(instanceName);
        save();
    }
}