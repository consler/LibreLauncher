package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.util.SystemUtils;
import net.consler.librelauncherlib.instance.Instance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class InstanceScreenshots extends InstanceItems
{
    private static final int THUMBNAIL_WIDTH = 180;
    private static final int THUMBNAIL_HEIGHT = 135;
    private int loadGeneration;
    private SwingWorker<Void, Thumbnail> loader;
    private boolean resizeListenerAdded;
    private JPanel selectedScreenshot;
    private Instance currentInstance;
    private boolean reloadWhenShown;

    protected InstanceScreenshots(Main main)
    {
        super(main, "Screenshots", "screenshots");
    }

    @Override
    public void populate(Instance instance, String search)
    {
        currentInstance = instance;
        addResizeListener();
        int generation = ++loadGeneration;
        if(loader != null) loader.cancel(true);

        selectedScreenshot = null;
        listPanel.removeAll();
        updateGridLayout();
        JLabel loadingLabel = new JLabel("Loading screenshots...", SwingConstants.CENTER);
        listPanel.add(loadingLabel);
        listPanel.revalidate();
        listPanel.repaint();

        Path screenshotsFolder = instance.getScreenshotsFolder();
        String query = search.toLowerCase(Locale.ROOT);
        loader = new SwingWorker<>()
        {
            private final Map<Path, JLabel> imageLabels = new HashMap<>();
            private boolean foundScreenshots;

            @Override
            protected Void doInBackground() throws Exception
            {
                if(!Files.isDirectory(screenshotsFolder)) return null;

                List<Path> screenshots = new ArrayList<>();
                try(Stream<Path> files = Files.list(screenshotsFolder))
                {
                    files.filter(Files::isRegularFile)
                            .filter(InstanceScreenshots::isImageFile)
                            .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains(query))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString(), Comparator.reverseOrder()))
                            .forEach(screenshots::add);
                }
                catch(UncheckedIOException e)
                {
                    throw e.getCause();
                }

                screenshots.forEach(path -> publish(new Thumbnail(path, null)));
                ExecutorService decoder = Executors.newFixedThreadPool(2);
                try
                {
                    ExecutorCompletionService<Thumbnail> completed = new ExecutorCompletionService<>(decoder);
                    for(Path screenshot : screenshots)
                    {
                        completed.submit(() ->
                        {
                            try
                            {
                                return new Thumbnail(screenshot, loadThumbnail(screenshot));
                            }
                            catch(IOException e)
                            {
                                return new Thumbnail(screenshot, null, e);
                            }
                        });
                    }

                    for(int i = 0; i < screenshots.size() && !isCancelled(); i++)
                    {
                        try
                        {
                            publish(completed.take().get());
                        }
                        catch(ExecutionException e)
                        {
                            throw new IOException("A screenshot decoder failed", e.getCause());
                        }
                    }
                }
                finally
                {
                    decoder.shutdownNow();
                }
                return null;
            }

            @Override
            protected void process(List<Thumbnail> thumbnails)
            {
                if(generation != loadGeneration) return;

                for(Thumbnail thumbnail : thumbnails)
                {
                    JLabel imageLabel = imageLabels.get(thumbnail.path());
                    if(imageLabel == null)
                    {
                        if(!foundScreenshots)
                        {
                            listPanel.remove(loadingLabel);
                            foundScreenshots = true;
                        }

                        JPanel card = new JPanel(new BorderLayout(0, 5));
                        card.setOpaque(false);
                        imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
                        imageLabel.setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
                        card.add(imageLabel, BorderLayout.CENTER);
                        JLabel nameLabel = new JLabel(thumbnail.path().getFileName().toString(), SwingConstants.CENTER);
                        card.add(nameLabel, BorderLayout.SOUTH);
                        configureScreenshotCard(card, imageLabel, nameLabel, thumbnail.path(), generation);
                        listPanel.add(card);
                        imageLabels.put(thumbnail.path(), imageLabel);
                    }

                    if(thumbnail.image() != null)
                    {
                        imageLabel.setText(null);
                        imageLabel.setIcon(new ImageIcon(thumbnail.image()));
                    }
                    else if(thumbnail.error() != null)
                    {
                        imageLabel.setText("Unable to load");
                        new ErrorDialog(main.client, thumbnail.error(),
                                "loading screenshot " + thumbnail.path().getFileName());
                    }
                }

                listPanel.revalidate();
                listPanel.repaint();
            }

            @Override
            protected void done()
            {
                if(generation != loadGeneration || isCancelled()) return;

                if(!foundScreenshots)
                {
                    listPanel.removeAll();
                    listPanel.add(new JLabel("No screenshots found", SwingConstants.CENTER));
                }

                listPanel.revalidate();
                listPanel.repaint();

                try
                {
                    get();
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch(ExecutionException e)
                {
                    listPanel.removeAll();
                    listPanel.add(new JLabel("Unable to list screenshots", SwingConstants.CENTER));
                    listPanel.revalidate();
                    listPanel.repaint();
                    new ErrorDialog(main.client, new Exception(e.getCause()), "listing screenshots");
                }
            }
        };
        loader.execute();
    }

    private void addResizeListener()
    {
        if(resizeListenerAdded) return;

        panel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentHidden(ComponentEvent e)
            {
                if(loader != null && !loader.isDone())
                {
                    ++loadGeneration;
                    loader.cancel(true);
                    loader = null;
                    reloadWhenShown = true;
                }
            }

            @Override
            public void componentShown(ComponentEvent e)
            {
                if(reloadWhenShown && currentInstance != null)
                {
                    reloadWhenShown = false;
                    populate(currentInstance, searchField.getText());
                }
            }
        });

        listPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateGridLayout();
            }
        });
        resizeListenerAdded = true;
    }

    private void configureScreenshotCard(JPanel card, JLabel imageLabel, JLabel nameLabel,
                                         Path screenshot, int generation)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(e ->
        {
            try
            {
                Files.delete(screenshot);
                if(generation == loadGeneration) populate(currentInstance, searchField.getText());
            }
            catch(IOException ex)
            {
                new ErrorDialog(main.client, ex, "deleting screenshot");
            }
        });
        popupMenu.add(delete);

        MouseAdapter mouseAdapter = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                selectScreenshot(card);
                if(e.getClickCount() == 2)
                {
                    try
                    {
                        SystemUtils.openFile(screenshot.toFile());
                    }
                    catch (Exception ex)
                    {
                        new ErrorDialog(main.client, ex, "opening screenshot");
                        throw new RuntimeException(ex);
                    }
                }
            }
        };

        for(JComponent component : new JComponent[]{card, imageLabel, nameLabel})
        {
            component.addMouseListener(mouseAdapter);
            component.setComponentPopupMenu(popupMenu);
        }
    }

    private void selectScreenshot(JPanel card)
    {
        if(selectedScreenshot != null)
            selectedScreenshot.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        selectedScreenshot = card;
        selectedScreenshot.setBorder(BorderFactory.createLineBorder(
                panel.getBackground().brighter(), 2));
    }

    private void updateGridLayout()
    {
        int columns = Math.max(1, (listPanel.getWidth() + 10) / (THUMBNAIL_WIDTH + 10));
        if(listPanel.getLayout() instanceof GridLayout grid && grid.getColumns() == columns) return;

        listPanel.setLayout(new GridLayout(0, columns, 10, 10));
        listPanel.revalidate();
        listPanel.repaint();
    }

    private static BufferedImage loadThumbnail(Path screenshot) throws IOException
    {
        try(ImageInputStream input = ImageIO.createImageInputStream(screenshot.toFile()))
        {
            if(input == null) throw new IOException("Unable to read image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new IOException("Unsupported or invalid image");

            ImageReader reader = readers.next();
            try
            {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int sample = Math.clamp(width / THUMBNAIL_WIDTH, 1, height / THUMBNAIL_HEIGHT);
                ImageReadParam params = reader.getDefaultReadParam();
                params.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage image = reader.read(0, params);
                if(image == null) throw new IOException("Unsupported or invalid image");

                double scale = Math.min((double) THUMBNAIL_WIDTH / image.getWidth(),
                        (double) THUMBNAIL_HEIGHT / image.getHeight());
                int thumbnailWidth = Math.max(1, (int) (image.getWidth() * scale));
                int thumbnailHeight = Math.max(1, (int) (image.getHeight() * scale));
                BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight,
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = thumbnail.createGraphics();
                try
                {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(image, 0, 0, thumbnailWidth, thumbnailHeight, null);
                }
                finally
                {
                    graphics.dispose();
                }
                return thumbnail;
            }
            finally
            {
                reader.dispose();
            }
        }
    }

    private record Thumbnail(Path path, BufferedImage image, IOException error)
    {
        private Thumbnail(Path path, BufferedImage image)
        {
            this(path, image, null);
        }
    }

    private static boolean isImageFile(Path path)
    {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    @Override
    public JPopupMenu createPopup(Instance instance)
    {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem openFolder = new JMenuItem("Open Folder");
        openFolder.addActionListener(e ->
        {
            try
            {
                SystemUtils.openDirectory(instance.getScreenshotsFolder().toFile());
            }
            catch (IOException ex)
            {
                new ErrorDialog(main.client, ex, "opening directory");
                throw new RuntimeException(ex);
            }
        });
        popupMenu.add(openFolder);

        return popupMenu;
    }
}
