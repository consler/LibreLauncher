package net.consler.librelauncher.ui.instance.manager.specific.category;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.consler.librelauncher.ui.instance.manager.specific.ManagerFormat;
import net.consler.librelauncherlib.instance.InstanceProfile;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class ScreenshotCategory extends InstanceCategory
{
    @Override
    public String title()
    {
        return "Screenshots";
    }

    @Override
    public File directory(InstanceProfile profile)
    {
        return profile.getScreenshotsFolder().toFile();
    }

    @Override
    public File[] items(InstanceProfile profile)
    {
        File[] files = super.items(profile);
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files;
    }

    @Override
    public Node icon(File file)
    {
        if (isImage(file))
        {
            ImageView imageView = new ImageView(new Image(file.toURI().toString(), 48, 48, true, true, true));
            imageView.getStyleClass().add("manager-item-image");
            return imageView;
        }
        return super.icon(file);
    }

    @Override
    public String description(File file)
    {
        return ManagerFormat.size(file.length()) + " • " + ManagerFormat.date(file.lastModified());
    }

    private boolean isImage(File file)
    {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    @Override
    protected String defaultIcon()
    {
        return "\u25a1";
    }
}