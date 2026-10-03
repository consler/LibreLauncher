package net.consler.librelauncher.config.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class InstancesConfig
{
    public static final File instancesConfigFile = new File(SettingsSaver.configDir.toFile(), "instances.properties");

    public final Map<String, Map<String, String>> instanceCache = new HashMap<>();
    public final Properties properties = new Properties();

    public InstancesConfig()
    {
        File parentDir = instancesConfigFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        if (!instancesConfigFile.exists())
        {
            try
            {
                instancesConfigFile.createNewFile();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to create instances.properties", e);
            }
        }

        if (instancesConfigFile.isFile())
        {
            try (FileInputStream fis = new FileInputStream(instancesConfigFile))
            {
                properties.load(fis);
                buildCache();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load instances.properties", e);
            }
        }
    }

    private void buildCache()
    {
        instanceCache.clear();
        for (String key : properties.stringPropertyNames())
        {
            int dotIndex = key.indexOf('.');
            if (dotIndex != -1)
            {
                String instanceId = key.substring(0, dotIndex);
                String propKey = key.substring(dotIndex + 1);
                String value = properties.getProperty(key);

                instanceCache.computeIfAbsent(instanceId, k -> new HashMap<>()).put(propKey, value);
            }
        }
    }

    public void saveProperty(String instanceId, String key, String value)
    {
        properties.setProperty(instanceId + "." + key, value);
        instanceCache.computeIfAbsent(instanceId, k -> new HashMap<>()).put(key, value);
    }

    public String getProperty(String instanceId, String key)
    {
        return properties.getProperty(instanceId + "." + key);
    }

    public void save()
    {
        try (FileOutputStream fos = new FileOutputStream(instancesConfigFile))
        {
            properties.store(fos, "LibreLauncher Instances Configuration");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save instances.properties", e);
        }
    }
}