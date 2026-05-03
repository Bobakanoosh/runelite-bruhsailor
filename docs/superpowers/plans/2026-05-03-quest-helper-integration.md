# Quest Helper Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render clickable chips below each step's content for quests Quest Helper can open; soft-dependency on QH (chips degrade gracefully when QH isn't installed).

**Architecture:** Five new units under `com.bruhsailor.plugin`: a `QuestEntry` value type, a `QuestRegistry` that loads bundled `quests.json` and enriches at runtime via reflection on QH, a `StepMappings` repository for `step_mappings.json` (parallel to `GuideRepository`), a `QuestHelperBridge` that owns all QH reflection, and a `QuestChip` Swing factory. `BruhsailorPanel` gains a chip row in `currentStepHolder.SOUTH`; `BruhsailorPlugin` wires the new collaborators. Spec: `docs/superpowers/specs/2026-05-03-quest-helper-integration-design.md`.

**Tech Stack:** Java 11, JUnit 4, Mockito, Swing, RuneLite client API (`PluginManager`, `ColorScheme`, `FontManager`), Gson (transitive via RuneLite).

---

## File Structure

```
src/main/java/com/bruhsailor/plugin/
    QuestEntry.java                  [Task 2]
    model/StepMapping.java           [Task 3]
    model/Item.java                  [Task 3]
    StepMappings.java                [Task 4]
    QuestRegistry.java               [Task 5]
    QuestHelperBridge.java           [Task 6]
    QuestChip.java                   [Task 7]
    BruhsailorPanel.java             [Task 8 modify]
    BruhsailorPlugin.java            [Task 9 modify]
src/main/resources/
    quests.json                      [Task 1 — copy from tools/sidecar-gen/data/]
    step_mappings.json               (already present)
src/test/java/com/bruhsailor/plugin/
    StepMappingsTest.java            [Task 4]
    QuestRegistryTest.java           [Task 5]
    QuestHelperBridgeTest.java       [Task 6]
    QuestChipTest.java               [Task 7]
```

---

## Task 1: Bundle `quests.json` as a classpath resource

**Files:**
- Create: `src/main/resources/quests.json` (copied from `tools/sidecar-gen/data/quests.json`)

- [ ] **Step 1: Copy the file**

```bash
cp tools/sidecar-gen/data/quests.json src/main/resources/quests.json
```

- [ ] **Step 2: Sanity-check the copy**

```bash
python -c "import json; d=json.load(open('src/main/resources/quests.json',encoding='utf-8')); print('quests:', len(d['quests']))"
```
Expected output: `quests: 266` (or larger if the sidecar regenerated since this plan was written; never smaller).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/quests.json
git commit -m "data: bundle quests.json (266 QH quest entries) as plugin resource"
```

---

## Task 2: `QuestEntry` value type

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/QuestEntry.java`

This type has no logic worth a unit test; it's exercised through `QuestRegistry` tests in Task 5.

- [ ] **Step 1: Write `QuestEntry.java`**

```java
package com.bruhsailor.plugin;

import java.util.Objects;

public final class QuestEntry
{
    private final String enumName;
    private final String displayName;

    public QuestEntry(String enumName, String displayName)
    {
        this.enumName = Objects.requireNonNull(enumName, "enumName");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    public String enumName() { return enumName; }
    public String displayName() { return displayName; }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof QuestEntry)) return false;
        QuestEntry other = (QuestEntry) o;
        return enumName.equals(other.enumName) && displayName.equals(other.displayName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(enumName, displayName);
    }

    @Override
    public String toString()
    {
        return "QuestEntry{" + enumName + " -> " + displayName + "}";
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/QuestEntry.java
git commit -m "feat: QuestEntry value type"
```

---

## Task 3: Model POJOs for `step_mappings.json`

`StepMapping` and `Item` are Gson-populated data carriers. No tests in this task; covered through `StepMappingsTest` in Task 4.

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/model/StepMapping.java`
- Create: `src/main/java/com/bruhsailor/plugin/model/Item.java`

- [ ] **Step 1: Write `Item.java`**

```java
package com.bruhsailor.plugin.model;

