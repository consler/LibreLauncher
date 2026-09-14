package net.consler.librelauncher.ui.settings;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import net.consler.librelauncher.Main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsController
{

    @FXML private StackPane contentArea;
    private final Map<String, Node> viewCache = new HashMap<>();

    @FXML
    public void initialize()
    {
        showCategory("minecraft_settings.fxml");
    }

    @FXML private void onMinecraftSelected()
    {
        showCategory("minecraft_settings.fxml");
    }
    @FXML private void onAccountsSelected()
    {
        showCategory("accounts_settings.fxml");
    }
    @FXML private void onUISelected()
    {
        showCategory("ui_settings.fxml");
    }

    private void showCategory(String fxmlPath)
    {

        try
        {
            if (!viewCache.containsKey(fxmlPath))
            {
                var resourceUrl = Main.class.getResource("settings/" + fxmlPath);
                Node node = FXMLLoader.load(Objects.requireNonNull(resourceUrl, "FXML file not found: " + fxmlPath));
                viewCache.put(fxmlPath, node);
            }
            contentArea.getChildren().setAll(viewCache.get(fxmlPath));
        }
        catch (IOException e)
        {
            throw new net.consler.librelauncher.exceptions.FxmlLoadException("Failed to load FXML file: " + fxmlPath, e);
        }
    }
}