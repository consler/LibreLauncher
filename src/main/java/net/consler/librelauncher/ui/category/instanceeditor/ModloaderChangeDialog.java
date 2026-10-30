package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.modloader.FabricInstaller;
import net.consler.librelauncherlib.modloader.ForgeInstaller;
import net.consler.librelauncherlib.modloader.ModloaderProfile;
import net.consler.librelauncherlib.modloader.NeoforgeInstaller;
import net.consler.librelauncherlib.modloader.QuiltInstaller;
import net.consler.librelauncherlib.util.JavaVersion;
import net.consler.librelauncherlib.versions.FabricVersions;
import net.consler.librelauncherlib.versions.ForgeVersions;
import net.consler.librelauncherlib.versions.NeoforgeVersions;
import net.consler.librelauncherlib.versions.QuiltVersions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class ModloaderChangeDialog
{
    private static final LoaderOption[] LOADERS =
            {
            new LoaderOption("Vanilla", ModloaderProfile.VANILLA_ID),
            new LoaderOption("Fabric", ModloaderProfile.FABRIC_ID),
            new LoaderOption("Forge", ModloaderProfile.FORGE_ID),
            new LoaderOption("NeoForge", ModloaderProfile.NEOFORGE_ID),
            new LoaderOption("Quilt", ModloaderProfile.QUILT_ID)
    };

    private final JFrame parentFrame;
    private final Instance instance;
    private final InstancesConfig instancesConfig;
    private final ModloaderProfile currentProfile;

    private JDialog dialog;
    private JComboBox<LoaderOption> loaderChoice;
    private JComboBox<String> versionChoice;
    private JLabel statusLabel;
    private JButton applyButton;
    private SwingWorker<List<String>, Void> versionsWorker;

    public ModloaderChangeDialog(JFrame parentFrame, Instance instance, InstancesConfig instancesConfig,
                                 String currentLoader, String currentVersion)
    {
        this.parentFrame = parentFrame;
        this.instance = instance;
        this.instancesConfig = instancesConfig;
        this.currentProfile = ModloaderProfile.VANILLA_ID.equals(currentLoader)
                ? ModloaderProfile.VANILLA()
                : new ModloaderProfile(currentLoader, currentVersion);
    }

    public void open()
    {
        dialog = new JDialog(parentFrame, "Change Mod Loader", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        loaderChoice = new JComboBox<>(LOADERS);
        versionChoice = new JComboBox<>();
        statusLabel = new JLabel("Select a mod loader.");
        applyButton = new JButton("Apply");
        applyButton.setEnabled(false);

        for (int i = 0; i < LOADERS.length; i++)
        {
            if (LOADERS[i].id().equals(currentProfile.loaderId()))
            {
                loaderChoice.setSelectedIndex(i);
                break;
            }
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        addRow(form, "Mod Loader", loaderChoice, 0);
        addRow(form, "Loader Version", versionChoice, 1);
        GridBagConstraints statusConstraints = new GridBagConstraints();
        statusConstraints.gridx = 0;
        statusConstraints.gridy = 2;
        statusConstraints.gridwidth = 2;
        statusConstraints.weightx = 1;
        statusConstraints.fill = GridBagConstraints.HORIZONTAL;
        statusConstraints.insets = new Insets(6, 0, 0, 0);
        form.add(statusLabel, statusConstraints);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        applyButton.addActionListener(e -> applySelection());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(applyButton);

        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(applyButton);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        loaderChoice.addActionListener(e -> loadVersions());
        loadVersions();

        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    public ModloaderProfile selectedProfile()
    {
        LoaderOption loader = (LoaderOption) loaderChoice.getSelectedItem();
        if (loader == null) return null;
        if (ModloaderProfile.VANILLA_ID.equals(loader.id())) return ModloaderProfile.VANILLA();

        String version = (String) versionChoice.getSelectedItem();
        return version == null ? null : new ModloaderProfile(loader.id(), version);
    }

    private void loadVersions()
    {
        LoaderOption loader = (LoaderOption) loaderChoice.getSelectedItem();
        if (loader == null) return;

        if (versionsWorker != null) versionsWorker.cancel(true);
        versionChoice.removeAllItems();
        boolean vanilla = ModloaderProfile.VANILLA_ID.equals(loader.id());
        versionChoice.setEnabled(!vanilla);
        applyButton.setEnabled(vanilla);

        if (vanilla)
        {
            statusLabel.setText("This will remove the current mod loader.");
            return;
        }

        statusLabel.setText("Loading compatible versions...");
        applyButton.setEnabled(false);
        versionsWorker = new SwingWorker<>()
        {
            @Override
            protected List<String> doInBackground() throws IOException
            {
                return getVersions(loader.id(), instance.getVersion());
            }

            @Override
            protected void done()
            {
                if (isCancelled() || !loader.equals(loaderChoice.getSelectedItem())) return;
                try
                {
                    List<String> versions = get();
                    for (String version : versions) versionChoice.addItem(version);

                    if (versions.isEmpty())
                    {
                        statusLabel.setText("No compatible versions were found.");
                        return;
                    }

                    if (loader.id().equals(currentProfile.loaderId()) && versions.contains(currentProfile.loaderVersion()))
                    {
                        versionChoice.setSelectedItem(currentProfile.loaderVersion());
                    }
                    statusLabel.setText("Choose a compatible version.");
                    applyButton.setEnabled(true);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Loading versions was interrupted.");
                }
                catch (ExecutionException e)
                {
                    statusLabel.setText("Failed to load compatible versions.");
                    new ErrorDialog(parentFrame, e, "loading mod loader versions");
                    throw new RuntimeException(e);
                }
            }
        };
        versionsWorker.execute();
    }

    private List<String> getVersions(String loader, String minecraftVersion)
    {
        return switch (loader)
        {
            case ModloaderProfile.FABRIC_ID -> FabricVersions.getVersionsCompatibleWith(minecraftVersion);
            case ModloaderProfile.FORGE_ID -> ForgeVersions.getVersionsCompatibleWith(minecraftVersion);
            case ModloaderProfile.NEOFORGE_ID -> NeoforgeVersions.getVersionsCompatibleWith(minecraftVersion);
            case ModloaderProfile.QUILT_ID -> QuiltVersions.getVersionsCompatibleWith(minecraftVersion);
            default -> List.of();
        };
    }

    private void applySelection()
    {
        ModloaderProfile profile = selectedProfile();
        if (profile == null) return;

        if (profile.equals(currentProfile))
        {
            dialog.dispose();
            return;
        }

        loaderChoice.setEnabled(false);
        versionChoice.setEnabled(false);
        applyButton.setEnabled(false);
        statusLabel.setText("Changing mod loader...");

        new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                changeModloader(profile);
                return null;
            }

            @Override
            protected void done()
            {
                try
                {
                    get();
                    onDone();
                    dialog.dispose();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    handleFailure(e);
                }
                catch (ExecutionException e)
                {
                    handleFailure(e.getCause() instanceof Exception exception ? exception : e);
                }
                catch (RuntimeException e)
                {
                    handleFailure(e);
                }
            }
        }.execute();
    }

    private void changeModloader(ModloaderProfile profile) throws Exception
    {
        Path gameDir = instance.getFolder();
        if (!ModloaderProfile.VANILLA_ID.equals(currentProfile.loaderId()))
            uninstall(currentProfile.loaderId(), gameDir);

        if (ModloaderProfile.VANILLA_ID.equals(profile.loaderId())) return;

        String minecraftVersion = instance.getVersion();
        Path javaPath = Path.of(instancesConfig.getProperty(instance.getName(), "java_path",
                JavaVersion.current().executable().toString()));
        switch (profile.loaderId())
        {
            case ModloaderProfile.FABRIC_ID -> FabricInstaller.install(profile, gameDir, minecraftVersion);
            case ModloaderProfile.FORGE_ID -> ForgeInstaller.install(profile, gameDir, minecraftVersion, javaPath);
            case ModloaderProfile.NEOFORGE_ID -> NeoforgeInstaller.install(profile, gameDir, minecraftVersion, javaPath);
            case ModloaderProfile.QUILT_ID -> QuiltInstaller.install(profile, gameDir, minecraftVersion);
            default -> throw new IllegalArgumentException("Unsupported mod loader: " + profile.loaderId());
        }
    }

    private void uninstall(String loader, Path gameDir)
    {
        switch (loader)
        {
            case ModloaderProfile.FABRIC_ID -> FabricInstaller.uninstall(gameDir);
            case ModloaderProfile.FORGE_ID -> ForgeInstaller.uninstall(gameDir);
            case ModloaderProfile.NEOFORGE_ID -> NeoforgeInstaller.uninstall(gameDir);
            case ModloaderProfile.QUILT_ID -> QuiltInstaller.uninstall(gameDir);
            default -> throw new IllegalArgumentException("Unsupported mod loader: " + loader);
        }
    }

    private void handleFailure(Exception exception)
    {
        loaderChoice.setEnabled(true);
        LoaderOption loader = (LoaderOption) loaderChoice.getSelectedItem();
        versionChoice.setEnabled(loader != null && !ModloaderProfile.VANILLA_ID.equals(loader.id()));
        applyButton.setEnabled(selectedProfile() != null);
        statusLabel.setText("Failed to change mod loader.");
        new ErrorDialog(parentFrame, exception, "changing mod loader");
    }

    private void addRow(JPanel panel, String label, JComponent component, int row)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(0, 0, 12, 12);
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        panel.add(component, constraints);
    }

    public abstract void onDone();

    private record LoaderOption(String label, String id)
    {
        @Override
        public String toString() {
            return label;
        }
    }
}
