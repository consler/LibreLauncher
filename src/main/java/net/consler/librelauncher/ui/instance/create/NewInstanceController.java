package net.consler.librelauncher.ui.instance.create;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.exceptions.InstanceCreationException;
import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.launcher.instance.Downloader;
import net.consler.librelauncher.ui.client.ClientController;
import net.consler.librelauncher.ui.instance.manager.InstanceManagerController;
import net.consler.librelauncher.utils.ExceptionAlert;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import net.consler.librelauncherlib.versions.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NewInstanceController implements Initializable
{

    @FXML
    private TextField nameField;

    @FXML
    private javafx.scene.layout.HBox loaderOptionsBox;

    @FXML
    private javafx.scene.layout.HBox modLoaderVersionBox;

    @FXML
    private ToggleGroup modLoaderGroup;

    @FXML
    private RadioButton loaderVanilla;

    @FXML
    private RadioButton loaderFabric;

    @FXML
    private RadioButton loaderForge;

    @FXML
    private RadioButton loaderNeoforge;
    @FXML
    private RadioButton loaderQuilt;

    @FXML
    private ComboBox<String> modLoaderVersionChoice;

    @FXML
    private ComboBox<String> versionChoice;

    @FXML
    private Label progressLabel;

    @FXML
    private Button createButton;

    @FXML
    private Button cancelButton;

    private ScheduledExecutorService progressScheduler;
    private ScheduledFuture<?> progressTask;

    @FXML
    private CheckBox filterReleases;

    @FXML
    private CheckBox filterSnapshots;

    @FXML
    private CheckBox filterBetas;

    @FXML
    private CheckBox filterAlphas;
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        filterReleases.selectedProperty().addListener((obs, oldV, newV) -> updateMinecraftVersions());
        filterSnapshots.selectedProperty().addListener((obs, oldV, newV) -> updateMinecraftVersions());
        filterBetas.selectedProperty().addListener((obs, oldV, newV) -> updateMinecraftVersions());
        filterAlphas.selectedProperty().addListener((obs, oldV, newV) -> updateMinecraftVersions());
        versionChoice.valueProperty().addListener((obs, oldValue, newValue) -> onModLoaderChanged());
        modLoaderGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateLoaderUi();
            onModLoaderChanged();
        });

        nameField.setText(nextDefaultInstanceName());
        updateLoaderUi();
        updateMinecraftVersions();
    }

    private void updateMinecraftVersions()
    {
        boolean releases = filterReleases.isSelected();
        boolean snapshots = filterSnapshots.isSelected();
        boolean betas = filterBetas.isSelected();
        boolean alphas = filterAlphas.isSelected();

        String selectedVersion = versionChoice.getValue();
        versionChoice.setPromptText("Loading versions...");

        CompletableFuture.supplyAsync(() -> VanillaVersions.getVersionsFiltered(releases, snapshots, betas, alphas))
                .thenAcceptAsync(versions ->
                {
                    versionChoice.getItems().setAll(versions);

                    if (!versions.isEmpty())
                    {
                        if (selectedVersion != null && versions.contains(selectedVersion)) versionChoice.getSelectionModel().select(selectedVersion);
                        else versionChoice.getSelectionModel().selectFirst();

                        versionChoice.setPromptText("Select Version...");
                    }
                    else
                    {
                        versionChoice.getSelectionModel().clearSelection();
                        versionChoice.setPromptText("No versions selected");
                    }

                    onModLoaderChanged();
                }, Platform::runLater);
    }

    private void onModLoaderChanged()
    {
        if (loaderVanilla.isSelected() || modLoaderGroup.getSelectedToggle() == null)
        {
            modLoaderVersionChoice.getItems().clear();
            modLoaderVersionChoice.setDisable(true);
            modLoaderVersionChoice.setPromptText("N/A for Vanilla");
            return;
        }

        String minecraftVersion = versionChoice.getValue();
        if (minecraftVersion == null || minecraftVersion.isBlank())
        {
            modLoaderVersionChoice.getItems().clear();
            modLoaderVersionChoice.setDisable(true);
            modLoaderVersionChoice.setPromptText("Select a Minecraft version first");
            return;
        }

        modLoaderVersionChoice.setDisable(true);
        modLoaderVersionChoice.getItems().clear();
        modLoaderVersionChoice.setPromptText("Loading versions...");

        CompletableFuture.supplyAsync(() ->
        {
            if (loaderFabric.isSelected()) return FabricVersions.getVersionsCompatibleWith(minecraftVersion);
            else if (loaderForge.isSelected()) return ForgeVersions.getVersionsCompatibleWith(minecraftVersion);
            else if (loaderNeoforge.isSelected()) return NeoforgeVersions.getVersionsCompatibleWith(minecraftVersion);
            else if (loaderQuilt.isSelected()) return QuiltVersions.getVersionsCompatibleWith(minecraftVersion);

            return Collections.<String>emptyList();
        }).thenAcceptAsync(versions ->
        {
            modLoaderVersionChoice.getItems().setAll(versions);

            if (!versions.isEmpty())
            {
                modLoaderVersionChoice.getSelectionModel().selectFirst();
                modLoaderVersionChoice.setDisable(false);
                modLoaderVersionChoice.setPromptText("Select Loader Version...");
            }
            else
            {
                modLoaderVersionChoice.setPromptText("No compatible versions for " + minecraftVersion);
            }
        }, Platform::runLater);
    }

    private void updateLoaderUi()
    {
        boolean vanillaSelected = loaderVanilla.isSelected();
        modLoaderVersionBox.setVisible(!vanillaSelected);
        modLoaderVersionBox.setManaged(!vanillaSelected);
        modLoaderVersionChoice.setVisible(!vanillaSelected);
        modLoaderVersionChoice.setManaged(!vanillaSelected);
        modLoaderVersionChoice.setDisable(vanillaSelected || modLoaderVersionChoice.getItems().isEmpty());
    }

    private String nextDefaultInstanceName()
    {
        int index = 1;
        while (true)
        {
            String candidate = "Instance" + index;
            if (!new File(Main.APPDATA_DIR, candidate + ".properties").exists() && !new File(Launcher.instanceDir, candidate).exists())
            {
                return candidate;
            }
            index++;
        }
    }

    private String getSelectedModLoader()
    {
        if (loaderVanilla.isSelected()) return "Vanilla";
        if (loaderFabric.isSelected()) return "Fabric";
        if (loaderForge.isSelected()) return "Forge";
        if (loaderNeoforge.isSelected()) return "Neoforge";
        if (loaderQuilt.isSelected()) return "Quilt";
        return null;
    }

    @FXML
    private void cancelButtonClicked()
    {
        NewInstanceApplication.close();
    }

    @FXML
    private void createButtonClicked()
    {
        String instanceName = nameField.getText() == null ? "" : nameField.getText().trim();
        if (instanceName.isBlank())
        {
            instanceName = nextDefaultInstanceName();
            nameField.setText(instanceName);
        }

        if (new File(Main.APPDATA_DIR, instanceName + ".properties").exists() || new File(Launcher.instanceDir, instanceName).exists())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "An instance with this name already exists.");
            alert.showAndWait();
            return;
        }

        String minecraftVersion = versionChoice.getValue();
        String modLoader = getSelectedModLoader();
        String loaderVersion = modLoader != null && !"Vanilla".equals(modLoader) ? modLoaderVersionChoice.getValue() : null;

        if (modLoader != null && !"Vanilla".equals(modLoader) && (loaderVersion == null || loaderVersion.isBlank()))
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please select a valid loader version before creating the instance.");
            alert.showAndWait();
            return;
        }

        if (minecraftVersion == null || minecraftVersion.isBlank())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please select a valid Minecraft version.");
            alert.showAndWait();
            return;
        }

        final String finalInstanceName = instanceName;
        final String finalMinecraftVersion = minecraftVersion;
        final String finalModLoader = modLoader;
        final String finalLoaderVersion = loaderVersion;

        progressLabel.setText("Starting...");
        progressLabel.setVisible(true);
        progressLabel.setManaged(true);

        createButton.setDisable(true);
        cancelButton.setDisable(true);
        nameField.setDisable(true);
        versionChoice.setDisable(true);
        loaderOptionsBox.setDisable(true);
        modLoaderVersionBox.setDisable(true);
        modLoaderVersionChoice.setDisable(true);
        filterReleases.setDisable(true);
        filterSnapshots.setDisable(true);
        filterAlphas.setDisable(true);
        filterBetas.setDisable(true);

        String finalInstanceName1 = instanceName;
        new Thread(() ->
        {
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            try
            {
                OutputStream forwardOut = new OutputStream()
                {
                    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

                    @Override
                    public synchronized void write(int b)
                    {
                        originalOut.write(b);
                        buf.write(b);
                        if (b == '\n')
                        {
                            String line = buf.toString(StandardCharsets.UTF_8).replaceAll("\n$", "");
                            buf.reset();
                            Platform.runLater(() -> progressLabel.setText(line));
                        }
                    }

                    @Override
                    public synchronized void write(byte @NotNull [] b, int off, int len) throws IOException
                    {
                        originalOut.write(b, off, len);
                        buf.write(b, off, len);
                        String s = buf.toString(StandardCharsets.UTF_8);
                        int lastNl = s.lastIndexOf('\n');
                        if (lastNl >= 0)
                        {
                            String upToNl = s.substring(0, lastNl + 1);
                            String[] parts = upToNl.split("\\n");
                            String lastLine = parts[parts.length - 1];
                            buf.reset();
                            if (lastNl < s.length() - 1)
                            {
                                buf.write(s.substring(lastNl + 1).getBytes(StandardCharsets.UTF_8));
                            }
                            final String show = lastLine;
                            Platform.runLater(() -> progressLabel.setText(show));
                        }
                    }
                };

                OutputStream forwardErr = new OutputStream()
                {
                    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

                    @Override
                    public synchronized void write(int b) {
                        originalErr.write(b);
                        buf.write(b);
                        if (b == '\n')
                        {
                            String line = buf.toString(StandardCharsets.UTF_8).replaceAll("\n$", "");
                            buf.reset();
                            Platform.runLater(() -> progressLabel.setText(line));
                        }
                    }

                    @Override
                    public synchronized void write(byte @NotNull [] b, int off, int len) throws IOException
                    {
                        originalErr.write(b, off, len);
                        buf.write(b, off, len);
                        String s = buf.toString(StandardCharsets.UTF_8);
                        int lastNl = s.lastIndexOf('\n');
                        if (lastNl >= 0)
                        {
                            String upToNl = s.substring(0, lastNl + 1);
                            String[] parts = upToNl.split("\\n");
                            String lastLine = parts[parts.length - 1];
                            buf.reset();
                            if (lastNl < s.length() - 1)
                            {
                                buf.write(s.substring(lastNl + 1).getBytes(StandardCharsets.UTF_8));
                            }
                            final String show = lastLine;
                            Platform.runLater(() -> progressLabel.setText(show));
                        }
                    }
                };

                PrintStream teeOut = new PrintStream(forwardOut, true, StandardCharsets.UTF_8);
                PrintStream teeErr = new PrintStream(forwardErr, true, StandardCharsets.UTF_8);

                System.setOut(teeOut);
                System.setErr(teeErr);

                Downloader.download(finalInstanceName, finalMinecraftVersion, finalModLoader.toLowerCase(), finalLoaderVersion);

                System.setOut(originalOut);
                System.setErr(originalErr);

                if (progressTask != null) progressTask.cancel(false);
                if (progressScheduler != null) progressScheduler.shutdownNow();

                Platform.runLater(() ->
                {
                    if (ClientController.getInstance() != null) ClientController.getInstance().loadInstances(finalInstanceName1);
                    if (InstanceManagerController.getInstance() != null) InstanceManagerController.getInstance().loadInstances();
                    NewInstanceApplication.close();
                });
            }
            catch (Exception e)
            {
                System.setOut(originalOut);
                System.setErr(originalErr);

                if (progressTask != null) progressTask.cancel(false);
                if (progressScheduler != null) progressScheduler.shutdownNow();

                Platform.runLater(() ->
                {
                    createButton.setDisable(false);
                    cancelButton.setDisable(false);
                    nameField.setDisable(false);
                    versionChoice.setDisable(false);
                    loaderOptionsBox.setDisable(false);
                    onModLoaderChanged();

                    progressLabel.setVisible(false);
                    progressLabel.setManaged(false);

                    ExceptionAlert.show(new InstanceCreationException(e.getMessage(), e));
                });
            }
        }, "instance-creation-thread").start();

    }
}