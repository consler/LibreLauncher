package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncherlib.instance.InstanceProfile;
import net.consler.librelauncherlib.integration.modrinth.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class ModDownloadDialog
{
    private static final String USER_AGENT = "consler/LibreLauncher";

    private final Main main;
    private final InstanceMods instanceMods;

    public JDialog dialog;
    public InstanceProfile instance;

    private JTextField searchField;
    private JButton downloadButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JPanel resultsPanel;
    private SwingWorker<List<ModrinthProject>, Void> searchWorker;
    private SwingWorker<Void, IconUpdate> iconWorker;
    private final List<ProjectRow> projectRows = new ArrayList<>();
    private final List<ModrinthProject> selectedProjects = new ArrayList<>();

    public ModDownloadDialog(Main main, InstanceProfile instance, InstanceMods instanceMods)
    {
        this.main = main;
        this.instance = instance;
        this.instanceMods = instanceMods;
    }

    public void open()
    {
        dialog = new JDialog(main.client, "Download Mods", true);
        dialog.setUndecorated(true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(main.client.getBackground().darker());
        dialog.add(main.titlebar.create(dialog, "Download Mods", false, dialog::dispose), BorderLayout.NORTH);
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
        main.themeManager.silentField(searchField);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(searchField, BorderLayout.CENTER);
        content.add(searchPanel, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setOpaque(false);
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));

        search("");

        JScrollPane resultsScrollPane = new JScrollPane(resultsPanel);
        resultsScrollPane.setOpaque(false);
        resultsScrollPane.getViewport().setOpaque(false);
        resultsScrollPane.setBorder(null);
        resultsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
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

        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e ->
        {
            downloadButton.setEnabled(false);
            downloadSelected();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(downloadButton);

        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setOpaque(false);
        footer.add(progressPanel, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);

        return content;
    }

    private void search(String query)
    {
        if (searchWorker != null) searchWorker.cancel(true);
        if (iconWorker != null) iconWorker.cancel(true);

        searchWorker = new SwingWorker<>()
        {
            @Override
            protected List<ModrinthProject> doInBackground() throws Exception
            {
                SearchQuery searchQuery = new SearchQuery(query)
                        .projectType(ProjectType.MOD)
                        .category(instance.getModloader())
                        .gameVersion(new InstancesConfig().getProperty(instance.getName(), "version"))
                        .limit(50);
                return Modrinth.search(searchQuery, USER_AGENT).get().hits();
            }

            @Override
            protected void done()
            {
                if (searchWorker != this || isCancelled()) return;

                resultsPanel.removeAll();
                projectRows.clear();
                resultsPanel.revalidate();
                resultsPanel.repaint();
                try
                {
                    List<ModrinthProject> projects = get();
                    if (projects.isEmpty())
                    {
                        downloadButton.setEnabled(false);
                        resultsPanel.removeAll();
                        JLabel label = new JLabel("No mods found.");
                        label.setBorder(new EmptyBorder(12, 8, 12, 8));
                        resultsPanel.add(label);
                        resultsPanel.revalidate();
                        resultsPanel.repaint();
                        statusLabel.setText(" ");
                        return;
                    }

                    for (ModrinthProject project : projects) projectRows.add(addProjectRow(project));
                    loadIcons(List.copyOf(projectRows));
                    statusLabel.setText("");
                    resultsPanel.revalidate();
                    resultsPanel.repaint();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    downloadButton.setEnabled(false);
                    statusLabel.setText("Search failed.");
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

        JCheckBox selected = new JCheckBox();
        selected.setOpaque(false);
        selected.setToolTipText("Select " + title);
        selected.addActionListener(e ->
        {
            if (selected.isSelected())
                selectedProjects.add(project);
            else
                selectedProjects.remove(project);

            downloadButton.setEnabled(!selectedProjects.isEmpty());
        });

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(50, 50));
        iconLabel.setOpaque(true);
        iconLabel.setBackground(dialog.getBackground().brighter());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setToolTipText(title);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        row.setPreferredSize(new Dimension(500, 60));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setBackground(dialog.getBackground().darker());
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 25)),
                new EmptyBorder(0, 6, 0, 6)
        ));
        row.add(selected);
        row.add(iconLabel);
        row.add(titleLabel);
        resultsPanel.add(row);
        resultsPanel.add(Box.createVerticalStrut(5));
        return new ProjectRow(project, iconLabel);
    }

    private void loadIcons(List<ProjectRow> rows)
    {
        iconWorker = new SwingWorker<>()
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
                        row.iconLabel.setIcon(new ImageIcon(icon.getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
                    }
                    catch (IOException | URISyntaxException e)
                    {
                        new ErrorDialog(main.client, e, "Failed to load icon for " + row.project().slug());
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        };
        iconWorker.execute();
    }

    private void downloadSelected()
    {
        List<ModrinthProject> projects = List.copyOf(selectedProjects);
        if (projects.isEmpty()) return;

        downloadButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setMaximum(projects.size());
        progressBar.setString("0 / " + projects.size() + " mods downloaded");
        progressBar.setVisible(true);
        statusLabel.setText("Downloading mods...");

        Path modsDirectory = instance.getModsFolder();
        try
        {
            Files.createDirectories(modsDirectory);
        }
        catch (IOException e)
        {
            new ErrorDialog(main.client, e, "preparing the mods folder");
            downloadButton.setEnabled(true);
            return;
        }

        AtomicInteger downloaded = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<?>> downloads = new ArrayList<>();
        for (ModrinthProject project : projects)
        {
            CompletableFuture<?> download;
            try
            {
                String gameVersion = new InstancesConfig().getProperty(instance.getName(), "version");
                VersionQuery versionQuery = new VersionQuery()
                        .loader(instance.getModloader())
                        .gameVersion(gameVersion);
                download = Modrinth.getProjectVersions(project.id(), versionQuery, USER_AGENT)
                        .thenCompose(versions ->
                        {
                            if (versions.isEmpty())
                                return CompletableFuture.failedFuture(new IllegalStateException(
                                        "No compatible version found for " + project.title()));

                            ModrinthVersion version = versions.getFirst();
                            return project.downloadAsMod(version, modsDirectory, USER_AGENT);
                        });
            }
            catch (RuntimeException e)
            {
                download = CompletableFuture.failedFuture(e);
            }

            downloads.add(download.whenComplete((result, error) ->
            {
                if (error == null)
                    downloaded.incrementAndGet();
                else
                    failures.add(error);


                SwingUtilities.invokeLater(() ->
                {
                    int completed = downloaded.get();
                    progressBar.setValue(completed);
                    progressBar.setString(completed + " / " + projects.size() + " mods downloaded");
                });
            }));
        }

        CompletableFuture.allOf(downloads.toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> SwingUtilities.invokeLater(() ->
                {
                    if (failures.isEmpty())
                    {
                        statusLabel.setText("All mods downloaded.");
                        dialog.dispose();
                        if(instanceMods != null) instanceMods.populate(instance);
                        return;
                    }

                    statusLabel.setText("Downloaded " + downloaded.get() + " of " + projects.size() + " mods; some failed.");
                    downloadButton.setEnabled(!selectedProjects.isEmpty());
                    Throwable failure = failures.peek();
                    while (Objects.requireNonNull(failure).getCause() != null) failure = failure.getCause();
                    Exception exception = failure instanceof Exception e ? e : new Exception(failure);
                    new ErrorDialog(main.client, exception, "downloading mods");
                }));
    }

    private void cancelWorkers()
    {
        if (searchWorker != null) searchWorker.cancel(true);
        if (iconWorker != null) iconWorker.cancel(true);
    }

    private record ProjectRow(ModrinthProject project, JLabel iconLabel) {}
    private record IconUpdate(ProjectRow row, BufferedImage image) {}
}
