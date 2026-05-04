package com.bruhsailor.plugin;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
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
    static final String COMPLETED_BULLETS_KEY = "completedBullets";

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
