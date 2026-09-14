package net.consler.librelauncher.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Versions
{
    private static final String MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    public record MCVersion(String id, String type) { }

    private static List<MCVersion> cachedVersions = null;

    public static synchronized List<MCVersion> fetchAll()
    {
        if (cachedVersions != null)
        {
            return cachedVersions;
        }

        List<MCVersion> list = new ArrayList<>();
        try
        {
            URL url = URI.create(MANIFEST_URL).toURL();
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray versions = json.getAsJsonArray("versions");

            for (JsonElement version : versions)
            {
                JsonObject obj = version.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String type = obj.get("type").getAsString();
                list.add(new MCVersion(id, type));
            }
            cachedVersions = list;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getFiltered(boolean releases, boolean snapshots, boolean betas, boolean alphas)
    {
        List<MCVersion> all = fetchAll();
        List<String> filtered = new ArrayList<>();

        for (MCVersion version : all)
        {
            String type = version.type().toLowerCase();
            String id = version.id().toLowerCase();

            boolean isRelease = "release".equals(type);
            boolean isSnapshot = "snapshot".equals(type) && !id.contains("experimental");
            boolean isBeta = "old_beta".equals(type);
            boolean isAlpha = "old_alpha".equals(type);

            if ((isRelease && releases) || (isSnapshot && snapshots) || (isBeta && betas) || (isAlpha && alphas)) filtered.add(version.id());
        }

        return filtered;
    }
}