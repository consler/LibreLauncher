package net.consler.librelauncher.launcher;

import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.category.instancemanager.CreateInstanceDialog;
import net.consler.librelauncher.ui.category.instancemanager.InstancePanel;
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

                        InstancesConfig instancesConfig = new InstancesConfig();
                        String name = CreateInstanceDialog.nameField.getText();

                        instancesConfig.saveProperty(name, "version", Objects.requireNonNull(CreateInstanceDialog.versionChoice.getSelectedItem()).toString());
                        instancesConfig.saveProperty(name, "modloader", CreateInstanceDialog.getActiveLoader());
                        instancesConfig.saveProperty(name, "modloader_version", Objects.requireNonNullElse(CreateInstanceDialog.modLoaderVersionChoice.getSelectedItem(), "0.0.0").toString());

                        instancesConfig.save();

                        InstancePanel.populate();
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

            installer.install(
                    Objects.requireNonNull(CreateInstanceDialog.versionChoice.getSelectedItem()).toString(),
                    InstancesConfig.instancesDir.resolve(CreateInstanceDialog.nameField.getText()),
                    new ModloaderProfile(CreateInstanceDialog.getActiveLoader(), Objects.requireNonNullElse(CreateInstanceDialog.modLoaderVersionChoice.getSelectedItem(), "").toString()),
                    Path.of(new SettingsSaver().get("java_path", SystemHelper.getJavaBin().toString())));

        }).start();

    }
}
