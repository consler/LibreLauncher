package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.Instance;
import net.consler.librelauncherlib.instance.World;
import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class InstanceWorlds extends InstanceItems
{
    private SwingWorker<Void, WorldEntry> worldLoader;
    private final List<WorldEntry> worldEntries = new ArrayList<>();

    public InstanceWorlds(Main main)
    {
        super(main, "Worlds", "World");
    }

    @Override
    public void populate(Instance instance, String search)
    {
        if (worldLoader != null) worldLoader.cancel(true);
        listPanel.removeAll();
        listPanel.revalidate();
        listPanel.repaint();

        worldLoader = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground() throws IOException
            {
                Path savesFolder = instance.getSavesFolder();
                if (!Files.isDirectory(savesFolder)) return null;

                try (Stream<Path> paths = Files.list(savesFolder))
                {
                    paths.filter(Files::isDirectory)
                            .filter(path -> path.getFileName().toString().toLowerCase().contains(search))
                            .forEach(worldFolder ->
                            {
                                if (isCancelled()) return;
                                try
                                {
                                    World world = new World(worldFolder);
                                    Image icon = world.getIcon() == null ?
                                            null :
                                            world.getIcon().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                                    publish(new WorldEntry(world.getLevelName(), icon));
                                }
                                catch (IOException ignored)
                                {
                                }
                            });
                }
                return null;
            }

            @Override
            protected void process(List<WorldEntry> entries)
            {
                if (worldLoader != this || isCancelled()) return;
                for (WorldEntry entry : entries) addWorldRow(entry);
                listPanel.revalidate();
                listPanel.repaint();
            }

            @Override
            protected void done()
            {
                if (worldLoader != this || isCancelled()) return;
                try
                {
                    PromptSupport.setPrompt("Search "  + worldEntries.size() + " worlds", searchField);
                    get();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    showListingError(e, "listing worlds");
                }
                catch (CancellationException ignored)
                {
                }
            }
        };
        worldLoader.execute();
    }

    private void addWorldRow(WorldEntry entry)
    {
        worldEntries.add(entry);
        addItemRow(createItemRow(entry.name(), entry.icon(), true));
    }

    @Override
    public JPopupMenu createPopup(Instance instance)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openFolder = new JMenuItem("Open Worlds Folder");
        openFolder.addActionListener(ev ->
        {
            try
            {
                SystemUtils.openDirectory(instance.getSavesFolder().toFile());
            }
            catch (IOException ex)
            {
                new ErrorDialog(main.client, ex, "opening worlds folder");
                throw new RuntimeException(ex);
            }
        });
        popupMenu.add(openFolder);

        return popupMenu;
    }

    private record WorldEntry(String name, Image icon) {}
}
