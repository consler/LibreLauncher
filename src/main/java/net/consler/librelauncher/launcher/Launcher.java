package net.consler.librelauncher.launcher;

import javafx.application.Platform;
import net.consler.librelauncher.exceptions.FailedToLaunchMinecraftException;
import net.consler.librelauncher.launcher.instance.InstanceInfo;
import dev.dirs.BaseDirectories;
import net.consler.librelauncher.ui.settings.AuthSaver;
import net.consler.librelauncher.ui.settings.SettingsSaver;
import net.consler.librelauncher.utils.ExceptionAlert;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.utill.SystemHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public class Launcher
{
    public static String instanceDir = new File(BaseDirectories.get().dataLocalDir, "LibreLauncher").getAbsolutePath();

    public static void launch(String name)
    {
        Map<String, String> instanceInfo = InstanceInfo.load(name);

        new Thread(() ->
        {
            try
            {
                String savedJavaPath = SettingsSaver.getSetting("java_path");
                Path javaPath = (savedJavaPath != null && !savedJavaPath.isBlank())
                        ? new File(savedJavaPath).toPath()
                        : SystemHelper.getJavaBin();

                LaunchProfile launchProfile = new LaunchProfile.Builder(instanceInfo.get("version"), new File(instanceDir, name).toPath())
                        .withLauncherName("LibreLauncher")
                        .withRamMb(SettingsSaver.getIntSetting("allocated_ram", 2048))
                        .withJavaPath(javaPath)
                        .build();

                ModloaderProfile modloaderProfile = new ModloaderProfile(instanceInfo.get("modLoader"), instanceInfo.get("loaderVersion"));
                AuthProfile authProfile = AuthSaver.getActiveAuthProfile();

                MinecraftLauncher launcher = new MinecraftLauncher();
                launcher.launch(launchProfile, authProfile, modloaderProfile);

                if (System.getProperty("close-on-launch") != null) System.exit(0);

            }
            catch (Exception e)
            {
                Platform.runLater(() -> ExceptionAlert.show(new FailedToLaunchMinecraftException(Arrays.toString(e.getStackTrace()))));
            }
        }).start();
    }
}