package net.consler.librelauncher.ui.settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.consler.librelauncher.ui.theme.ThemeManager;

import static net.consler.librelauncher.Main.APPDATA_DIR;

public class SettingsSaver
{
    private static final Path CONFIG_FILE = new File(APPDATA_DIR, "settings.txt").toPath();

    private static final Saver SAVER;

    static
    {
        if (!APPDATA_DIR.exists())
        {
            APPDATA_DIR.mkdirs();
        }

        try
        {
            if (!CONFIG_FILE.toFile().exists())
            {
                Files.createFile(CONFIG_FILE);
            }
        }
        catch (IOException ignored)
        {
        }

        SAVER = new Saver(CONFIG_FILE);

        String uiTheme = SAVER.get("ui_theme");
        if (uiTheme == null || uiTheme.isBlank())
        {
            SAVER.set("ui_theme", ThemeManager.DEFAULT_THEME);
        }

        String allocatedRam = SAVER.get("allocated_ram");
        if (allocatedRam == null || allocatedRam.isBlank())
        {
            SAVER.set("allocated_ram", Integer.toString(2048));
        }
    }

    public static void saveSetting(String key, String value)
    {
        SAVER.set(key, value);
    }

    public static void saveSetting(String key, int value)
    {
        saveSetting(key, Integer.toString(value));
    }

    public static String getSetting(String key)
    {
        return SAVER.get(key);
    }

    public static int getIntSetting(String key, int defaultValue)
    {
        String rawValue = getSetting(key);
        if (rawValue == null || rawValue.isBlank())
        {
            return defaultValue;
        }

        try
        {
            return (int) Math.round(Double.parseDouble(rawValue.trim()));
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
}
