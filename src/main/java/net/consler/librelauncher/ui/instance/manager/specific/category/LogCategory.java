package net.consler.librelauncher.ui.instance.manager.specific.category;

import net.consler.librelauncher.ui.instance.manager.specific.ManagerFormat;
import net.consler.librelauncherlib.instance.InstanceProfile;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class LogCategory extends InstanceCategory
{
    @Override
    public String title()
    {
        return "Logs";
    }

    @Override
    public File directory(InstanceProfile profile)
    {
        return profile.getLogsFolder().toFile();
    }

    @Override
    public File[] items(InstanceProfile profile)
    {
        File[] files = super.items(profile);
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files;
    }

    @Override
    public String primaryText(File file)
    {
        return file.getName();
    }

    @Override
    public String description(File file)
    {
        String current = file.getName().equalsIgnoreCase("latest.log") ? "Current session • " : "";
        return current + ManagerFormat.size(file.length()) + " • " + ManagerFormat.date(file.lastModified());
    }

    @Override
    protected String defaultIcon()
    {
        return "\u2261";
    }
}