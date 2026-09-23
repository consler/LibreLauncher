package net.consler.librelauncher.ui.instance.manager.specific;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.consler.librelauncher.Main;

import java.io.IOException;
import net.consler.librelauncher.ui.theme.ThemeManager;

public class InstanceSpecificManagerApplication
{

    public static void open() throws IOException
    {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("instancespecificmanager-view.fxml"));
        Scene scene = new Scene(loader.load());
        ThemeManager.styleScene(scene);

        Stage stage = new Stage();
        stage.setTitle("Manage Instance");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }
}
