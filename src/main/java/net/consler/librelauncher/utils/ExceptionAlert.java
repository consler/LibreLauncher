package net.consler.librelauncher.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import net.consler.librelauncher.exceptions.AuthException;
import net.consler.librelauncher.exceptions.FailedToLaunchMinecraftException;
import net.consler.librelauncher.exceptions.InstanceCreationException;
import net.consler.librelauncher.exceptions.FxmlLoadException;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionAlert
{
    public static void show(Throwable t)
    {
        Runnable displayAlert = () ->
        {
            String title = "Error";
            String header = switch (t)
            {
                case InstanceCreationException instanceCreationException -> "Failed to create instance";
                case FxmlLoadException fxmlLoadException -> "Failed to load UI";
                case FailedToLaunchMinecraftException failedToLaunchMinecraftException -> "Failed to launch game";
                case AuthException authException -> "Authentication failed";
                default -> t.getClass().getSimpleName();
            };

            String content = t.getMessage();

            if (content == null && t.getCause() != null) content = t.getCause().getMessage();

            if (content == null) content = "An unexpected error occurred.";

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            t.printStackTrace(printWriter);
            String errorText = stringWriter.toString();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.setResizable(true);

            ButtonType copyButton = new ButtonType("Copy Error");
            alert.getButtonTypes().setAll(copyButton, ButtonType.OK);

            TextArea textArea = new TextArea(errorText);
            textArea.setEditable(false);
            textArea.setWrapText(false);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(new Label("Details:"), 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);

            alert.showAndWait().ifPresent(buttonType ->
            {
                if (buttonType == copyButton)
                {
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(errorText);
                    Clipboard.getSystemClipboard().setContent(clipboardContent);
                }
            });
        };

        if (Platform.isFxApplicationThread()) displayAlert.run();
        else Platform.runLater(displayAlert);
    }
}