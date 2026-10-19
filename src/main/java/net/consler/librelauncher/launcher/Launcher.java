package net.consler.librelauncher.launcher;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.AuthManager;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.instancemanager.InstancePanel;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.launch.LaunchProfile;
import net.consler.librelauncherlib.launch.MinecraftLauncher;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.util.SystemHelper;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

public class Launcher
{
    public static void launch(String instanceId)
    {
        SettingsSaver settingsSaver = new SettingsSaver();

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
                .linkProcess(false)
                .build();
        ModloaderProfile modloaderProfile = new ModloaderProfile(
                instancesConfig.getProperty(instanceId, "modloader"),
                instancesConfig.getProperty(instanceId, "modloader_version"));
        AuthProfile authProfile = authManager.getActiveAccount().getAuthProfile();

        MinecraftLauncher launcher;
        try
        {
            launcher = new MinecraftLauncher(launchProfile, authProfile, modloaderProfile);
        }
        catch (IOException e)
        {
            new ErrorDialog(Main.client, e, "launching");
            throw new RuntimeException(e);
        }

        Process gameProcess;
        try
        {
            gameProcess = launcher.launch();
        }
        catch (IOException e)
        {
            new ErrorDialog(Main.client, e, "launching");
            throw new RuntimeException(e);
        }

        instancesConfig.saveProperty(instanceId, "pid", String.valueOf(gameProcess.pid()));
        instancesConfig.save();

        InstancePanel.populate();

         if(settingsSaver.get("close_after_launch").equals("true"))
         {
             System.exit(0);
         }

         gameProcess.onExit().thenAccept(action ->
         {
             instancesConfig.saveProperty(instanceId, "pid", null);
             instancesConfig.save();
             InstancePanel.populate();
         });
    }
}
