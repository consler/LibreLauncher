package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;
import net.consler.librelauncherlib.integration.modrinth.ProjectType;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ModDownloadDialog extends ModrinthDownloadDialog
{
    private final InstanceMods instanceMods;

    public ModDownloadDialog(Main main, InstanceProfile instance, InstanceMods instanceMods)
    {
        super(main, instance);
        this.instanceMods = instanceMods;
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
    protected CompletableFuture<?> downloadProject(
            ModrinthProject project, ModrinthVersion version, Path destination, String userAgent)
    {
        return project.downloadAsMod(version, destination, userAgent);
    }

    @Override
    protected void onDownloadsComplete()
    {
        if (instanceMods != null) instanceMods.populate(instance);
    }

    @Override
    protected boolean filterByModloader()
    {
        return true;
    }
}
