package com.bruhsailor.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Remaps persisted positional {@link StepId}s when a guide update inserts, removes or
 * reorders steps. Each migration covers one bundled guide version to the next; they chain.
 *
 * When bundling a new guide version whose step positions or sentence splits change, add a
 * migration here keyed by the previous {@code updatedOn}. All IDs passed to the builder
 * methods are OLD IDs unless noted.
 */
final class StepIdMigrations
{
    /** Guide version bundled before progress was version-stamped. */
    static final String LEGACY_GUIDE_VERSION = "2026-04-10";

    static final class Migration
    {
        final String from;
        final String to;
        // Steps whose position changed. Unlisted steps keep their ID.
        private final Map<StepId, StepId> moved = new HashMap<>();
        // Steps that no longer exist, mapped to the step that now follows their old position.
        private final Map<StepId, StepId> removed = new HashMap<>();
        // Where the current-step pointer goes when it differs from the step's own new ID,
        // e.g. because steps the user hasn't done yet were moved ahead of it.
        private final Map<StepId, StepId> currentOverride = new HashMap<>();
        // Steps that gained substantial new work; their done marks are cleared.
        private final Set<StepId> rewritten = new HashSet<>();
        // Per-step old -> new bullet (sentence) index. Listed steps keep only mapped
        // indices; unlisted steps keep indices unchanged.
        private final Map<StepId, Map<Integer, Integer>> bulletRemap = new HashMap<>();

        private Migration(String from, String to)
        {
            this.from = from;
            this.to = to;
        }

        private Migration shift(int chapter, int section, int firstStep, int lastStep, int delta)
        {
            for (int s = firstStep; s <= lastStep; s++)
            {
                move(StepId.of(chapter, section, s), StepId.of(chapter, section, s + delta));
            }
            return this;
        }

        private Migration move(StepId from, StepId to)
        {
            moved.put(from, to);
            return this;
        }

        private Migration remove(StepId id, StepId successor)
        {
            removed.put(id, successor);
            return this;
        }

        /** @param newCurrent NEW ID */
        private Migration current(StepId id, StepId newCurrent)
        {
            currentOverride.put(id, newCurrent);
            return this;
        }

        private Migration rewritten(StepId... ids)
        {
            Collections.addAll(rewritten, ids);
            return this;
        }

        /** @param mapping comma-separated {@code old>new} bullet indices; empty drops all */
        private Migration bullets(StepId id, String mapping)
        {
            Map<Integer, Integer> m = new HashMap<>();
            for (String pair : mapping.split(","))
            {
                if (pair.isEmpty()) continue;
                String[] parts = pair.split(">");
                m.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
            bulletRemap.put(id, m);
            return this;
        }

        /** New ID for a step, ignoring completion; empty if the step was removed. */
        Optional<StepId> remapStep(StepId id)
        {
            if (removed.containsKey(id)) return Optional.empty();
            return Optional.of(moved.getOrDefault(id, id));
        }

        /** New ID for a completed step; empty if removed or rewritten with new work. */
        Optional<StepId> remapCompleted(StepId id)
        {
            if (rewritten.contains(id)) return Optional.empty();
            return remapStep(id);
        }

        /** New ID for the current-step pointer. */
        StepId remapCurrent(StepId id)
        {
            StepId override = currentOverride.get(id);
            if (override != null) return override;
            StepId successor = removed.get(id);
            return successor != null ? successor : moved.getOrDefault(id, id);
        }

        /** New bullet index within {@link #remapStep}'s step; empty if that sentence is gone. */
        OptionalInt remapBullet(StepId id, int idx)
        {
            Map<Integer, Integer> m = bulletRemap.get(id);
            if (m == null) return OptionalInt.of(idx);
            Integer mapped = m.get(idx);
            return mapped == null ? OptionalInt.empty() : OptionalInt.of(mapped);
        }
    }

    private static final Map<String, Migration> BY_FROM_VERSION;

