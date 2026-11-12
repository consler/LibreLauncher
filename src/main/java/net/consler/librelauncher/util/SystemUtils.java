package net.consler.librelauncher.util;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SystemUtils
{
    public static void openDirectory(File file) throws IOException
    {
        Desktop.getDesktop().open(file);
    }

    public static void openFile(File file) throws IOException, UnsupportedOperationException
    {
        if (!Desktop.isDesktopSupported())
            throw new UnsupportedOperationException("Desktop integration isn't available");

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN))
            throw new UnsupportedOperationException("Opening files isn't supported");

        desktop.open(file);
    }

    public static File importFile(File directory, String extension)
    {
        JFileChooser fileChooser = new JFileChooser(directory);

        fileChooser.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return file.isDirectory() || file.getName().endsWith(extension);
            }

            @Override
            public String getDescription()
            {
                return "*" + extension;
            }
        });

        int result = fileChooser.showOpenDialog(null);
        return result == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
    }

    public static void exportFile(File directory, File fileToSave) throws IOException
    {
        JFileChooser chooser = new JFileChooser(directory);
        chooser.setSelectedFile(new File(directory, fileToSave.getName()));

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
        {
            File destination = chooser.getSelectedFile();
            Files.copy(fileToSave.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static File chooseSaveFile(File directory, String suggestedName, String extension)
    {
        JFileChooser chooser = new JFileChooser(directory);
        chooser.setSelectedFile(new File(directory, suggestedName));

        while (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();

            if (!file.getName().toLowerCase().endsWith(extension))
                file = new File(file.getParentFile(), file.getName() + extension);

            if (!file.exists() || JOptionPane.showConfirmDialog(chooser,
                    file.getName() + " already exists. Replace it?",
                    "Confirm overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            {
                return file;
            }
        }
        return null;
    }
}
