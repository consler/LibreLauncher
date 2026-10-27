package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.category.instanceeditor.InstanceMods;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.MrpackInstaller;
import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;
import net.consler.librelauncherlib.integration.modrinth.ProjectType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

public class ModpackDownloadDialog extends ModrinthDownloadDialog
{

    public ModpackDownloadDialog(Main main, InstanceProfile instance)
    {
        super(main, instance);
    }

    @Override
    protected ProjectType projectType()
    {
        return ProjectType.MODPACK;
    }

    @Override
    protected String itemNamePlural()
    {
        return "Modpacks";
    }

    @Override
    protected Path downloadDirectory() throws IOException
    {
        return Files.createTempDirectory("librelauncher-modrinth-modpack-");
    }

    @Override
    protected CompletableFuture<?> downloadProject(
            ModrinthProject project, ModrinthVersion version, Path destination, String userAgent)
    {
        String gameVersion = new InstancesConfig().getProperty(instance.getName(), "version");
        return project.downloadAsModpack(version, destination, userAgent)
                .thenApplyAsync(pack ->
                {
                    String packGameVersion = pack.getDependencies().get("minecraft");
                    if (packGameVersion != null && !packGameVersion.equals(gameVersion))
                    {
                        CompletionException ex = new CompletionException(new IllegalStateException("Modpack requires Minecraft " + packGameVersion + ", but this instance uses " + gameVersion));
                        new ErrorDialog(main.client, ex, "installing modpack");
                        throw ex;
                    }
                    try
                    {
                        return new MrpackInstaller(pack).installTo(instance.getFolder(), MrpackInstaller.Side.CLIENT);
                    }
                    catch (IOException e)
                    {
                        new ErrorDialog(main.client, e, "installing modpack");
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    protected boolean allowMultipleSelection()
    {
        return false;
    }

    @Override
    protected boolean showProgressBar()
    {
        return false;
    }

    @Override
    protected String dialogTitle()
    {
        return "Install Modpack";
    }

    @Override
    protected String actionButtonText()
    {
        return "Install";
    }

    @Override
    protected String actionProgressText()
    {
        return "Installing modpack...";
    }

    @Override
    protected String completedItemText()
    {
        return "modpack installed";
    }

    @Override
    protected String completionStatusText()
    {
        return "Modpack installed.";
    }

    @Override
    protected void onDownloadsComplete()
    {
        InstanceMods.getInstance().populate(instance);
    }

    @Override
    protected void cleanupDownloadDirectory(Path destination) throws IOException
    {
        try (Stream<Path> paths = Files.walk(destination))
        {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
            {
                Files.deleteIfExists(path);
            }
        }
    }
}
