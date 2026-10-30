package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

final class ModrinthVersionSelector extends JPanel
{
    private final JComboBox<ModrinthVersion> versionBox = new JComboBox<>();
    private final Function<ModrinthProject, CompletableFuture<List<ModrinthVersion>>> versionLoader;
    private final Consumer<Throwable> errorHandler;
    private final ConcurrentMap<String, CompletableFuture<List<ModrinthVersion>>> versionCache =
            new ConcurrentHashMap<>();
    private final Map<String, String> selectedVersionIds = new HashMap<>();
    private ModrinthProject selectedProject;
    private boolean updatingSelection;

    ModrinthVersionSelector(
            Function<ModrinthProject, CompletableFuture<List<ModrinthVersion>>> versionLoader,
            Consumer<Throwable> errorHandler)
    {
        super(new BorderLayout(8, 0));
        this.versionLoader = versionLoader;
        this.errorHandler = errorHandler;

        versionBox.setEnabled(false);
        versionBox.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                String label = value instanceof ModrinthVersion version
                        ? version.versionNumber() + (version.name() == null || version.name().isBlank()
                                ? "" : " - " + version.name())
                        : "Select a version";
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        versionBox.addActionListener(event ->
        {
            if (updatingSelection || selectedProject == null) return;
            ModrinthVersion version = (ModrinthVersion) versionBox.getSelectedItem();
            if (version != null) selectedVersionIds.put(selectedProject.id(), version.id());
        });

        setOpaque(false);
        setBorder(new EmptyBorder(8, 4, 0, 4));
        add(new JLabel("Version:"), BorderLayout.WEST);
        add(versionBox, BorderLayout.CENTER);
        setVisible(false);
    }

    CompletableFuture<List<ModrinthVersion>> versionsFor(ModrinthProject project)
    {
        return versionCache.computeIfAbsent(project.id(), projectId ->
                versionLoader.apply(project).whenComplete((versions, error) ->
                {
                    if (error != null) versionCache.remove(projectId);
                }));
    }

    String selectedVersionId(String projectId)
    {
        return selectedVersionIds.get(projectId);
    }

    void selectProject(ModrinthProject project)
    {
        selectedProject = project;
        setVisible(true);
        versionBox.removeAllItems();
        versionBox.setEnabled(false);
        versionBox.setToolTipText("Loading versions...");
        revalidate();
        repaint();
        if (getParent() != null)
        {
            getParent().revalidate();
            getParent().repaint();
        }

        versionsFor(project).whenComplete((versions, error) -> SwingUtilities.invokeLater(() ->
        {
            if (selectedProject != project || !isVisible()) return;
            if (error != null)
            {
                versionBox.setToolTipText("Could not load versions");
                errorHandler.accept(error);
                return;
            }
            if (versions.isEmpty())
            {
                versionBox.setToolTipText("No compatible versions found");
                return;
            }

            updatingSelection = true;
            for (ModrinthVersion version : versions) versionBox.addItem(version);
            String selectedId = selectedVersionIds.get(project.id());
            for (int i = 0; i < versions.size(); i++)
            {
                if (Objects.equals(versions.get(i).id(), selectedId))
                {
                    versionBox.setSelectedIndex(i);
                    break;
                }
            }
            updatingSelection = false;
            versionBox.setEnabled(true);
            versionBox.setToolTipText(null);
        }));
    }

    void deselectProject(ModrinthProject project, ModrinthProject fallback)
    {
        if (selectedProject == project)
        {
            if (fallback == null) clear();
            else selectProject(fallback);
        }
    }

    void clear()
    {
        selectedProject = null;
        updatingSelection = true;
        versionBox.removeAllItems();
        updatingSelection = false;
        versionBox.setEnabled(false);
        versionBox.setToolTipText(null);
        setVisible(false);
        if (getParent() != null)
        {
            getParent().revalidate();
            getParent().repaint();
        }
    }
}
