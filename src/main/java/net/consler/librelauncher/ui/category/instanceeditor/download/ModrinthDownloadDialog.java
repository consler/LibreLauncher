package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.integration.modrinth.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ModrinthDownloadDialog
{
    private static final String USER_AGENT = "consler/LibreLauncher";
    private static final int SEARCH_PAGE_SIZE = 20;

    protected final Main main;
    protected final Instance instance;

    public JDialog dialog;

    private JTextField searchField;
    private JButton downloadButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JPanel resultsPanel;
    private JScrollPane resultsScrollPane;
    private ModrinthDescriptionView modrinthDescriptionView;
    private JSplitPane resultsAndDescription;
    private ModrinthVersionSelector versionSelector;
    private ModrinthProject selectedDescriptionProject;
    private SwingWorker<SearchPage, Void> searchWorker;
    private final List<SwingWorker<Void, Void>> iconWorkers = new ArrayList<>();
    private final List<ModrinthProject> selectedProjects = new ArrayList<>();
    private final ConcurrentMap<String, CompletableFuture<ModrinthProject>> projectDetails = new ConcurrentHashMap<>();
    private ButtonGroup selectionGroup;
    private String currentQuery = "";
    private int nextSearchOffset;
    private boolean loadingSearchPage;
    private boolean hasMoreSearchResults = true;
    private volatile Set<String> installedModHashes;
    private final ConcurrentMap<String, CompletableFuture<Boolean>> installedProjectChecks = new ConcurrentHashMap<>();

    protected ModrinthDownloadDialog(Main main, Instance instance)
    {
        this.main = main;
        this.instance = instance;
    }

    protected abstract ModrinthProjectType projectType();
    protected abstract String itemNamePlural();
    protected abstract Path downloadDirectory() throws IOException;
    protected abstract CompletableFuture<?> downloadProject(
            ModrinthProject project, ModrinthVersion version, Path destination, String userAgent);
    protected abstract void onDownloadsComplete();

    protected boolean filterByModloader()
    {
        return false;
    }

    protected boolean projectExists(ModrinthProject project)
    {
        return false;
    }

    protected boolean allowMultipleSelection()
    {
        return true;
    }

    protected boolean showProgressBar()
    {
        return true;
    }

    protected String dialogTitle()
    {
        return "Download " + itemNamePlural();
    }

    protected String actionButtonText()
    {
        return "Download";
    }

    protected String actionProgressText()
    {
        return "Downloading " + itemNamePlural().toLowerCase() + "...";
    }

    protected String completedItemText()
    {
        return itemNamePlural().toLowerCase() + " downloaded";
    }

    protected String completionStatusText()
    {
        return "All " + completedItemText() + ".";
    }

    protected void cleanupDownloadDirectory(Path destination) throws IOException
    {
    }

    public void open()
    {
        String title = dialogTitle();
        dialog = new JDialog(main.client, title, true);
        dialog.setUndecorated(true);
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        dialog.add(main.titlebar.create(dialog, title, false, dialog::dispose), BorderLayout.NORTH);
        dialog.add(createContent(), BorderLayout.CENTER);
        dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 20, 20));
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                cancelWorkers();
            }
        });
        dialog.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 20, 20));
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(main.client);
        searchField.requestFocusInWindow();
        searchField.setToolTipText("Search for " + itemNamePlural().toLowerCase());
        dialog.setVisible(true);
    }

    private JPanel createContent()
    {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        content.setPreferredSize(new Dimension(980, 500));

        searchField = new JTextField();
        searchField.addActionListener(e -> search(searchField.getText().trim()));
        ThemeManager.silentField(searchField);
        content.add(searchField, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setOpaque(false);
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));

        resultsScrollPane = new JScrollPane(resultsPanel);
        resultsScrollPane.setOpaque(false);
        resultsScrollPane.getViewport().setOpaque(false);
        resultsScrollPane.setBorder(null);
        resultsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        resultsScrollPane.getVerticalScrollBar().getModel().addChangeListener(e ->
                SwingUtilities.invokeLater(this::loadNextPageAtScrollEnd));

        versionSelector = new ModrinthVersionSelector(
                this::loadProjectVersions, this::showVersionLoadingError);

        JPanel resultsSide = new JPanel(new BorderLayout(0, 8));
        resultsSide.setOpaque(false);
        resultsSide.add(resultsScrollPane, BorderLayout.CENTER);
        resultsSide.add(versionSelector, BorderLayout.SOUTH);

        modrinthDescriptionView = new ModrinthDescriptionView();
        resultsAndDescription = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resultsSide, null);
        resultsAndDescription.setResizeWeight(1.0);
        resultsAndDescription.setContinuousLayout(true);
        resultsAndDescription.setBorder(null);
        resultsAndDescription.setOpaque(false);
        content.add(resultsAndDescription, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusLabel = new JLabel(" ");
        JPanel progressPanel = new JPanel(new BorderLayout(0, 6));
        progressPanel.setOpaque(false);
        progressPanel.add(statusLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e ->
        {
            cancelWorkers();
            dialog.dispose();
        });

        downloadButton = new JButton(actionButtonText());
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e ->
        {
            downloadButton.setEnabled(false);
            downloadSelected();
        });
        selectionGroup = new ButtonGroup();

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(downloadButton);

        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setOpaque(false);
        footer.add(progressPanel, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);

        search("");
        return content;
    }

    private void search(String query)
    {
        if (searchWorker != null) searchWorker.cancel(true);
        cancelIconWorkers();
        selectedProjects.clear();
        versionSelector.clear();
        selectedDescriptionProject = null;
        if (modrinthDescriptionView != null) modrinthDescriptionView.clear();
        selectionGroup = new ButtonGroup();
        downloadButton.setEnabled(false);
        resultsPanel.removeAll();
        nextSearchOffset = 0;
        loadingSearchPage = false;
        hasMoreSearchResults = true;
        currentQuery = query;
        loadSearchPage(query, 0);
    }

    private void loadNextPageAtScrollEnd()
    {
        if (!dialog.isVisible() || resultsScrollPane == null) return;
        JScrollBar scrollBar = resultsScrollPane.getVerticalScrollBar();
        if (scrollBar.getValue() + scrollBar.getVisibleAmount() >= scrollBar.getMaximum() - 32)
            loadSearchPage(currentQuery, nextSearchOffset);
    }

    private void loadSearchPage(String query, int offset)
    {
        if (loadingSearchPage || !hasMoreSearchResults) return;
        loadingSearchPage = true;
        statusLabel.setText(offset == 0 ? "Searching Modrinth..." : "Loading more...");
        searchWorker = new SwingWorker<>()
        {
            @Override
            protected SearchPage doInBackground() throws Exception
            {
                ModrinthSearchQuery searchQuery = new ModrinthSearchQuery(query)
                        .projectType(projectType())
                        .gameVersion(new InstancesConfig().getProperty(instance.getName(), "version"))
                        .limit(SEARCH_PAGE_SIZE)
                        .offset(offset);
                if (filterByModloader()) searchQuery.category(instance.getModloader());
                List<ModrinthProject> projects = Modrinth.search(searchQuery, USER_AGENT).get().hits();
                Set<String> installedProjectIds = new HashSet<>();
                if (projectType() == ModrinthProjectType.MOD)
                {
                    Set<String> installedHashes = getInstalledModHashes();
                    if (!installedHashes.isEmpty())
                    {
                        List<CompletableFuture<Boolean>> projectChecks = projects.stream()
                                .map(project -> installedProjectChecks.computeIfAbsent(project.id(), projectId ->
                                        versionSelector.versionsFor(project)
                                                .thenApply(versions ->
                                                        InstalledModHashes.containsAnyVersion(versions, installedHashes))
                                                .whenComplete((result, error) ->
                                                {
                                                    if (error != null) installedProjectChecks.remove(projectId);
                                                })))
                                .toList();
                        CompletableFuture.allOf(projectChecks.toArray(CompletableFuture[]::new)).get();
                        for (int i = 0; i < projects.size(); i++)
                        {
                            if (projectChecks.get(i).get()) installedProjectIds.add(projects.get(i).id());
                        }
                    }
                }
                return new SearchPage(projects, installedProjectIds);
            }

            @Override
            protected void done()
            {
                if (searchWorker != this || isCancelled()) return;
                loadingSearchPage = false;
                try
                {
                    SearchPage page = get();
                    List<ModrinthProject> projects = page.projects();
                    if (offset == 0 && projects.isEmpty())
                    {
                        JLabel label = new JLabel("No " + itemNamePlural().toLowerCase() + " found.");
                        label.setBorder(new EmptyBorder(12, 8, 12, 8));
                        resultsPanel.add(label);
                        statusLabel.setText(" ");
                        resultsPanel.revalidate();
                        resultsPanel.repaint();
                        return;
                    }

                    List<ProjectRow> newRows = new ArrayList<>();
                    for (ModrinthProject project : projects)
                    {
                        ProjectRow row = addProjectRow(project, page.installedProjectIds().contains(project.id()));
                        newRows.add(row);
                    }
                    nextSearchOffset = offset + projects.size();
                    hasMoreSearchResults = projects.size() == SEARCH_PAGE_SIZE;
                    loadIcons(newRows);
                    statusLabel.setText(hasMoreSearchResults ? " " : "");
                    resultsPanel.revalidate();
                    resultsPanel.repaint();
                    loadNextPageAtScrollEnd();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    hasMoreSearchResults = false;
                    statusLabel.setText(offset == 0 ? "Search failed." : "Loading more results failed.");
                    new ErrorDialog(main.client, e, "searching Modrinth");
                }
                catch (CancellationException ignored)
                {
                }
            }
        };
        searchWorker.execute();
    }

    private ProjectRow addProjectRow(ModrinthProject project, boolean installedByHash)
    {
        boolean exists = installedByHash || projectExists(project);

        ModrinthDownloadDialogItem row = new ModrinthDownloadDialogItem(
                project,
                exists,
                allowMultipleSelection(),
                ThemeManager.surfaceColor(dialog.getBackground()),
                ThemeManager.outlineColor(dialog.getBackground()),
                selected ->
                {
                    if (selected.isSelected())
                    {
                        if (!allowMultipleSelection()) selectedProjects.clear();
                        if (!selectedProjects.contains(project)) selectedProjects.add(project);
                    }
                    else
                    {
                        selectedProjects.remove(project);
                    }
                    downloadButton.setEnabled(!selectedProjects.isEmpty());
                    if (projectType() == ModrinthProjectType.MOD)
                    {
                        if (selected.isSelected()) versionSelector.selectProject(project);
                        else versionSelector.deselectProject(project, selectedProjects.isEmpty()
                                ? null : selectedProjects.getLast());
                    }
                },
                AbstractButton::doClick,
                this::showProjectDescription,
                this::highlightRow);
        if (!allowMultipleSelection()) selectionGroup.add(row.selectionButton());
        resultsPanel.add(row);
        resultsPanel.add(Box.createVerticalStrut(5));
        return new ProjectRow(project, row.iconLabel());
    }

    private void highlightRow(ModrinthDownloadDialogItem selectedRow)
    {
        Color background = ThemeManager.surfaceColor(dialog.getBackground());
        for (Component component : resultsPanel.getComponents())
        {
            if (component instanceof ModrinthDownloadDialogItem row)
                row.setBackground(ThemeManager.surfaceColor(dialog.getBackground()));
        }
        selectedRow.setBackground(background.brighter());
    }

    private void showProjectDescription(ModrinthProject project)
    {
        if (selectedDescriptionProject != null
                && Objects.equals(selectedDescriptionProject.id(), project.id())) return;

        selectedDescriptionProject = project;
        if (resultsAndDescription.getRightComponent() == null)
        {
            resultsAndDescription.setRightComponent(modrinthDescriptionView);
            resultsAndDescription.setResizeWeight(0.55);
            resultsAndDescription.setDividerLocation(0.55);
        }
        modrinthDescriptionView.showLoading(project.title());

        projectDetails.computeIfAbsent(project.id(), ignored -> project.fetchDetails(USER_AGENT)
                .whenComplete((details, error) ->
                {
                    if (error != null) projectDetails.remove(project.id());
                }))
                .whenComplete((details, error) -> SwingUtilities.invokeLater(() ->
                {
                    if (selectedDescriptionProject != project || !dialog.isDisplayable()) return;
                    if (error != null)
                    {
                        modrinthDescriptionView.showError();
                        Throwable cause = error;
                        while (cause.getCause() != null) cause = cause.getCause();
                        Exception exception = cause instanceof Exception e ? e : new Exception(cause);
                        new ErrorDialog(main.client, exception, "loading project description");
                        return;
                    }
                    modrinthDescriptionView.showProject(details);
                }));
    }

    private CompletableFuture<List<ModrinthVersion>> loadProjectVersions(ModrinthProject project)
    {
        String gameVersion = new InstancesConfig().getProperty(instance.getName(), "version");
        ModrinthVersionQuery versionQuery = new ModrinthVersionQuery().gameVersion(gameVersion);
        if (filterByModloader()) versionQuery.loader(instance.getModloader());
        return Modrinth.getProjectVersions(project.id(), versionQuery, USER_AGENT);
    }

    private void showVersionLoadingError(Throwable error)
    {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        Exception exception = cause instanceof Exception e ? e : new Exception(cause);
        new ErrorDialog(main.client, exception, "loading mod versions");
    }

    private void loadIcons(List<ProjectRow> rows)
    {
        SwingWorker<Void, Void> worker = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground()
            {
                for (ProjectRow row : rows)
                {
                    if (isCancelled()) break;
                    try
                    {
                        BufferedImage icon = loadProjectIcon(row.project());
                        Image scaled = icon.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() ->
                        {
                            if (!isCancelled()) row.iconLabel().setIcon(new ImageIcon(scaled));
                        });
                    }
                    catch (IOException | URISyntaxException e)
                    {
                        System.err.println("Failed to load icon for " + row.project().slug() + ": " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void done()
            {
                iconWorkers.remove(this);
            }
        };
        iconWorkers.add(worker);
        worker.execute();
    }

    private static BufferedImage loadProjectIcon(ModrinthProject project) throws IOException, URISyntaxException
    {
        if (project.iconUrl() == null || project.iconUrl().isBlank())
            throw new IOException("Project has no icon URL");

        URLConnection connection = new URI(project.iconUrl()).toURL().openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        try (InputStream input = connection.getInputStream())
        {
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IOException("Unsupported or invalid image: " + project.iconUrl());
            return image;
        }
    }

    private void downloadSelected()
    {
        List<ModrinthProject> projects = List.copyOf(selectedProjects);
        if (projects.isEmpty()) return;

        downloadButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setMaximum(projects.size());
        progressBar.setString("0 / " + projects.size() + " " + completedItemText());
        progressBar.setVisible(showProgressBar());

        Path destination;
        try
        {
            destination = downloadDirectory();
            Files.createDirectories(destination);
        }
        catch (IOException e)
        {
            new ErrorDialog(main.client, e, "preparing the " + itemNamePlural().toLowerCase() + " folder");
            downloadButton.setEnabled(true);
            return;
        }
        statusLabel.setText(actionProgressText());

        AtomicInteger downloaded = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<?>> downloads = new ArrayList<>();
        for (ModrinthProject project : projects)
        {
            CompletableFuture<?> download;
            try
            {
                download = versionSelector.versionsFor(project)
                        .thenCompose(versions ->
                        {
                            if (versions.isEmpty())
                                return CompletableFuture.failedFuture(new IllegalStateException("No compatible version found for " + project.title()));
                            String selectedVersionId = versionSelector.selectedVersionId(project.id());
                            ModrinthVersion version = versions.stream()
                                    .filter(candidate -> Objects.equals(candidate.id(), selectedVersionId))
                                    .findFirst()
                                    .orElse(versions.getFirst());
                            return downloadProject(project, version, destination, USER_AGENT);
                        });
            }
            catch (RuntimeException e)
            {
                download = CompletableFuture.failedFuture(e);
            }

            downloads.add(download.whenComplete((result, error) ->
            {
                if (error == null) downloaded.incrementAndGet();
                else failures.add(error);

                SwingUtilities.invokeLater(() ->
                {
                    int completed = downloaded.get();
                    progressBar.setValue(completed);
                    progressBar.setString(completed + " / " + projects.size() + " " + completedItemText());
                });
            }));
        }

        CompletableFuture.allOf(downloads.toArray(CompletableFuture[]::new))
                .whenComplete((result, error) ->
                {
                    try
                    {
                        cleanupDownloadDirectory(destination);
                    }
                    catch (IOException cleanupError)
                    {
                        failures.add(cleanupError);
                    }

                    SwingUtilities.invokeLater(() ->
                    {
                        if (failures.isEmpty())
                        {
                            statusLabel.setText(completionStatusText());
                            onDownloadsComplete();
                            dialog.dispose();
                            return;
                        }

                        statusLabel.setText("Downloaded " + downloaded.get() + " of " + projects.size() + " " + itemNamePlural().toLowerCase() + "; some failed.");
                        downloadButton.setEnabled(!selectedProjects.isEmpty());
                        Throwable failure = failures.peek();
                        while (Objects.requireNonNull(failure).getCause() != null) failure = failure.getCause();
                        Exception exception = failure instanceof Exception e ? e : new Exception(failure);
                        new ErrorDialog(main.client, exception, actionProgressText().replace("...", ""));
                    });
                });
    }

    private Set<String> getInstalledModHashes() throws IOException
    {
        Set<String> hashes = installedModHashes;
        if (hashes != null) return hashes;

        synchronized (this)
        {
            hashes = installedModHashes;
            if (hashes == null)
            {
                hashes = InstalledModHashes.read(instance.getModsFolder());
                installedModHashes = hashes;
            }
        }
        return hashes;
    }

    private void cancelWorkers()
    {
        if (searchWorker != null) searchWorker.cancel(true);
        cancelIconWorkers();
    }

    private void cancelIconWorkers()
    {
        for (SwingWorker<Void, Void> worker : iconWorkers) worker.cancel(true);
        iconWorkers.clear();
    }

    private record ProjectRow(ModrinthProject project, JLabel iconLabel) {}
    private record SearchPage(List<ModrinthProject> projects, Set<String> installedProjectIds) {}
}
