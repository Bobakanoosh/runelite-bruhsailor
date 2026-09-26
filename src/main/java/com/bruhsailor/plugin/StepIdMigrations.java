package com.bruhsailor.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Remaps persisted positional {@link StepId}s when a guide update inserts, removes or
 * reorders steps. Each migration covers one bundled guide version to the next; they chain.
 *
 * When bundling a new guide version whose step positions shift, add a migration here
 * keyed by the previous {@code updatedOn}.
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

        /** New ID for a completed step or bullet; empty if the step was removed. */
        Optional<StepId> remap(StepId id)
        {
            if (removed.containsKey(id)) return Optional.empty();
            return Optional.of(moved.getOrDefault(id, id));
        }

        /** New ID for the current-step pointer; removed steps land on their successor. */
        StepId remapCurrent(StepId id)
        {
            StepId successor = removed.get(id);
            return successor != null ? successor : moved.getOrDefault(id, id);
        }
    }

    private static final Map<String, Migration> BY_FROM_VERSION;

    static
    {
        Map<String, Migration> m = new HashMap<>();

        // 2026-04-10 -> 2026-08-30: Karamja diaries (2.2.10) folded into a later step, Jim/sloop/chins
        // steps reordered, hybrid-ore smelting step inserted at 2.2.35, Kourend medium (2.3.10) removed.
        m.put(LEGACY_GUIDE_VERSION, new Migration(LEGACY_GUIDE_VERSION, "2026-08-30")
            .remove(StepId.of(2, 2, 10), StepId.of(2, 2, 10))
            .shift(2, 2, 11, 33, -1)
            .move(StepId.of(2, 2, 34), StepId.of(2, 2, 33))
            .move(StepId.of(2, 2, 35), StepId.of(2, 2, 36))
            .move(StepId.of(2, 2, 36), StepId.of(2, 2, 34))
            .remove(StepId.of(2, 3, 10), StepId.of(2, 3, 10))
            .shift(2, 3, 11, 15, -1));

        BY_FROM_VERSION = Collections.unmodifiableMap(m);
    }

    private StepIdMigrations()
    {
    }

    static Optional<Migration> from(String version)
    {
        return Optional.ofNullable(BY_FROM_VERSION.get(version));
    }
}
