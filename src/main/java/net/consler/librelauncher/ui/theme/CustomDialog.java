package net.consler.librelauncher.ui.theme;

import javafx.scene.control.*;
import net.consler.librelauncher.Main;

import java.util.Objects;

public class CustomDialog
{

    public static TextInputDialog textInputDialog(String title, String header, String content, String defaultValue)
    {
        TextField field = new TextField(defaultValue);

        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add("modern-dialog");
        pane.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("custom/dialog.css")).toExternalForm());
        pane.setGraphic(null);

        return dialog;
    }

    public static Alert alertDialog(String title, String header, String content)
    {
        Alert field = new Alert(Alert.AlertType.INFORMATION);
        field.setTitle(title);
        field.setHeaderText(header);
        field.setContentText(content);

        DialogPane pane = field.getDialogPane();
        pane.getStylesheets().add("modern-dialog");
        pane.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("custom/dialog.css")).toExternalForm());
        pane.setGraphic(null);

        return field;
    }
}

