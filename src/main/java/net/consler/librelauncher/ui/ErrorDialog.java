package net.consler.librelauncher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;

public class ErrorDialog extends JDialog
{
    public ErrorDialog(JFrame parentFrame, Exception ex)
    {
        this(parentFrame, ex, null);
    }

    public ErrorDialog(JFrame parentFrame, Exception ex, String action)
    {
        super(parentFrame, true);

        setTitle("LibreLauncher error");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        String stackTrace = getStackTraceAsString(ex);

        JLabel messageLabel = new JLabel();
        if(action != null && !action.isBlank()) messageLabel.setText("An error occurred during " + action + ":");
        else messageLabel.setText("An unexpected error!");

        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(messageLabel, BorderLayout.NORTH);

        JButton copy = new JButton("Copy Error");
        JButton report = new JButton("Report error");
        JButton close = new JButton("Close");

        copy.addActionListener(e ->
        {
            StringSelection selection = new StringSelection(stackTrace);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        report.addActionListener(e ->
        {
            try
            {
                Desktop.getDesktop().browse(URI.create("https://github.com/consler/librelauncher/issues"));
            } catch (Exception ex2)
            {
                new ErrorDialog(parentFrame, ex2, "opening browser");
            }
        });

        close.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 10));
        buttonPanel.add(copy);
        buttonPanel.add(report);
        buttonPanel.add(close);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parentFrame);
    }

    private static String getStackTraceAsString(Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}