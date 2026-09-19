package net.consler.librelauncher.launcher.instance;

import net.consler.librelauncher.ui.settings.Saver;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.consler.librelauncher.Main.APPDATA_DIR;

public class InstanceInfo
{
    public static void save(String name, String version, String modLoader, String loaderVersion)
    {
        Path configFile = new File(APPDATA_DIR, name + ".properties").toPath();
        Saver saver = new Saver(configFile);

        saver.set("version", version);
        saver.set("modLoader", modLoader);
        saver.set("loaderVersion", Objects.requireNonNullElse(loaderVersion, "null"));
        saver.save();
    }

    public static Map<String, String> load(String name)
    {
        Path configFile = new File(APPDATA_DIR, name + ".properties").toPath();
        Saver saver = new Saver(configFile);

        HashMap<String, String> instanceInfo = new HashMap<>();
        instanceInfo.put("version", saver.get("version"));
        instanceInfo.put("modLoader", saver.get("modLoader"));
        instanceInfo.put("loaderVersion", saver.get("loaderVersion"));

        return instanceInfo;
    }
}
