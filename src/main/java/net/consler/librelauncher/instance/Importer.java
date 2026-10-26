package net.consler.librelauncher.instance;

import dev.dirs.UserDirectories;
import net.consler.librelauncher.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Importer
{
    private static final int BUFFER_SIZE = 128 * 1024;

    private static final int THREADS = Math.clamp(Runtime.getRuntime().availableProcessors(), 2, 8);

    private static final ThreadLocal<byte[]> BUFFER = ThreadLocal.withInitial(() -> new byte[BUFFER_SIZE]);

    public static Path doImport(File directoryToImportTo) throws IOException
    {
        String downloadsDir = UserDirectories.get().downloadDir;

        File zipFile = SystemUtils.importFile(new File(downloadsDir), ".zip");
        if (zipFile == null) return null;

        unzip(zipFile.toPath(), directoryToImportTo.toPath());

        return Path.of(directoryToImportTo.getPath(), zipFile.getName().substring(0, zipFile.getName().length() - 4));
    }

    private static void unzip(Path zipPath, Path targetDir) throws IOException
    {
        Files.createDirectories(targetDir);
        Path normalizedTarget = targetDir.toAbsolutePath().normalize();

        try (ZipFile zip = new ZipFile(zipPath.toFile()))
        {
            List<ZipEntry> files = new ArrayList<>();
            List<Path> outPaths = new ArrayList<>();
            Set<Path> createdDirs = new HashSet<>();

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                Path outPath = normalizedTarget.resolve(entry.getName()).normalize();

                if (!outPath.startsWith(normalizedTarget))
                    throw new IOException("Blocked zip entry outside target dir: " + entry.getName());

                if (entry.isDirectory())
                {
                    Files.createDirectories(outPath);
                    continue;
                }

                Path parent = outPath.getParent();
                if (createdDirs.add(parent)) Files.createDirectories(parent);

                files.add(entry);
                outPaths.add(outPath);
            }

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            try
            {
                List<Future<?>> futures = new ArrayList<>(files.size());
                for (int i = 0; i < files.size(); i++)
                {
                    ZipEntry entry = files.get(i);
                    Path outPath = outPaths.get(i);
                    futures.add(pool.submit(() ->
                    {
                        extract(zip, entry, outPath);
                        return null;
                    }));
                }

                for (Future<?> future : futures)
                {
                    try
                    {
                        future.get();
                    }
                    catch (ExecutionException e)
                    {
                        if (e.getCause() instanceof IOException io) throw io;
                        throw new IOException(e.getCause());
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        throw new IOException("Import interrupted", e);
                    }
                }
            }
            finally
            {
                pool.shutdownNow();
                try { pool.awaitTermination(30, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private static void extract(ZipFile zip, ZipEntry entry, Path outPath) throws IOException
    {
        byte[] buffer = BUFFER.get();

        try (InputStream in = zip.getInputStream(entry);
             OutputStream out = Files.newOutputStream(outPath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE))
        {
            int read;
            while ((read = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read);
            }
        }
    }
}