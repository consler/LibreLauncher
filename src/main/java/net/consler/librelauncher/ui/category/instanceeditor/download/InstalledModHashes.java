package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncherlib.integration.modrinth.ModrinthVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class InstalledModHashes
{
    private InstalledModHashes()
    {
    }

    static Set<String> read(Path modsFolder) throws IOException
    {
        if (!Files.isDirectory(modsFolder)) return Set.of();

        Set<String> hashes = new HashSet<>();
        try (var files = Files.list(modsFolder))
        {
            for (Path path : files.filter(Files::isRegularFile)
                    .filter(InstalledModHashes::isModJar)
                    .toList())
                hashes.add(sha1(path));
        }
        return Set.copyOf(hashes);
    }

    static boolean containsAnyVersion(List<ModrinthVersion> versions, Set<String> installedHashes)
    {
        for (ModrinthVersion version : versions)
        {
            for (ModrinthVersion.VersionFile file : version.files())
            {
                Map<String, String> hashes = file.hashes();
                String sha1 = hashes == null ? null : hashes.get("sha1");
                if (sha1 != null && installedHashes.contains(sha1.toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }

    private static boolean isModJar(Path path)
    {
        String name = path.getFileName().toString();
        return name.endsWith(".jar") || name.endsWith(".jar.disabled");
    }

    static String sha1(Path file) throws IOException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(file))
            {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

}
