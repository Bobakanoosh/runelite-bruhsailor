# BRUHsailer Plugin — Feature 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a working RuneLite plugin that displays the bundled BRUHsailer guide in a side panel with rich-text rendering, prev/next navigation, jump-to-step list, and persisted current/completed state.

**Architecture:** Six small Java units under `com.bruhsailor.plugin`: a `StepId` value type, a `GuideRepository` that loads the bundled JSON once, a `GuideStateService` owning user pointer + completed set with `ConfigManager` persistence, a `StepRenderer` that converts a step to a styled `JTextPane`, a `BruhsailorPanel` (PluginPanel) wiring it all together with an `EventBus` subscription, and a `BruhsailorPlugin` for lifecycle. Spec: `docs/superpowers/specs/2026-05-03-bruhsailor-plugin-feature1-design.md`.

**Tech Stack:** Java 11, Gradle, RuneLite client API, Gson (transitive via RuneLite), Swing (`JTextPane`/`StyledDocument`/`JList`), JUnit 4 (RuneLite convention), Mockito.

---

## Reference: example-plugin template

RuneLite publishes a one-plugin Gradle scaffold at `https://github.com/runelite/example-plugin`. The shape is:

- Single-module Gradle build, Java 11.
- `build.gradle` declares `compileOnly 'net.runelite:client:${runeLiteVersion}'` plus Lombok and JUnit.
- Test source root contains a `RuneLite`-launching runner that boots the client with the plugin pre-loaded so a developer can manually smoke-test.

We mirror that shape exactly. Tasks below give the exact file contents — no need to clone the template.

---

## File Structure

```
build.gradle                                 [Task 1]
settings.gradle                              [Task 1]
gradle.properties                            [Task 1]
checkstyle.xml                               [Task 1]
src/main/java/com/bruhsailor/plugin/
    BruhsailorPlugin.java                    [Task 1 stub, Task 9 final]
    StepId.java                              [Task 2]
    model/ContentFragment.java               [Task 3]
    model/Metadata.java                      [Task 3]
    model/Step.java                          [Task 3]
    model/GuideData.java                     [Task 3]
    GuideRepository.java                     [Task 4]
    GuideStateService.java                   [Task 5]
    GuideStateChanged.java                   [Task 5]
    StepRenderer.java                        [Task 6]
    BruhsailorPanel.java                     [Tasks 7-8]
src/main/resources/
    guide_data.json                          [exists]
    step_mappings.json                       [exists, unused]
    icon.png                                 [Task 9]
src/test/java/com/bruhsailor/plugin/
    StepIdTest.java                          [Task 2]
    GuideRepositoryTest.java                 [Task 4]
    GuideStateServiceTest.java               [Task 5]
    StepRendererTest.java                    [Task 6]
    BruhsailorPluginTest.java                [Task 1, dev runner]
```

---

## Task 1: Gradle scaffold + plugin stub that loads in the dev runner

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `gradle.properties`
- Create: `checkstyle.xml`
- Create: `src/main/java/com/bruhsailor/plugin/BruhsailorPlugin.java`
- Create: `src/test/java/com/bruhsailor/plugin/BruhsailorPluginTest.java`
- Create: `.gitignore`

- [ ] **Step 1: Write `settings.gradle`**

```groovy
rootProject.name = 'bruhsailor-plugin'
```

- [ ] **Step 2: Write `gradle.properties`**

```
runeLiteVersion=1.10.43
```

- [ ] **Step 3: Write `build.gradle`**

```groovy
plugins {
    id 'java'
    id 'checkstyle'
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = 'https://repo.runelite.net/' }
}

def runeLiteVersion = project.property('runeLiteVersion')

dependencies {
    compileOnly group: 'net.runelite', name: 'client', version: runeLiteVersion

    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'

    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.7.0'
    testImplementation 'org.slf4j:slf4j-simple:1.7.36'
    testImplementation group: 'net.runelite', name: 'client', version: runeLiteVersion
    testImplementation group: 'net.runelite', name: 'jshell', version: runeLiteVersion
}

group = 'com.bruhsailor'
version = '0.1.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

checkstyle {
    toolVersion = '10.12.4'
    configFile = file('checkstyle.xml')
}

test {
    useJUnit()
}
```

- [ ] **Step 4: Write `checkstyle.xml`**

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <property name="severity" value="warning"/>
    <module name="TreeWalker">
        <module name="UnusedImports"/>
        <module name="RedundantImport"/>
    </module>
</module>
```

- [ ] **Step 5: Write `.gitignore`**

```
.gradle/
build/
*.iml
.idea/
out/
.vscode/
```

- [ ] **Step 6: Write `BruhsailorPlugin.java` stub**

```java
package com.bruhsailor.plugin;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
    name = "BRUHsailer Guide",
    description = "BRUHsailer ironman guide in a side panel",
    tags = {"ironman", "guide", "bruhsailer"}
)
public class BruhsailorPlugin extends Plugin
{
}
```

- [ ] **Step 7: Write the dev runner `BruhsailorPluginTest.java`**

```java
package com.bruhsailor.plugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BruhsailorPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(BruhsailorPlugin.class);
        RuneLite.main(args);
    }
}
```

- [ ] **Step 8: Verify the build succeeds**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`. (Tests are excluded; we have none yet.)

- [ ] **Step 9: Commit**

```bash
git add build.gradle settings.gradle gradle.properties checkstyle.xml .gitignore src/main/java src/test/java
git commit -m "scaffold: gradle build and empty BRUHsailer plugin"
```

---

## Task 2: `StepId` value type

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/StepId.java`
- Test: `src/test/java/com/bruhsailor/plugin/StepIdTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.bruhsailor.plugin;

import org.junit.Test;
import static org.junit.Assert.*;

