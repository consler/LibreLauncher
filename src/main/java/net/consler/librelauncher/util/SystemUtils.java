package net.consler.librelauncher.util;

import net.consler.librelauncher.settings.InstancesConfig;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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
}
