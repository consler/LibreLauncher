package net.consler.librelauncher.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import net.consler.librelauncher.exceptions.FailedToLaunchMinecraftException;
import net.consler.librelauncher.exceptions.InstanceCreationException;
import net.consler.librelauncher.exceptions.FxmlLoadException;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionAlert
{
    public static void show(Throwable t)
    {
        Platform.runLater(() ->
        {
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

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            t.printStackTrace(printWriter);
            String errorText = stringWriter.toString();

            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.resizableProperty().setValue(true);

            ButtonType copyButton = new ButtonType("Copy Error");
            alert.getButtonTypes().add(copyButton);

            alert.setOnCloseRequest(event ->
            {
                if (alert.getResult() == copyButton)
                {
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(errorText);
                    Clipboard.getSystemClipboard().setContent(clipboardContent);
                }
            });

            alert.showAndWait();
        });
    }
}