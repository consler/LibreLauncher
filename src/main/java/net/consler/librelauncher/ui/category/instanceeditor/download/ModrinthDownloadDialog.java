package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.integration.modrinth.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ModrinthDownloadDialog
{
    private static final String USER_AGENT = "consler/LibreLauncher";
    private static final int SEARCH_PAGE_SIZE = 20;

    protected final Main main;
    protected final InstanceProfile instance;

    public JDialog dialog;

    private JTextField searchField;
    private JButton downloadButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JPanel resultsPanel;
    private JScrollPane resultsScrollPane;
    private SwingWorker<List<ModrinthProject>, Void> searchWorker;
    private final List<SwingWorker<Void, Void>> iconWorkers = new ArrayList<>();
    private final List<ProjectRow> projectRows = new ArrayList<>();
    private final List<ModrinthProject> selectedProjects = new ArrayList<>();
    private ButtonGroup selectionGroup;
    private String currentQuery = "";
    private int nextSearchOffset;
    private boolean loadingSearchPage;
    private boolean hasMoreSearchResults = true;

    protected ModrinthDownloadDialog(Main main, InstanceProfile instance)
    {
        this.main = main;
        this.instance = instance;
    }

    protected abstract ProjectType projectType();
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
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        dialog.add(main.titlebar.create(dialog, title, false, dialog::dispose), BorderLayout.NORTH);
        dialog.add(createContent(), BorderLayout.CENTER);
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                cancelWorkers();
            }
        });

        dialog.pack();
        dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 20, 20));
        dialog.setLocationRelativeTo(main.client);
        searchField.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private JPanel createContent()
    {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        content.setPreferredSize(new Dimension(740, 500));

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
        content.add(resultsScrollPane, BorderLayout.CENTER);

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
        selectionGroup = new ButtonGroup();
        downloadButton.setEnabled(false);
        resultsPanel.removeAll();
        projectRows.clear();
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
            protected List<ModrinthProject> doInBackground() throws Exception
            {
                SearchQuery searchQuery = new SearchQuery(query)
                        .projectType(projectType())
                        .gameVersion(new InstancesConfig().getProperty(instance.getName(), "version"))
                        .limit(SEARCH_PAGE_SIZE)
                        .offset(offset);
                if (filterByModloader()) searchQuery.category(instance.getModloader());
                return Modrinth.search(searchQuery, USER_AGENT).get().hits();
            }

            @Override
            protected void done()
            {
                if (searchWorker != this || isCancelled()) return;
                loadingSearchPage = false;
                try
                {
                    List<ModrinthProject> projects = get();
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
                        ProjectRow row = addProjectRow(project);
                        projectRows.add(row);
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

    private ProjectRow addProjectRow(ModrinthProject project)
    {
        String title = project.title() == null || project.title().isBlank() ? project.slug() : project.title();
        boolean exists = projectExists(project);

        AbstractButton selected = allowMultipleSelection() ? new JCheckBox() : new JRadioButton();
        selected.setOpaque(false);
        selected.setToolTipText("Select " + title);
        selected.setEnabled(!exists);
        if (!allowMultipleSelection()) selectionGroup.add(selected);
        selected.addActionListener(e ->
        {
            if (selected.isSelected())
            {
                if (!allowMultipleSelection()) selectedProjects.clear();
                selectedProjects.add(project);
            }
            else
            {
                selectedProjects.remove(project);
            }
            downloadButton.setEnabled(!selectedProjects.isEmpty());
        });

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(50, 50));
        iconLabel.setOpaque(true);
        iconLabel.setBackground(ThemeManager.surfaceColor(dialog.getBackground()));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setToolTipText(exists ? title + " is already installed" : title);
        if (exists) titleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        JLabel descriptionLabel = new JLabel(project.description());
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(10f));
        descriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));


        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        row.setPreferredSize(new Dimension(500, 60));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setBackground(ThemeManager.surfaceColor(dialog.getBackground()));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.outlineColor(dialog.getBackground())),
                new EmptyBorder(0, 6, 0, 6)
        ));

        MouseAdapter mouseAdapter = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                Arrays.stream(resultsPanel.getComponents()).forEach(component ->
                        component.setBackground(ThemeManager.surfaceColor(dialog.getBackground())));
                row.setBackground(ThemeManager.surfaceColor(dialog.getBackground()).brighter());

                if(!exists && e.getClickCount() == 2)
                {
                    if (!allowMultipleSelection()) selectedProjects.clear();
                    selectedProjects.add(project);
                    selected.setSelected(true);
                }
            }
        };
        row.addMouseListener(mouseAdapter);
        titleLabel.addMouseListener(mouseAdapter);
        titleLabel.addMouseListener(mouseAdapter);

        row.add(selected);
        row.add(iconLabel);
        row.add(titleLabel);
        row.add(descriptionLabel);

        resultsPanel.add(row);
        resultsPanel.add(Box.createVerticalStrut(5));
        return new ProjectRow(project, selected, iconLabel);
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
                        BufferedImage icon = row.project().getIcon();
                        Image scaled = icon.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() ->
                        {
                            if (!isCancelled()) row.iconLabel().setIcon(new ImageIcon(scaled));
                        });
                    }
                    catch (IOException | URISyntaxException e)
                    {
                        SwingUtilities.invokeLater(() ->
                                new ErrorDialog(main.client, e, "Failed to load icon for " + row.project().slug()));
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
                String gameVersion = new InstancesConfig().getProperty(instance.getName(), "version");
                VersionQuery versionQuery = new VersionQuery().gameVersion(gameVersion);
                if (filterByModloader()) versionQuery.loader(instance.getModloader());
                download = Modrinth.getProjectVersions(project.id(), versionQuery, USER_AGENT)
                        .thenCompose(versions ->
                        {
                            if (versions.isEmpty())
                                return CompletableFuture.failedFuture(new IllegalStateException("No compatible version found for " + project.title()));
                            return downloadProject(project, versions.getFirst(), destination, USER_AGENT);
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

    private record ProjectRow(ModrinthProject project, AbstractButton selection, JLabel iconLabel) {}
}