    static
    {
        Map<String, Migration> m = new HashMap<>();

        // 2026-04-10 -> 2026-08-30: Karamja diaries (2.2.10) folded into a later step, Jim/sloop/chins
        // steps reordered, hybrid-ore smelting + sepulchre prep inserted at 2.2.35 (partly split out of
        // old 2.2.38), Kourend medium (2.3.10) removed.
        m.put(LEGACY_GUIDE_VERSION, new Migration(LEGACY_GUIDE_VERSION, "2026-08-30")
            .remove(id("2.2.10"), id("2.2.10"))
            .shift(2, 2, 11, 33, -1)
            .move(id("2.2.34"), id("2.2.33"))
            .move(id("2.2.35"), id("2.2.36"))
            .move(id("2.2.36"), id("2.2.34"))
            .remove(id("2.3.10"), id("2.3.10"))
            .shift(2, 3, 11, 15, -1)
            // Chins (now 2.2.34) moved ahead of the sloop step; new 2.2.35 comes before sepulchre.
            .current(id("2.2.35"), id("2.2.34"))
            .current(id("2.2.37"), id("2.2.35"))
            .current(id("2.2.38"), id("2.2.35"))
            // Changelog "Long"/"Medium" items that add work to an existing step: hunter progression
            // (spotted kebbits, trapper's tipple, blue crabs), Fallen From Grace, Mad Angel + crafting
            // to 80, The Red Reef + camphor, The Blood Moon Rises, ToA for thread.
            .rewritten(id("1.4.31"), id("2.2.33"), id("2.2.34"), id("3.1.5"), id("3.1.7"),
                id("3.1.12"), id("3.1.13"), id("3.1.14"), id("3.2.4"), id("3.2.11"))
            // Generated by matching each old step's sentence split (StepRenderer) against its new step.
            .bullets(id("1.1.6"), "0>0,1>1,2>2,3>3,4>4,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>13,14>14,15>15,16>16,17>17")
            .bullets(id("1.1.9"), "1>1,2>2,4>4,5>5,6>6")
            .bullets(id("1.1.10"), "0>0,1>1,2>2,3>3,4>4,5>5,8>10")
            .bullets(id("1.1.11"), "0>0,2>1,3>2,4>3,5>4,6>5,7>6,8>7,9>8,11>10,12>11,13>12,14>13,15>14,16>15,17>16,18>17")
            .bullets(id("1.1.12"), "0>0,1>1")
            .bullets(id("1.1.13"), "0>0,1>1,2>2,3>3,4>4,5>5,7>8,8>9,9>10,10>11,11>12,12>13,13>14,14>15,15>16,16>17,17>18")
            .bullets(id("1.1.16"), "0>0")
            .bullets(id("1.1.17"), "1>1,2>2,3>4,4>5")
            .bullets(id("1.1.18"), "0>0,1>1,2>2,3>3,4>4,5>5,7>8,8>9,9>10,10>11,11>12,12>13,13>14,14>15,15>16,16>17,17>18,18>19,19>20,20>21,21>22,22>23,23>24,24>25,25>28,26>29,27>30,28>31,29>32,30>33,31>34,32>35,33>36,34>37,35>38,36>39,37>40,38>41,39>42,40>43,41>44,42>45")
            .bullets(id("1.2.3"), "0>0,1>1,2>2,4>4,5>5,8>7,9>8")
            .bullets(id("1.2.4"), "1>1,2>2,3>3,4>4,5>5,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>13,14>14,15>15")
            .bullets(id("1.2.8"), "0>0,1>1,3>4,4>5,5>6,6>7,7>8,8>9,9>10,10>11,11>12,12>13,13>14,14>15,15>16,16>17,17>18,18>19,19>20,20>21,21>22,22>23,23>24,24>25,25>26,26>27,27>28,28>29,29>30,30>31")
            .bullets(id("1.2.9"), "0>0,1>1,2>2,3>3,5>5,6>6,7>7,8>8,9>9,10>10")
            .bullets(id("1.2.13"), "0>0,1>1,2>2,3>3,4>4,5>6,6>7,7>8,8>9,9>10,10>11")
            .bullets(id("1.3.3"), "0>0,1>1,2>2")
            .bullets(id("1.3.8"), "0>0,1>2,2>3,3>4,4>5,5>6,6>7")
            .bullets(id("1.3.18"), "0>0,1>1,2>2,3>3,5>5,6>6,7>7,8>8,9>9")
            .bullets(id("1.3.31"), "0>0,1>1,2>2,3>3")
            .bullets(id("1.4.6"), "0>0,1>1,2>2,3>3,4>4,5>5")
            .bullets(id("1.4.8"), "0>0,1>1,4>6")
            .bullets(id("1.4.9"), "0>0,1>1,2>2,4>4,5>5,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>13,14>14,15>15,16>16,17>17,18>18,19>19,20>20,21>21,22>22,23>23,24>24,25>25,26>26,27>27,28>28,29>29,30>30,31>31,32>32,33>33,34>34,35>35,36>36,37>37,38>38,39>39,40>40,41>41")
            .bullets(id("1.4.11"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7,8>8,9>9,10>11,11>12")
            .bullets(id("1.4.12"), "1>1,2>2,3>3,4>4,5>5,6>6")
            .bullets(id("1.4.14"), "0>0,1>1,2>2,3>3,4>4,6>7,7>8,8>9,9>10,10>11,11>12,12>13,13>14,14>15,15>16,16>17,17>18")
            .bullets(id("1.4.18"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7,9>9,10>10,11>11,12>13,13>14,14>15,15>16,16>17,17>18,18>19")
            .bullets(id("1.4.19"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7")
            .bullets(id("1.4.24"), "0>0,1>1,2>2,3>3,4>4,5>5,7>7,8>8")
            .bullets(id("1.4.27"), "0>0,1>1,2>2,3>3")
            .bullets(id("1.4.28"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7,8>9,9>10,10>11")
            .bullets(id("1.4.29"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>13,14>14,15>15,16>16,17>17,18>18,19>19,20>20,21>21,22>22,23>23,24>24,25>25,26>26,27>27,28>28,29>29,30>30,31>31,32>32,33>33,34>34,35>35,36>36,37>37,38>38,39>39,40>40,41>41,42>42,43>43,44>44,46>46,47>47,48>48")
            .bullets(id("1.4.31"), "2>4,3>5,4>6,5>7,6>8,7>9,8>10,9>11,10>12")
            .bullets(id("1.4.32"), "0>2,1>3")
            .bullets(id("2.1.2"), "0>0,1>1,2>2,4>4,5>5,6>6,7>7,8>8,9>9")
            .bullets(id("2.1.5"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7")
            .bullets(id("2.1.6"), "0>0,1>1,2>2,3>3")
            .bullets(id("2.1.7"), "0>0,1>1,3>3,4>4")
            .bullets(id("2.1.10"), "1>1,2>2")
            .bullets(id("2.1.13"), "0>0,1>1,2>3,3>4,4>5,5>6,6>7")
            .bullets(id("2.1.18"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7,8>8,9>9,10>10,11>12,12>13,13>14,14>15,15>16,16>17,18>19,19>20,20>21,21>22,22>23,23>24,24>25,25>26,26>27")
            .bullets(id("2.1.21"), "0>0,1>1,2>2,3>3,4>4,9>6,10>7,11>8")
            .bullets(id("2.1.30"), "1>1,2>2,3>3,4>4,5>5,7>7,8>8,9>9")
            .bullets(id("2.1.31"), "0>0,2>1")
            .bullets(id("2.1.33"), "0>0,1>1,3>3,4>4,5>5,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>13,14>14")
            .bullets(id("2.1.34"), "0>0,3>2,4>3,6>8,8>10")
            .bullets(id("2.1.40"), "0>0,2>2,3>3,4>4,5>5")
            .bullets(id("2.1.41"), "0>0,2>1,3>2,4>3,5>4,6>5,7>6,8>7")
            .bullets(id("2.1.44"), "0>0,2>2,3>3,4>4,5>5,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>13,14>14,15>15")
            .bullets(id("2.1.45"), "")
            .bullets(id("2.2.1"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>8,8>9,11>11,12>12")
            .bullets(id("2.2.2"), "1>0,4>2,5>3,6>4")
            .bullets(id("2.2.5"), "0>1,1>2,2>3,3>4,4>5,5>6")
            .bullets(id("2.2.7"), "0>0,1>1,2>2,3>3")
            .bullets(id("2.2.9"), "0>0,1>1")
            .bullets(id("2.2.13"), "0>0,2>3")
            .bullets(id("2.2.25"), "0>0,1>1,2>2,3>3,4>4,5>5,7>7")
            .bullets(id("2.2.26"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7,8>8,10>10")
            .bullets(id("2.2.27"), "0>0,1>1,2>2,3>3,5>5,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>13")
            .bullets(id("2.2.28"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>9,8>10,9>11,10>12,11>13,12>14,13>15,14>16,15>17,16>18,17>19,18>20,19>21,20>22,22>26")
            .bullets(id("2.2.30"), "0>0,1>1,2>2,3>3,4>4,5>5,7>7,8>8,9>11,10>12,11>13,12>17,13>18")
            .bullets(id("2.2.32"), "0>0,1>1,2>2,3>3,4>5,5>6,7>8")
            .bullets(id("2.2.33"), "0>0,1>1,2>2,3>3,6>6,7>7,8>8,9>9,10>10,11>11,12>12,13>14,14>15,15>16,16>17,17>18,18>19,19>20,20>21,21>22,22>23")
            .bullets(id("2.2.34"), "2>2,3>3,4>4,5>5,6>6,7>7,8>8,9>9,10>10,11>18,12>19,15>22,16>23")
            .bullets(id("2.2.35"), "2>1,4>3,5>4,6>5,7>6,8>7")
            .bullets(id("2.2.36"), "0>0,1>1,2>3,3>4,4>5,5>6,6>7,7>8,8>9,9>10,10>11,11>12")
            .bullets(id("2.2.37"), "0>0,3>7,5>8,6>18,7>14,8>15,9>16,10>17,11>20,12>21,14>23,15>24,16>25,17>26,19>28,20>29,21>30,22>31,23>32,24>33,25>34,26>35,36>36,37>37,38>38,39>39")
            .bullets(id("2.2.38"), "7>7,8>8,9>9,10>10,13>14")
            .bullets(id("2.2.39"), "0>0,2>1,3>2,4>3,6>5,7>7,13>8,14>9,16>12,17>11,18>13,19>14,20>15,21>16,22>17,23>29,25>31,26>32,27>33,28>34,30>36,32>38,35>43,36>44")
            .bullets(id("2.3.4"), "1>1")
            .bullets(id("3.1.2"), "")
            .bullets(id("3.1.3"), "1>1")
            .bullets(id("3.1.5"), "0>0,1>1,2>2,3>3,4>4,5>5")
            .bullets(id("3.1.7"), "0>2,1>3,3>7,4>8,5>9,6>10,7>11,8>12,9>13,10>16,11>17,12>18,13>19,14>20,15>21,16>22,17>23,18>24,19>25,20>26,21>27,22>28,24>30,25>31,26>32,27>33,28>34,29>35,30>36")
            .bullets(id("3.1.8"), "4>3,5>4")
            .bullets(id("3.1.10"), "0>0,1>3,2>4,4>6,5>7,6>8,7>9,8>7,11>14,12>15,13>16,14>17,15>18,16>19,17>20,18>21,19>22,20>23,21>24,22>25,23>26,24>27,25>28")
            .bullets(id("3.1.12"), "0>0,2>3,3>4,4>5,6>7,9>10,10>11,11>12")
            .bullets(id("3.1.13"), "0>0,1>1,2>2,3>3,4>4,6>6,7>9,8>10,9>11,10>12,11>13,12>14,13>15,14>16,15>17,16>18,17>19,18>20,19>21,20>22,21>20,22>24,23>25")
            .bullets(id("3.1.14"), "")
            .bullets(id("3.1.16"), "0>0,1>1,2>2,3>3,6>6,7>7,8>19,9>20,10>21,11>22,12>23,13>8,14>9,15>10")
            .bullets(id("3.2.1"), "0>0,1>1,2>2,3>3,4>4,5>5,6>6,7>7")
            .bullets(id("3.2.3"), "0>0,1>1,2>2,3>3,4>4")
            .bullets(id("3.2.4"), "0>3,1>4,2>5,4>8,5>9,6>10,7>11,8>12,9>14,10>15")
            .bullets(id("3.2.5"), "0>0,1>1,4>5,5>6,6>7,7>8,8>9,9>10,10>11,11>12,12>13,13>14,14>15,16>21,17>22,18>24,19>25,20>26,21>28,22>29,23>30,28>35,29>36,30>37,31>38,32>39,33>43,34>44,35>45,36>46,37>47,38>48,39>49,40>50")
            .bullets(id("3.2.11"), "0>0,1>2,2>3,3>4,4>5,5>6,6>7,7>8,8>9"));

        BY_FROM_VERSION = Collections.unmodifiableMap(m);
    }

    private StepIdMigrations()
    {
    }

    private static StepId id(String s)
    {
        return StepId.parse(s);
    }

    static Optional<Migration> from(String version)
    {
        return Optional.ofNullable(BY_FROM_VERSION.get(version));
    }
}
