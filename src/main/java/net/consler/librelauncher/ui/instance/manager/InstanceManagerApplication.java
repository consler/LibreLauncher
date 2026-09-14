package net.consler.librelauncher.ui.instance.manager;

import net.consler.librelauncher.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class InstanceManagerApplication
{
    public static void open() throws IOException
    {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("instancemanager-view.fxml"));
        Scene scene = new Scene(loader.load());

        Stage stage = new Stage();
        stage.setTitle("Instance Manager");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }
}
