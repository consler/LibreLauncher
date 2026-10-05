package net.consler.librelauncher.launcher;

import net.consler.librelauncher.config.settings.InstancesConfig;
import net.consler.librelauncher.config.settings.SettingsSaver;
import net.consler.librelauncher.ui.category.instancemanager.CreateInstanceDialog;
import net.consler.librelauncherlib.install.InstallerListener;
import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.util.SystemHelper;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Objects;

public class Download
{
    public static void createInstance()
    {
        MinecraftInstaller installer = new MinecraftInstaller();

        new Thread(()->
        {
            installer.setListener(new InstallerListener()
            {
                @Override
                public void onStart()
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        if (CreateInstanceDialog.progressBar != null)  CreateInstanceDialog.progressBar.setVisible(true);
                        if (CreateInstanceDialog.dialog != null) CreateInstanceDialog.dialog.enableInputMethods(false);
                    });
                }
                @Override
                public void onFinish()
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        CreateInstanceDialog.close();
                    });
                }
                @Override
                public void onNewPercentage(int percentage)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        if(CreateInstanceDialog.progressBar != null)
                        {
                            CreateInstanceDialog.progressBar.setVisible(true);
                            CreateInstanceDialog.progressBar.setValue(percentage);
                        }
                    });
                }
            });

            installer.install(Objects.requireNonNull(CreateInstanceDialog.versionChoice.getSelectedItem()).toString(),
                    InstancesConfig.instancesDir.resolve(CreateInstanceDialog.nameField.getText()),
                    new ModloaderProfile(CreateInstanceDialog.getActiveLoader(), Objects.requireNonNullElse(CreateInstanceDialog.modLoaderVersionChoice.getSelectedItem(), "").toString()),
                    Path.of(new SettingsSaver().get("java_path", SystemHelper.getJavaBin().toString())));

        }).start();

    }
}