public class Item
{
    public int id;
    public String name;
    public Integer qty;
    public String source;
}
```

- [ ] **Step 2: Write `StepMapping.java`**

```java
package com.bruhsailor.plugin.model;

import java.util.List;

public class StepMapping
{
    public String contentHash;
    public String title;
    public List<String> questIds;
    public List<Item> items;
    public List<String> abstractItems;
    public List<String> unresolvedItems;
    public boolean verified;
    public double verifierConfidence;
    public List<String> verifierFlags;
    public String verifierNotes;
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/model/StepMapping.java src/main/java/com/bruhsailor/plugin/model/Item.java
git commit -m "feat: model POJOs for step_mappings.json"
```

---

## Task 4: `StepMappings` repository

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/StepMappings.java`
- Test: `src/test/java/com/bruhsailor/plugin/StepMappingsTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.StepMapping;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class StepMappingsTest
{
    @Test
    public void loadsAll227StepIds()
    {
        StepMappings mappings = StepMappings.loadBundled();
        // Sanity check: every step id from the guide should have a mapping.
        // Guide is 227 entries; mappings.size() == 227 unless the JSON drifted.
        assertEquals(227, mappings.size());
    }

    @Test
    public void findByIdReturnsPresentForKnownStep()
    {
        StepMappings mappings = StepMappings.loadBundled();
        Optional<StepMapping> found = mappings.findById(StepId.parse("1.1.3"));
        assertTrue(found.isPresent());
        StepMapping m = found.get();
        assertNotNull(m.questIds);
        assertNotNull(m.items);
    }

    @Test
    public void findByIdReturnsEmptyForUnknownStep()
    {
        StepMappings mappings = StepMappings.loadBundled();
        assertFalse(mappings.findById(StepId.of(99, 99, 99)).isPresent());
    }

    @Test
    public void everyMappingHasNonNullQuestAndItemLists()
    {
        StepMappings mappings = StepMappings.loadBundled();
        mappings.allIds().forEach(id -> {
            StepMapping m = mappings.findById(id).orElseThrow(AssertionError::new);
            assertNotNull("questIds null for " + id, m.questIds);
            assertNotNull("items null for " + id, m.items);
        });
    }

    @Test
    public void emptyInstanceReturnsEmptyForAnyId()
    {
        StepMappings empty = StepMappings.empty();
        assertEquals(0, empty.size());
        assertFalse(empty.findById(StepId.parse("1.1.1")).isPresent());
    }
}
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew test --tests StepMappingsTest`
Expected: FAIL — `StepMappings` does not exist.

- [ ] **Step 3: Implement `StepMappings`**

```java
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
```

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew test --tests StepMappingsTest`
Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/StepMappings.java src/test/java/com/bruhsailor/plugin/StepMappingsTest.java
git commit -m "feat: StepMappings loads bundled step_mappings.json"
```

---

## Task 5: `QuestRegistry`

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/QuestRegistry.java`
- Test: `src/test/java/com/bruhsailor/plugin/QuestRegistryTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.bruhsailor.plugin;

import net.runelite.client.plugins.PluginManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuestRegistryTest
{
    private PluginManager pluginManager;

    @Before
    public void setUp()
    {
        pluginManager = mock(PluginManager.class);
        when(pluginManager.getPlugins()).thenReturn(Collections.emptyList());
    }

    @Test
    public void bundledLoadHasAtLeastTwoHundredEntries()
    {
        QuestRegistry registry = QuestRegistry.create(pluginManager);
        assertTrue(registry.size() >= 200);
    }

    @Test
    public void resolvesKnownEnumNameToDisplayName()
    {
        QuestRegistry registry = QuestRegistry.create(pluginManager);
        Optional<QuestEntry> e = registry.resolve("COOKS_ASSISTANT");
        assertTrue(e.isPresent());
        assertEquals("COOKS_ASSISTANT", e.get().enumName());
        assertEquals("Cook's Assistant", e.get().displayName());
    }

    @Test
    public void resolvesUnknownEnumNameToEmpty()
    {
        QuestRegistry registry = QuestRegistry.create(pluginManager);
        assertFalse(registry.resolve("NOT_A_REAL_QUEST_XYZ").isPresent());
    }

    @Test
    public void skillMarkerIsNotResolved()
    {
        // The sidecar uses bare skill names like "AGILITY" as filter tags;
        // those are not QH quests and must not produce a chip.
        QuestRegistry registry = QuestRegistry.create(pluginManager);
        assertFalse(registry.resolve("AGILITY").isPresent());
    }

    @Test
    public void enrichedFromRuntimeIsFalseWhenNoQHPluginPresent()
    {
        QuestRegistry registry = QuestRegistry.create(pluginManager);
        assertFalse(registry.enrichedFromRuntime());
    }
}
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew test --tests QuestRegistryTest`
Expected: FAIL — `QuestRegistry` does not exist.

- [ ] **Step 3: Implement `QuestRegistry`**

```java
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
                map.put(enumName, new QuestEntry(enumName, displayName));
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
```

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew test --tests QuestRegistryTest`
Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/QuestRegistry.java src/test/java/com/bruhsailor/plugin/QuestRegistryTest.java
git commit -m "feat: QuestRegistry loads bundled quests + enriches from QH at runtime"
```

---

## Task 6: `QuestHelperBridge`

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/QuestHelperBridge.java`
- Test: `src/test/java/com/bruhsailor/plugin/QuestHelperBridgeTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.bruhsailor.plugin;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuestHelperBridgeTest
{
    @Test
    public void noQHInstalledReportsNotInstalled()
    {
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugins()).thenReturn(Collections.emptyList());
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.isInstalled());
    }

    @Test
    public void openWithoutQHReturnsFalse()
    {
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugins()).thenReturn(Collections.emptyList());
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.open(new QuestEntry("COOKS_ASSISTANT", "Cook's Assistant")));
    }

    @Test
    public void unrelatedPluginsDoNotCountAsQH()
    {
        PluginManager pm = mock(PluginManager.class);
        Plugin other = mock(Plugin.class, "OtherPlugin");
        // Mockito-mocked Plugin's class is a generated subclass; its name will
        // not match "QuestHelperPlugin", so isInstalled() must stay false.
        when(pm.getPlugins()).thenReturn(Arrays.asList(other));
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.isInstalled());
    }

    @Test
    public void nullArgsAreSafe()
    {
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugins()).thenReturn(Collections.emptyList());
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.open(null));
    }
}
```

- [ ] **Step 2: Run tests, verify fail**

Run: `./gradlew test --tests QuestHelperBridgeTest`
Expected: FAIL — `QuestHelperBridge` does not exist.

- [ ] **Step 3: Implement `QuestHelperBridge`**

```java
package com.bruhsailor.plugin;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;

public final class QuestHelperBridge
{
    private static final Logger log = LoggerFactory.getLogger(QuestHelperBridge.class);
    private static final String QH_PLUGIN_CLASS = "com.questhelper.QuestHelperPlugin";
    private static final String QH_QUEST_ENUM = "com.questhelper.questhelpers.QuestHelperQuest";

    private final PluginManager pluginManager;
    private volatile boolean reflectionDisabled;
    private volatile Boolean cachedInstalled;

    public QuestHelperBridge(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public boolean isInstalled()
    {
        Boolean cached = cachedInstalled;
        if (cached != null) return cached;
        boolean found = locateQHPlugin() != null;
        cachedInstalled = found;
        return found;
    }

    public boolean open(QuestEntry quest)
    {
        if (quest == null) return false;
        if (reflectionDisabled) return false;
        Plugin qh = locateQHPlugin();
        if (qh == null) return false;
        try
        {
            return invokeDisplayPanel(qh, quest);
        }
        catch (Throwable t)
        {
            log.warn("Quest Helper open failed for {}; disabling reflection", quest.enumName(), t);
            reflectionDisabled = true;
            return false;
        }
    }

    private Plugin locateQHPlugin()
    {
        if (pluginManager == null) return null;
        Collection<Plugin> plugins;
        try
        {
            plugins = pluginManager.getPlugins();
        }
        catch (Throwable t)
        {
            log.debug("PluginManager.getPlugins threw", t);
            return null;
        }
        if (plugins == null) return null;
        for (Plugin p : plugins)
        {
            if (p == null) continue;
            String cls = p.getClass().getName();
            if (cls.equals(QH_PLUGIN_CLASS) || cls.endsWith(".QuestHelperPlugin"))
            {
                return p;
            }
        }
        return null;
    }

    private boolean invokeDisplayPanel(Plugin qh, QuestEntry quest) throws ReflectiveOperationException
    {
        ClassLoader cl = qh.getClass().getClassLoader();

        // Preferred: displayPanel(QuestHelperQuest)
        Class<?> enumClass = null;
        try
        {
            enumClass = cl.loadClass(QH_QUEST_ENUM);
        }
        catch (ClassNotFoundException ignored)
        {
            // fall through to String overload
        }

        if (enumClass != null)
        {
            Method m = findMethod(qh.getClass(), "displayPanel", enumClass);
            if (m != null)
            {
                Object enumValue = enumValueOf(enumClass, quest.enumName());
                if (enumValue != null)
                {
                    m.invoke(qh, enumValue);
                    return true;
                }
            }
        }

        // Fallback: displayPanel(String) — accepts the display name.
        Method strMethod = findMethod(qh.getClass(), "displayPanel", String.class);
        if (strMethod != null)
        {
            strMethod.invoke(qh, quest.displayName());
            return true;
        }

        // Last resort: a no-arg displayPanel() that just brings the panel up.
        Method noArg = findMethod(qh.getClass(), "displayPanel");
        if (noArg != null)
        {
            noArg.invoke(qh);
            return true;
        }

        log.warn("Quest Helper has no recognised displayPanel method; reflection disabled.");
        reflectionDisabled = true;
        return false;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... paramTypes)
    {
        try
        {
            return cls.getMethod(name, paramTypes);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private static Object enumValueOf(Class<?> enumClass, String name)
    {
        try
        {
            for (Object c : enumClass.getEnumConstants())
            {
                if (((Enum<?>) c).name().equals(name)) return c;
            }
        }
        catch (Throwable t)
        {
            // ignore — return null
        }
        return null;
    }
}
```

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew test --tests QuestHelperBridgeTest`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/QuestHelperBridge.java src/test/java/com/bruhsailor/plugin/QuestHelperBridgeTest.java
git commit -m "feat: QuestHelperBridge — soft-dep facade for opening QH"
```

---

## Task 7: `QuestChip`

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/QuestChip.java`
- Test: `src/test/java/com/bruhsailor/plugin/QuestChipTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.bruhsailor.plugin;

import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QuestChipTest
{
    private final QuestEntry cooks = new QuestEntry("COOKS_ASSISTANT", "Cook's Assistant");

    @Test
    public void labelTextEndsWithArrow()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);

