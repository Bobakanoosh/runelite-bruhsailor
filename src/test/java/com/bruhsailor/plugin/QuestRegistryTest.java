package com.bruhsailor.plugin;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class QuestRegistryTest
{
    @Test
    public void bundledLoadHasAtLeastTwoHundredEntries()
    {
        QuestRegistry registry = QuestRegistry.create(new com.google.gson.Gson());
        assertTrue(registry.size() >= 200);
    }

    @Test
    public void resolvesKnownEnumNameToDisplayName()
    {
        QuestRegistry registry = QuestRegistry.create(new com.google.gson.Gson());
        Optional<QuestEntry> e = registry.resolve("COOKS_ASSISTANT");
        assertTrue(e.isPresent());
        assertEquals("COOKS_ASSISTANT", e.get().enumName());
        assertEquals("Cook's Assistant", e.get().displayName());
    }

    @Test
    public void resolvesUnknownEnumNameToEmpty()
    {
        QuestRegistry registry = QuestRegistry.create(new com.google.gson.Gson());
        assertFalse(registry.resolve("NOT_A_REAL_QUEST_XYZ").isPresent());
    }

    @Test
    public void clearlyInvalidEnumIsNotResolved()
    {
        QuestRegistry registry = QuestRegistry.create(new com.google.gson.Gson());
        assertFalse(registry.resolve("BANANA_JUICE_QUEST").isPresent());
    }
}
