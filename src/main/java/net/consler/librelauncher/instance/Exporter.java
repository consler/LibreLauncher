package net.consler.librelauncher.instance;

import dev.dirs.UserDirectories;
import net.consler.librelauncher.util.SystemUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    public static void export(File directoryToZip) throws IOException
    {
        String downloadsDir = UserDirectories.get().downloadDir;
        File downloadDirectory = downloadsDir == null ? null : new File(downloadsDir);
        File destination = SystemUtils.chooseSaveFile(
                downloadDirectory,
                directoryToZip.getName() + ".zip",
                ".zip");
        if (destination == null) return;

        Path zipPath = destination.toPath().toAbsolutePath().normalize();
        Path temporaryZip = Files.createTempFile(zipPath.getParent(), ".librelauncher-export-", ".tmp");

        try
        {
            zipDirectory(directoryToZip.toPath(), zipPath, temporaryZip);
            try
            {
                Files.move(temporaryZip, zipPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                throw new IOException("The destination filesystem does not support atomic export", e);
            }
        }
        finally
        {
            Files.deleteIfExists(temporaryZip);
        }
    }

    private static void zipDirectory(Path sourceDir, Path zipPath, Path temporaryZip) throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        Path normalizedZipPath = zipPath.toAbsolutePath().normalize();
        Path normalizedTemporaryZip = temporaryZip.toAbsolutePath().normalize();

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(temporaryZip), BUFFER_SIZE));
             Stream<Path> paths = Files.walk(sourceDir))
        {
            String rootName = sourceDir.getFileName().toString();

            for (Path path : (Iterable<Path>) paths::iterator)
            {
                Path normalizedPath = path.toAbsolutePath().normalize();
                if (path.equals(sourceDir) || normalizedPath.equals(normalizedZipPath)
                        || normalizedPath.equals(normalizedTemporaryZip)) continue;

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