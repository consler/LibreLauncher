package net.consler.librelauncher.ui;

import com.formdev.flatlaf.*;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ThemeManager
{
    private final Main main;
    public Font comfortaa;
    public Font comfortaaBold;

    public ThemeManager(Main main)
    {
        this.main = main;
    }

    public void init()
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

        try
        {
            UIManager.setLookAndFeel(new MetalLookAndFeel()
            {
                @Override
                public void provideErrorFeedback(Component component) {}
            });
        }
        catch (UnsupportedLookAndFeelException e)
        {
            new ErrorDialog(main.client, e, "setting theme");
            throw new RuntimeException(e);
        }

        setTheme();
    }

    public void setTheme()
    {
        String theme = new SettingsSaver().get("theme", "Dark");
        switch (theme)
        {
            case "Dark" -> FlatDarkLaf.setup();
            case "Light" -> FlatLightLaf.setup();
            case "Darcula" -> FlatDarculaLaf.setup();
            case "Intellij" -> FlatIntelliJLaf.setup();
        }

        if(main.client == null) return;

        if (main.sidebar != null) SwingUtilities.updateComponentTreeUI(main.sidebar.sidebar);
        if (main.titlebar != null) SwingUtilities.updateComponentTreeUI(main.titlebar.titlebar);
        SwingUtilities.updateComponentTreeUI(main.client);
    }

    private void loadFonts()
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

    public static void silentField(JTextComponent field)
    {
        ActionMap map = field.getActionMap();

        guard(field, map, DefaultEditorKit.deletePrevCharAction, true);
        guard(field, map, DefaultEditorKit.deletePrevWordAction, true);
        guard(field, map, DefaultEditorKit.deleteNextCharAction, false);
        guard(field, map, DefaultEditorKit.deleteNextWordAction, false);
    }

    private static void guard(JTextComponent field, ActionMap map, String actionName, boolean backwards)
    {
        Action original = map.get(actionName);
        if (original == null) return;

        map.put(actionName, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (!field.isEditable() || !field.isEnabled()) return;

                if (field.getSelectionStart() != field.getSelectionEnd())
                {
                    field.replaceSelection("");
                    return;
                }

                int caret = field.getCaretPosition();
                boolean nothingToDelete = backwards
                        ? caret == 0
                        : caret == field.getDocument().getLength();

                if (!nothingToDelete)
                {
                    original.actionPerformed(e);
                }
            }
        });
    }
}