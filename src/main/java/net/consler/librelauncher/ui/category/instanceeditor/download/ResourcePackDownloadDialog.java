package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.category.instanceeditor.InstanceResourcePacks;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;
import net.consler.librelauncherlib.integration.modrinth.ProjectType;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ResourcePackDownloadDialog extends ModrinthDownloadDialog
{

    public ResourcePackDownloadDialog(Main main, Instance instance)
    {
        super(main, instance);
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
    protected CompletableFuture<?> downloadProject(ModrinthProject project, ModrinthVersion version, Path destination, String userAgent)
    {
        return project.downloadAsResourcePack(version, destination, userAgent);
    }

    @Override
    protected void onDownloadsComplete()
    {
        InstanceResourcePacks.getInstance().populate(instance);
    }

    @Override
    protected boolean projectExists(ModrinthProject project)
    {
        InstanceResourcePacks resourcePacks = InstanceResourcePacks.getInstance();
        return resourcePacks != null
                && (resourcePacks.doesExist(project.slug())
                || resourcePacks.doesExist(project.title())
                || resourcePacks.doesExist(project.id()));
    }
}