        JComponent chip = QuestChip.create(cooks, bridge);
        JLabel label = (JLabel) chip;
        assertTrue(label.getText().contains("Cook's Assistant"));
        assertTrue(label.getText().contains("↗"));
    }

    @Test
    public void installedChipUsesHandCursor()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);

        JComponent chip = QuestChip.create(cooks, bridge);
        assertEquals(Cursor.HAND_CURSOR, chip.getCursor().getType());
        assertTrue(chip.getToolTipText().toLowerCase().contains("open"));
    }

    @Test
    public void uninstalledChipUsesDefaultCursorAndExplainTooltip()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(false);

        JComponent chip = QuestChip.create(cooks, bridge);
        assertEquals(Cursor.DEFAULT_CURSOR, chip.getCursor().getType());
        assertTrue(chip.getToolTipText().toLowerCase().contains("not installed"));
    }

    @Test
    public void clickOnInstalledChipInvokesBridgeOpen()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);
        when(bridge.open(cooks)).thenReturn(true);

        JComponent chip = QuestChip.create(cooks, bridge);
        // Synthesize a click via dispatching the platform mouse listener path.
        for (java.awt.event.MouseListener ml : chip.getMouseListeners())
        {
            ml.mouseClicked(new MouseEvent(chip, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        }
        verify(bridge).open(cooks);
    }

    @Test
    public void clickOnUninstalledChipDoesNotInvokeBridgeOpen()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(false);

        JComponent chip = QuestChip.create(cooks, bridge);
        for (java.awt.event.MouseListener ml : chip.getMouseListeners())
        {
            ml.mouseClicked(new MouseEvent(chip, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        }
        verify(bridge, never()).open(any());
    }

    @Test
    public void failedOpenUpdatesTooltip()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);
        when(bridge.open(cooks)).thenReturn(false);

        JComponent chip = QuestChip.create(cooks, bridge);
        for (java.awt.event.MouseListener ml : chip.getMouseListeners())
        {
            ml.mouseClicked(new MouseEvent(chip, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        }
        assertTrue(chip.getToolTipText().toLowerCase().contains("couldn"));
    }
}
```

- [ ] **Step 2: Run tests, verify fail**

Run: `./gradlew test --tests QuestChipTest`
Expected: FAIL — `QuestChip` does not exist.

- [ ] **Step 3: Implement `QuestChip`**

```java
package com.bruhsailor.plugin;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class QuestChip
{
    private static final String ARROW = "↗"; // ↗

    private QuestChip() {}

    public static JComponent create(QuestEntry quest, QuestHelperBridge bridge)
    {
        boolean installed = bridge.isInstalled();
        JLabel label = new JLabel(quest.displayName() + " " + ARROW);
        label.setOpaque(true);
        label.setFont(FontManager.getRunescapeFont().deriveFont(14f));
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
        Border line = BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR, 1);
        Border pad = BorderFactory.createEmptyBorder(4, 8, 4, 8);
        label.setBorder(BorderFactory.createCompoundBorder(line, pad));

        if (installed)
        {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setToolTipText("Open in Quest Helper");
        }
        else
        {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            label.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
            label.setToolTipText("Quest Helper plugin not installed");
        }

        label.addMouseListener(new MouseAdapter()
        {
            private boolean disabled;

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (disabled) return;
                if (!bridge.isInstalled()) return;
                boolean ok = bridge.open(quest);
                if (!ok)
                {
                    label.setToolTipText("Couldn't open Quest Helper - see logs");
                    disabled = true;
                }
            }
        });

        return label;
    }
}
```

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew test --tests QuestChipTest`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/QuestChip.java src/test/java/com/bruhsailor/plugin/QuestChipTest.java
git commit -m "feat: QuestChip pill component with click-to-open handler"
```

---

## Task 8: Wire chips into `BruhsailorPanel`

**Files:**
- Modify: `src/main/java/com/bruhsailor/plugin/BruhsailorPanel.java`

- [ ] **Step 1: Add imports + new constructor params + field**

At the top of `BruhsailorPanel.java`, in the imports block, add (alphabetically grouped):

```java
import com.bruhsailor.plugin.model.StepMapping;
import java.awt.FlowLayout;
```

Replace the current constructor signature:

```java
    public BruhsailorPanel(GuideRepository repo, GuideStateService state, EventBus bus)
