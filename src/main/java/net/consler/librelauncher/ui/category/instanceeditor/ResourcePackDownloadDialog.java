package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;
import net.consler.librelauncherlib.integration.modrinth.ProjectType;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ResourcePackDownloadDialog extends ModrinthDownloadDialog
{
    private final InstanceResourcePacks instanceResourcePacks;

    public ResourcePackDownloadDialog(Main main, InstanceProfile instance, InstanceResourcePacks instanceResourcePacks)
    {
        super(main, instance);
        this.instanceResourcePacks = instanceResourcePacks;
    }

    @Override
    protected ProjectType projectType()
    {
        return ProjectType.RESOURCE_PACK;
    }

    @Override
    protected String itemNamePlural()
    {
        return "Resource Packs";
    }

    @Override
    protected Path downloadDirectory()
    {
        return instance.getResourcePackFolder();
    }

    @Override
    protected CompletableFuture<?> downloadProject(
            ModrinthProject project, ModrinthVersion version, Path destination, String userAgent)
    {
        return project.downloadAsResourcePack(version, destination, userAgent);
    }

    @Override
    protected void onDownloadsComplete()
    {
        if (instanceResourcePacks != null) instanceResourcePacks.populate(instance);
    }
}
