package net.consler.librelauncher.ui.settings.categories;

import com.sun.management.OperatingSystemMXBean;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.consler.librelauncher.ui.settings.SettingsSaver;
import net.consler.librelauncherlib.utill.SystemHelper;

import java.lang.management.ManagementFactory;

public class MinecraftSettingsController
{
    private static final int DEFAULT_RAM_MB = 2048;

    @FXML private Slider ramSlider;
    @FXML private Label ramValueLabel;
    @FXML private TextField javaPathField;

    @FXML
    public void initialize()
    {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int maxRamMb = (int) Math.max(4096, Math.floor(os.getTotalMemorySize() / 1000000.0));
        ramSlider.setMax(maxRamMb);
        ramSlider.setMinorTickCount(0);

        int savedRamMb = SettingsSaver.getIntSetting("allocated_ram", DEFAULT_RAM_MB);
        savedRamMb = Math.clamp(savedRamMb, 256, maxRamMb);
        ramSlider.setValue(savedRamMb);
        ramValueLabel.setText(savedRamMb + " MB");

        ramSlider.valueProperty().addListener((obs, oldVal, newVal) ->
        {
            int ramMb = (int) Math.round(newVal.doubleValue());
            ramSlider.setValue(ramMb);
            ramValueLabel.setText(ramMb + " MB");
            SettingsSaver.saveSetting("allocated_ram", ramMb);
        });

        String javaPath = SettingsSaver.getSetting("java_path");
        if (javaPath == null || javaPath.isBlank())
        {
            javaPathField.setText(SystemHelper.getJavaBin().toString());
        }
        else
        {
            javaPathField.setText(javaPath);
        }

        javaPathField.setOnAction(event -> SettingsSaver.saveSetting("java_path", javaPathField.getText()));
    }

    @FXML
    private void onBrowseJavaPath()
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Java Executable");
        var file = chooser.showOpenDialog(javaPathField.getScene().getWindow());
        if (file != null)
        {
            javaPathField.setText(file.getAbsolutePath());
        }
    }
}