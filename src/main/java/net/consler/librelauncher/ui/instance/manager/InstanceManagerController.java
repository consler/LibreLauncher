package net.consler.librelauncher.ui.instance.manager;

import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.ui.client.ClientController;
import net.consler.librelauncher.ui.instance.create.NewInstanceApplication;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import net.consler.librelauncher.ui.theme.CustomDialog;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import static net.consler.librelauncher.Main.APPDATA_DIR;

public class InstanceManagerController implements Initializable
{
    private static InstanceManagerController instance;

    public static InstanceManagerController getInstance()
    {
        return instance;
    }

    public static HostServices hostServices;

    @FXML
    private ListView<String> instanceList;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        loadInstances();
        setupContextMenu();
    }

    public void loadInstances()
    {
        instance = this;

        instanceList.getItems().clear();

        if (APPDATA_DIR.exists() && APPDATA_DIR.isDirectory())
        {
            File[] files = APPDATA_DIR.listFiles((dir, name) -> name.endsWith(".properties"));

            if (files != null)
            {
                for (File file : files)
                {
                    String fileName = file.getName();
                    instanceList.getItems().add(fileName.substring(0, fileName.length() - 11));
                }
            }
        }
    }

    private void setupContextMenu()
    {
        instanceList.setCellFactory(lv -> new ListCell<>()
        {
            @Override
            protected void updateItem(String item, boolean empty)
            {
                super.updateItem(item, empty);

                if (empty || item == null)
                {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setContextMenu(null);
                }
                else
                {
                    setText(item);

                    boolean isLast = getIndex() == getListView().getItems().size() - 1;
                    setStyle(isLast
                            ? ""
                            : "-fx-border-color: transparent transparent #444444 transparent; -fx-border-width: 0 0 1 0;");

                    ContextMenu contextMenu = new ContextMenu();

                    MenuItem launchItem = new MenuItem("Launch");
                    launchItem.setOnAction(e -> Launcher.launch(item));

                    MenuItem renameItem = new MenuItem("Rename");
                    renameItem.setOnAction(e -> renameInstance(item));

                    MenuItem deleteItem = new MenuItem("Delete");
                    deleteItem.setOnAction(e -> deleteInstance(item));

                    MenuItem openFolderItem = new MenuItem("Open Folder");
                    openFolderItem.setOnAction(e -> openInstanceFolder(item));

                    contextMenu.getItems().addAll(launchItem, renameItem, deleteItem, openFolderItem);
                    setContextMenu(contextMenu);
                }
            }
        });
    }

    private ContextMenu contextMenuFor(String item)
    {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem launchItem = new MenuItem("Launch");
        launchItem.setOnAction(e -> Launcher.launch(item));

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> renameInstance(item));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> deleteInstance(item));

        MenuItem openFolderItem = new MenuItem("Open Folder");
        openFolderItem.setOnAction(e -> openInstanceFolder(item));

        contextMenu.getItems().addAll(launchItem, renameItem, deleteItem, openFolderItem);
        return contextMenu;
    }


    private void renameInstance(String oldName)
    {
        TextInputDialog dialog = CustomDialog.textInputDialog("Rename Instance", "Rename: " + oldName, "New name: ", "");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName ->
        {
            new File(APPDATA_DIR, oldName + ".properties").renameTo(new File(APPDATA_DIR, newName + ".properties"));
            new File(Launcher.instanceDir, oldName).renameTo(new File(Launcher.instanceDir, newName));

            refreshAllLists(newName);
        });
    }

    private void deleteInstance(String name)
    {
        Alert alert = CustomDialog.alertDialog("Delete Instance", "Delete: " + name, "Are you sure? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK)
        {
            File propertiesFile = new File(APPDATA_DIR, name + ".properties");
            if (propertiesFile.exists()) propertiesFile.delete();

            File gameDir = new File(Launcher.instanceDir, name);
            deleteDirectory(gameDir);

            refreshAllLists(null);
        }
    }

    private void deleteDirectory(File dir)
    {
        if (dir != null && dir.exists())
        {
            File[] files = dir.listFiles();
            if (files != null)
            {
                for (File file : files)
                {
                    if (file.isDirectory()) deleteDirectory(file);
                    else file.delete();
                }
            }
            dir.delete();
        }
    }

    private void openInstanceFolder(String instanceName)
    {
        hostServices.showDocument(new File(Launcher.instanceDir, instanceName).getAbsolutePath());
    }

    private void refreshAllLists(String instanceToSelect)
    {
        loadInstances();
        if (ClientController.getInstance() != null)
        {
            ClientController.getInstance().loadInstances(instanceToSelect);
        }
    }

    @FXML
    void newInstanceButtonClicked() throws IOException
    {
        NewInstanceApplication.open();
    }

    @FXML
    void openFolderButtonClicked()
    {
        hostServices.showDocument(Launcher.instanceDir);
    }

}