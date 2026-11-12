package net.consler.librelauncher.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.dirs.BaseDirectories;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public class InstancesConfig
{
    public static final File instancesConfigFile = new File(SettingsSaver.configDir.toFile(), "instances.json");
    public static final Path instancesDir = Path.of(BaseDirectories.get().dataLocalDir).resolve("LibreLauncher");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Map<String, String>>>()
    {
    }.getType();
    private static final Object FILE_LOCK = new Object();
    public final Map<String, Map<String, String>> instanceCache = new HashMap<>();
    private final Map<String, Map<String, String>> pendingProperties = new HashMap<>();
    private final Set<String> removedInstances = new HashSet<>();

    public InstancesConfig()
    {
        File parentDir = instancesConfigFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        synchronized (FILE_LOCK)
        {
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
    }

    public synchronized void saveProperty(String instanceId, String key, String value)
    {
        if (value == null)
        {
            Map<String, String> properties = instanceCache.get(instanceId);
            if (properties != null) properties.remove(key);
        }
        else
        {
            instanceCache.computeIfAbsent(instanceId, k -> new HashMap<>()).put(key, value);
        }
        pendingProperties.computeIfAbsent(instanceId, k -> new HashMap<>()).put(key, value);
        removedInstances.remove(instanceId);
    }

    public synchronized String getProperty(String instanceId, String key)
    {
        return getProperty(instanceId, key, null);
    }

    public synchronized String getProperty(String instanceId, String key, String defaultValue)
    {
        Map<String, String> instanceProps = instanceCache.get(instanceId);

        if (instanceProps == null) return defaultValue;

        String value = instanceProps.get(key);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    public synchronized void save()
    {
        synchronized (FILE_LOCK)
        {
            Map<String, Map<String, String>> mergedCache = new HashMap<>();
            if (instancesConfigFile.isFile())
            {
                try (FileReader reader = new FileReader(instancesConfigFile))
                {
                    Map<String, Map<String, String>> loadedCache = GSON.fromJson(reader, MAP_TYPE);
                    if (loadedCache != null)
                    {
                        loadedCache.forEach((instanceId, properties) ->
                                mergedCache.put(instanceId, new HashMap<>(properties)));
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Failed to load instances.json", e);
                }
            }

            removedInstances.forEach(mergedCache::remove);
            pendingProperties.forEach((instanceId, properties) ->
            {
                Map<String, String> mergedProperties = mergedCache.computeIfAbsent(instanceId, k -> new HashMap<>());
                properties.forEach((key, value) ->
                {
                    if (value == null) mergedProperties.remove(key);
                    else mergedProperties.put(key, value);
                });
            });

            Path target = instancesConfigFile.toPath();
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp))
            {
                GSON.toJson(mergedCache, MAP_TYPE, writer);
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

            instanceCache.clear();
            mergedCache.forEach((instanceId, properties) ->
                    instanceCache.put(instanceId, new HashMap<>(properties)));
            pendingProperties.clear();
            removedInstances.clear();
        }
    }

    public synchronized ArrayList<String> getInstanceList()
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
                            }
                            catch (IOException e)
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

        synchronized (this)
        {
            instanceCache.remove(instanceName);
            pendingProperties.remove(instanceName);
            removedInstances.add(instanceName);
        }
        save();
    }
}