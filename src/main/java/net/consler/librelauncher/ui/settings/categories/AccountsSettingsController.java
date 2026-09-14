package net.consler.librelauncher.ui.settings.categories;

import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.consler.librelauncher.launcher.auth.AuthInfosSaver;
import net.consler.librelauncher.launcher.auth.Authorization;
import net.consler.librelauncher.ui.theme.CustomDialog;

import java.util.Optional;
import java.util.UUID;

public class AccountsSettingsController
{

    @FXML private ListView<String> accountList;
    private final ObservableList<String> accounts = FXCollections.observableArrayList();

    @FXML
    public void initialize()
    {
        accountList.setItems(accounts);
        accountList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setupContextMenu();
        loadAccounts();

        String currentActiveAccount = AuthInfosSaver.getActiveAccount();
        if (currentActiveAccount != null && !currentActiveAccount.isBlank() && accounts.contains(currentActiveAccount)) accountList.getSelectionModel().select(currentActiveAccount);
        else if (!accounts.isEmpty()) accountList.getSelectionModel().selectFirst();
    }

    @FXML
    private void onAddMicrosoftAccount()
    {
        new Thread(() ->
        {
            try
            {
                AuthInfos authInfos = Authorization.authorizeWithWebView();
                String username = authInfos.getUsername() == null || authInfos.getUsername().isBlank() ? authInfos.getUuid() : authInfos.getUsername();

                Platform.runLater(() ->
                {
                    if (AuthInfosSaver.listAccountNames().contains(username))
                    {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "An account with this name already exists.");
                        alert.showAndWait();
                        return;
                    }

                    AuthInfosSaver.saveAuthInfos(username, authInfos);
                    AuthInfosSaver.setActiveAccount(username);
                    loadAccounts();
                    accountList.getSelectionModel().select(username);
                });
            }
            catch (Exception e)
            {
                Platform.runLater(() ->
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add Microsoft account: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }, "ms-auth-thread").start();
    }

    @FXML
    private void onAddOfflineAccount()
    {
        TextInputDialog dialog = CustomDialog.make("Add Offline Account", "Create an offline Minecraft profile", "Username:", "Player");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name ->
        {
            String username = name.trim();
            if (username.isEmpty() || !username.matches("[A-Za-z0-9_]+"))
            {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Offline usernames may only contain letters, numbers, and underscores.");
                alert.showAndWait();
                return;
            }

            if (AuthInfosSaver.listAccountNames().contains(username))
            {
                Alert alert = new Alert(Alert.AlertType.WARNING, "An account with this name already exists.");
                alert.showAndWait();
                return;
            }

            AuthInfosSaver.saveAuthInfos(username, new AuthInfos(username, "0", UUID.nameUUIDFromBytes((username).getBytes()).toString()));
            AuthInfosSaver.setActiveAccount(username);
            loadAccounts();
            accountList.getSelectionModel().select(username);
        });
    }

    private void loadAccounts()
    {
        accounts.setAll(AuthInfosSaver.listAccountNames());
    }

    private void setupContextMenu()
    {
        accountList.setCellFactory(listView -> new ListCell<>()
        {
            private final ContextMenu contextMenu = new ContextMenu();

            {
                MenuItem setActiveItem = new MenuItem("Set Active");
                setActiveItem.setOnAction(event ->
                {
                    String selected = getItem();
                    if (selected != null)
                    {
                        AuthInfosSaver.setActiveAccount(selected);
                        accountList.refresh();
                    }
                });

                MenuItem removeItem = new MenuItem("Remove");
                removeItem.setOnAction(event ->
                {
                    String selected = getItem();
                    if (selected != null)
                    {
                        AuthInfosSaver.deleteAccount(selected);
                        loadAccounts();
                    }
                });

                contextMenu.getItems().addAll(setActiveItem, removeItem);
            }

            @Override
            protected void updateItem(String item, boolean empty)
            {
                super.updateItem(item, empty);
                if (empty || item == null)
                {
                    setText(null);
                    setContextMenu(null);
                    return;
                }

                String activeAccount = AuthInfosSaver.getActiveAccount();
                setText((item.equals(activeAccount) ? "● " : "  ") + item);
                setContextMenu(contextMenu);
            }
        });
    }
}