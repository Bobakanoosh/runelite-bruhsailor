package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.StepMapping;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class StepMappingsTest
{
    @Test
    public void loadsAll227StepIds()
    {
        StepMappings mappings = StepMappings.loadBundled(new com.google.gson.Gson());
        // Sanity check: every step id from the guide should have a mapping.
        // Guide is 227 entries; mappings.size() == 227 unless the JSON drifted.
        assertEquals(227, mappings.size());
    }

    @Test
    public void findByIdReturnsPresentForKnownStep()
    {
        StepMappings mappings = StepMappings.loadBundled(new com.google.gson.Gson());
        Optional<StepMapping> found = mappings.findById(StepId.parse("1.1.3"));
        assertTrue(found.isPresent());
        StepMapping m = found.get();
        assertNotNull(m.questIds);
        assertNotNull(m.items);
    }

    @Test
    public void findByIdReturnsEmptyForUnknownStep()
    {
        StepMappings mappings = StepMappings.loadBundled(new com.google.gson.Gson());
        assertFalse(mappings.findById(StepId.of(99, 99, 99)).isPresent());
    }

    @Test
    public void everyMappingHasNonNullQuestAndItemLists()
    {
        StepMappings mappings = StepMappings.loadBundled(new com.google.gson.Gson());
        mappings.allIds().forEach(id -> {
            StepMapping m = mappings.findById(id).orElseThrow(AssertionError::new);
            assertNotNull("questIds null for " + id, m.questIds);
            assertNotNull("items null for " + id, m.items);
        });
    }

    @Test
    public void emptyInstanceReturnsEmptyForAnyId()
    {
        StepMappings empty = StepMappings.empty();
        assertEquals(0, empty.size());
        assertFalse(empty.findById(StepId.parse("1.1.1")).isPresent());
    }
}
