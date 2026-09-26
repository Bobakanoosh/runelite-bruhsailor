package com.bruhsailor.plugin;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GuideStateService
{
    private static final Logger log = LoggerFactory.getLogger(GuideStateService.class);

    static final String GROUP = "bruhsailor";
    static final String CURRENT_KEY = "currentStepId";
    static final String COMPLETED_KEY = "completedStepIds";
    static final String COMPLETED_BULLETS_KEY = "completedBullets";
    static final String GUIDE_VERSION_KEY = "guideVersion";

    private final GuideRepository repo;
    private final ConfigManager config;
    private final EventBus bus;

    private StepId current;
    private final Set<StepId> completed = new TreeSet<>();
    // Per-bullet completion: stored as "{stepId}#{bulletIdx}" tokens.
    private final Set<String> completedBullets = new HashSet<>();

    public GuideStateService(GuideRepository repo, ConfigManager config, EventBus bus)
    {
        this.repo = repo;
        this.config = config;
        this.bus = bus;
        loadFromConfig();
    }

    /**
     * Step IDs are positional, so a guide update that inserts/removes steps shifts them.
     * Rewrites persisted IDs from the guide version they were saved against to the bundled one.
     */
    private void migrateStoredProgress()
    {
        String target = repo.updatedOn();
        String stored = config.getConfiguration(GROUP, GUIDE_VERSION_KEY);
        if (target == null || target.equals(stored)) return;

        String rawCurrent = config.getConfiguration(GROUP, CURRENT_KEY);
        String rawCompleted = config.getConfiguration(GROUP, COMPLETED_KEY);
        String rawBullets = config.getConfiguration(GROUP, COMPLETED_BULLETS_KEY);

        if (stored == null)
        {
            boolean hasProgress = !isNullOrEmpty(rawCurrent) || !isNullOrEmpty(rawCompleted) || !isNullOrEmpty(rawBullets);
            if (!hasProgress)
            {
                config.setConfiguration(GROUP, GUIDE_VERSION_KEY, target);
                return;
            }
            stored = StepIdMigrations.LEGACY_GUIDE_VERSION;
        }

        String version = stored;
        while (!version.equals(target))
        {
            Optional<StepIdMigrations.Migration> m = StepIdMigrations.from(version);
            if (!m.isPresent()) break;
            StepIdMigrations.Migration migration = m.get();
            rawCurrent = migrateCurrent(rawCurrent, migration);
            rawCompleted = migrateCompleted(rawCompleted, migration);
            rawBullets = migrateBullets(rawBullets, migration);
            version = migration.to;
        }

        if (!version.equals(stored))
        {
            log.info("Migrated saved progress from guide {} to {}", stored, version);
            if (rawCurrent != null) config.setConfiguration(GROUP, CURRENT_KEY, rawCurrent);
            if (rawCompleted != null) config.setConfiguration(GROUP, COMPLETED_KEY, rawCompleted);
            if (rawBullets != null) config.setConfiguration(GROUP, COMPLETED_BULLETS_KEY, rawBullets);
            config.setConfiguration(GROUP, GUIDE_VERSION_KEY, version);
        }
        if (!version.equals(target))
        {
            // Leave the stamp at the last version we could reach so a later release that adds the
            // missing migration still runs. Until then some saved IDs may point at the wrong steps.
            log.warn("No step ID migration path from guide {} to {}; saved progress may be misaligned", version, target);
        }
    }

    private static String migrateCurrent(String raw, StepIdMigrations.Migration m)
    {
        if (isNullOrEmpty(raw)) return raw;
        try
        {
            return m.remapCurrent(StepId.parse(raw.trim())).toString();
        }
        catch (IllegalArgumentException e)
        {
            return raw;
        }
    }

    private static String migrateCompleted(String raw, StepIdMigrations.Migration m)
    {
        if (isNullOrEmpty(raw)) return raw;
        Set<StepId> out = new TreeSet<>();
        for (String token : raw.split(","))
        {
            String t = token.trim();
            if (t.isEmpty()) continue;
            try
            {
                m.remapCompleted(StepId.parse(t)).ifPresent(out::add);
            }
            catch (IllegalArgumentException e)
            {
                // Malformed: drop now rather than carry it forward.
            }
        }
        return out.stream().map(StepId::toString).collect(Collectors.joining(","));
    }

    private static String migrateBullets(String raw, StepIdMigrations.Migration m)
    {
        if (isNullOrEmpty(raw)) return raw;
        Set<String> out = new TreeSet<>();
        for (String token : raw.split(","))
        {
            String t = token.trim();
            int hash = t.indexOf('#');
            if (hash < 0) continue;
            try
            {
                StepId oldId = StepId.parse(t.substring(0, hash));
                int oldIdx = Integer.parseInt(t.substring(hash + 1));
                Optional<StepId> newId = m.remapStep(oldId);
                OptionalInt newIdx = m.remapBullet(oldId, oldIdx);
                if (newId.isPresent() && newIdx.isPresent())
                {
                    out.add(bulletKey(newId.get(), newIdx.getAsInt()));
                }
            }
            catch (IllegalArgumentException e)
            {
                // Malformed: drop.
            }
        }
        return String.join(",", out);
    }

    private static boolean isNullOrEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

    private void loadFromConfig()
    {
        migrateStoredProgress();

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

        String rawBullets = config.getConfiguration(GROUP, COMPLETED_BULLETS_KEY);
        if (rawBullets != null && !rawBullets.isEmpty())
        {
            for (String token : rawBullets.split(","))
            {
                String t = token.trim();
                if (t.isEmpty()) continue;
                completedBullets.add(t);
            }
        }
    }

    private static String bulletKey(StepId id, int idx)
    {
        return id.toString() + "#" + idx;
    }

    public boolean isBulletComplete(StepId id, int idx)
    {
        return completedBullets.contains(bulletKey(id, idx));
    }

    public void toggleBullet(StepId id, int idx)
    {
        String k = bulletKey(id, idx);
        if (!completedBullets.add(k)) completedBullets.remove(k);
        persistBullets();
        bus.post(new GuideStateChanged(current, current, true));
    }

    private void persistBullets()
    {
        String joined = completedBullets.stream().sorted().collect(Collectors.joining(","));
        config.setConfiguration(GROUP, COMPLETED_BULLETS_KEY, joined);
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
