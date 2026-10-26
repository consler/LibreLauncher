package net.consler.librelauncher.util;

import net.consler.librelauncher.settings.InstancesConfig;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class SystemUtils
{
    public static void openDirectory(File file) throws IOException
    {
        Desktop.getDesktop().open(file);
    }

    public static boolean isRunningAndClean(String instanceId)
    {
        InstancesConfig instancesConfig = new InstancesConfig();
        String pid = instancesConfig.getProperty(instanceId, "pid");
        if (pid == null) return false;
        Optional<ProcessHandle> process = ProcessHandle.of(Long.parseLong(pid));
        if(process.isEmpty() || !process.get().isAlive())
        {
            instancesConfig.saveProperty(instanceId, "pid", null);
            instancesConfig.save();
            return false;
        }
        return true;
    }

    public static void killInstance(String instanceId)
    {
        InstancesConfig instancesConfig = new InstancesConfig();
        String pid = instancesConfig.getProperty(instanceId, "pid");
        if (pid == null) return;
        Optional<ProcessHandle> process = ProcessHandle.of(Long.parseLong(pid));
        if (process.isPresent() && process.get().isAlive())
        {
            process.get().destroy();
        }
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
