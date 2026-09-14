package net.consler.librelauncher.ui.theme;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import javafx.application.Application;
import net.consler.librelauncher.Main;

import java.util.Objects;

public final class ThemeManager
{
    public static final String DEFAULT_THEME = "Cupertino Dark";

    private ThemeManager()
    {
    }

    public static void applyTheme(String themeName)
    {
        String resolvedTheme = themeName == null || themeName.isBlank() ? DEFAULT_THEME : themeName;
        Application.setUserAgentStylesheet(getStylesheet(resolvedTheme));
    }

    public static String getStylesheet(String themeName)
    {
        return switch (themeName)
        {
            case "Cupertino Light" -> new CupertinoLight().getUserAgentStylesheet();
            case "Nord Dark" -> new NordDark().getUserAgentStylesheet();
            case "Nord Light" -> new NordLight().getUserAgentStylesheet();
            case "Dracula" -> new Dracula().getUserAgentStylesheet();
            case "Winter Dark" -> Objects.requireNonNull(Main.class.getResource("themes/winter-dark.css")).toExternalForm();
            case "Winter Light" -> Objects.requireNonNull(Main.class.getResource("themes/winter-light.css")).toExternalForm();
            case "Spring Dark" -> Objects.requireNonNull(Main.class.getResource("themes/spring-dark.css")).toExternalForm();
            case "Spring Light" -> Objects.requireNonNull(Main.class.getResource("themes/spring-light.css")).toExternalForm();
            case "Blacky" -> Objects.requireNonNull(Main.class.getResource("themes/blacky.css")).toExternalForm();
            case "News" -> Objects.requireNonNull(Main.class.getResource("themes/news.css")).toExternalForm();
            case "Browny" -> Objects.requireNonNull(Main.class.getResource("themes/browny.css")).toExternalForm();

            default -> new CupertinoDark().getUserAgentStylesheet();
        };
    }
}
