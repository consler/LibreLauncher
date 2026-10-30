package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.instance.Mod;
import net.consler.librelauncherlib.integration.modrinth.Modrinth;
import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersionQuery;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ModrinthUpdater
{
    private static final String USER_AGENT = "consler/LibreLauncher";

    private final Main main;
    private final Instance instance;
    private final List<ModFile> mods;

    public JDialog dialog;

    private JPanel modListPanel;
    private JButton updateButton;
    private JLabel statusLabel;
    private final List<ModRow> modRows = new ArrayList<>();
    private List<ModCandidate> candidates = List.of();
    private SwingWorker<List<ModCandidate>, Void> lookupWorker;
    private SwingWorker<UpdateResult, Void> downloadWorker;

    public ModrinthUpdater(Main main, Instance instance, List<Mod> mods)
    {
        this.main = main;
        this.instance = instance;

        List<ModFile> modFiles = new ArrayList<>(mods.size());
        for (Mod mod : mods)
        {
            modFiles.add(new ModFile(mod, mod.getModFile().toPath()));
        }
        this.mods = List.copyOf(modFiles);
    }

    public void open()
    {
        String title = "Update mods";
        dialog = new JDialog(main.client, title, true);
        dialog.setUndecorated(true);
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
        dialog.add(main.titlebar.create(dialog, title, false, dialog::dispose), BorderLayout.NORTH);
        dialog.add(createContent(), BorderLayout.CENTER);
        dialog.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 20, 20));
            }
        });
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                if (lookupWorker != null) lookupWorker.cancel(true);
                if (downloadWorker != null) downloadWorker.cancel(true);
            }
        });

        dialog.pack();
        dialog.setMinimumSize(new Dimension(620, 360));
        dialog.setLocationRelativeTo(main.client);
        startLookup();
        dialog.setVisible(true);
    }

    private JPanel createContent()
    {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        content.setPreferredSize(new Dimension(760, 440));

        modListPanel = new JPanel();
        modListPanel.setOpaque(false);
        modListPanel.setLayout(new BoxLayout(modListPanel, BoxLayout.Y_AXIS));
        JScrollPane modsScrollPane = new JScrollPane(modListPanel);
        modsScrollPane.setOpaque(false);
        modsScrollPane.getViewport().setOpaque(false);
        modsScrollPane.setBorder(null);
        modsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        content.add(modsScrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Looking up installed mods on Modrinth...");
        updateButton = new JButton("Update selected mods");
        updateButton.setEnabled(false);
        updateButton.addActionListener(event -> updateSelectedMods());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(updateButton);

        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setOpaque(false);
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    private void startLookup()
    {
        lookupWorker = new SwingWorker<>()
        {
            @Override
            protected List<ModCandidate> doInBackground() throws Exception
            {
                String gameVersion = new InstancesConfig().getProperty(instance.getName(), "version");
                List<CompletableFuture<ModCandidate>> lookups = mods.stream()
                        .map(mod -> lookupMod(mod, gameVersion))
                        .toList();
                CompletableFuture.allOf(lookups.toArray(CompletableFuture[]::new)).get();
                return lookups.stream().map(CompletableFuture::join).toList();
            }

            @Override
            protected void done()
            {
                if (isCancelled() || !dialog.isDisplayable()) return;
                try
                {
                    candidates = get();
                    showModRows();
                    long failures = candidates.stream().filter(candidate -> candidate.error() != null).count();
                    if (candidates.isEmpty()) statusLabel.setText("No mods to update.");
                    else if (failures > 0) statusLabel.setText(failures + " mod lookup(s) failed; see affected rows.");
                    else statusLabel.setText("Select the mods you want to update.");
                    updateButton.setEnabled(modRows.stream().anyMatch(row -> row.checkbox().isSelected()));
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    statusLabel.setText("Mod lookup failed.");
                    showError(e, "looking up installed mods on Modrinth");
                }
            }

        };
        lookupWorker.execute();
    }

    private CompletableFuture<ModCandidate> lookupMod(ModFile mod, String gameVersion)
    {
        CompletableFuture<ModCandidate> lookup;
        try
        {
            String sha1 = InstalledModHashes.sha1(mod.path());
            CompletableFuture<ModrinthVersion> installedVersion =
                    Modrinth.getVersionBySha1(sha1, USER_AGENT);
            lookup = installedVersion.thenCombine(
                    Modrinth.getProjectBySha1(sha1, USER_AGENT),
                    InstalledProjectVersion::new)
                    .thenCompose(installed ->
                    {
                        ModrinthVersionQuery query = new ModrinthVersionQuery().gameVersion(gameVersion);
                        query.loader(instance.getModloader());
                        return Modrinth.getProjectVersions(installed.project().id(), query, USER_AGENT)
                                .thenApply(versions -> new ModCandidate(
                                        mod, installed.project(), installed.version().id(), versions, null));
                    });
        }
        catch (Exception e)
        {
            lookup = CompletableFuture.failedFuture(e);
        }

        return lookup.handle((candidate, error) -> error == null
                ? candidate
                : new ModCandidate(mod, null, null, List.of(), rootCause(error)));
    }

    private void showModRows()
    {
        modListPanel.removeAll();
        modRows.clear();
        for (ModCandidate candidate : candidates)
        {
            boolean available = candidate.error() == null && !candidate.versions().isEmpty();
            JCheckBox checkbox = new JCheckBox();
            checkbox.setOpaque(false);
            checkbox.setEnabled(available);
            checkbox.setSelected(available);
            String name = candidate.project() == null
                    ? candidate.modFile().mod().getName()
                    : candidate.project().title();
            if (name == null || name.isBlank()) name = candidate.modFile().path().getFileName().toString();
            JLabel iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(36, 36));
            BufferedImage icon = candidate.modFile().mod().getIcon();
            if (icon != null)
                iconLabel.setIcon(new ImageIcon(icon.getScaledInstance(36, 36, Image.SCALE_SMOOTH)));
            iconLabel.setToolTipText(candidate.error() == null ? null : candidate.error().getMessage());

            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            nameLabel.setToolTipText(candidate.error() == null
                    ? Objects.toString(candidate.project() == null ? null : candidate.project().slug(), name)
                    : candidate.error().getMessage());

            JComboBox<ModrinthVersion> rowVersionBox = new JComboBox<>();
            rowVersionBox.setRenderer(new DefaultListCellRenderer()
            {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
                {
                    String label = value instanceof ModrinthVersion version
                            ? version.versionNumber() + (version.name() == null || version.name().isBlank()
                                    ? "" : " - " + version.name())
                            : "No compatible versions";
                    return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                }
            });
            for (ModrinthVersion version : candidate.versions()) rowVersionBox.addItem(version);
            rowVersionBox.setEnabled(available);
            rowVersionBox.setPreferredSize(new Dimension(190, rowVersionBox.getPreferredSize().height));
            boolean alreadyCurrent = available && isInstalledVersion(candidate,
                    (ModrinthVersion) rowVersionBox.getSelectedItem());
            checkbox.setSelected(available && !alreadyCurrent);
            checkbox.setEnabled(available && !alreadyCurrent);
            checkbox.setToolTipText(alreadyCurrent
                    ? "This version is already installed"
                    : null);

            JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
            rowPanel.setOpaque(true);
            rowPanel.setBackground(ThemeManager.surfaceColor(dialog.getBackground()));
            rowPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.outlineColor(dialog.getBackground())),
                    new EmptyBorder(8, 8, 8, 8)));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            leading.setOpaque(false);
            leading.add(checkbox);
            leading.add(iconLabel);
            rowPanel.add(leading, BorderLayout.WEST);
            rowPanel.add(nameLabel, BorderLayout.CENTER);
            rowPanel.add(rowVersionBox, BorderLayout.EAST);
            ModRow modRow = new ModRow(candidate, checkbox, rowVersionBox);
            modRows.add(modRow);
            checkbox.addActionListener(event ->
            {
                updateButton.setEnabled(modRows.stream().anyMatch(row -> row.checkbox().isSelected()));
                statusLabel.setText(modRows.stream().filter(row -> row.checkbox().isSelected()).count()
                        + " mod(s) selected.");
            });
            rowVersionBox.addActionListener(event ->
            {
                boolean currentVersion = isInstalledVersion(candidate,
                        (ModrinthVersion) rowVersionBox.getSelectedItem());
                if (currentVersion) checkbox.setSelected(false);
                checkbox.setEnabled(available && !currentVersion);
                checkbox.setToolTipText(currentVersion ? "This version is already installed" : null);
                updateButton.setEnabled(modRows.stream().anyMatch(row -> row.checkbox().isSelected()));
                statusLabel.setText(currentVersion
                        ? "This version is already installed."
                        : modRows.stream().filter(row -> row.checkbox().isSelected()).count()
                                + " mod(s) selected.");
            });
            modListPanel.add(rowPanel);
        }
        modListPanel.revalidate();
        modListPanel.repaint();
    }

    private void updateSelectedMods()
    {
        List<ModUpdate> selectedUpdates = modRows.stream()
                .filter(row -> row.checkbox().isSelected())
                .map(row -> new ModUpdate(row.candidate(), (ModrinthVersion) row.versionBox().getSelectedItem()))
                .filter(update -> update.version() != null)
                .filter(update -> !isInstalledVersion(update.candidate(), update.version()))
                .toList();
        if (selectedUpdates.isEmpty()) return;

        updateButton.setEnabled(false);
        modRows.forEach(row -> row.versionBox().setEnabled(false));
        statusLabel.setText("Updating " + selectedUpdates.size() + " mod(s)...");
        downloadWorker = new SwingWorker<>()
        {
            @Override
            protected UpdateResult doInBackground()
            {
                List<Throwable> failures = new ArrayList<>();
                int updated = 0;
                for (ModUpdate update : selectedUpdates)
                {
                    try
                    {
                        downloadAndReplace(update.candidate(), update.version());
                        updated++;
                    }
                    catch (Exception e)
                    {
                        failures.add(e);
                    }
                }
                return new UpdateResult(updated, failures);
            }

            @Override
            protected void done()
            {
                if (!dialog.isDisplayable()) return;
                try
                {
                    UpdateResult result = get();
                    net.consler.librelauncher.ui.category.instanceeditor.InstanceMods.getInstance().populate(instance);
                    if (result.failures().isEmpty())
                        statusLabel.setText(result.updated() + " mod(s) updated.");
                    else
                    {
                        statusLabel.setText("Updated " + result.updated() + " mod(s); "
                                + result.failures().size() + " failed.");
                        showError(new IOException(result.failures().size() + " mod update(s) failed.",
                                result.failures().getFirst()), "updating mods");
                    }
                    dialog.dispose();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    statusLabel.setText("Update failed.");
                    updateButton.setEnabled(true);
                    modRows.forEach(row -> row.versionBox().setEnabled(
                            row.candidate().error() == null && !row.candidate().versions().isEmpty()));
                    showError(e, "updating a mod");
                }
            }
        };
        downloadWorker.execute();
    }

    private void downloadAndReplace(ModCandidate candidate, ModrinthVersion version) throws Exception
    {
        ModrinthVersion.VersionFile file = version.primaryFile();
        if (file == null) throw new IOException("Selected version has no downloadable file");

        Path modsFolder = instance.getModsFolder().toAbsolutePath().normalize();
        Files.createDirectories(modsFolder);
        Path stagingDirectory = Files.createTempDirectory(modsFolder, ".modrinth-update-");
        try
        {
            Path downloadedFile = Modrinth.downloadVersionFile(file, stagingDirectory, USER_AGENT).get();
            candidate.project().toMod(downloadedFile);

            String filename = file.filename();
            if (filename == null || filename.isBlank())
                throw new IOException("Selected version has an invalid filename");
            if (candidate.modFile().path().getFileName().toString().endsWith(".disabled"))
                filename += ".disabled";

            Path target = modsFolder.resolve(filename).normalize();
            if (!Objects.equals(target.getParent(), modsFolder))
                throw new IOException("Selected version has an invalid filename");
            Path original = candidate.modFile().path().toAbsolutePath().normalize();
            if (Files.exists(target) && !Files.isSameFile(target, original))
            {
                String existingSha1 = InstalledModHashes.sha1(target);
                ModrinthProject existingProject = Modrinth.getProjectBySha1(existingSha1, USER_AGENT).get();
                if (!Objects.equals(existingProject.id(), candidate.project().id()))
                    throw new FileAlreadyExistsException(
                            target.toString(), null, "a different Modrinth project already uses this filename");
            }

            Files.move(downloadedFile, target, StandardCopyOption.REPLACE_EXISTING);
            if (!Files.isSameFile(target, original)) Files.delete(original);
        }
        finally
        {
            try (var files = Files.list(stagingDirectory))
            {
                for (Path path : files.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(stagingDirectory);
        }
    }

    private static Throwable rootCause(Throwable error)
    {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null)
            cause = cause.getCause();
        return cause;
    }

    private static boolean isInstalledVersion(ModCandidate candidate, ModrinthVersion version)
    {
        return version != null && Objects.equals(candidate.installedVersionId(), version.id());
    }

    private void showError(Throwable error, String action)
    {
        Throwable cause = rootCause(error);
        Exception exception = cause instanceof Exception e ? e : new Exception(cause);
        new ErrorDialog(main.client, exception, action);
    }

    private record ModFile(Mod mod, Path path) {}
    private record InstalledProjectVersion(ModrinthVersion version, ModrinthProject project) {}
    private record ModCandidate(
            ModFile modFile, ModrinthProject project, String installedVersionId,
            List<ModrinthVersion> versions, Throwable error) {}
    private record ModRow(
            ModCandidate candidate, JCheckBox checkbox, JComboBox<ModrinthVersion> versionBox) {}
    private record ModUpdate(ModCandidate candidate, ModrinthVersion version) {}
    private record UpdateResult(int updated, List<Throwable> failures) {}
}
