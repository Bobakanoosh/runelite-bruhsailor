package com.bruhsailor.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class QuestRegistry
{
    private static final Logger log = LoggerFactory.getLogger(QuestRegistry.class);
    private static final String RESOURCE = "/quests.json";

    private final Map<String, QuestEntry> byEnumName;
    private final boolean enrichedFromRuntime;

    private QuestRegistry(Map<String, QuestEntry> byEnumName, boolean enrichedFromRuntime)
    {
        this.byEnumName = Collections.unmodifiableMap(byEnumName);
        this.enrichedFromRuntime = enrichedFromRuntime;
    }

    public static QuestRegistry create(PluginManager pluginManager)
    {
        Map<String, QuestEntry> map = new HashMap<>();
        loadBundled(map);
        boolean enriched = false;
        try
        {
            enriched = enrichFromRuntime(pluginManager, map);
        }
        catch (Throwable t)
        {
            log.warn("Quest Helper runtime enrichment failed; using bundled list only", t);
        }
        return new QuestRegistry(map, enriched);
    }

    private static void loadBundled(Map<String, QuestEntry> map)
    {
        InputStream in = QuestRegistry.class.getResourceAsStream(RESOURCE);
        if (in == null)
        {
            log.error("Missing classpath resource {}", RESOURCE);
            return;
        }
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            JsonObject root = new Gson().fromJson(r, JsonObject.class);
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

    private static boolean enrichFromRuntime(PluginManager pluginManager, Map<String, QuestEntry> map)
    {
        if (pluginManager == null) return false;
        Collection<Plugin> plugins = pluginManager.getPlugins();
        if (plugins == null) return false;

        Plugin qh = null;
        for (Plugin p : plugins)
        {
            if (p == null) continue;
            String cls = p.getClass().getName();
            if (cls.equals("com.questhelper.QuestHelperPlugin")
                || cls.endsWith(".QuestHelperPlugin"))
            {
                qh = p;
                break;
            }
        }
        if (qh == null) return false;

        // Find the QuestHelperQuest enum class via the QH plugin's classloader.
        Class<?> enumClass;
        try
        {
            enumClass = qh.getClass().getClassLoader()
                .loadClass("com.questhelper.questhelpers.QuestHelperQuest");
        }
        catch (ClassNotFoundException e)
        {
            log.warn("QuestHelperQuest enum not found via QH classloader", e);
            return false;
        }
        if (!enumClass.isEnum()) return false;

        // For each enum constant, take name() as the canonical id and try to
        // pull a friendlier display name off a getName()/getQuest().getName()
        // method. Fall back to a Title-Case version of the enum name.
        Object[] constants = enumClass.getEnumConstants();
        Method nameAccessor = findNoArgMethod(enumClass, "getName", "name");
        int added = 0;
        for (Object c : constants)
        {
            try
            {
                String enumName = ((Enum<?>) c).name();
                String display = null;
                if (nameAccessor != null)
                {
                    Object got = nameAccessor.invoke(c);
                    if (got instanceof String) display = (String) got;
                }
                if (display == null || display.isEmpty())
                {
                    display = humanize(enumName);
                }
                map.put(enumName, new QuestEntry(enumName, display));
                added++;
            }
            catch (Throwable t)
            {
                log.debug("Failed to read QH enum constant", t);
            }
        }
        return added > 0;
    }

    private static Method findNoArgMethod(Class<?> cls, String... names)
    {
        for (String n : names)
        {
            try
            {
                Method m = cls.getMethod(n);
                if (m.getReturnType() == String.class) return m;
            }
            catch (NoSuchMethodException ignored)
            {
                // try next
            }
        }
        return null;
    }

    private static String humanize(String enumName)
    {
        String lower = enumName.replace('_', ' ').toLowerCase();
        StringBuilder sb = new StringBuilder(lower.length());
        boolean capitalize = true;
        for (int i = 0; i < lower.length(); i++)
        {
            char ch = lower.charAt(i);
            if (capitalize && Character.isLetter(ch))
            {
                sb.append(Character.toUpperCase(ch));
                capitalize = false;
            }
            else
            {
                sb.append(ch);
                if (ch == ' ') capitalize = true;
            }
        }
        return sb.toString();
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

    public boolean enrichedFromRuntime()
    {
        return enrichedFromRuntime;
    }
}
