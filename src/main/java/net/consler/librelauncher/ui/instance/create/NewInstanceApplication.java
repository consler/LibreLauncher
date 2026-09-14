package net.consler.librelauncher.ui.instance.create;

import net.consler.librelauncher.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NewInstanceApplication
{

    private static final Stage stage = new Stage();
    public static void open() throws IOException
    {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("newinstance-view.fxml"));
        Scene scene = new Scene(loader.load());

        stage.setTitle("New Instance");
        stage.setScene(scene);
        stage.show();
    }

    public static void close()
    {
        stage.close();
    }
}
