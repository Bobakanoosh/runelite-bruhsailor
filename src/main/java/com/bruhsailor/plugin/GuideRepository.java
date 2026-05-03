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
