package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.StepMapping;
import com.google.gson.Gson;
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
import java.util.Set;

public final class StepMappings
{
    private static final Logger log = LoggerFactory.getLogger(StepMappings.class);
    private static final String RESOURCE = "/step_mappings.json";

    private final Map<StepId, StepMapping> byId;

    private StepMappings(Map<StepId, StepMapping> byId)
    {
        this.byId = Collections.unmodifiableMap(byId);
    }

    public static StepMappings empty()
    {
        return new StepMappings(Collections.emptyMap());
    }

    public static StepMappings loadBundled()
    {
        InputStream in = StepMappings.class.getResourceAsStream(RESOURCE);
        if (in == null)
        {
            throw new IllegalStateException("Missing classpath resource " + RESOURCE);
        }
        Gson gson = new Gson();
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            JsonObject root = gson.fromJson(r, JsonObject.class);
            JsonObject steps = root.getAsJsonObject("steps");
            Map<StepId, StepMapping> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : steps.entrySet())
            {
                StepId id;
                try
                {
                    id = StepId.parse(e.getKey());
                }
                catch (IllegalArgumentException ex)
                {
                    log.warn("Skipping malformed step_mappings key: {}", e.getKey());
                    continue;
                }
                StepMapping m = gson.fromJson(e.getValue(), StepMapping.class);
                map.put(id, m);
            }
            return new StepMappings(map);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to parse " + RESOURCE, e);
        }
    }

    public int size()
    {
        return byId.size();
    }

    public Optional<StepMapping> findById(StepId id)
    {
        return Optional.ofNullable(byId.get(id));
    }

    public Set<StepId> allIds()
    {
        return byId.keySet();
    }
}
