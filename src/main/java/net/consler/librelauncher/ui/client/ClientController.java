package net.consler.librelauncher.ui.client;

import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.launcher.auth.Authorization;
import net.consler.librelauncher.ui.instance.create.NewInstanceApplication;
import net.consler.librelauncher.ui.instance.manager.InstanceManagerApplication;
import net.consler.librelauncher.ui.settings.SettingsApplication;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static net.consler.librelauncher.Main.APPDATA_DIR;

public class ClientController implements Initializable
{
    private static ClientController instance;

    public static HostServices hostServices;
    public static javafx.stage.Stage primaryStage;

    @FXML
    private ChoiceBox<String> launchChoiceBox;

    public static ClientController getInstance()
    {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        instance = this;
        loadInstances(null);
    }

    public void loadInstances(String instanceToSelect)
    {
        launchChoiceBox.getItems().clear();

        if (APPDATA_DIR.exists() && APPDATA_DIR.isDirectory())
        {
            File[] files = APPDATA_DIR.listFiles((dir, name) -> name.endsWith(".properties"));

            if (files != null)
            {
                List<String> instanceNames = new ArrayList<>();
                for (File file : files)
                {
                    String fileName = file.getName();
                    instanceNames.add(fileName.substring(0, fileName.length() - 11));
                }

                launchChoiceBox.getItems().addAll(instanceNames);

                if (instanceToSelect != null && instanceNames.contains(instanceToSelect))
                {
                    launchChoiceBox.getSelectionModel().select(instanceToSelect);
                }
                else if (!instanceNames.isEmpty())
                {
                    launchChoiceBox.getSelectionModel().selectFirst();
                }
            }
        }
    }

    @FXML
    void launchButtonClicked()
    {
        String selectedInstance = launchChoiceBox.getValue();
        if (selectedInstance != null && !selectedInstance.isEmpty())
        {
            Launcher.launch(selectedInstance);
        }
    }

    @FXML
    void instanceManageButtonClicked() throws IOException
    {
        InstanceManagerApplication.open();
    }

    @FXML
    void newInstanceButtonClicked() throws IOException
    {
        NewInstanceApplication.open();
    }

    @FXML
    void githubPageButtonClicked()
    {
        if (hostServices != null)
        {
            hostServices.showDocument("https://github.com/consler/ConslerClient");
        }
    }

    @FXML
    void openSettingsButtonClicked() throws IOException
    {
        SettingsApplication.open();
    }
}