package net.consler.librelauncher.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ThemeManager
{

    public enum Theme
    {
        DARK("Dark", true, false),
        LIGHT("Light", false, false),
        DARK_MAC("Dark (mac)", true, true),
        LIGHT_MAC("Light (mac)", false, true);

        public final String label;
        public final boolean dark;
        public final boolean mac;

        Theme(String label, boolean dark, boolean mac)
        {
            this.label = label;
            this.dark = dark;
            this.mac = mac;
        }

        public Theme counterpart()
        {
            for (Theme t : values())
                if (t.dark == !dark && t.mac == mac) return t;
            throw new IllegalStateException("No counterpart for " + this);
        }

        public static Theme fromLabel(String label)
        {
            for (Theme t : values())
                if (t.label.equalsIgnoreCase(label)) return t;
            return DARK;
        }


        LookAndFeel create()
        {
            return switch (this)
            {
                case DARK -> new FlatDarkLaf()
                {
                    @Override public void provideErrorFeedback(Component c) {}
                };
                case LIGHT -> new FlatLightLaf()
                {
                    @Override public void provideErrorFeedback(Component c) {}
                };
                case DARK_MAC -> new FlatMacDarkLaf()
                {
                    @Override public void provideErrorFeedback(Component c) {}
                };
                case LIGHT_MAC -> new FlatMacLightLaf()
                {
                    @Override public void provideErrorFeedback(Component c) {}
                };
            };
        }
    }

    private static final String SETTING_KEY = "theme";
    private static final String FONT_RESOURCE = "font/comfortaa-bold.ttf";
    private static final float FONT_SIZE = 12f;

    private static final String FILL = "lighten($Panel.background,6%)";

    private static final Map<String, String> EXTRA_DEFAULTS = Map.ofEntries(
            Map.entry("Component.arc", "12"),
            Map.entry("TextComponent.arc", "12"),
            Map.entry("Button.arc", "25"),
            Map.entry("Component.borderWidth", "0"),
            Map.entry("Component.focusWidth", "0"),
            Map.entry("Button.default.borderWidth", "0"),

            Map.entry("TextField.background", FILL),
            Map.entry("PasswordField.background", FILL),
            Map.entry("FormattedTextField.background", FILL),
            Map.entry("TextArea.background", FILL),
            Map.entry("ComboBox.background", FILL),
            Map.entry("ComboBox.buttonBackground", FILL),
            Map.entry("ComboBox.buttonEditableBackground", FILL),
            Map.entry("ComboBox.buttonSeparatorColor", FILL),

            Map.entry("TextField.margin", "6,12,6,12"),
            Map.entry("ComboBox.padding", "6,10,6,10"),

            Map.entry("ProgressBar.arc", "999"),
            Map.entry("ScrollBar.thumbArc", "999"),
            Map.entry("ScrollBar.thumbInsets", "2,2,2,2")
    );

    private final Main main;
    private final List<Consumer<Theme>> listeners = new CopyOnWriteArrayList<>();

    public Font comfortaaBold;

    private Theme current = Theme.DARK;
    private boolean initialized;

    public ThemeManager(Main main)
    {
        this.main = main;
    }

    public void init()
    {
        if (initialized) return;
        initialized = true;

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        loadFonts();
        FlatLaf.setGlobalExtraDefaults(EXTRA_DEFAULTS);

        apply(loadSavedTheme());
    }

    public void setTheme()
    {
        runOnEdt(() -> apply(loadSavedTheme()));
    }

    public void setTheme(Theme theme)
    {
        Objects.requireNonNull(theme, "theme");
        new SettingsSaver().set(SETTING_KEY, theme.label);
        runOnEdt(() -> apply(theme));
    }

    public void setTheme(String label)
    {
        setTheme(Theme.fromLabel(label));
    }

    public void toggleDarkLight()
    {
        setTheme(current.counterpart());
    }

    public Theme getTheme()
    {
        return current;
    }

    public boolean isDarkTheme()
    {
        return current.dark;
    }

    public static String[] themeLabels()
    {
        return Arrays.stream(Theme.values()).map(t -> t.label).toArray(String[]::new);
    }

    public void addThemeListener(Consumer<Theme> listener)
    {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeThemeListener(Consumer<Theme> listener)
    {
        listeners.remove(listener);
    }

    private Theme loadSavedTheme()
    {
        return Theme.fromLabel(new SettingsSaver().get(SETTING_KEY, Theme.DARK.label));
    }

    private void apply(Theme theme)
    {
        if (comfortaaBold != null) UIManager.put("defaultFont", comfortaaBold);

        try
        {
            UIManager.setLookAndFeel(theme.create());
        }
        catch (UnsupportedLookAndFeelException e)
        {
            new ErrorDialog(main.client, e, "setting theme");
            return;
        }

        current = theme;

        FlatLaf.updateUI();
        if (main.client != null) main.reloadThemeUI();

        listeners.forEach(l -> l.accept(theme));
    }

    private static void runOnEdt(Runnable task)
    {
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }

    private void loadFonts()
    {
        try (InputStream in = Main.class.getResourceAsStream(FONT_RESOURCE))
        {
            if (in == null) throw new IOException("Missing font resource: " + FONT_RESOURCE);

            Font base = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
            comfortaaBold = base.deriveFont(FONT_SIZE);
        }
        catch (FontFormatException | IOException e)
        {
            System.err.println("Could not load font, using theme default: " + e);
            comfortaaBold = null;
        }
    }

    public static boolean isDark(Color color)
    {
        Objects.requireNonNull(color, "color");
        double luminance = 0.2126 * color.getRed()
                + 0.7152 * color.getGreen()
                + 0.0722 * color.getBlue();
        return luminance < 128;
    }

    public static Color mix(Color from, Color to, float amount)
    {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        float t = Math.clamp(amount, 0f, 1f);
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                from.getAlpha()
        );
    }

    public static Color darken(Color color, float amount)
    {
        return mix(color, Color.BLACK, amount);
    }

    public static Color lighten(Color color, float amount)
    {
        return mix(color, Color.WHITE, amount);
    }

    public static Color withAlpha(Color color, int alpha)
    {
        Objects.requireNonNull(color, "color");
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.clamp(alpha, 0, 255));
    }

    public static Color readableForeground(Color background)
    {
        return isDark(background) ? new Color(0xF2F2F2) : new Color(0x1E1E1E);
    }

    public static Color surfaceColor(Color background)
    {
        return darken(background, isDark(background) ? 0.30f : 0.04f);
    }

    public static Color outlineColor(Color background)
    {
        return isDark(background) ? lighten(background, 0.14f) : darken(background, 0.12f);
    }

    public static Color hoverColor(Color background)
    {
        return hoverColor(background, 0.08f);
    }

    public static Color hoverColor(Color background, float amount)
    {
        Objects.requireNonNull(background, "background");

        float[] hsb = Color.RGBtoHSB(background.getRed(), background.getGreen(), background.getBlue(), null);
        hsb[2] = isDark(background)
                ? Math.min(1.0f, hsb[2] + amount)
                : Math.max(0.0f, hsb[2] - amount);

        Color rgb = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), background.getAlpha());
    }

    public static Color panelBackground()
    {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : new Color(0x3C3F41);
    }

    public static Color accentColor()
    {
        for (String key : new String[] {"Component.accentColor", "Button.default.background", "Component.focusColor"})
        {
            Color c = UIManager.getColor(key);
            if (c != null) return c;
        }
        return new Color(0x4B6EAF);
    }
}