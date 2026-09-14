package net.consler.librelauncher.ui.settings.categories;

import com.sun.management.OperatingSystemMXBean;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.consler.librelauncher.ui.settings.SettingsSaver;

import java.lang.management.ManagementFactory;

public class MinecraftSettingsController
{
    private static final int DEFAULT_RAM_MB = 2048;

    @FXML private Slider ramSlider;
    @FXML private Label ramValueLabel;
    @FXML private TextField javaPathField;
    @FXML private CheckBox chkFullscreen;

    @FXML
    public void initialize()
    {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int maxRamMb = (int) Math.max(4096, Math.floor(os.getTotalMemorySize() / 1000000.0));
        ramSlider.setMin(256);
        ramSlider.setMax(maxRamMb);
        ramSlider.setBlockIncrement(256);
        ramSlider.setMajorTickUnit(512);
        ramSlider.setMinorTickCount(0);
        ramSlider.setSnapToTicks(true);

        int savedRamMb = SettingsSaver.getIntSetting("allocated_ram", DEFAULT_RAM_MB);
        savedRamMb = Math.min(Math.max(savedRamMb, 256), maxRamMb);
        ramSlider.setValue(savedRamMb);
        ramValueLabel.setText(savedRamMb + " MB");

        ramSlider.valueProperty().addListener((obs, oldVal, newVal) ->
        {
            int ramMb = (int) Math.round(newVal.doubleValue());
            ramSlider.setValue(ramMb);
            ramValueLabel.setText(ramMb + " MB");
            SettingsSaver.saveSetting("allocated_ram", ramMb);
        });
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