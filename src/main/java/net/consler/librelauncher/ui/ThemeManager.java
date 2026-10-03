package net.consler.librelauncher.ui;

import com.formdev.flatlaf.*;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.config.settings.SettingsSaver;

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

            Map.entry("TextField.margin", "6,12,6,12"),
            Map.entry("ComboBox.padding", "6,10,6,10"),

            Map.entry("ProgressBar.arc", "999"),
            Map.entry("ScrollBar.thumbArc", "999"),
            Map.entry("ScrollBar.thumbInsets", "2,2,2,2")
        ));

        setTheme();
    }

    public static void setTheme()
    {
        String theme = new SettingsSaver().get("theme", "Dark");
        switch (theme)
        {
            case "Dark" -> FlatDarkLaf.setup();
            case "Light" -> FlatLightLaf.setup();
            case "Darcula" -> FlatDarculaLaf.setup();
            case "Intellij" -> FlatIntelliJLaf.setup();
        }

        if(Main.client == null) return;

        SwingUtilities.updateComponentTreeUI(Sidebar.sidebar);
        SwingUtilities.updateComponentTreeUI(Titlebar.titlebar);
        SwingUtilities.updateComponentTreeUI(Main.client);
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