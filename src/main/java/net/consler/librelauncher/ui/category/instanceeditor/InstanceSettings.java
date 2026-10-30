package net.consler.librelauncher.ui.category.instanceeditor;

import com.google.gson.JsonParser;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.settings.AutoDetectJavaDialog;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.util.JavaVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;

public class InstanceSettings
{
    public JPanel panel;

    private final InstancesConfig instancesConfig;

    public InstanceSettings(Main main, InstancesConfig instancesConfig)
    {
        this.instancesConfig = instancesConfig;
    }

    public JPanel createPanel(Instance instance)
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Settings.createHeader("Default Minecraft Settings"));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel ramLabel = new JLabel("Maximum Memory Allocation (MB)");
        ramLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ramLabel);

        JTextField ramField = new JTextField();
        ramField.setText(instancesConfig.getProperty(instance.getName(), "ram", "2048"));
        ramField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ramField.setAlignmentX(Component.LEFT_ALIGNMENT);

        ThemeManager.silentField(ramField);

        ramField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                instancesConfig.saveProperty(instance.getName(), "ram", ramField.getText());
                instancesConfig.save();
            }
        });
        panel.add(ramField);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel javaPathLabel = new JLabel("Java Path");
        javaPathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(javaPathLabel);

        JTextField javaPathField = new JTextField();
        javaPathField.setText(instancesConfig.getProperty(instance.getName(), "java_path", JavaVersion.current().executable().toString()));
        javaPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        javaPathField.setAlignmentX(Component.LEFT_ALIGNMENT);
        ThemeManager.silentField(javaPathField);

        javaPathField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                instancesConfig.saveProperty(instance.getName(), "java_path", javaPathField.getText());
                instancesConfig.save();
            }
        });

        JButton autoDetectJavaButton = new JButton("Auto Detect");
        autoDetectJavaButton.setMaximumSize(new Dimension(150, 32));
        autoDetectJavaButton.setFocusable(false);
        autoDetectJavaButton.addActionListener(e ->
                new AutoDetectJavaDialog(Main.getInstance().client, javaPathField)
                {
                    @Override
                    public void onDone()
                    {
                        instancesConfig.saveProperty(instance.getName(), "java_path", javaPathField.getText());
                        instancesConfig.save();
                    }
                });

        JPanel javaPathRow = new JPanel(new BorderLayout(8, 0));
        javaPathRow.setOpaque(false);
        javaPathRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        javaPathRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        javaPathRow.add(javaPathField, BorderLayout.CENTER);
        javaPathRow.add(autoDetectJavaButton, BorderLayout.EAST);

        panel.add(javaPathRow);

        try
        {
            panel.add(Box.createRigidArea(new Dimension(0, 8)));

            ModloaderInfo modloaderInfo = getModloaderInfo(instance);
            String currentLoader = modloaderInfo.loader();
            String currentLoaderVersion = modloaderInfo.version();

            JLabel modLoader = new JLabel("Mod Loader: " + currentLoader + " " + currentLoaderVersion);
            modLoader.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(modLoader);

            JButton changeModloader = new JButton("Change");
            changeModloader.setMaximumSize(new Dimension(80, 32));
            changeModloader.addActionListener(e -> new ModloaderChangeDialog(
                    Main.getInstance().client, instance, instancesConfig, currentLoader, currentLoaderVersion)
                    {
                        @Override
                        public void onDone()
                        {
                            ModloaderProfile profile = selectedProfile();
                            if (profile == null) return;

                            instancesConfig.saveProperty(instance.getName(), "modloader", profile.loaderId());
                            instancesConfig.saveProperty(instance.getName(), "modloader_version",
                                    profile.loaderVersion() == null ? "0.0.0" : profile.loaderVersion());
                            instancesConfig.save();

                            Main.getInstance().refreshInstanceViews();
                            Main.getInstance().instanceEditor.show(instance);
                        }
                    }.open());
            panel.add(changeModloader);
        }
        catch (IOException e)
        {
            new ErrorDialog(Main.getInstance().client, e, "loading mod loader info");
            throw new RuntimeException(e);
        }

        return panel;
    }

    private ModloaderInfo getModloaderInfo(Instance instance) throws IOException
    {
        String loader = instance.getModloader();
        try
        {
            return new ModloaderInfo(loader, getLoaderVersion(instance, loader));
        }
        catch (NoSuchFileException e)
        {
            String currentLoader = instance.getModloader();
            if (!currentLoader.equals(loader))
            {
                return new ModloaderInfo(currentLoader, getLoaderVersion(instance, currentLoader));
            }

            String configuredLoader = instancesConfig.getProperty(instance.getName(), "modloader");
            String configuredVersion = instancesConfig.getProperty(instance.getName(), "modloader_version");
            if (loader.equals(configuredLoader) && configuredVersion != null)
            {
                return new ModloaderInfo(loader, configuredVersion);
            }
            return new ModloaderInfo(currentLoader, "unknown");
        }
    }

    private String getLoaderVersion(Instance instance, String loader) throws IOException
    {
        if (ModloaderProfile.QUILT_ID.equals(loader))
        {
            String profileId = JsonParser.parseString(Files.readString(instance.getFolder().resolve("quilt-profile.json")))
                    .getAsJsonObject()
                    .get("id")
                    .getAsString();
            int firstHyphen = profileId.indexOf('-');
            int secondHyphen = profileId.indexOf('-', firstHyphen + 1);
            int lastHyphen = profileId.lastIndexOf('-');
            if (secondHyphen < 0 || lastHyphen <= secondHyphen)
            {
                throw new IOException("Unexpected Quilt profile ID: " + profileId);
            }
            return profileId.substring(secondHyphen + 1, lastHyphen);
        }
        return instance.getLoaderVersion();
    }

    private record ModloaderInfo(String loader, String version)
    {
    }
}