```

with:

```java
    public BruhsailorPanel(GuideRepository repo, GuideStateService state, EventBus bus,
                           StepMappings stepMappings, QuestRegistry questRegistry,
                           QuestHelperBridge questBridge)
```

In the field block at the top of the class, after the existing `EventBus bus;` field, add:

```java
    private final StepMappings stepMappings;
    private final QuestRegistry questRegistry;
    private final QuestHelperBridge questBridge;
    private final JPanel chipsRow = new JPanel();
```

In the constructor, near the existing `this.bus = bus;` line, add:

```java
        this.stepMappings = stepMappings;
        this.questRegistry = questRegistry;
        this.questBridge = questBridge;
```

- [ ] **Step 2: Configure `chipsRow` and add it to `currentStepHolder.SOUTH`**

In the constructor, after the existing `currentStepHolder.setBorder(...)` line and before the `JScrollPane stepScroll` block, add:

```java
        chipsRow.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
        chipsRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        chipsRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        currentStepHolder.add(chipsRow, BorderLayout.SOUTH);
```

- [ ] **Step 3: Rebuild chips during `refreshAll`**

At the end of the existing `refreshAll()` method, AFTER the `SwingUtilities.invokeLater(...)` block that scrolls `stepScroll` to top, add:

```java
        rebuildChips(id);
