package net.consler.librelauncher.instance;

import dev.dirs.UserDirectories;
import net.consler.librelauncher.util.SystemUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Exporter
{
    private static final int BUFFER_SIZE = 256 * 1024;

    private static final Set<String> INCOMPRESSIBLE = Set.of(
            "jar", "zip", "gz", "xz", "7z", "rar", "zst", "bz2",
            "png", "jpg", "jpeg", "gif", "webp",
            "ogg", "mp3", "mp4", "mkv", "webm", "flac"
    );

    /**
     * Asks for a destination first, then zips straight into it (no temp copy).
     * Call this off the UI thread if you want a responsive UI; for that, call
     * SystemUtils.chooseSaveFile on the UI thread and zipDirectory in the background.
     */
    public static void export(File directoryToZip) throws IOException
    {
        File destination = SystemUtils.chooseSaveFile(
                new File(UserDirectories.get().downloadDir),
                directoryToZip.getName() + ".zip",
                ".zip");
        if (destination == null) return;

        Path zipPath = destination.toPath().toAbsolutePath().normalize();

        try
        {
            zipDirectory(directoryToZip.toPath(), zipPath);
        }
        catch (IOException | RuntimeException e)
        {
            Files.deleteIfExists(zipPath);
            throw e;
        }
    }

    private static void zipDirectory(Path sourceDir, Path zipPath) throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE];

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(zipPath), BUFFER_SIZE));
             Stream<Path> paths = Files.walk(sourceDir))
        {
            String rootName = sourceDir.getFileName().toString();

            for (Path path : (Iterable<Path>) paths::iterator)
            {
                if (path.equals(sourceDir) || path.toAbsolutePath().normalize().equals(zipPath)) continue;

                String entryName = rootName + "/" + sourceDir.relativize(path).toString().replace(File.separatorChar, '/');

                if (Files.isDirectory(path))
                {
                    zos.putNextEntry(new ZipEntry(entryName + "/"));
                    zos.closeEntry();
                }
                else
                {
                    zos.setLevel(isIncompressible(path) ? Deflater.NO_COMPRESSION : Deflater.BEST_SPEED);

                    zos.putNextEntry(new ZipEntry(entryName));
                    try (InputStream in = Files.newInputStream(path))
                    {
                        int read;
                        while ((read = in.read(buffer)) != -1)
                        {
                            zos.write(buffer, 0, read);
                        }
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    private static boolean isIncompressible(Path path)
    {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;
        return INCOMPRESSIBLE.contains(name.substring(dot + 1).toLowerCase());
    }
}