package net.consler.librelauncher.ui.client;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.instance.manager.InstanceManagerController;
import net.consler.librelauncher.ui.settings.SettingsSaver;
import net.consler.librelauncher.ui.theme.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ClientApplication extends Application
{

    @Override
    public void start(Stage stage) throws IOException
    {
        ThemeManager.applyTheme(SettingsSaver.getSetting("ui_theme"));

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("client-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setMaximized(true);
        stage.setTitle("LibreLauncher");
        stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("conslerpfp.jpg"))));

        ClientController.hostServices = getHostServices();
        InstanceManagerController.hostServices = getHostServices();

        ClientController.primaryStage = stage;

        stage.setScene(scene);
        stage.show();
    }
}
