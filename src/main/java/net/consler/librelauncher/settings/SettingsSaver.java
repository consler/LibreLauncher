package net.consler.librelauncher.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dirs.BaseDirectories;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class SettingsSaver
{
    public static final Path configDir = Path.of(BaseDirectories.get().configDir, "LibreLauncher");
    public static final File settingsFile = new File(configDir.toFile(), "settings.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public JsonObject settings = new JsonObject();

    public SettingsSaver()
    {
        if (!configDir.toFile().exists())
        {
            configDir.toFile().mkdirs();
        }

        if (settingsFile.isFile())
        {
            try (FileReader reader = new FileReader(settingsFile))
            {
                JsonObject loaded = JsonParser.parseReader(reader).getAsJsonObject();
                if (loaded != null)
                {
                    settings = loaded;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load settings.json", e);
            }
        }
    }

    public String get(String key)
    {
        return settings.has(key) && !settings.get(key).isJsonNull() ? settings.get(key).getAsString() : null;
    }

    public String get(String key, String defaultValue)
    {
        String val = get(key);
        return val != null ? val : defaultValue;
    }

    public int getInt(String key)
    {
        return settings.has(key) ? settings.get(key).getAsInt() : 0;
    }

    public int getInt(String key, int defaultValue)
    {
        return settings.has(key) ? settings.get(key).getAsInt() : defaultValue;
    }

    public void set(String key, String value)
    {
        settings.addProperty(key, value);
    }

    public void set(String key, int value)
    {
        settings.addProperty(key, value);
    }

    public void save()
    {
        try (FileWriter writer = new FileWriter(settingsFile))
        {
            GSON.toJson(settings, writer);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save settings.json", e);
        }
    }
}