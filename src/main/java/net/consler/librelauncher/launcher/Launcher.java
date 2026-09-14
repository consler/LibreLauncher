package net.consler.librelauncher.launcher;

import net.consler.librelauncher.launcher.instance.InstanceInfo;
import dev.dirs.BaseDirectories;

import java.io.File;
import java.util.Map;

public class Launcher
{
    public static String instanceDir = new File(BaseDirectories.get().dataLocalDir, "LibreLauncher").getAbsolutePath();

    public static void launch(String name)
    {
        Map<String, String> instanceInfo = InstanceInfo.load(name);

        LaunchGame.launch(name, instanceInfo.get("version"), instanceInfo.get("modLoader"), instanceInfo.get("loaderVersion"));
    }

    public static void launch(String version, String username)
    {

    }
}