public class StepIdTest
{
    @Test
    public void parseAndFormatRoundtrip()
    {
        StepId id = StepId.parse("1.2.3");
        assertEquals(1, id.chapter());
        assertEquals(2, id.section());
        assertEquals(3, id.step());
        assertEquals("1.2.3", id.toString());
    }

    @Test
    public void orderingAcrossBoundaries()
    {
        assertTrue(StepId.parse("1.9.99").compareTo(StepId.parse("2.1.1")) < 0);
        assertTrue(StepId.parse("1.1.2").compareTo(StepId.parse("1.1.10")) < 0);
        assertEquals(0, StepId.parse("1.1.1").compareTo(StepId.parse("1.1.1")));
    }

    @Test
    public void equalsAndHashCode()
    {
        assertEquals(StepId.parse("1.1.1"), StepId.parse("1.1.1"));
        assertEquals(StepId.parse("1.1.1").hashCode(), StepId.parse("1.1.1").hashCode());
        assertNotEquals(StepId.parse("1.1.1"), StepId.parse("1.1.2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedTwoParts()
    {
        StepId.parse("1.2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonNumeric()
    {
        StepId.parse("1.a.3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositive()
    {
        StepId.parse("0.1.1");
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `./gradlew test --tests StepIdTest`
Expected: FAIL — `StepId` does not exist.

- [ ] **Step 3: Implement `StepId`**

```java
package com.bruhsailor.plugin;

import java.util.Objects;

public final class StepId implements Comparable<StepId>
{
    private final int chapter;
    private final int section;
    private final int step;

    private StepId(int chapter, int section, int step)
    {
        this.chapter = chapter;
        this.section = section;
        this.step = step;
    }

    public static StepId of(int chapter, int section, int step)
    {
        if (chapter < 1 || section < 1 || step < 1)
        {
            throw new IllegalArgumentException("StepId components must be >= 1");
        }
        return new StepId(chapter, section, step);
    }

    public static StepId parse(String s)
    {
        String[] parts = s.split("\\.");
        if (parts.length != 3)
        {
            throw new IllegalArgumentException("Expected chapter.section.step, got: " + s);
        }
        try
        {
            return of(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Non-numeric component in: " + s, e);
        }
    }

    public int chapter() { return chapter; }
    public int section() { return section; }
    public int step() { return step; }

    @Override
    public int compareTo(StepId o)
    {
        int c = Integer.compare(chapter, o.chapter);
        if (c != 0) return c;
        c = Integer.compare(section, o.section);
        if (c != 0) return c;
        return Integer.compare(step, o.step);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof StepId)) return false;
        StepId other = (StepId) o;
        return chapter == other.chapter && section == other.section && step == other.step;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(chapter, section, step);
    }

    @Override
    public String toString()
    {
        return chapter + "." + section + "." + step;
    }
}
```

- [ ] **Step 4: Run the tests and verify they pass**

Run: `./gradlew test --tests StepIdTest`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/StepId.java src/test/java/com/bruhsailor/plugin/StepIdTest.java
git commit -m "feat: StepId value type with parse, ordering, equality"
```

---

## Task 3: Model classes (`ContentFragment`, `Metadata`, `Step`, `GuideData`)

These are pure data carriers populated by Gson. No tests — covered through `GuideRepositoryTest` in Task 4.

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/model/ContentFragment.java`
- Create: `src/main/java/com/bruhsailor/plugin/model/Metadata.java`
- Create: `src/main/java/com/bruhsailor/plugin/model/Step.java`
- Create: `src/main/java/com/bruhsailor/plugin/model/GuideData.java`

- [ ] **Step 1: Write `ContentFragment.java`**

```java
package com.bruhsailor.plugin.model;

import java.util.Map;

public class ContentFragment
{
    public String text;
    public Formatting formatting;

    public static class Formatting
    {
        public Boolean bold;
        public Double fontSize;
        public Color color;
    }

    public static class Color
    {
        public double r;
        public double g;
        public double b;
    }
}
```

- [ ] **Step 2: Write `Metadata.java`**

```java
package com.bruhsailor.plugin.model;

public class Metadata
{
    public String gp_stack;
    public String items_needed;
    public String total_time;
    public String skills_quests_met;
}
```

- [ ] **Step 3: Write `Step.java`**

The JSON has no `id` field on a step; we assign one positionally in `GuideRepository`. Until then this is just the JSON shape.

```java
package com.bruhsailor.plugin.model;

import com.bruhsailor.plugin.StepId;
import java.util.List;

public class Step
{
    public List<ContentFragment> content;
    public List<List<ContentFragment>> nestedContent;
    public Metadata metadata;

    // Populated after JSON parse by GuideRepository.
    public transient StepId id;
}
```

- [ ] **Step 4: Write `GuideData.java`**

```java
package com.bruhsailor.plugin.model;

import java.util.List;

public class GuideData
{
    public String title;
    public String updatedOn;
    public List<Chapter> chapters;

    public static class Chapter
    {
        public String title;
        public List<Section> sections;
    }

    public static class Section
    {
        public String title;
        public List<Step> steps;
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/model
git commit -m "feat: model classes for parsed guide JSON"
```

---

## Task 4: `GuideRepository`

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/GuideRepository.java`
- Test: `src/test/java/com/bruhsailor/plugin/GuideRepositoryTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.Step;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class GuideRepositoryTest
{
    private GuideRepository repo;

    @Before
    public void setUp()
    {
        repo = GuideRepository.loadBundled();
    }

    @Test
    public void loadsExpectedStepCount()
    {
        assertEquals(227, repo.steps().size());
    }

    @Test
    public void everyStepIdIsUniqueAndLookupable()
    {
        Set<StepId> seen = new HashSet<>();
        for (Step s : repo.steps())
        {
            assertNotNull("step missing id", s.id);
            assertTrue("duplicate id " + s.id, seen.add(s.id));
            assertSame(s, repo.findById(s.id).orElseThrow(AssertionError::new));
        }
    }

    @Test
    public void everyStepHasAtLeastOneContentFragment()
    {
        for (Step s : repo.steps())
        {
            assertNotNull(s.content);
            assertFalse("empty content for " + s.id, s.content.isEmpty());
        }
    }

    @Test
    public void chapterAndSectionTitlesResolve()
    {
        StepId first = repo.steps().get(0).id;
        assertNotNull(repo.chapterTitleFor(first));
        assertNotNull(repo.sectionTitleFor(first));
        assertFalse(repo.chapterTitleFor(first).isEmpty());
    }

    @Test
    public void indexOfAndIdAtAreInverses()
    {
        for (int i = 0; i < repo.steps().size(); i++)
        {
            StepId id = repo.idAt(i).orElseThrow(AssertionError::new);
            assertEquals(i, repo.indexOf(id));
        }
    }

    @Test
    public void indexOfUnknownIsMinusOne()
    {
        assertEquals(-1, repo.indexOf(StepId.of(99, 99, 99)));
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `./gradlew test --tests GuideRepositoryTest`
Expected: FAIL — `GuideRepository` does not exist.

- [ ] **Step 3: Implement `GuideRepository`**

```java
package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.GuideData;
import com.bruhsailor.plugin.model.Step;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GuideRepository
{
    private static final String RESOURCE = "/guide_data.json";

    private final List<Step> steps;
    private final Map<StepId, Step> byId;
    private final Map<StepId, Integer> indexById;
    private final Map<StepId, String> chapterTitleByStep;
    private final Map<StepId, String> sectionTitleByStep;

    private GuideRepository(
        List<Step> steps,
        Map<StepId, Step> byId,
        Map<StepId, Integer> indexById,
        Map<StepId, String> chapterTitleByStep,
        Map<StepId, String> sectionTitleByStep)
    {
        this.steps = Collections.unmodifiableList(steps);
        this.byId = byId;
        this.indexById = indexById;
        this.chapterTitleByStep = chapterTitleByStep;
        this.sectionTitleByStep = sectionTitleByStep;
    }

    public static GuideRepository loadBundled()
    {
        InputStream in = GuideRepository.class.getResourceAsStream(RESOURCE);
        if (in == null)
        {
            throw new IllegalStateException("Missing classpath resource " + RESOURCE);
        }
        GuideData data;
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            data = new Gson().fromJson(r, GuideData.class);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to parse " + RESOURCE, e);
        }
        return build(data);
    }

    private static GuideRepository build(GuideData data)
    {
        List<Step> flat = new ArrayList<>();
        Map<StepId, Step> byId = new HashMap<>();
        Map<StepId, Integer> indexById = new HashMap<>();
        Map<StepId, String> chapterTitle = new HashMap<>();
        Map<StepId, String> sectionTitle = new HashMap<>();

        for (int ci = 0; ci < data.chapters.size(); ci++)
        {
            GuideData.Chapter chapter = data.chapters.get(ci);
            for (int si = 0; si < chapter.sections.size(); si++)
            {
                GuideData.Section section = chapter.sections.get(si);
                for (int ti = 0; ti < section.steps.size(); ti++)
                {
                    Step step = section.steps.get(ti);
                    StepId id = StepId.of(ci + 1, si + 1, ti + 1);
                    step.id = id;
                    indexById.put(id, flat.size());
                    flat.add(step);
                    byId.put(id, step);
                    chapterTitle.put(id, chapter.title);
                    sectionTitle.put(id, section.title);
                }
            }
        }

        return new GuideRepository(flat, byId, indexById, chapterTitle, sectionTitle);
    }

    public List<Step> steps()
    {
        return steps;
    }

    public Optional<Step> findById(StepId id)
    {
        return Optional.ofNullable(byId.get(id));
    }

    public int indexOf(StepId id)
    {
        Integer i = indexById.get(id);
        return i == null ? -1 : i;
    }

    public Optional<StepId> idAt(int index)
    {
        if (index < 0 || index >= steps.size()) return Optional.empty();
        return Optional.of(steps.get(index).id);
    }

    public String chapterTitleFor(StepId id)
    {
        return chapterTitleByStep.get(id);
    }

    public String sectionTitleFor(StepId id)
    {
        return sectionTitleByStep.get(id);
    }
}
```

- [ ] **Step 4: Run the tests and verify they pass**

Run: `./gradlew test --tests GuideRepositoryTest`
Expected: PASS, 6 tests. If step count differs from 227, update the assertion AFTER confirming the JSON itself is correct — do not silently weaken the test.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/GuideRepository.java src/test/java/com/bruhsailor/plugin/GuideRepositoryTest.java
git commit -m "feat: GuideRepository loads bundled guide_data.json"
```

---

## Task 5: `GuideStateService` + `GuideStateChanged` event

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/GuideStateChanged.java`
- Create: `src/main/java/com/bruhsailor/plugin/GuideStateService.java`
- Test: `src/test/java/com/bruhsailor/plugin/GuideStateServiceTest.java`

- [ ] **Step 1: Write `GuideStateChanged.java`**

```java
package com.bruhsailor.plugin;

import lombok.Value;

@Value
public class GuideStateChanged
{
    StepId previousCurrent;
    StepId newCurrent;
    boolean completionChanged;
}
```

- [ ] **Step 2: Write the failing tests**

```java
package com.bruhsailor.plugin;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GuideStateServiceTest
{
    private static final String GROUP = "bruhsailor";
    private static final String CURRENT_KEY = "currentStepId";
    private static final String COMPLETED_KEY = "completedStepIds";

    private ConfigManager config;
    private EventBus bus;
    private GuideRepository repo;

    @Before
    public void setUp()
    {
        config = mock(ConfigManager.class);
        bus = mock(EventBus.class);
        repo = GuideRepository.loadBundled();
    }

    @Test
    public void emptyConfigDefaultsToFirstStep()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn(null);
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn(null);

        GuideStateService svc = new GuideStateService(repo, config, bus);

        assertEquals(repo.steps().get(0).id, svc.getCurrent());
        assertFalse(svc.isComplete(repo.steps().get(0).id));
    }

    @Test
    public void persistedValidStateRoundtrips()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn("1.1.2");
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn("1.1.1,1.1.2");

        GuideStateService svc = new GuideStateService(repo, config, bus);

        assertEquals(StepId.parse("1.1.2"), svc.getCurrent());
        assertTrue(svc.isComplete(StepId.parse("1.1.1")));
        assertTrue(svc.isComplete(StepId.parse("1.1.2")));
    }

    @Test
    public void unknownCurrentFallsBackToFirst()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn("99.99.99");
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn(null);

        GuideStateService svc = new GuideStateService(repo, config, bus);

        assertEquals(repo.steps().get(0).id, svc.getCurrent());
    }

    @Test
    public void unknownCompletedIdsAreDropped()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn(null);
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn("1.1.1,99.99.99,1.1.2");

        GuideStateService svc = new GuideStateService(repo, config, bus);

        assertTrue(svc.isComplete(StepId.parse("1.1.1")));
        assertTrue(svc.isComplete(StepId.parse("1.1.2")));
        assertFalse(svc.isComplete(StepId.of(99, 99, 99)));
    }

    @Test
    public void prevAtFirstStepIsNoop()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn(null);
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn(null);
        GuideStateService svc = new GuideStateService(repo, config, bus);

        StepId before = svc.getCurrent();
        svc.prev();

        assertEquals(before, svc.getCurrent());
        verify(bus, never()).post(any(GuideStateChanged.class));
    }

    @Test
    public void nextAtLastStepIsNoop()
    {
        StepId last = repo.steps().get(repo.steps().size() - 1).id;
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn(last.toString());
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn(null);
        GuideStateService svc = new GuideStateService(repo, config, bus);

        svc.next();

        assertEquals(last, svc.getCurrent());
        verify(bus, never()).post(any(GuideStateChanged.class));
    }

    @Test
    public void nextAdvancesAndPersistsAndFires()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn(null);
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn(null);
        GuideStateService svc = new GuideStateService(repo, config, bus);

        StepId before = svc.getCurrent();
        svc.next();
        StepId after = svc.getCurrent();

        assertNotEquals(before, after);
        verify(config).setConfiguration(eq(GROUP), eq(CURRENT_KEY), eq(after.toString()));
        verify(bus).post(any(GuideStateChanged.class));
    }

    @Test
    public void setCompleteTogglesAndPersistsSorted()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn(null);
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn(null);
        GuideStateService svc = new GuideStateService(repo, config, bus);

        svc.setComplete(StepId.parse("1.1.2"), true);
        verify(config, times(1)).setConfiguration(GROUP, COMPLETED_KEY, "1.1.2");

        svc.setComplete(StepId.parse("1.1.1"), true);
        verify(config, times(1)).setConfiguration(GROUP, COMPLETED_KEY, "1.1.1,1.1.2");

        svc.setComplete(StepId.parse("1.1.1"), false);
        verify(config, times(2)).setConfiguration(GROUP, COMPLETED_KEY, "1.1.2");

        assertFalse(svc.isComplete(StepId.parse("1.1.1")));
        assertTrue(svc.isComplete(StepId.parse("1.1.2")));
    }

    @Test
    public void setCompleteAlreadyDoneIsNoop()
    {
        when(config.getConfiguration(GROUP, CURRENT_KEY)).thenReturn(null);
        when(config.getConfiguration(GROUP, COMPLETED_KEY)).thenReturn("1.1.1");
        GuideStateService svc = new GuideStateService(repo, config, bus);
        reset(config);

        svc.setComplete(StepId.parse("1.1.1"), true);

        verify(config, never()).setConfiguration(eq(GROUP), eq(COMPLETED_KEY), anyString());
        verify(bus, never()).post(any(GuideStateChanged.class));
    }
}
```

- [ ] **Step 3: Run the tests and verify they fail**

Run: `./gradlew test --tests GuideStateServiceTest`
Expected: FAIL — `GuideStateService` does not exist.

- [ ] **Step 4: Implement `GuideStateService`**

```java
package com.bruhsailor.plugin;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GuideStateService
{
    private static final Logger log = LoggerFactory.getLogger(GuideStateService.class);

    static final String GROUP = "bruhsailor";
    static final String CURRENT_KEY = "currentStepId";
    static final String COMPLETED_KEY = "completedStepIds";

    private final GuideRepository repo;
    private final ConfigManager config;
    private final EventBus bus;

    private StepId current;
    private final Set<StepId> completed = new TreeSet<>();

    public GuideStateService(GuideRepository repo, ConfigManager config, EventBus bus)
    {
        this.repo = repo;
        this.config = config;
        this.bus = bus;
        loadFromConfig();
    }

    private void loadFromConfig()
    {
        String rawCurrent = config.getConfiguration(GROUP, CURRENT_KEY);
        StepId resolved = null;
        if (rawCurrent != null && !rawCurrent.isEmpty())
        {
            try
            {
                StepId parsed = StepId.parse(rawCurrent);
                if (repo.findById(parsed).isPresent())
                {
                    resolved = parsed;
                }
                else
                {
                    log.warn("Persisted currentStepId {} not found in guide; falling back", rawCurrent);
                }
            }
            catch (IllegalArgumentException e)
            {
                log.warn("Persisted currentStepId {} is malformed; falling back", rawCurrent);
            }
        }
        current = (resolved != null) ? resolved : repo.steps().get(0).id;

        String rawCompleted = config.getConfiguration(GROUP, COMPLETED_KEY);
        if (rawCompleted != null && !rawCompleted.isEmpty())
        {
            int dropped = 0;
            for (String token : rawCompleted.split(","))
            {
                String t = token.trim();
                if (t.isEmpty()) continue;
                try
                {
                    StepId id = StepId.parse(t);
                    if (repo.findById(id).isPresent())
                    {
                        completed.add(id);
                    }
                    else
                    {
                        dropped++;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    dropped++;
                }
            }
            if (dropped > 0)
            {
                log.warn("Dropped {} unknown/malformed entries from completedStepIds", dropped);
            }
        }
    }

    public StepId getCurrent()
    {
        return current;
    }

    public void setCurrent(StepId id)
    {
        if (!repo.findById(id).isPresent() || id.equals(current))
        {
            return;
        }
        StepId prev = current;
        current = id;
        config.setConfiguration(GROUP, CURRENT_KEY, current.toString());
        bus.post(new GuideStateChanged(prev, current, false));
    }

    public void next()
    {
        int idx = repo.indexOf(current);
        repo.idAt(idx + 1).ifPresent(this::setCurrent);
    }

    public void prev()
    {
        int idx = repo.indexOf(current);
        repo.idAt(idx - 1).ifPresent(this::setCurrent);
    }

    public boolean isComplete(StepId id)
    {
        return completed.contains(id);
    }

    public void setComplete(StepId id, boolean done)
    {
        if (!repo.findById(id).isPresent()) return;
        boolean changed = done ? completed.add(id) : completed.remove(id);
        if (!changed) return;
        String joined = completed.stream().map(StepId::toString).collect(Collectors.joining(","));
        config.setConfiguration(GROUP, COMPLETED_KEY, joined);
        bus.post(new GuideStateChanged(current, current, true));
    }

    public Set<StepId> completedSnapshot()
    {
        return new LinkedHashSet<>(completed);
    }
}
```

- [ ] **Step 5: Run the tests and verify they pass**

Run: `./gradlew test --tests GuideStateServiceTest`
Expected: PASS, 9 tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/GuideStateService.java src/main/java/com/bruhsailor/plugin/GuideStateChanged.java src/test/java/com/bruhsailor/plugin/GuideStateServiceTest.java
git commit -m "feat: GuideStateService persists current step and completed set"
```

---

## Task 6: `StepRenderer`

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/StepRenderer.java`
- Test: `src/test/java/com/bruhsailor/plugin/StepRendererTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.ContentFragment;
import com.bruhsailor.plugin.model.Step;
import org.junit.Test;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class StepRendererTest
{
    private Step stepWith(List<ContentFragment> content)
    {
        Step s = new Step();
        s.id = StepId.of(1, 1, 1);
        s.content = content;
        s.nestedContent = new ArrayList<>();
        return s;
    }

    private ContentFragment frag(String text, Boolean bold, Double size, Double r, Double g, Double b)
    {
        ContentFragment f = new ContentFragment();
        f.text = text;
        f.formatting = new ContentFragment.Formatting();
        f.formatting.bold = bold;
        f.formatting.fontSize = size;
        if (r != null)
        {
            f.formatting.color = new ContentFragment.Color();
            f.formatting.color.r = r;
            f.formatting.color.g = g;
            f.formatting.color.b = b;
        }
        return f;
    }

    @Test
    public void emitsTextInOrder()
    {
        List<ContentFragment> content = new ArrayList<>();
        content.add(frag("Hello ", null, null, null, null, null));
        content.add(frag("world", null, null, null, null, null));
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        assertTrue(pane.getText().contains("Hello world"));
    }

    @Test
    public void boldFragmentHasBoldAttribute()
    {
        List<ContentFragment> content = new ArrayList<>();
        content.add(frag("plain ", null, null, null, null, null));
        content.add(frag("bold", true, null, null, null, null));
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        StyledDocument doc = pane.getStyledDocument();

        Element boldElem = doc.getCharacterElement("plain ".length());
        AttributeSet attrs = boldElem.getAttributes();
        assertTrue(StyleConstants.isBold(attrs));
    }

    @Test
    public void coloredFragmentPreservesExactColor()
    {
        List<ContentFragment> content = new ArrayList<>();
        content.add(frag("red", null, null, 1.0, 0.0, 0.0));
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        StyledDocument doc = pane.getStyledDocument();

        AttributeSet attrs = doc.getCharacterElement(0).getAttributes();
        Color c = StyleConstants.getForeground(attrs);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    public void nestedContentAppearsAfterMainContent()
    {
        Step s = new Step();
        s.id = StepId.of(1, 1, 1);
        s.content = new ArrayList<>();
        s.content.add(frag("outer", null, null, null, null, null));
        s.nestedContent = new ArrayList<>();
        List<ContentFragment> sub = new ArrayList<>();
        sub.add(frag("inner", null, null, null, null, null));
        s.nestedContent.add(sub);

        JTextPane pane = (JTextPane) StepRenderer.render(s);
        String all = pane.getText();
        assertTrue(all.contains("outer"));
        assertTrue(all.contains("inner"));
        assertTrue(all.indexOf("outer") < all.indexOf("inner"));
    }

    @Test
    public void fragmentWithNullFormattingRendersAsPlain()
    {
        ContentFragment f = new ContentFragment();
        f.text = "no formatting object";
        f.formatting = null;
        List<ContentFragment> content = new ArrayList<>();
        content.add(f);
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        assertTrue(pane.getText().contains("no formatting object"));
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `./gradlew test --tests StepRendererTest`
Expected: FAIL — `StepRenderer` does not exist.

- [ ] **Step 3: Implement `StepRenderer`**

```java
package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.ContentFragment;
import com.bruhsailor.plugin.model.Step;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

public final class StepRenderer
{
    private static final Logger log = LoggerFactory.getLogger(StepRenderer.class);

    private static final int BASE_SIZE = 12;
    private static final int SMALL_SIZE = 10;
    private static final int LARGE_SIZE = 14;
    private static final float NESTED_INDENT = 16f;

    private StepRenderer() {}

    public static JComponent render(Step step)
    {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        Font base = FontManager.getRunescapeFont();
        if (base != null)
        {
            pane.setFont(base);
        }

        StyledDocument doc = pane.getStyledDocument();

        if (step.content != null)
        {
            for (ContentFragment f : step.content)
            {
                appendFragment(doc, f, 0f);
            }
        }

        if (step.nestedContent != null)
        {
            for (List<ContentFragment> sub : step.nestedContent)
            {
                ensureParagraphBreak(doc);
                if (sub == null) continue;
                for (ContentFragment f : sub)
                {
                    appendFragment(doc, f, NESTED_INDENT);
                }
            }
        }

        return pane;
    }

    private static void ensureParagraphBreak(StyledDocument doc)
    {
        try
        {
            doc.insertString(doc.getLength(), "\n", null);
        }
        catch (BadLocationException e)
        {
            // unreachable: appending at end
        }
    }

    private static void appendFragment(StyledDocument doc, ContentFragment frag, float indent)
    {
        if (frag == null || frag.text == null) return;
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        try
        {
            applyFormatting(attrs, frag, indent);
        }
        catch (RuntimeException e)
        {
            log.warn("Bad formatting on fragment, falling back to plain", e);
            attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, ColorScheme.LIGHT_GRAY_COLOR);
        }
        try
        {
            doc.insertString(doc.getLength(), frag.text, attrs);
        }
        catch (BadLocationException e)
        {
            // unreachable: appending at end
        }
    }

    private static void applyFormatting(SimpleAttributeSet attrs, ContentFragment frag, float indent)
    {
        StyleConstants.setForeground(attrs, ColorScheme.LIGHT_GRAY_COLOR);
        StyleConstants.setFontSize(attrs, BASE_SIZE);
        if (indent > 0f)
        {
            StyleConstants.setLeftIndent(attrs, indent);
        }

        ContentFragment.Formatting f = frag.formatting;
        if (f == null) return;

        if (Boolean.TRUE.equals(f.bold))
        {
            StyleConstants.setBold(attrs, true);
        }

        if (f.fontSize != null)
        {
            double s = f.fontSize;
            if (s <= 11.0) StyleConstants.setFontSize(attrs, SMALL_SIZE);
            else if (s >= 14.0) StyleConstants.setFontSize(attrs, LARGE_SIZE);
            else StyleConstants.setFontSize(attrs, BASE_SIZE);
        }

        if (f.color != null)
        {
            Color c = new Color(
                clamp01((float) f.color.r),
                clamp01((float) f.color.g),
                clamp01((float) f.color.b));
            StyleConstants.setForeground(attrs, c);
        }
    }

    private static float clamp01(float v)
    {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
```

- [ ] **Step 4: Run the tests and verify they pass**

Run: `./gradlew test --tests StepRendererTest`
Expected: PASS, 5 tests. If `FontManager` is null in the test JVM (no AWT init), the renderer guards against it; tests should still pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/StepRenderer.java src/test/java/com/bruhsailor/plugin/StepRendererTest.java
git commit -m "feat: StepRenderer converts Step to styled JTextPane"
```

---

## Task 7: `BruhsailorPanel` — header + current step block + nav controls

This task builds the top half of the panel only. The step list comes in Task 8.

**Files:**
- Create: `src/main/java/com/bruhsailor/plugin/BruhsailorPanel.java`

- [ ] **Step 1: Write `BruhsailorPanel.java` (top half)**

```java
package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.Step;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

public class BruhsailorPanel extends PluginPanel
{
    private final GuideRepository repo;
    private final GuideStateService state;
    private final EventBus bus;

    private final JLabel chapterLabel = new JLabel();
    private final JLabel sectionLabel = new JLabel();
    private final JPanel currentStepHolder = new JPanel(new BorderLayout());
    private final JLabel metadataLabel = new JLabel();
    private final JButton prevButton = new JButton("Prev");
    private final JButton nextButton = new JButton("Next");
    private final JToggleButton completeToggle = new JToggleButton("Mark complete");

    public BruhsailorPanel(GuideRepository repo, GuideStateService state, EventBus bus)
    {
        super(false);
        this.repo = repo;
        this.state = state;
        this.bus = bus;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(ColorScheme.DARK_GRAY_COLOR);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        chapterLabel.setFont(FontManager.getRunescapeBoldFont());
        chapterLabel.setForeground(ColorScheme.BRAND_ORANGE);
        chapterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionLabel.setFont(FontManager.getRunescapeFont());
        sectionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        currentStepHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        currentStepHolder.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        currentStepHolder.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane stepScroll = new JScrollPane(currentStepHolder);
        stepScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepScroll.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 16, 240));
        stepScroll.setBorder(BorderFactory.createEmptyBorder());

        metadataLabel.setFont(FontManager.getRunescapeSmallFont());
        metadataLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        metadataLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(prevButton);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(completeToggle);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(nextButton);

        prevButton.addActionListener(e -> state.prev());
        nextButton.addActionListener(e -> state.next());
        completeToggle.addActionListener(e ->
            state.setComplete(state.getCurrent(), completeToggle.isSelected()));

        root.add(chapterLabel);
        root.add(sectionLabel);
        root.add(Box.createVerticalStrut(8));
        root.add(stepScroll);
        root.add(Box.createVerticalStrut(4));
        root.add(metadataLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(controls);

        add(root, BorderLayout.NORTH);

        bus.register(this);
        refreshAll();
    }

    public void unregister()
    {
        bus.unregister(this);
    }

    @Subscribe
    public void onGuideStateChanged(GuideStateChanged e)
    {
        SwingUtilities.invokeLater(this::refreshAll);
    }

    private void refreshAll()
    {
        StepId id = state.getCurrent();
        Step step = repo.findById(id).orElse(null);
        if (step == null) return;

        chapterLabel.setText(repo.chapterTitleFor(id));
        sectionLabel.setText(repo.sectionTitleFor(id));

        currentStepHolder.removeAll();
        JComponent rendered = (JComponent) StepRenderer.render(step);
        currentStepHolder.add(rendered, BorderLayout.CENTER);
        currentStepHolder.revalidate();
        currentStepHolder.repaint();

        metadataLabel.setText(formatMetadata(step));

        int idx = repo.indexOf(id);
        prevButton.setEnabled(idx > 0);
        nextButton.setEnabled(idx >= 0 && idx < repo.steps().size() - 1);

        completeToggle.setSelected(state.isComplete(id));
    }

    private static String formatMetadata(Step step)
    {
        if (step.metadata == null) return "";
        StringBuilder sb = new StringBuilder("<html>");
        appendMeta(sb, "gp", step.metadata.gp_stack);
        appendMeta(sb, "time", step.metadata.total_time);
        appendMeta(sb, "skills", step.metadata.skills_quests_met);
        sb.append("</html>");
        return sb.toString();
    }

    private static void appendMeta(StringBuilder sb, String label, String value)
    {
        if (value == null || value.isEmpty()) return;
        if (sb.length() > "<html>".length()) sb.append(" &middot; ");
        sb.append(label).append(": ").append(value);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/BruhsailorPanel.java
git commit -m "feat: BruhsailorPanel renders current step with nav and complete toggle"
```

---

## Task 8: Add the step list to `BruhsailorPanel`

Append a `JList` of all 227 step rows + section/chapter headers below the current-step block.

**Files:**
- Modify: `src/main/java/com/bruhsailor/plugin/BruhsailorPanel.java`

- [ ] **Step 1: Add row types as nested classes inside `BruhsailorPanel`**

Insert at the bottom of the class, before the closing brace:

```java
    private interface ListRow {}

    private static final class HeaderRow implements ListRow
    {
        final String text;
        final boolean isChapter;
        HeaderRow(String text, boolean isChapter) { this.text = text; this.isChapter = isChapter; }
    }

    private static final class StepRow implements ListRow
    {
        final StepId id;
        final String label;
        StepRow(StepId id, String label) { this.id = id; this.label = label; }
    }
```

- [ ] **Step 2: Add field declarations**

Near the other fields at the top of `BruhsailorPanel`:

```java
    private final javax.swing.DefaultListModel<ListRow> listModel = new javax.swing.DefaultListModel<>();
    private final javax.swing.JList<ListRow> stepList = new javax.swing.JList<>(listModel);
```

- [ ] **Step 3: Build the model in the constructor**

After the existing `add(root, BorderLayout.NORTH);` line, replace with:

```java
        add(root, BorderLayout.NORTH);

        buildListModel();
        stepList.setCellRenderer(new RowRenderer());
        stepList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            ListRow row = stepList.getSelectedValue();
            if (row instanceof StepRow)
            {
                state.setCurrent(((StepRow) row).id);
            }
        });
        stepList.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane listScroll = new JScrollPane(stepList);
        listScroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(listScroll, BorderLayout.CENTER);
```

- [ ] **Step 4: Add `buildListModel`, `firstLineSnippet`, and `RowRenderer`**

Insert into `BruhsailorPanel`:

```java
    private void buildListModel()
    {
        Integer lastChapter = null;
        Integer lastSection = null;
        for (Step step : repo.steps())
        {
            StepId id = step.id;
            if (lastChapter == null || id.chapter() != lastChapter)
            {
                listModel.addElement(new HeaderRow(repo.chapterTitleFor(id), true));
                lastChapter = id.chapter();
                lastSection = null;
            }
            if (lastSection == null || id.section() != lastSection)
            {
                listModel.addElement(new HeaderRow(repo.sectionTitleFor(id), false));
                lastSection = id.section();
            }
            listModel.addElement(new StepRow(id, id + "  " + firstLineSnippet(step)));
        }
    }

    private static String firstLineSnippet(Step step)
    {
        if (step.content == null) return "";
        StringBuilder sb = new StringBuilder();
        for (com.bruhsailor.plugin.model.ContentFragment f : step.content)
        {
            if (f == null || f.text == null) continue;
            sb.append(f.text);
            if (sb.length() >= 60) break;
        }
        String s = sb.toString().replace('\n', ' ').trim();
        if (s.length() > 50) s = s.substring(0, 50) + "…";
        return s;
    }

    private final class RowRenderer extends javax.swing.JLabel
        implements javax.swing.ListCellRenderer<ListRow>
    {
        RowRenderer()
        {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }

        @Override
        public Component getListCellRendererComponent(
            javax.swing.JList<? extends ListRow> list,
            ListRow value, int index, boolean isSelected, boolean cellHasFocus)
        {
            if (value instanceof HeaderRow)
            {
                HeaderRow h = (HeaderRow) value;
                setText(h.text);
                setFont(h.isChapter ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeFont());
                setForeground(h.isChapter ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
                setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
            else
            {
                StepRow r = (StepRow) value;
                boolean isCurrent = r.id.equals(state.getCurrent());
                boolean done = state.isComplete(r.id);
                String text = done
                    ? "<html><strike>" + escape(r.label) + "</strike></html>"
                    : r.label;
                setText(text);
                setFont(FontManager.getRunescapeSmallFont());
                setForeground(done ? ColorScheme.LIGHT_GRAY_COLOR.darker() : ColorScheme.LIGHT_GRAY_COLOR);
                setBackground(isCurrent ? ColorScheme.BRAND_ORANGE.darker() : ColorScheme.DARK_GRAY_COLOR);
            }
            return this;
        }

        private String escape(String s)
        {
            return s.replace("&", "&amp;").replace("<", "&lt;");
        }
    }
```

- [ ] **Step 5: Update `refreshAll` to repaint the list when state changes**

At the end of the existing `refreshAll` method, add:

```java
        stepList.repaint();
```

- [ ] **Step 6: Verify compilation**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/BruhsailorPanel.java
git commit -m "feat: step list with chapter/section headers, current highlight, completed strike-through"
```

---

## Task 9: Wire the plugin lifecycle and navigation button

**Files:**
- Modify: `src/main/java/com/bruhsailor/plugin/BruhsailorPlugin.java`
- Create: `src/main/resources/icon.png` (16×16 placeholder, any small PNG; can be a single-color square)

- [ ] **Step 1: Add a 16×16 `icon.png`**

Use any small PNG. Quickest path: copy any 16×16 PNG you have, or use the RuneLite default. Run:

```bash
# Create a placeholder if you have ImageMagick:
# magick -size 16x16 xc:#f08000 src/main/resources/icon.png
# Otherwise, drop any 16×16 .png at src/main/resources/icon.png.
```

If you cannot produce one easily, the navigation button can still register without an icon (RuneLite falls back); revisit before Plugin Hub submission.

- [ ] **Step 2: Replace `BruhsailorPlugin.java` with the wired version**

```java
package com.bruhsailor.plugin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
    name = "BRUHsailer Guide",
    description = "BRUHsailer ironman guide in a side panel",
    tags = {"ironman", "guide", "bruhsailer"}
)
public class BruhsailorPlugin extends Plugin
{
    @Inject private ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    @Inject private EventBus eventBus;

    private NavigationButton navButton;
    private BruhsailorPanel panel;

    @Override
    protected void startUp() throws Exception
    {
        GuideRepository repo;
        try
        {
            repo = GuideRepository.loadBundled();
        }
        catch (RuntimeException e)
        {
            log.error("Failed to load BRUHsailer guide data", e);
            installErrorPanel("Failed to load BRUHsailer guide data");
            return;
        }

        GuideStateService state = new GuideStateService(repo, configManager, eventBus);
        panel = new BruhsailorPanel(repo, state, eventBus);

        BufferedImage icon = loadIcon();
        navButton = NavigationButton.builder()
            .tooltip("BRUHsailer Guide")
            .priority(7)
            .icon(icon)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        if (panel != null)
        {
            panel.unregister();
            panel = null;
        }
    }

    private BufferedImage loadIcon()
    {
        try
        {
            return ImageUtil.loadImageResource(getClass(), "/icon.png");
        }
        catch (Exception e)
        {
            log.warn("BRUHsailer icon missing; using blank placeholder", e);
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private void installErrorPanel(String message)
    {
        net.runelite.client.ui.PluginPanel error = new net.runelite.client.ui.PluginPanel(false) {};
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        error.add(label);
        BufferedImage icon = loadIcon();
        navButton = NavigationButton.builder()
            .tooltip("BRUHsailer Guide (error)")
            .priority(7)
            .icon(icon)
            .panel(error)
            .build();
        clientToolbar.addNavigation(navButton);
    }
}
```

- [ ] **Step 3: Verify compilation and tests**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. Tests from prior tasks still pass; no new tests for this task (lifecycle + DI is exercised via the dev runner).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/bruhsailor/plugin/BruhsailorPlugin.java src/main/resources/icon.png
git commit -m "feat: wire plugin lifecycle, navigation button, and panel"
```

---

## Task 10: Manual smoke test via the dev runner

No code changes. Confirm the plugin works end-to-end inside a real RuneLite client.

- [ ] **Step 1: Launch the dev runner**

Run: `./gradlew test --tests BruhsailorPluginTest -i` *or* run `BruhsailorPluginTest.main` from your IDE.

This boots RuneLite with the plugin pre-registered. Log in to a free-to-play world.

- [ ] **Step 2: Verify the navigation button appears**

Expected: a "BRUHsailer Guide" entry in the right-hand sidebar. Click it.

- [ ] **Step 3: Verify the panel renders the first step**

Expected:
- Chapter title at the top: "Chapter 1: Get to da chopper earth staves".
- Section title under it: "1.1: Tutorial island up to and including Wintertodt".
- The first step's content rendered in styled text. Red-warning fragments appear red. Bold fragments appear bold.
- Metadata line under the step: `gp: 25 gp · time: 35 minutes · skills: yes`.
- `[Prev]` disabled, `[Mark complete ☐]` unchecked, `[Next]` enabled.
- A scrollable list of all steps below, with chapter and section headers, the first step row highlighted.

- [ ] **Step 4: Verify navigation works**

- Click `Next` → current step advances; list highlight follows.
- Click `Prev` → goes back. At step 1, `Prev` disables.
- Toggle `Mark complete` on a step → that row gets struck through in the list.
- Click any step row in the list → that step becomes the current step.

- [ ] **Step 5: Verify persistence**

- Mark step 1.1.2 complete, set current to 1.1.5.
- Close RuneLite, relaunch the dev runner.
- Expected: panel opens to step 1.1.5 with 1.1.2 still struck through.

- [ ] **Step 6: Document the smoke result**

Update the commit log with a short note:

```bash
git commit --allow-empty -m "test: manual smoke against dev runner — feature 1 verified"
```

If any step in the smoke fails, file a follow-up and do not mark feature 1 complete.

---

## Spec coverage summary

| Spec section                       | Implemented in   |
|------------------------------------|------------------|
| `StepId` value type                | Task 2           |
| `GuideRepository` (load + lookups) | Task 4           |
| `GuideStateService` (state + persistence + events) | Task 5 |
| `StepRenderer` (rich-text rules)   | Task 6           |
| `BruhsailorPanel` (header + step block + nav) | Task 7 |
| `BruhsailorPanel` (step list)      | Task 8           |
| `BruhsailorPlugin` (wiring, error fallback, icon) | Task 9 |
| Persistence format & recovery rules | Task 5         |
| Error handling for malformed JSON  | Task 9 (`installErrorPanel`) |
| Test plan (StepId, repository, state, renderer) | Tasks 2,4,5,6 |
| Manual smoke                       | Task 10          |
