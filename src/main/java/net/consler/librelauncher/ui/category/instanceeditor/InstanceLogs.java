package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncherlib.instance.Instance;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class InstanceLogs extends InstanceItems
{
    private static final int CHUNK_SIZE = 64 * 1024;
    private static final long POLL_INTERVAL_MS = 100;
    private static final int MAX_CHARS = 4_000_000;
    private static final long MAX_INITIAL_BYTES = 4L * 1024 * 1024;

    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JComboBox<String> logCombo;
    private final DefaultComboBoxModel<String> logsModel = new DefaultComboBoxModel<>();

    private Path currentLogsFolder;
    private LogLoader loader;
    private boolean updatingModel = false;

    private volatile boolean showing = true;

    private record Chunk(String text, boolean reset) {}

    public InstanceLogs(Main main)
    {
        super(main, "Logs", "Log");
    }

    @Override
    public void populate(Instance instance, String search)
    {
        buildUi();

        stopLoader();

        Path logsFolder = instance.getLogsFolder();
        if(logsFolder == null || !Files.isDirectory(logsFolder)) return;
        currentLogsFolder = logsFolder;

        List<String> names = listLogs(logsFolder);

        updatingModel = true;
        logsModel.removeAllElements();
        names.forEach(logsModel::addElement);
        updatingModel = false;

        if(names.isEmpty())
        {
            textArea.setText("");
            return;
        }

        String first = names.getFirst();
        logsModel.setSelectedItem(first);
        loadLog(logsFolder.resolve(first), first.equals("latest.log"));
    }

    private void buildUi()
    {
        if(textArea != null) return;

        panel.remove(searchField);

        logCombo = new JComboBox<>(logsModel);
        logCombo.addActionListener(e ->
        {
            if(updatingModel || currentLogsFolder == null) return;

            String name = (String) logCombo.getSelectedItem();
            if(name == null) return;

            loadLog(currentLogsFolder.resolve(name), name.equals("latest.log"));
        });

        textArea = new JTextArea();
        textArea.setEditable(false);
        ((DefaultCaret) textArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        if(Boolean.parseBoolean(new SettingsSaver().get("wrap_logs_lines")))
        {
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
        }
        textArea.addHierarchyListener(e ->
        {
            if((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
                showing = textArea.isShowing();
        });

        scrollPane = new JScrollPane(textArea);

        panel.add(logCombo, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    private List<String> listLogs(Path logsFolder)
    {
        List<String> names = new ArrayList<>();

        try(Stream<Path> stream = Files.list(logsFolder))
        {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted(Comparator.comparing((String n) -> !n.equals("latest.log"))
                            .thenComparing(Comparator.reverseOrder()))
                    .forEach(names::add);
        }
        catch (IOException e)
        {
            new ErrorDialog(main.client, e, "listing logs");
        }

        return names;
    }

    private void stopLoader()
    {
        if(loader != null)
        {
            loader.cancel(true);
            loader = null;
        }
    }

    private void loadLog(Path file, boolean live)
    {
        stopLoader();

        textArea.setText("Loading " + file.getFileName() + "...");

        loader = new LogLoader(file, live);
        loader.execute();
    }

    private boolean isAtBottom()
    {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 20;
    }

    private final class LogLoader extends SwingWorker<Void, Chunk>
    {
        private final Path file;
        private final boolean live;
        private boolean started = false; // EDT only

        LogLoader(Path file, boolean live)
        {
            this.file = file;
            this.live = live;
        }

        @Override
        protected Void doInBackground() throws Exception
        {
            publish(new Chunk("", true)); // clears the "Loading..." text

            if(file.getFileName().toString().toLowerCase().endsWith(".gz"))
                readGzip();
            else
                readAndFollow();

            return null;
        }

        private void readGzip() throws IOException
        {
            try(InputStream raw = Files.newInputStream(file);
                Reader reader = new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))
            {
                char[] buf = new char[CHUNK_SIZE];
                int n;
                while(!isCancelled() && (n = reader.read(buf)) != -1)
                    publish(new Chunk(new String(buf, 0, n), false));
            }
        }

        private void readAndFollow() throws IOException, InterruptedException
        {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            ByteBuffer bytes = ByteBuffer.allocate(CHUNK_SIZE);
            CharBuffer chars = CharBuffer.allocate(CHUNK_SIZE);

            long position = -1;

            while(!isCancelled())
            {
                if(showing)
                {
                    try
                    {
                        long size = Files.size(file);

                        if(position < 0)
                        {
                            position = Math.max(0, size - MAX_INITIAL_BYTES);
                            if(position > 0)
                                publish(new Chunk("[... earlier output skipped ...]\n", false));
                        }
                        else if(size < position)
                        {
                            publish(new Chunk("", true));
                            position = 0;
                            decoder.reset();
                            bytes.clear();
                        }

                        if(size > position)
                            position = readFrom(position, size, decoder, bytes, chars);
                    }
                    catch (NoSuchFileException ignored)
                    {
                    }
                }

                if(!live) return;
                Thread.sleep(POLL_INTERVAL_MS);
            }
        }

        private long readFrom(long position, long size, CharsetDecoder decoder,
                              ByteBuffer bytes, CharBuffer chars) throws IOException
        {
            try(FileChannel ch = FileChannel.open(file, StandardOpenOption.READ))
            {
                ch.position(position);

                while(!isCancelled() && position < size)
                {
                    int n = ch.read(bytes);
                    if(n <= 0) break;
                    position += n;

                    bytes.flip();
                    decode(decoder, bytes, chars);
                    bytes.compact();
                }
            }
            return position;
        }

        private void decode(CharsetDecoder decoder, ByteBuffer bytes, CharBuffer chars)
        {
            while(true)
            {
                CoderResult result = decoder.decode(bytes, chars, false);
                chars.flip();
                if(chars.hasRemaining()) publish(new Chunk(chars.toString(), false));
                chars.clear();
                if(result.isUnderflow()) break;
            }
        }

        @Override
        protected void process(List<Chunk> chunks)
        {
            if(isCancelled() || loader != this) return;

            boolean firstBatch = !started;
            started = true;
            boolean stick = live && (firstBatch || isAtBottom());

            StringBuilder sb = new StringBuilder();
            boolean clear = false;
            for(Chunk c : chunks)
            {
                if(c.reset())
                {
                    sb.setLength(0);
                    clear = true;
                }
                sb.append(c.text());
            }

            if(clear) textArea.setText(sb.toString());
            else if(!sb.isEmpty()) textArea.append(sb.toString());

            int excess = textArea.getDocument().getLength() - MAX_CHARS;
            if(excess > 0) textArea.replaceRange("", 0, excess);

            if(stick)
                textArea.setCaretPosition(textArea.getDocument().getLength());
            else if(firstBatch || clear)
                textArea.setCaretPosition(0);
        }

        @Override
        protected void done()
        {
            if(isCancelled()) return;

            try
            {
                get();
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            catch (ExecutionException ex)
            {
                new ErrorDialog(main.client, ex, "loading " + file.getFileName());
            }
        }
    }

    @Override
    public JPopupMenu createPopup(Instance instance)
    {
        return null;
    }
}