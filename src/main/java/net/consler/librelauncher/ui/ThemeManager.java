package net.consler.librelauncher.ui;

import com.formdev.flatlaf.*;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
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

    public void silentField(JTextField textField)
    {
        textField.getActionMap().put(DefaultEditorKit.beepAction, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e) {}
        });

        Action deletePrevChar = textField.getActionMap().get(DefaultEditorKit.deletePrevCharAction);
        if (deletePrevChar != null)
        {
            textField.getActionMap().put(DefaultEditorKit.deletePrevCharAction, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (textField.getDocument().getLength() > 0)
                    {
                        deletePrevChar.actionPerformed(e);
                    }
                }
            });
        }

        Action deletePrevWord = textField.getActionMap().get(DefaultEditorKit.deletePrevWordAction);
        if(deletePrevWord != null)
        {
            textField.getActionMap().put(DefaultEditorKit.deletePrevWordAction, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (textField.getDocument().getLength() > 0)
                    {
                        deletePrevWord.actionPerformed(e);
                    }
                }
            });
        }

        Action deleteNextChar = textField.getActionMap().get(DefaultEditorKit.deleteNextCharAction);
        if(deleteNextChar != null)
        {
            textField.getActionMap().put(DefaultEditorKit.deleteNextCharAction, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (textField.getDocument().getLength() > 0)
                    {
                        deleteNextChar.actionPerformed(e);
                    }
                }
            });
        }

        Action deleteNextWord = textField.getActionMap().get(DefaultEditorKit.deleteNextWordAction);
        if(deleteNextWord != null)
        {
            textField.getActionMap().put(DefaultEditorKit.deleteNextWordAction, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (textField.getDocument().getLength() > 0)
                    {
                        deleteNextWord.actionPerformed(e);
                    }
                }
            });
        }
    }
}