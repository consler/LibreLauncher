package net.consler.librelauncher.ui;

import com.formdev.flatlaf.FlatLaf;
import net.consler.librelauncher.Main;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ThemeManager
{
    public static Font comfortaa;
    public static Font comfortaaBold;

    public static void init()
    {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        loadFonts();

        UIManager.put("defaultFont", comfortaaBold);

        UIManager.put("Button.arc", 25);

        String fill = "lighten($Panel.background,6%)";
        FlatLaf.setGlobalExtraDefaults(Map.ofEntries(
            Map.entry("Component.arc", "12"),
            Map.entry("TextComponent.arc", "12"),
            Map.entry("Component.borderWidth", "0"),
            Map.entry("Component.focusWidth", "0"),
            Map.entry("Button.default.borderWidth", "0"),

            Map.entry("TextField.background", fill),
            Map.entry("PasswordField.background", fill),
            Map.entry("FormattedTextField.background", fill),
            Map.entry("TextArea.background", fill),
            Map.entry("ComboBox.background", fill),
            Map.entry("ComboBox.buttonBackground", fill),
            Map.entry("ComboBox.buttonEditableBackground", fill),
            Map.entry("ComboBox.buttonSeparatorColor", fill),

            // More breathing room
            Map.entry("TextField.margin", "6,12,6,12"),
            Map.entry("ComboBox.padding", "6,10,6,10"),

            // Rounded progress bar and slim scrollbars
            Map.entry("ProgressBar.arc", "999"),
            Map.entry("ScrollBar.thumbArc", "999"),
            Map.entry("ScrollBar.thumbInsets", "2,2,2,2")
        ));
    }

    private static void loadFonts()
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try
        {
            comfortaa = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Main.class.getResourceAsStream("font/comfortaa.ttf")));
            comfortaa = comfortaa.deriveFont(12f);
            ge.registerFont(comfortaa);

            comfortaaBold = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Main.class.getResourceAsStream("font/comfortaa-bold.ttf")));
            comfortaaBold = comfortaaBold.deriveFont(12f);
            ge.registerFont(comfortaaBold);
        }
        catch (FontFormatException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}