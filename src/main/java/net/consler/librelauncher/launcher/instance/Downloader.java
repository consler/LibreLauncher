package net.consler.librelauncher.launcher.instance;

import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.ui.settings.Saver;
import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.modloader.ModloaderProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static net.consler.librelauncher.Main.APPDATA_DIR;

public class Downloader
{
    public static void download(String name, String version, String modLoader, String loaderVersion)
    {
        Path gameDirPath = new File(Launcher.instanceDir, name).toPath();

        MinecraftInstaller installer = new MinecraftInstaller();

        try
        {
            File properties = new File(APPDATA_DIR, name + ".properties");
            properties.createNewFile();

            Saver saver = new Saver(properties.toPath());
            saver.set("version", version);
            saver.set("modLoader", modLoader);
            saver.set("loaderVersion", Objects.requireNonNullElse(loaderVersion, "null"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        System.out.println(modLoader + " " + loaderVersion);
        installer.install(version, gameDirPath, new ModloaderProfile(modLoader, loaderVersion));
    }
}
