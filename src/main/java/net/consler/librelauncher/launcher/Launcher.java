package net.consler.librelauncher.launcher;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.AuthManager;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.util.SystemHelper;

import javax.swing.*;
import java.nio.file.Path;

public class Launcher
{
    public static void launch(String instanceId)
    {
        SettingsSaver settingsSaver = new SettingsSaver();

        MinecraftLauncher launcher = new MinecraftLauncher();

        InstancesConfig instancesConfig = new InstancesConfig();
        AuthManager authManager = new AuthManager();

        if(authManager.getActiveAccount() == null)
        {
            JDialog noActiveAccount = new JDialog(Main.client, "No account", true);
            noActiveAccount.add(new JLabel("Please add an account before launching the game"));
            noActiveAccount.pack();
            noActiveAccount.setLocationRelativeTo(Main.client);
            noActiveAccount.setVisible(true);
            return;
        }
        if (instancesConfig.getProperty(instanceId, "version") == null)
        {
            JDialog noVersion = new JDialog(Main.client, "Corrupted instance", true);
            noVersion.add(new JLabel("The selected instance is corrupted. Please reinstall it"));
            noVersion.pack();
            noVersion.setLocationRelativeTo(Main.client);
            noVersion.setVisible(true);
            return;
        }

        Path javaPath = settingsSaver.get("java_path") == null ?
                SystemHelper.getJavaBin() :
                Path.of(settingsSaver.get("java_path"));

        LaunchProfile launchProfile = new LaunchProfile.Builder(
                instancesConfig.getProperty(instanceId, "version"),
                InstancesConfig.instancesDir.resolve(instanceId))
                .withJavaPath(javaPath)
                .withRamMb(settingsSaver.getInt("ram", 2048))
                .build();
        ModloaderProfile modloaderProfile = new ModloaderProfile(
                instancesConfig.getProperty(instanceId, "modloader"),
                instancesConfig.getProperty(instanceId, "modloader_version"));
        AuthProfile authProfile = authManager.getActiveAccount().getAuthProfile();

         Process p = launcher.launch(launchProfile, authProfile, modloaderProfile);

         if(settingsSaver.get("close_after_launch").equals("true"))
         {
             System.exit(0);
         }
    }
}
