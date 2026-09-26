package net.consler.librelauncher.ui.instance.manager.specific;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.consler.librelauncher.launcher.Launcher;
import net.consler.librelauncher.ui.instance.manager.InstanceManagerController;
import net.consler.librelauncher.ui.instance.manager.specific.category.DatapackCategory;
import net.consler.librelauncher.ui.instance.manager.specific.category.InstanceCategory;
import net.consler.librelauncher.ui.instance.manager.specific.category.LogCategory;
import net.consler.librelauncher.ui.instance.manager.specific.category.ModCategory;
import net.consler.librelauncher.ui.instance.manager.specific.category.ResourcePackCategory;
import net.consler.librelauncher.ui.instance.manager.specific.category.ScreenshotCategory;
import net.consler.librelauncher.ui.instance.manager.specific.category.WorldCategory;
import net.consler.librelauncher.ui.theme.CustomDialog;
import net.consler.librelauncher.utils.ExceptionAlert;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.instance.Servers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InstanceSpecificManagerController implements Initializable
{
    private static final ExecutorService ITEM_LOADER = Executors.newFixedThreadPool(2, runnable ->
    {
        Thread thread = new Thread(runnable, "manager-item-loader");
        thread.setDaemon(true);
        return thread;
    });

    @FXML
    private StackPane contentArea;

    private String instanceName;
    private InstanceProfile profile;

    // Tracks which page is showing so the Refresh action (and the context-menu
    // Refresh item) knows how to redraw it. currentCategory is only meaningful
    // when showingServers is false.
    private InstanceCategory currentCategory;
    private boolean showingServers;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
    }

    public void setInstanceName(String instanceName)
    {
        this.instanceName = instanceName;
        this.profile = new InstanceProfile(new File(Launcher.instanceDir, instanceName).toPath());
        onWorldsSelected();
    }

    @FXML
    private void onLogsSelected()
    {
        showItems(new LogCategory());
    }

    @FXML
    private void onServersSelected()
    {
        showServers();
    }

    @FXML
    private void onWorldsSelected()
    {
        showItems(new WorldCategory());
    }

    @FXML
    private void onModsSelected()
    {
        showItems(new ModCategory());
    }

    @FXML
    private void onResourcePacksSelected()
    {
        showItems(new ResourcePackCategory());
    }

    @FXML
    private void onDatapacksSelected()
    {
        showItems(new DatapackCategory());
    }

    @FXML
    private void onScreenshotsSelected()
    {
        showItems(new ScreenshotCategory());
    }

    // ------------------------------------------------------------------
    // File-backed categories (Worlds, Mods, Resource Packs, Datapacks, Logs, Screenshots)
    // ------------------------------------------------------------------

    private void showItems(InstanceCategory category)
    {
        showingServers = false;
        currentCategory = category;

        ObservableList<File> allItems = FXCollections.observableArrayList();
        FilteredList<File> filteredItems = new FilteredList<>(allItems, f -> true);
        Map<File, ItemDetails> itemDetails = new HashMap<>();
        Set<File> loadingItems = new HashSet<>();
        String[] query = {""};

        VBox page = new VBox(12);
        page.getStyleClass().add("manager-page");

        Label countLabel = new Label(countText(0));
        countLabel.getStyleClass().add("manager-count");

        page.getChildren().add(buildHeader(category.title(), category.locationLabel(profile), countLabel));

        TextField searchField = new TextField();
        searchField.setPromptText("Filter " + category.title().toLowerCase() + "...");
        searchField.getStyleClass().add("manager-search");
        searchField.textProperty().addListener((obs, oldValue, newValue) ->
        {
            query[0] = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredItems.setPredicate(file -> matchesSearch(file, itemDetails, query[0]));
            countLabel.setText(countText(filteredItems.size()));
        });
        page.getChildren().add(searchField);

        ListView<File> itemList = new ListView<>(filteredItems);
        itemList.getStyleClass().add("manager-list");
        itemList.setPlaceholder(new Label("Loading items..."));

        setupItemContextMenu(itemList, category, itemDetails, loadingItems, filteredItems, countLabel, query);

        VBox.setVgrow(itemList, Priority.ALWAYS);
        page.getChildren().add(itemList);

        contentArea.getChildren().setAll(page);

        ITEM_LOADER.execute(() ->
        {
            List<File> visibleFiles;
            try
            {
                visibleFiles = Arrays.stream(category.items(profile))
                        .filter(file -> !file.isHidden())
                        .toList();
            }
            catch (RuntimeException e)
            {
                Platform.runLater(() ->
                {
                    if (contentArea.getChildren().contains(page))
                    {
                        itemList.setPlaceholder(new Label("Unable to load items."));
                        ExceptionAlert.show(new IOException("Unable to load " + category.title().toLowerCase(), e));
                    }
                });
                return;
            }

            Platform.runLater(() ->
            {
                if (!contentArea.getChildren().contains(page)) return;

                allItems.setAll(visibleFiles);
                itemList.setPlaceholder(new Label(category.emptyMessage()));
                filteredItems.setPredicate(file -> matchesSearch(file, itemDetails, query[0]));
                countLabel.setText(countText(filteredItems.size()));
            });
        });
    }

    private void setupItemContextMenu(ListView<File> itemList, InstanceCategory category,
                                      Map<File, ItemDetails> itemDetails, Set<File> loadingItems,
                                      FilteredList<File> filteredItems, Label countLabel, String[] query)
    {
        itemList.setCellFactory(lv -> new ListCell<>()
        {
            @Override
            protected void updateItem(File file, boolean empty)
            {
                super.updateItem(file, empty);

                if (empty || file == null)
                {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setContextMenu(null);

                    return;
                }

                ItemDetails details = itemDetails.get(file);
                if (details != null)
                {
                    renderItemCell(this, file, category, details);
                    return;
                }

                setText("Loading item...");
                setGraphic(null);
                setContextMenu(null);
                if (loadingItems.add(file))
                {
                    loadItemDetails(file, category, this, itemDetails, loadingItems,
                            filteredItems, countLabel, query);
                }
            }
        });
    }

    private void loadItemDetails(File file, InstanceCategory category, ListCell<File> cell,
                                 Map<File, ItemDetails> itemDetails, Set<File> loadingItems,
                                 FilteredList<File> filteredItems, Label countLabel, String[] query)
    {
        Task<ItemDetails> task = new Task<>()
        {
            @Override
            protected ItemDetails call()
            {
                return new ItemDetails(category.primaryText(file), category.description(file));
            }
        };

        task.setOnSucceeded(event ->
        {
            loadingItems.remove(file);
            ItemDetails details = task.getValue();
            itemDetails.put(file, details);
            filteredItems.setPredicate(candidate -> matchesSearch(candidate, itemDetails, query[0]));
            countLabel.setText(countText(filteredItems.size()));
            if (file.equals(cell.getItem())) renderItemCell(cell, file, category, details);
        });
        task.setOnFailed(event ->
        {
            loadingItems.remove(file);
            itemDetails.put(file, new ItemDetails(file.getName(), "Unable to load item details"));
            ExceptionAlert.show(new IOException("Unable to load details for " + file.getAbsolutePath(),
                    task.getException()));
            filteredItems.setPredicate(candidate -> matchesSearch(candidate, itemDetails, query[0]));
            countLabel.setText(countText(filteredItems.size()));
            if (file.equals(cell.getItem())) renderItemCell(cell, file, category, itemDetails.get(file));
        });
        ITEM_LOADER.execute(task);
    }

    private void renderItemCell(ListCell<File> cell, File file, InstanceCategory category, ItemDetails itemDetails)
    {
        boolean isLast = cell.getIndex() == cell.getListView().getItems().size() - 1;
        cell.setStyle(isLast ? "" : "-fx-border-color: transparent transparent #444444 transparent; -fx-border-width: 0 0 1 0;");

        HBox row = new HBox(12);
        row.getStyleClass().add("manager-item");
        row.setPadding(new Insets(10));

        Node icon = category.icon(file);

        VBox details = new VBox(3);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label name = new Label(itemDetails.name());
        name.getStyleClass().add("manager-item-name");
        Label description = new Label(itemDetails.description());
        description.getStyleClass().add("manager-item-description");
        details.getChildren().addAll(name, description);

        row.getChildren().addAll(icon, details);
        cell.setText(null);
        cell.setGraphic(row);

        ContextMenu contextMenu = new ContextMenu();

        MenuItem openItem = new MenuItem("Open Folder");
        openItem.setOnAction(e -> openFile(file.isDirectory() ? file : file.getParentFile()));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> deleteFile(file, category));

        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> refreshCurrentView());

        contextMenu.getItems().addAll(openItem, deleteItem, refreshItem);
        cell.setContextMenu(contextMenu);
    }

    private boolean matchesSearch(File file, Map<File, ItemDetails> itemDetails, String query)
    {
        if (query.isEmpty()) return true;
        ItemDetails details = itemDetails.get(file);
        return file.getName().toLowerCase().contains(query)
                || (details != null && details.name().toLowerCase().contains(query));
    }

    private record ItemDetails(String name, String description)
    {
    }

    private void deleteFile(File file, InstanceCategory category)
    {
        String name = category.primaryText(file);

        Alert confirmation = CustomDialog.alertDialog(
                "Delete " + name,
                "Delete: " + name,
                "Delete '" + file.getName() + "' and all of its contents? This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK)
        {
            deleteRecursively(file);
            refreshCurrentView();
        }
    }

    private void deleteRecursively(File file)
    {
        if (file.isDirectory())
        {
            File[] children = file.listFiles();
            if (children != null)
            {
                for (File child : children) deleteRecursively(child);
            }
        }
        if (!file.delete())
        {
            ExceptionAlert.show(new IOException("Unable to delete " + file.getAbsolutePath()));
        }
    }

    // ------------------------------------------------------------------
    // Servers - backed by a single servers.dat, not a folder of files
    // ------------------------------------------------------------------

    private void showServers()
    {
        showingServers = true;

        File serversFile = profile.getFolder().resolve("servers.dat").toFile();
        Servers servers = new Servers(serversFile);

        ObservableList<Servers.ServerEntry> entries = FXCollections.observableArrayList();
        try
        {
            entries.addAll(servers.getServers());
        }
        catch (Exception e)
        {
            ExceptionAlert.show(new IOException("Unable to read servers.dat", e));
        }

        VBox page = new VBox(12);
        page.getStyleClass().add("manager-page");

        Label countLabel = new Label(countText(entries.size()));
        countLabel.getStyleClass().add("manager-count");

        page.getChildren().add(buildHeader("Servers", serversFile.getAbsolutePath(), countLabel));

        ListView<Servers.ServerEntry> serverList = new ListView<>(entries);
        serverList.getStyleClass().add("manager-list");
        serverList.setPlaceholder(new Label("No servers found."));

        setupServerContextMenu(serverList, servers, entries);

        VBox.setVgrow(serverList, Priority.ALWAYS);
        page.getChildren().add(serverList);

        contentArea.getChildren().setAll(page);
    }

    private void setupServerContextMenu(ListView<Servers.ServerEntry> serverList, Servers servers,
                                        ObservableList<Servers.ServerEntry> entries)
    {
        serverList.setCellFactory(lv -> new ListCell<>()
        {
            @Override
            protected void updateItem(Servers.ServerEntry entry, boolean empty)
            {
                super.updateItem(entry, empty);

                if (empty || entry == null)
                {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setContextMenu(null);

                    return;
                }

                boolean isLast = getIndex() == getListView().getItems().size() - 1;
                setStyle(isLast ? "" : "-fx-border-color: transparent transparent #444444 transparent; -fx-border-width: 0 0 1 0;");

                HBox row = new HBox(12);
                row.getStyleClass().add("manager-item");
                row.setPadding(new Insets(10));

                BufferedImage iconImage = entry.getIconImage();
                Node icon = ManagerFormat.imageIcon(iconImage, iconImage != null, "\u2302");

                VBox details = new VBox(3);
                HBox.setHgrow(details, Priority.ALWAYS);
                Label name = new Label(entry.name == null || entry.name.isBlank() ? "(Unnamed server)" : entry.name);
                name.getStyleClass().add("manager-item-name");
                Label description = new Label(entry.ip + (entry.hidden ? " • Hidden from list" : ""));
                description.getStyleClass().add("manager-item-description");
                details.getChildren().addAll(name, description);

                row.getChildren().addAll(icon, details);
                setText(null);
                setGraphic(row);

                ContextMenu contextMenu = new ContextMenu();

                MenuItem copyItem = new MenuItem("Copy Address");
                copyItem.setOnAction(e ->
                {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(entry.ip == null ? "" : entry.ip);
                    Clipboard.getSystemClipboard().setContent(content);
                });

                MenuItem removeItem = new MenuItem("Remove Server");
                removeItem.setOnAction(e -> removeServer(servers, entries, entry));

                MenuItem refreshItem = new MenuItem("Refresh");
                refreshItem.setOnAction(e -> refreshCurrentView());

                contextMenu.getItems().addAll(copyItem, removeItem, refreshItem);
                setContextMenu(contextMenu);
            }
        });
    }

    private void removeServer(Servers servers, ObservableList<Servers.ServerEntry> entries, Servers.ServerEntry entry)
    {
        String name = (entry.name == null || entry.name.isBlank()) ? entry.ip : entry.name;

        Alert confirmation = CustomDialog.alertDialog(
                "Remove " + name,
                "Remove Server: " + name,
                "Remove '" + name + "' from the server list? This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK)
        {
            List<Servers.ServerEntry> remaining = new ArrayList<>(entries);
            remaining.remove(entry);

            try
            {
                servers.saveServers(remaining);
            }
            catch (Exception e)
            {
                ExceptionAlert.show(new IOException("Unable to save servers.dat", e));
            }
            refreshCurrentView();
        }
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    private HBox buildHeader(String title, String location, Label countLabel)
    {
        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label locationLabel = new Label(location);
        locationLabel.getStyleClass().add("manager-location");
        titleBox.getChildren().addAll(titleLabel, locationLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshCurrentView());

        HBox header = new HBox(10, titleBox, countLabel, refreshButton);
        header.getStyleClass().add("manager-header");
        return header;
    }

    private String countText(int count)
    {
        return count + (count == 1 ? " item" : " items");
    }

    private void refreshCurrentView()
    {
        if (showingServers) showServers();
        else if (currentCategory != null) showItems(currentCategory);
    }

    private void openFile(File file)
    {
        if (file == null || !file.exists()) return;
        InstanceManagerController.hostServices.showDocument(file.getAbsolutePath());
    }
}