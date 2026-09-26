package com.bruhsailor.plugin;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
    private static final String BULLETS_KEY = "completedBullets";
    private static final String VERSION_KEY = "guideVersion";

    private ConfigManager config;
    private EventBus bus;
    private GuideRepository repo;

    @Before
    public void setUp()
    {
        config = mock(ConfigManager.class);
        bus = mock(EventBus.class);
        repo = GuideRepository.loadBundled(new com.google.gson.Gson());
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

    /** ConfigManager mock backed by a map so writes made during construction are visible to reads. */
    private Map<String, String> backConfigWith(Map<String, String> values)
    {
        Map<String, String> store = new HashMap<>(values);
        when(config.getConfiguration(eq(GROUP), anyString())).thenAnswer(inv -> store.get(inv.<String>getArgument(1)));
        doAnswer(inv ->
        {
            store.put(inv.getArgument(1), inv.getArgument(2));
            return null;
        }).when(config).setConfiguration(eq(GROUP), anyString(), anyString());
        return store;
    }

    @Test
    public void bundledGuideIsTheMigrationTarget()
    {
        assertEquals("2026-08-30", repo.updatedOn());
    }

    @Test
    public void unversionedProgressIsMigratedFromLegacyGuide()
    {
        Map<String, String> initial = new HashMap<>();
        initial.put(CURRENT_KEY, "2.2.20");
        initial.put(COMPLETED_KEY, "2.2.9,2.2.10,2.2.11,2.2.35,2.3.10,2.3.15");
        initial.put(BULLETS_KEY, "2.2.36#2,2.3.10#0,1.1.1#0");
        Map<String, String> store = backConfigWith(initial);

        GuideStateService svc = new GuideStateService(repo, config, bus);

        assertEquals(StepId.parse("2.2.19"), svc.getCurrent());
        // 2.2.10 and 2.3.10 were removed; 2.2.11 -> 2.2.10, 2.2.35 -> 2.2.36, 2.3.15 -> 2.3.14
        assertEquals("2.2.9,2.2.10,2.2.36,2.3.14", store.get(COMPLETED_KEY));
        assertTrue(svc.isComplete(StepId.parse("2.2.10")));
        assertFalse(svc.isComplete(StepId.parse("2.3.10")));
        // Old 2.2.36 (chins) is now 2.2.34 and gained a sentence before bullet 2.
        assertEquals("1.1.1#0,2.2.34#3", store.get(BULLETS_KEY));
        assertTrue(svc.isBulletComplete(StepId.parse("2.2.34"), 3));
        assertEquals("2026-08-30", store.get(VERSION_KEY));
    }

    @Test
    public void currentOnRemovedStepLandsOnSuccessor()
    {
        Map<String, String> initial = new HashMap<>();
        initial.put(CURRENT_KEY, "2.2.10");
        backConfigWith(initial);

        GuideStateService svc = new GuideStateService(repo, config, bus);

        // Old 2.2.10 (Karamja diaries) is gone; old 2.2.11 now sits at 2.2.10.
        assertEquals(StepId.parse("2.2.10"), svc.getCurrent());
    }

    @Test
    public void currentVersionProgressIsNotRemapped()
    {
        Map<String, String> initial = new HashMap<>();
        initial.put(VERSION_KEY, "2026-08-30");
        initial.put(CURRENT_KEY, "2.2.20");
        initial.put(COMPLETED_KEY, "2.3.10");
        backConfigWith(initial);

        GuideStateService svc = new GuideStateService(repo, config, bus);

        assertEquals(StepId.parse("2.2.20"), svc.getCurrent());
        assertTrue(svc.isComplete(StepId.parse("2.3.10")));
        verify(config, never()).setConfiguration(anyString(), anyString(), anyString());
    }

    @Test
    public void freshInstallOnlyStampsVersion()
    {
        Map<String, String> store = backConfigWith(new HashMap<>());

        new GuideStateService(repo, config, bus);

        assertEquals("2026-08-30", store.get(VERSION_KEY));
        assertNull(store.get(CURRENT_KEY));
        assertNull(store.get(COMPLETED_KEY));
    }

    @Test
    public void currentIsPulledBackToStepsMovedAheadOfIt()
    {
        Map<String, String> initial = new HashMap<>();
        // Old 2.2.35 (sloop) came before chins; chins now precede it at 2.2.34.
        initial.put(CURRENT_KEY, "2.2.35");
        backConfigWith(initial);
        assertEquals(StepId.parse("2.2.34"), new GuideStateService(repo, config, bus).getCurrent());

        initial.put(CURRENT_KEY, "2.2.38");
        backConfigWith(initial);
        // New 2.2.35 (smelting + sepulchre prep) was split out of old 2.2.38.
        assertEquals(StepId.parse("2.2.35"), new GuideStateService(repo, config, bus).getCurrent());
    }

    @Test
    public void bulletsFollowTheirSentence()
    {
        Map<String, String> initial = new HashMap<>();
        // 2.2.5 gained a new first sentence; 1.1.11 lost its sentence 1.
        initial.put(BULLETS_KEY, "2.2.5#0,1.1.11#1,1.1.11#2");
        Map<String, String> store = backConfigWith(initial);

        new GuideStateService(repo, config, bus);

        assertEquals("1.1.11#1,2.2.5#1", store.get(BULLETS_KEY));
    }

    @Test
    public void rewrittenStepsAreNoLongerComplete()
    {
        Map<String, String> initial = new HashMap<>();
        // 3.1.12 now includes starting The Red Reef.
        initial.put(COMPLETED_KEY, "3.1.11,3.1.12");
        Map<String, String> store = backConfigWith(initial);

        GuideStateService svc = new GuideStateService(repo, config, bus);

        assertEquals("3.1.11", store.get(COMPLETED_KEY));
        assertFalse(svc.isComplete(StepId.parse("3.1.12")));
    }

    @Test
    public void unknownVersionIsNotStampedOver()
    {
        Map<String, String> initial = new HashMap<>();
        initial.put(VERSION_KEY, "2020-01-01");
        initial.put(COMPLETED_KEY, "2.2.20");
        Map<String, String> store = backConfigWith(initial);

        new GuideStateService(repo, config, bus);

        assertEquals("2020-01-01", store.get(VERSION_KEY));
        assertEquals("2.2.20", store.get(COMPLETED_KEY));
    }
}
