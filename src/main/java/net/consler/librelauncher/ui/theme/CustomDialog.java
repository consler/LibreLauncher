package net.consler.librelauncher.ui.theme;

import javafx.scene.control.*;
import net.consler.librelauncher.Main;

import java.util.Objects;

public class CustomDialog
{

    public static TextInputDialog make(String title, String header, String content, String defaultValue)
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
}