```

Then add a new private method to the class (placement-wise: just below `refreshAll`):

```java
    private void rebuildChips(StepId id)
    {
        chipsRow.removeAll();
        chipsRow.setVisible(false);

        java.util.Optional<StepMapping> mappingOpt = stepMappings.findById(id);
        if (!mappingOpt.isPresent()) { chipsRow.revalidate(); chipsRow.repaint(); return; }
        StepMapping mapping = mappingOpt.get();
        if (mapping.questIds == null || mapping.questIds.isEmpty())
        {
            chipsRow.revalidate(); chipsRow.repaint(); return;
        }

        int added = 0;
        for (String enumName : mapping.questIds)
        {
            java.util.Optional<QuestEntry> entry = questRegistry.resolve(enumName);
            if (!entry.isPresent()) continue;
            chipsRow.add(QuestChip.create(entry.get(), questBridge));
            added++;
        }

        chipsRow.setVisible(added > 0);
        chipsRow.revalidate();
        chipsRow.repaint();
    }
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

The plugin wiring still passes the old 3-arg constructor; that breaks compilation in `BruhsailorPlugin.java` until Task 9. Step 4 is therefore EXPECTED TO FAIL with a "constructor does not exist" error against `BruhsailorPlugin.java`. Move directly to Task 9 — do not commit yet.

