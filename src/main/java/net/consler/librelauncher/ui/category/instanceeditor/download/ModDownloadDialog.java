package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.category.instanceeditor.InstanceMods;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;
import net.consler.librelauncherlib.integration.modrinth.ProjectType;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ModDownloadDialog extends ModrinthDownloadDialog
{

    public ModDownloadDialog(Main main, InstanceProfile instance)
    {
        super(main, instance);
    }

    @Override
    protected ProjectType projectType()
    {
        return ProjectType.MOD;
    }

    @Override
    protected String itemNamePlural()
    {
        return "Mods";
    }

    @Override
    protected Path downloadDirectory()
    {
        return instance.getModsFolder();
    }

    @Override
    protected CompletableFuture<?> downloadProject(ModrinthProject project, ModrinthVersion version, Path destination, String userAgent)
    {
        return project.downloadAsMod(version, destination, userAgent);
    }

    @Override
    protected void onDownloadsComplete()
    {
        InstanceMods.getInstance().populate(instance);
    }

    @Override
    protected boolean filterByModloader()
    {
        return true;
    }

}
