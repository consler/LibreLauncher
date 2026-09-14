package net.consler.librelauncher.launcher.instance;

import net.consler.librelauncher.launcher.Launcher;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.VanillaVersion;
import fr.flowarg.flowupdater.versions.fabric.FabricVersion;
import fr.flowarg.flowupdater.versions.fabric.FabricVersionBuilder;
import fr.flowarg.flowupdater.versions.forge.ForgeVersion;
import fr.flowarg.flowupdater.versions.forge.ForgeVersionBuilder;
import fr.flowarg.flowupdater.versions.neoforge.NeoForgeVersion;
import fr.flowarg.flowupdater.versions.neoforge.NeoForgeVersionBuilder;

import java.io.File;
import java.nio.file.Path;

public class Downloader
{
    public static void download(String name, String version, String modLoader, String loaderVersion) throws Exception
    {
        Path gameDirPath = new File(Launcher.instanceDir, name).toPath();

        VanillaVersion vv = new VanillaVersion.VanillaVersionBuilder().withName(version).build();
        FlowUpdater.FlowUpdaterBuilder fub = new FlowUpdater.FlowUpdaterBuilder().withVanillaVersion(vv);

        switch(modLoader)
        {
            case "Vanilla" ->
            {
            }
            case "Fabric" ->
            {
                FabricVersion fv = new FabricVersionBuilder().withFabricVersion(loaderVersion).build();
                fub.withModLoaderVersion(fv);
            }
            case "Forge" ->
            {
                ForgeVersion fv = new ForgeVersionBuilder().withForgeVersion(loaderVersion).build();
                fub.withModLoaderVersion(fv);
            }
            case "Neoforge" ->
            {
                NeoForgeVersion nfv = new NeoForgeVersionBuilder().withNeoForgeVersion(loaderVersion).build();
                fub.withModLoaderVersion(nfv);
            }
            default -> throw new IllegalArgumentException("Unsupported mod loader: " + modLoader);
        }

        InstanceInfo.save(name, version, modLoader, loaderVersion);

        fub.build().update(gameDirPath);

    }
}
