package net.consler.librelauncher.ui.settings.categories;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import net.consler.librelauncher.ui.settings.SettingsSaver;
import net.consler.librelauncher.ui.theme.ThemeManager;

public class UISettingsController
{

    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox chkCloseOnLaunch;

    @FXML
    public void initialize()
    {
        themeComboBox.setItems(FXCollections.observableArrayList(
                "Cupertino Dark",
                "Cupertino Light",
                "Nord Dark",
                "Nord Light",
                "Winter Dark",
                "Winter Light",
                "Spring Dark",
                "Spring Light",
                "Dracula",
                "Blacky",
                "Browny",
                "News"
        ));

        String savedTheme = SettingsSaver.getSetting("ui_theme");
        if (savedTheme == null || savedTheme.isBlank()) savedTheme = ThemeManager.DEFAULT_THEME;

        if (themeComboBox.getItems().contains(savedTheme)) themeComboBox.getSelectionModel().select(savedTheme);
        else themeComboBox.getSelectionModel().selectFirst();

        ThemeManager.applyTheme(themeComboBox.getValue());
        themeComboBox.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            if (newValue == null || newValue.isBlank()) return;

            SettingsSaver.saveSetting("ui_theme", newValue);
            ThemeManager.applyTheme(newValue);
        });

        String closeOnLaunch = SettingsSaver.getSetting("close-on-launch");
        chkCloseOnLaunch.setSelected(closeOnLaunch == null || closeOnLaunch.isBlank() || Boolean.parseBoolean(closeOnLaunch));
        chkCloseOnLaunch.selectedProperty().addListener((obs, oldV, newV) -> SettingsSaver.saveSetting("close-on-launch", newV));
    }
}