package com.bruhsailor.plugin;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class InlineMatcherTest
{
    private final QuestEntry cooks = new QuestEntry("COOKS_ASSISTANT", "Cook's Assistant");
    private final QuestEntry ds1 = new QuestEntry("DRAGON_SLAYER_I", "Dragon Slayer I",
        Arrays.asList("Dragon Slayer", "DS", "DS1"));
    private final QuestEntry ds2 = new QuestEntry("DRAGON_SLAYER_II", "Dragon Slayer II",
        Arrays.asList("DS2"));
    private final QuestEntry mm = new QuestEntry("MISTHALIN_MYSTERY", "Misthalin Mystery");

    @Test
    public void emptyTextReturnsEmpty()
    {
        assertTrue(InlineMatcher.findFirstPerQuest("", Arrays.asList(cooks)).isEmpty());
    }

    @Test
    public void noMatchReturnsEmpty()
    {
        assertTrue(InlineMatcher.findFirstPerQuest("No quests here.", Arrays.asList(cooks)).isEmpty());
    }

    @Test
    public void exactDisplayNameMatches()
    {
        List<InlineMatcher.Match> ms = InlineMatcher.findFirstPerQuest(
            "Go south and do Misthalin Mystery now.", Arrays.asList(mm));
        assertEquals(1, ms.size());
        assertEquals(mm, ms.get(0).entry);
        assertEquals("Misthalin Mystery", "Go south and do Misthalin Mystery now.".substring(ms.get(0).start, ms.get(0).end));
    }

    @Test
    public void caseInsensitiveMatch()
    {
        List<InlineMatcher.Match> ms = InlineMatcher.findFirstPerQuest(
            "do misthalin mystery", Arrays.asList(mm));
        assertEquals(1, ms.size());
    }

    @Test
    public void wordBoundaryPreventsPartialMatch()
    {
        // "Cooked" should not match "Cook" candidate
        QuestEntry cook = new QuestEntry("COOK", "Cook");
        List<InlineMatcher.Match> ms = InlineMatcher.findFirstPerQuest(
            "Cooked redberry pie", Collections.singletonList(cook));
        assertTrue(ms.isEmpty());
    }

    @Test
    public void longerCandidateBeatsShorter()
    {
        // Both DS1 and DS2 have aliases; "Dragon Slayer II" must win over DS1's "Dragon Slayer".
        List<InlineMatcher.Match> ms = InlineMatcher.findFirstPerQuest(
            "Complete Dragon Slayer II next.", Arrays.asList(ds1, ds2));
        assertEquals(1, ms.size());
        assertEquals(ds2, ms.get(0).entry);
    }

    @Test
    public void firstOccurrenceOnlyPerQuest()
    {
        List<InlineMatcher.Match> ms = InlineMatcher.findFirstPerQuest(
            "Misthalin Mystery first time. Misthalin Mystery second time.",
            Arrays.asList(mm));
        assertEquals(1, ms.size());
        assertEquals(0, ms.get(0).start);
    }

    @Test
    public void shortAliasesUnderThreeCharsAreSkipped()
    {
        // "DS" is 2 chars; should be skipped to avoid matching "DS" inside random words.
        QuestEntry q = new QuestEntry("X", "Some Quest", Arrays.asList("DS", "DSX"));
        List<InlineMatcher.Match> ms = InlineMatcher.findFirstPerQuest(
            "Use DSX for the route.", Collections.singletonList(q));
        assertEquals(1, ms.size());
        assertEquals("DSX", "Use DSX for the route.".substring(ms.get(0).start, ms.get(0).end));
    }

    @Test
    public void matchesReturnedInOrder()
    {
        List<InlineMatcher.Match> ms = InlineMatcher.findFirstPerQuest(
            "Cook's Assistant first, then Misthalin Mystery, finally Dragon Slayer II.",
            Arrays.asList(mm, cooks, ds2));
        assertEquals(3, ms.size());
        assertTrue(ms.get(0).start < ms.get(1).start);
        assertTrue(ms.get(1).start < ms.get(2).start);
    }
}
