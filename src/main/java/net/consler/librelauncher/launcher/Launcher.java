package net.consler.librelauncher.launcher;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.AuthManager;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.instancemanager.InstancePanel;
import net.consler.librelauncher.ui.category.settings.Account;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.exception.TokenRefreshException;
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
        SwingUtilities.invokeLater(() ->
        {
            SettingsSaver settingsSaver = new SettingsSaver();
            InstancesConfig instancesConfig = new InstancesConfig();
            AuthManager authManager = new AuthManager();
            Account activeAccount = authManager.getActiveAccount();

            if (activeAccount == null)
            {
                JOptionPane.showMessageDialog(Main.client,
                        "Please add an account before launching the game",
                        "No account",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (instancesConfig.getProperty(instanceId, "version") == null)
            {
                JOptionPane.showMessageDialog(Main.client,
                        "The selected instance is corrupted. Please reinstall it",
                        "Corrupted instance",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            new Thread(() -> launchInBackground(instanceId, settingsSaver, instancesConfig, activeAccount), "Game-Launcher-Thread").start();
        });
    }

    private static void launchInBackground(String instanceId, SettingsSaver settingsSaver, InstancesConfig instancesConfig, Account activeAccount)
    {
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

        AuthProfile authProfile = activeAccount.getAuthProfile();

        MinecraftLauncher launcher = start(launchProfile, authProfile, modloaderProfile, 3);

        Process gameProcess;
        try
        {
            gameProcess = launcher.launch();
        }
        catch (IOException e)
        {
            SwingUtilities.invokeLater(() -> new ErrorDialog(Main.client, e, "launching"));
            throw new RuntimeException(e);
        }

        instancesConfig.saveProperty(instanceId, "pid", String.valueOf(gameProcess.pid()));
        instancesConfig.save();

        SwingUtilities.invokeLater(InstancePanel::populate);

        if ("true".equals(settingsSaver.get("close_after_launch")))
        {
            System.exit(0);
        }

        gameProcess.onExit().thenAccept(action ->
        {
            instancesConfig.saveProperty(instanceId, "pid", null);
            instancesConfig.save();

            SwingUtilities.invokeLater(InstancePanel::populate);
        });
    }

    private static MinecraftLauncher start(LaunchProfile launchProfile, AuthProfile authProfile, ModloaderProfile modloaderProfile, int retriesLeft)
    {
        try
        {
            return new MinecraftLauncher(launchProfile, authProfile, modloaderProfile);
        }
        catch (TokenRefreshException e)
        {
            if (retriesLeft <= 0)
            {
                SwingUtilities.invokeLater(() -> new ErrorDialog(Main.client, e, "refreshing auth token"));
                throw new RuntimeException("Failed to refresh authentication token.", e);
            }
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
            return start(launchProfile, authProfile, modloaderProfile, retriesLeft - 1);
        }
        catch (IOException e)
        {
            SwingUtilities.invokeLater(() -> new ErrorDialog(Main.client, e, "launching"));
            throw new RuntimeException(e);
        }
    }
}