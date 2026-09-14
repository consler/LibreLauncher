package net.consler.librelauncher.ui.settings;

import net.consler.librelauncher.Main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.consler.librelauncher.utils.ExceptionAlert;

import java.io.IOException;

public abstract class SettingsApplication extends Application
{
    public static void open() throws IOException
    {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("settings/settings-view.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setMaximized(true);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            ExceptionAlert.show(e instanceof net.consler.librelauncher.exceptions.FxmlLoadException ? e : new net.consler.librelauncher.exceptions.FxmlLoadException("Failed to open settings UI", e));
        }
    }
}
