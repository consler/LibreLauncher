package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncherlib.util.JavaVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class AutoDetectJavaDialog extends JDialog
{
    private final JFrame main;
    private final JTextField javaPathField;
    private final JPanel listPanel = new JPanel();
    private final ButtonGroup group = new ButtonGroup();
    private final JButton doneButton = new JButton("Done");

    private JavaVersion selected;

    public AutoDetectJavaDialog(JFrame parentFrame, JTextField javaPathField)
    {
        super(parentFrame, "Detect Java", true);
        this.main = parentFrame;
        this.javaPathField = javaPathField;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        addLabel("Detecting Java installations...");

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        doneButton.setEnabled(false);
        doneButton.addActionListener(e ->
        {
            if (selected != null)
                javaPathField.setText(selected.executable().toString());
            onDone();
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(doneButton);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(doneButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setSize(700, 400);
        setLocationRelativeTo(parentFrame);

        startDetection();

        setVisible(true);
    }

    private void startDetection()
    {
        new SwingWorker<List<JavaVersion>, Void>()
        {
            @Override
            protected List<JavaVersion> doInBackground()
            {
                return JavaVersion.autoDetect();
            }

            @Override
            protected void done()
            {
                try
                {
                    populate(get());
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    new ErrorDialog(main, e, "detecting java");
                }
            }
        }.execute();
    }

    private void populate(List<JavaVersion> versions)
    {
        listPanel.removeAll();

        if (versions.isEmpty())
        {
            addLabel("No Java installations found.");
        }
        else
        {
            Path currentPath = currentFieldPath();

            for (JavaVersion version : versions)
            {
                JRadioButton radio = new JRadioButton(version.toString());
                radio.setAlignmentX(Component.LEFT_ALIGNMENT);
                radio.setToolTipText(version.toString());
                radio.setFont(radio.getFont().deriveFont(14f));
                radio.addActionListener(e ->
                {
                    selected = version;
                    doneButton.setEnabled(true);
                });

                group.add(radio);
                listPanel.add(radio);
                listPanel.add(Box.createVerticalStrut(25));
                listPanel.add(new JSeparator(SwingConstants.HORIZONTAL));

                if (currentPath != null && currentPath.equals(version.executable()))
                {
                    radio.setSelected(true);
                    selected = version;
                    doneButton.setEnabled(true);
                }
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void addLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(label);
    }

    private Path currentFieldPath()
    {
        String text = javaPathField.getText();
        if (text == null || text.isBlank()) return null;
        try
        {
            return Paths.get(text.trim()).toRealPath();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public abstract void onDone();
}