If you want to verify Task 8's changes in isolation, run `./gradlew compileJava 2>&1 | head -20` and confirm the only error is in `BruhsailorPlugin.java`'s `panel = new BruhsailorPanel(...)` call.

- [ ] **Step 5: Stage but DO NOT commit yet**

```bash
git add src/main/java/com/bruhsailor/plugin/BruhsailorPanel.java
```

The commit happens at the end of Task 9 (atomic — wiring + panel changes ship together).

---

## Task 9: Wire `BruhsailorPlugin` to construct + pass new collaborators

**Files:**
- Modify: `src/main/java/com/bruhsailor/plugin/BruhsailorPlugin.java`

- [ ] **Step 1: Add imports**

At the top of `BruhsailorPlugin.java`, add to the import block:

```java
import net.runelite.client.plugins.PluginManager;
```

- [ ] **Step 2: Inject `PluginManager`**

In the `BruhsailorPlugin` class body, near the other `@Inject` fields, add:

```java
    @Inject private PluginManager pluginManager;
```

- [ ] **Step 3: Replace the body of `startUp()` (between the existing repo-load block and the existing `BufferedImage icon = loadIcon();` line)**

Find this block:

```java
        GuideStateService state = new GuideStateService(repo, configManager, eventBus);
        panel = new BruhsailorPanel(repo, state, eventBus);
```

Replace with:

