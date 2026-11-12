package net.consler.librelauncher.util;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;

import javax.swing.*;
import java.util.Optional;

public class InstanceUtils
{

    public static synchronized boolean isRunningAndClean(String instanceId)
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

    public static synchronized boolean isAuthenticating(String instanceId)
    {
        InstancesConfig instancesConfig = new InstancesConfig();
        return Boolean.parseBoolean(instancesConfig.getProperty(instanceId, "authenticating", "false"));
    }

    public static synchronized void killInstance(String instanceId)
    {
        InstancesConfig instancesConfig = new InstancesConfig();
        String pid = instancesConfig.getProperty(instanceId, "pid");
        if (pid == null) return;
        Optional<ProcessHandle> process = ProcessHandle.of(Long.parseLong(pid));
        if (process.isPresent() && process.get().isAlive())
        {
            process.get().destroy();
            process.get().onExit().thenAccept(action ->
                    SwingUtilities.invokeLater(() ->
                            Main.getInstance().refreshInstanceViews()));
        }
    }
}
