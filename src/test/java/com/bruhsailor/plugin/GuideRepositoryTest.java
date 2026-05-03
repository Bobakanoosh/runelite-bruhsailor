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
