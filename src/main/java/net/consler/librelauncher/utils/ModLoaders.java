package net.consler.librelauncher.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncher.exceptions.FailedToLoadModVersionsException;
import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import static javafx.collections.FXCollections.observableArrayList;

public class ModLoaders
{
    public static ObservableList<String> list()
    {
        ObservableList<String> modLoaders = observableArrayList();
        modLoaders.add("Vanilla");
        modLoaders.add("Fabric");
        modLoaders.add("Forge");
        modLoaders.add("Neoforge");

        return modLoaders;
    }

    public static ObservableList<String> listFabricVersions()
    {
        return listFabricVersions(null);
    }

    public static ObservableList<String> listFabricVersions(String minecraftVersion)
    {
        ObservableList<String> versions = observableArrayList();
        try
        {
            URL url = URI.create("https://meta.fabricmc.net/v2/versions/loader").toURL();
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();

            for (int i = 0; i < jsonArray.size(); i++)
            {
                JsonObject obj = jsonArray.get(i).getAsJsonObject();
                String loaderVersion = obj.get("version").getAsString();
                String gameVersion = obj.has("game_version") ? obj.get("game_version").getAsString() : null;
                if (minecraftVersion != null && !minecraftVersion.isBlank() && gameVersion != null && !minecraftVersion.equals(gameVersion))
                {
                    continue;
                }
                versions.add(loaderVersion);
            }
        }
        catch (Exception e)
        {
            throw new FailedToLoadModVersionsException("Failed to load Fabric versions");
        }
        return versions;
    }

    public static ObservableList<String> listForgeVersions()
    {
        return listForgeVersions(null);
    }

    public static ObservableList<String> listForgeVersions(String minecraftVersion)
    {
        return fetchVersionsFromMavenXML("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml", minecraftVersion);
    }

    public static ObservableList<String> listNeoforgeVersions()
    {
        return listNeoforgeVersions(null);
    }

    public static ObservableList<String> listNeoforgeVersions(String minecraftVersion)
    {
        return fetchVersionsFromMavenXML("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml", minecraftVersion);
    }

    private static ObservableList<String> fetchVersionsFromMavenXML(String metadataUrl, String minecraftVersion)
    {
        ObservableList<String> versions = observableArrayList();
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(URI.create(metadataUrl).toURL().openStream());

            String normalizedMcVersion = minecraftVersion == null ? null : minecraftVersion.trim();
            if (normalizedMcVersion != null && normalizedMcVersion.startsWith("1."))
            {
                normalizedMcVersion = normalizedMcVersion.substring(2);
            }

            NodeList versionNodes = doc.getElementsByTagName("version");
            for (int i = 0; i < versionNodes.getLength(); i++)
            {
                String version = versionNodes.item(i).getTextContent();
                if (normalizedMcVersion != null && !normalizedMcVersion.isBlank() && !matchesMinecraftVersion(version, normalizedMcVersion))
                {
                    continue;
                }
                versions.add(version);
            }
        }
        catch (Exception e)
        {
            throw new FailedToLoadModVersionsException("Failed to load neoforge/forge versions from Maven XML");
        }
        return versions;
    }

    private static boolean matchesMinecraftVersion(String loaderVersion, String minecraftVersion)
    {
        if (loaderVersion == null || loaderVersion.isBlank())
        {
            return false;
        }

        if (loaderVersion.startsWith(minecraftVersion + "-"))
        {
            return true;
        }

        String normalizedLoader = loaderVersion.replace("-beta", "").replace("-alpha", "");
        String[] mcParts = minecraftVersion.split("\\.");
        if (mcParts.length >= 2)
        {
            String shortVersion = mcParts[0] + "." + mcParts[1];
            return normalizedLoader.startsWith(shortVersion + ".") || normalizedLoader.startsWith(shortVersion + "-");
        }

        return false;
    }
}