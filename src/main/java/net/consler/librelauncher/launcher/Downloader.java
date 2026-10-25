package net.consler.librelauncher.launcher;

import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.category.instancemanager.InstancePanel;
import net.consler.librelauncher.ui.category.instancemanager.CreateInstanceDialog;
import net.consler.librelauncherlib.install.InstallerListener;
import net.consler.librelauncherlib.install.MinecraftInstaller;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.util.SystemHelper;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Objects;

public class Downloader
{
    private final CreateInstanceDialog dialog;
    private final InstancePanel instancePanel;
    private volatile MinecraftInstaller installer;
    private Thread t;

    public Downloader(CreateInstanceDialog dialog, InstancePanel instancePanel)
    {
        this.dialog = dialog;
        this.instancePanel = instancePanel;
    }

    public void createInstance()
    {
        String name = dialog.nameField.getText();
        String version = Objects.requireNonNull(dialog.versionChoice.getSelectedItem()).toString();
        String loader = dialog.getActiveLoader();
        String loaderVersion = Objects.toString(dialog.modLoaderVersionChoice.getSelectedItem(), "");

        installer = new MinecraftInstaller();

        t = new Thread(()->
        {
            installer.setListener(new InstallerListener()
            {
                @Override
                public void onStart()
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        if (dialog.progressBar != null) dialog.progressBar.setVisible(true);
                        if (dialog.dialog != null) dialog.dialog.enableInputMethods(false);
                    });
                }
                @Override
                public void onFinish()
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        dialog.close();

                        InstancesConfig instancesConfig = new InstancesConfig();

                        instancesConfig.saveProperty(name, "version", version);
                        instancesConfig.saveProperty(name, "modloader", loader);
                        instancesConfig.saveProperty(name, "modloader_version", loaderVersion.isBlank() ? "0.0.0" : loaderVersion);

                        instancesConfig.save();

                        instancePanel.populate();
                    });
                }
                @Override
                public void onNewPercentage(int percentage)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        if(dialog.progressBar != null)
                        {
                            dialog.progressBar.setVisible(true);
                            dialog.progressBar.setValue(percentage);
                        }
                    });
                }
            });

            installer.install(
                    version,
                    InstancesConfig.instancesDir.resolve(name),
                    new ModloaderProfile(loader, loaderVersion),
                    Path.of(new SettingsSaver().get("java_path", SystemHelper.getJavaBin().toString())));

        });
        t.start();
    }

    public void cancelInstanceCreation()
    {
        if (t != null) t.interrupt();
    }
}
