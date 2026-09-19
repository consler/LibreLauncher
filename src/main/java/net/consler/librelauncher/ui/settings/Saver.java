package net.consler.librelauncher.ui.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Saver
{
    private final Path file;
    private final Properties properties = new Properties();

    public Saver(Path file)
    {
        this.file = file;
        load();
    }

    public Saver(File file)
    {
        this.file = file.toPath();
        load();
    }

    private synchronized void load()
    {
        if (Files.exists(file))
        {
            try (InputStream in = Files.newInputStream(file))
            {
                properties.load(in);
            }
            catch (IOException ignored)
            {
            }
        }
    }

    public synchronized String get(String key)
    {
        return properties.getProperty(key);
    }

    public synchronized void set(String key, String value)
    {
        if (value == null) properties.remove(key);
        else properties.setProperty(key, value);

        save();
    }

    public synchronized void save()
    {
        try (OutputStream out = Files.newOutputStream(file))
        {
            properties.store(out, "LibreLauncher Configuration");
        }
        catch (IOException ignored) {}
    }

    public synchronized void remove(String key)
    {
        properties.remove(key);
    }
}