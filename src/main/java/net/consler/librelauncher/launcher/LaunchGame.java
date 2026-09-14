package net.consler.librelauncher.launcher;

import javafx.application.Platform;
import net.consler.librelauncher.exceptions.FailedToLaunchMinecraftException;
import net.consler.librelauncher.launcher.auth.Authorization;
import fr.flowarg.openlauncherlib.NoFramework;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.minecraft.GameFolder;
import net.consler.librelauncher.ui.client.ClientController;
import net.consler.librelauncher.ui.settings.SettingsSaver;
import net.consler.librelauncher.utils.ExceptionAlert;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class LaunchGame
{

    public static void launch(String name, String version, String modLoader, String loaderVersion)
    {
        Path gameDirPath = new File(Launcher.instanceDir, name).toPath();

        new Thread(() ->
        {
            try
            {
                AuthInfos sessionAuth = Authorization.authorize();

                NoFramework noFramework = new NoFramework(gameDirPath, sessionAuth, GameFolder.FLOW_UPDATER);

                int allocatedRamMb = SettingsSaver.getIntSetting("allocated_ram", 2048);
                allocatedRamMb = Math.max(256, allocatedRamMb);

                ArrayList<String> additionalVmArgs = new ArrayList<>();
                additionalVmArgs.add("-Xms" + allocatedRamMb + "M");
                additionalVmArgs.add("-Xmx" + allocatedRamMb + "M");
                noFramework.setAdditionalVmArgs(additionalVmArgs);

                String closeOnLaunch = SettingsSaver.getSetting("close_on_launch");
                boolean shouldClose = closeOnLaunch == null || closeOnLaunch.isBlank() || Boolean.parseBoolean(closeOnLaunch);
                if (shouldClose && ClientController.primaryStage != null)
                {
                    Platform.runLater(() -> ClientController.primaryStage.hide());
                }

                switch(modLoader)
                {
                    case "Vanilla" -> noFramework.launch(version, loaderVersion, NoFramework.ModLoader.VANILLA);
                    case "Fabric" -> noFramework.launch(version, loaderVersion, NoFramework.ModLoader.FABRIC);
                    case "Forge" -> noFramework.launch(version, loaderVersion, NoFramework.ModLoader.FORGE);
                    case "Neoforge" -> noFramework.launch(version, loaderVersion, NoFramework.ModLoader.NEO_FORGE);
                    default -> throw new FailedToLaunchMinecraftException(modLoader + " is not a mod loader!");
                }
            }
            catch (Exception e)
            {
                Platform.runLater(() -> ExceptionAlert.show(new FailedToLaunchMinecraftException(e.getMessage())));
            }
        }).start();
    }
}
