package net.consler.librelauncher.config.settings;

import dev.dirs.BaseDirectories;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class Settings
{
    public static final Path configDir = Path.of(BaseDirectories.get().configDir, "LibreLauncher");
    public static final File settingsFile = new File(configDir.toFile(), "settings.properties");

    public final Properties properties = new Properties();

    public Settings()
    {
        if (!configDir.toFile().exists()) configDir.toFile().mkdirs();

        if (!settingsFile.exists())
        {
            try
            {
                settingsFile.createNewFile();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to create settings.properties", e);
            }
        }

        if (settingsFile.isFile())
        {
            try (FileInputStream fis = new FileInputStream(settingsFile))
            {
                properties.load(fis);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load settings.properties", e);
            }
        }
    }

    public String get(String key)
    {
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue)
    {
        return properties.getProperty(key, defaultValue);
    }

    public void set(String key, String value)
    {
        properties.setProperty(key, value);
    }

    public void save()
    {
        try (FileOutputStream fos = new FileOutputStream(settingsFile))
        {
            properties.store(fos, "LibreLauncher Global Settings");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save settings.properties", e);
        }
    }
}