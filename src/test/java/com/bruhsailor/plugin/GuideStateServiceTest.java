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