```java
        StepMappings mappings;
        try
        {
            mappings = StepMappings.loadBundled();
        }
        catch (RuntimeException e)
        {
            log.error("Failed to load step_mappings.json; chips disabled", e);
            mappings = StepMappings.empty();
        }
        QuestRegistry questRegistry = QuestRegistry.create(pluginManager);
        QuestHelperBridge questBridge = new QuestHelperBridge(pluginManager);

        GuideStateService state = new GuideStateService(repo, configManager, eventBus);
        panel = new BruhsailorPanel(repo, state, eventBus, mappings, questRegistry, questBridge);
```

- [ ] **Step 4: Run full build and tests**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. All previous tests still pass; new tests from Tasks 4-7 pass.

- [ ] **Step 5: Commit (covers Task 8 + Task 9 atomically)**

```bash
git add src/main/java/com/bruhsailor/plugin/BruhsailorPlugin.java
git commit -m "feat: render Quest Helper chips below current step content"
```

(`BruhsailorPanel.java` was already staged at the end of Task 8.)

---

## Task 10: Manual smoke test

No code changes. Confirm chips render and click correctly inside a real RuneLite client.

- [ ] **Step 1: Launch the dev runner**

Run: `./gradlew runClient --no-daemon`

Wait for RuneLite to come up; click the BRUHsailer panel button in the right sidebar.

- [ ] **Step 2: Verify chip rendering on a known-quest step**

Navigate to step `1.1.3` (it has `MISTHALIN_MYSTERY` mapped). Expected: a single chip reading `Misthalin Mystery ↗` appears below the step text inside the step block.

Navigate to step `1.1.1`. Expected: NO chip (empty `questIds`).

- [ ] **Step 3: Verify chip click (QH installed)**

Install the Quest Helper plugin via the RuneLite plugin hub. Restart the dev runner.

Navigate back to step `1.1.3`, click `Misthalin Mystery ↗`. Expected: the right sidebar switches to the Quest Helper panel with Misthalin Mystery selected (or at minimum, QH's panel becomes active and visible).

- [ ] **Step 4: Verify chip click (QH absent)**

Disable Quest Helper. Reload the panel (close and reopen via the nav button). Expected: the chip on step `1.1.3` is grayed out, its tooltip reads "Quest Helper plugin not installed", and clicking it does nothing.

- [ ] **Step 5: Verify resilience**

Pick a step with multiple `questIds` — for example, run:

```bash
python -c "
import json
d = json.load(open('src/main/resources/step_mappings.json',encoding='utf-8'))
multi = [(k, v['questIds']) for k, v in d['steps'].items() if len(v['questIds']) >= 2]
print(multi[:3])
"
```

Navigate to one of those step IDs. Expected: multiple chips render in the row, wrapping if they don't fit.

- [ ] **Step 6: Document the smoke result**

If everything works:

```bash
git commit --allow-empty -m "test: manual smoke for QH integration verified"
```

If any step fails, file a follow-up issue and DO NOT mark this feature complete.

---

## Spec coverage summary

| Spec section                                         | Implemented in       |
|------------------------------------------------------|----------------------|
| Bundle `quests.json` (266 entries)                   | Task 1               |
| `QuestEntry` value type                              | Task 2               |
| `StepMapping` + `Item` POJOs                         | Task 3               |
| `StepMappings.loadBundled()` + `findById` + `empty()` | Task 4               |
| `QuestRegistry.create()` + bundled load              | Task 5               |
| `QuestRegistry` runtime enrichment from QH enum      | Task 5               |
| `QuestHelperBridge.isInstalled()` + caching          | Task 6               |
| `QuestHelperBridge.open()` reflection plan (enum/string/no-arg) | Task 6   |
| `QuestChip.create()` + click handler + failure tooltip | Task 7             |
| Panel chip row at `currentStepHolder.SOUTH`          | Task 8               |
| Soft-filter (skip unresolved enum names)             | Task 8 (`rebuildChips`) |
| Plugin wiring (`PluginManager` injection)            | Task 9               |
| Soft-dep error path (`StepMappings.empty()` on failure) | Task 9            |
| Manual smoke checklist                               | Task 10              |
