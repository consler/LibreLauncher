package net.consler.librelauncher.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import net.consler.librelauncher.exceptions.FailedToLaunchMinecraftException;
import net.consler.librelauncher.exceptions.InstanceCreationException;
import net.consler.librelauncher.exceptions.FxmlLoadException;

public class ExceptionAlert
{
    public static void show(Throwable t)
    {
        Platform.runLater(() -> {
            String title = "Error";
            String header = t.getClass().getSimpleName();
            String content = t.getMessage() == null ? "An unexpected error occurred." : t.getMessage();

            Alert.AlertType type = Alert.AlertType.ERROR;

            switch (t)
            {
                case InstanceCreationException instanceCreationException -> header = "Failed to create instance";
                case FxmlLoadException fxmlLoadException -> header = "Failed to load UI";
                case FailedToLaunchMinecraftException failedToLaunchMinecraftException -> header = "Failed to launch game";
                default -> {}
            }

            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
