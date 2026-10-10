package net.consler.librelauncher.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.dirs.BaseDirectories;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, String> instanceProps = instanceCache.get(instanceId);
        return instanceProps != null ? instanceProps.get(key) : null;
    }

    public void save()
    {
        try (FileWriter writer = new FileWriter(instancesConfigFile))
        {
            GSON.toJson(instanceCache, MAP_TYPE, writer);
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
        instancesDir.resolve(instanceName).toFile().delete();
        instanceCache.remove(instanceName);
        save();
    }
}