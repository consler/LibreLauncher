package net.consler.librelauncher.launcher;

import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.modloader.ModloaderProfile;

import java.nio.file.Path;

public class Download
{
    public static void createInstance()
    {

    }

    public static void download(String version, String loaderId, String loaderVersion, Path javaPath, Path installationPath)
    {
        MinecraftInstaller installer = new MinecraftInstaller();

        installer.install(version, installationPath, new ModloaderProfile(loaderId, loaderVersion), javaPath);
    }
}
