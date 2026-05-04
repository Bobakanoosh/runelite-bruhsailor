package com.bruhsailor.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Finds substring matches of quest names (and aliases) inside step prose.
 *
 * Rules:
 * - Case-insensitive.
 * - Word-boundary aware: must be flanked by non-letter-or-digit on both sides
 *   (so "Cook" inside "Cookery" doesn't match).
 * - Longest candidate wins ("Dragon Slayer II" before "Dragon Slayer").
 * - First-occurrence per quest. We don't link every mention, only the first —
 *   keeps the prose readable.
 * - Matches never overlap: once a span is consumed, no other candidate may
 *   reuse those characters.
 */
public final class InlineMatcher
{
    public static final class Match
    {
        public final int start;
        public final int end;
        public final QuestEntry entry;

        Match(int start, int end, QuestEntry entry)
        {
            this.start = start;
            this.end = end;
            this.entry = entry;
        }
    }

    private InlineMatcher() {}

    public static List<Match> findFirstPerQuest(String text, Collection<QuestEntry> quests)
    {
        if (text == null || text.isEmpty() || quests == null || quests.isEmpty())
        {
            return Collections.emptyList();
        }

        // Build sorted candidate list: (text, entry) pairs, longest first.
        List<Candidate> candidates = new ArrayList<>();
        for (QuestEntry q : quests)
        {
            if (q == null) continue;
            addCandidate(candidates, q.displayName(), q);
            for (String a : q.aliases()) addCandidate(candidates, a, q);
        }
        candidates.sort((a, b) -> b.text.length() - a.text.length());

        String lower = text.toLowerCase();
        boolean[] consumed = new boolean[text.length()];
        List<QuestEntry> linked = new ArrayList<>();
        List<Match> matches = new ArrayList<>();

        for (Candidate c : candidates)
        {
            if (linked.contains(c.entry)) continue; // first-occurrence per quest
            int idx = findWordBoundary(lower, text, c.lower, 0, consumed);
            if (idx < 0) continue;
            int end = idx + c.lower.length();
            for (int i = idx; i < end; i++) consumed[i] = true;
            linked.add(c.entry);
            matches.add(new Match(idx, end, c.entry));
        }

        matches.sort((a, b) -> Integer.compare(a.start, b.start));
        return matches;
    }

    private static int findWordBoundary(String lower, String original, String needle, int from, boolean[] consumed)
    {
        int searchFrom = from;
        while (searchFrom <= lower.length() - needle.length())
        {
            int idx = lower.indexOf(needle, searchFrom);
            if (idx < 0) return -1;
            int end = idx + needle.length();
            boolean leftOK = (idx == 0) || !isWordChar(original.charAt(idx - 1));
            boolean rightOK = (end == original.length()) || !isWordChar(original.charAt(end));
            boolean overlap = false;
            if (leftOK && rightOK)
            {
                for (int i = idx; i < end; i++)
                {
                    if (consumed[i]) { overlap = true; break; }
                }
                if (!overlap) return idx;
            }
            searchFrom = idx + 1;
        }
        return -1;
    }

    private static boolean isWordChar(char c)
    {
        return Character.isLetterOrDigit(c);
    }

    private static void addCandidate(List<Candidate> out, String text, QuestEntry entry)
    {
        if (text == null || text.length() < 3) return; // skip noise like "DT"
        out.add(new Candidate(text, entry));
    }

    private static final class Candidate
    {
        final String text;
        final String lower;
        final QuestEntry entry;

        Candidate(String text, QuestEntry entry)
        {
            this.text = text;
            this.lower = text.toLowerCase();
            this.entry = entry;
        }
    }
}
