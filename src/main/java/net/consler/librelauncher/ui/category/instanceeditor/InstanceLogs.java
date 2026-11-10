package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncherlib.instance.Instance;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class InstanceLogs extends InstanceItems
{
    private JTextArea textArea;
    private JComboBox<String> logCombo;
    private final DefaultComboBoxModel<String> logsModel = new DefaultComboBoxModel<>();

    private Path currentLogsFolder;
    private SwingWorker<String, Void> loader;
    private boolean updatingModel = false;

    public InstanceLogs(Main main)
    {
        super(main, "Logs", "Log");
    }

    @Override
    public void populate(Instance instance, String search)
    {
        buildUi();

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

        logsModel.setSelectedItem(names.getFirst());
        loadLog(logsFolder.resolve(names.getFirst()), true);
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
        if(Boolean.parseBoolean(new SettingsSaver().get("wrap_logs_lines")))
        {
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
        }

        JScrollPane scrollPane = new JScrollPane(textArea);

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

    private void loadLog(Path file, boolean scrollToEnd)
    {
        if(loader != null) loader.cancel(true);

        textArea.setText("Loading " + file.getFileName() + "...");

        loader = new SwingWorker<>()
        {
            @Override
            protected String doInBackground() throws IOException
            {
                return readLog(file);
            }

            @Override
            protected void done()
            {
                if(isCancelled()) return;

                try
                {
                    textArea.setText(get());
                    textArea.setCaretPosition(scrollToEnd ? textArea.getDocument().getLength() : 0);
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException ex)
                {
                    textArea.setText("");
                    new ErrorDialog(main.client, ex, "loading " + file.getFileName());
                }
            }
        };
        loader.execute();
    }

    private String readLog(Path file) throws IOException
    {
        InputStream in = Files.newInputStream(file);
        if(file.getFileName().toString().toLowerCase().endsWith(".gz"))
            in = new GZIPInputStream(in);

        try(BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            return br.readAllAsString();
        }
    }

    @Override
    public JPopupMenu createPopup(Instance instance)
    {
        return null;
    }
}