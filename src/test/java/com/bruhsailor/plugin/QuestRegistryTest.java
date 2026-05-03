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
    public void clearlyInvalidEnumIsNotResolved()
    {
        // The bundled set mirrors QH's QuestHelperQuest enum, which includes
        // skill helpers like AGILITY. Use a bogus name to assert miss behaviour.
        QuestRegistry registry = QuestRegistry.create(pluginManager);
        assertFalse(registry.resolve("BANANA_JUICE_QUEST").isPresent());
    }

    @Test
    public void enrichedFromRuntimeIsFalseWhenNoQHPluginPresent()
    {
        QuestRegistry registry = QuestRegistry.create(pluginManager);
        assertFalse(registry.enrichedFromRuntime());
    }
}
