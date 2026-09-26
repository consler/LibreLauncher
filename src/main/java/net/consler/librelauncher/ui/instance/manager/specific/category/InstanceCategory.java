package net.consler.librelauncher.ui.instance.manager.specific.category;

import javafx.scene.Node;
import javafx.scene.control.Label;
import net.consler.librelauncherlib.instance.InstanceProfile;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public abstract class InstanceCategory
{
    public abstract String title();

    public abstract File directory(InstanceProfile profile);

    public File[] items(InstanceProfile profile)
    {
        File directory = directory(profile);
        File[] files = directory.listFiles();
        if (files == null) return new File[0];

        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return files;
    }

    public String locationLabel(InstanceProfile profile)
    {
        return directory(profile).getAbsolutePath();
    }

    public Node icon(File file)
    {
        Label icon = new Label(defaultIcon());
        icon.getStyleClass().add("manager-item-icon");
        return icon;
    }

    public String primaryText(File file)
    {
        return stripExtension(file.getName());
    }

    public abstract String description(File file);

    protected abstract String defaultIcon();

    public String emptyMessage()
    {
        return "No " + title().toLowerCase() + " found.";
    }

    protected static String stripExtension(String name)
    {
        int extension = name.lastIndexOf('.');
        return extension > 0 ? name.substring(0, extension) : name;
    }
}