package com.bruhsailor.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class QuestRegistry
{
    private static final Logger log = LoggerFactory.getLogger(QuestRegistry.class);
    private static final String RESOURCE = "/quests.json";

    private final Map<String, QuestEntry> byEnumName;

    private QuestRegistry(Map<String, QuestEntry> byEnumName)
    {
        this.byEnumName = Collections.unmodifiableMap(byEnumName);
    }

    public static QuestRegistry create(Gson gson)
    {
        Map<String, QuestEntry> map = new HashMap<>();
        loadBundled(map, gson);
        return new QuestRegistry(map);
    }

    private static void loadBundled(Map<String, QuestEntry> map, Gson gson)
    {
        InputStream in = QuestRegistry.class.getResourceAsStream(RESOURCE);
        if (in == null)
        {
            log.error("Missing classpath resource {}", RESOURCE);
            return;
        }
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            JsonObject root = gson.fromJson(r, JsonObject.class);
            JsonArray quests = root.getAsJsonArray("quests");
            for (JsonElement e : quests)
            {
                JsonObject q = e.getAsJsonObject();
                String enumName = q.get("enumName").getAsString();
                String displayName = q.get("displayName").getAsString();
                java.util.List<String> aliases = new java.util.ArrayList<>();
                if (q.has("aliases") && q.get("aliases").isJsonArray())
                {
                    for (JsonElement a : q.getAsJsonArray("aliases")) aliases.add(a.getAsString());
                }
                if (q.has("shortAliases") && q.get("shortAliases").isJsonArray())
                {
                    for (JsonElement a : q.getAsJsonArray("shortAliases")) aliases.add(a.getAsString());
                }
                map.put(enumName, new QuestEntry(enumName, displayName, aliases));
            }
        }
        catch (Exception e)
        {
            log.error("Failed to parse {}", RESOURCE, e);
        }
    }

    public int size()
    {
        return byEnumName.size();
    }

    public Optional<QuestEntry> resolve(String enumName)
    {
        if (enumName == null) return Optional.empty();
        return Optional.ofNullable(byEnumName.get(enumName));
    }
}
