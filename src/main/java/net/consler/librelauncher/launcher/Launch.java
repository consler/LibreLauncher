package net.consler.librelauncher.launcher;

import net.consler.librelauncher.settings.AuthManager;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.modloader.ModloaderProfile;

public class Launch
{
    public static void launch(String instanceId)
    {
        MinecraftLauncher launcher = new MinecraftLauncher();

        InstancesConfig instancesConfig = new InstancesConfig();
        AuthManager authManager = new AuthManager();

        LaunchProfile launchProfile = new LaunchProfile.Builder(
                instancesConfig.getProperty(instanceId, "version"),
                InstancesConfig.instancesDir.resolve(instanceId))
                .withRamMb(new SettingsSaver().getInt("ram", 2048))
                .build();
        ModloaderProfile modloaderProfile = new ModloaderProfile(
                instancesConfig.getProperty(instanceId, "modloader"),
                instancesConfig.getProperty(instanceId, "modloader_version"));
        AuthProfile authProfile = authManager.getActiveAccount().getAuthProfile();

         Process p = launcher.launch(launchProfile, authProfile, modloaderProfile);
    }
}
