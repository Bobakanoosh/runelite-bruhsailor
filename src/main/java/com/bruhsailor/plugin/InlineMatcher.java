package com.bruhsailor.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Finds substring matches of named entities (quests, NPCs, locations) inside step prose.
 *
 * Rules:
 * - Case-insensitive.
 * - Word-boundary aware: must be flanked by non-letter-or-digit on both sides
 *   (so "Cook" inside "Cookery" doesn't match).
 * - Longest candidate wins ("Dragon Slayer II" before "Dragon Slayer").
 * - First-occurrence per entry. We don't link every mention, only the first —
 *   keeps the prose readable.
 * - Matches never overlap: once a span is consumed, no other candidate may
 *   reuse those characters.
 * - Curly punctuation (smart quotes, em/en dashes) is normalized to ASCII so
 *   "Heroes' Quest" matches prose typed as "Heroes’ Quest".
 */
public final class InlineMatcher
{
    public static final class Match<T>
    {
        public final int start;
        public final int end;
        public final T entry;

        Match(int start, int end, T entry)
        {
            this.start = start;
            this.end = end;
            this.entry = entry;
        }
    }

    private InlineMatcher() {}

    public static List<Match<QuestEntry>> findFirstPerQuest(String text, Collection<QuestEntry> quests)
    {
        return findFirstPer(text, quests, q -> {
            List<String> all = new ArrayList<>();
            all.add(q.displayName());
            all.addAll(q.aliases());
            return all;
        });
    }

    /** Convenience for plain-string entries (NPCs, locations). The matched value is the name itself. */
    public static List<Match<String>> findFirstPerString(String text, Collection<String> names)
    {
        return findFirstPer(text, names, n -> Collections.singletonList(n));
    }

    public static <T> List<Match<T>> findFirstPer(
        String text,
        Collection<T> entries,
        Function<T, List<String>> aliasesOf)
    {
        if (text == null || text.isEmpty() || entries == null || entries.isEmpty())
        {
            return Collections.emptyList();
        }

        List<Candidate<T>> candidates = new ArrayList<>();
        for (T e : entries)
        {
            if (e == null) continue;
            for (String a : aliasesOf.apply(e)) addCandidate(candidates, a, e);
        }
        candidates.sort((a, b) -> b.text.length() - a.text.length());

        String lower = normalizePunctuation(text).toLowerCase();
        boolean[] consumed = new boolean[text.length()];
        List<T> linked = new ArrayList<>();
        List<Match<T>> matches = new ArrayList<>();

        for (Candidate<T> c : candidates)
        {
            if (linked.contains(c.entry)) continue;
            int idx = findWordBoundary(lower, text, c.lower, 0, consumed);
            if (idx < 0) continue;
            int end = idx + c.lower.length();
            for (int i = idx; i < end; i++) consumed[i] = true;
            linked.add(c.entry);
            matches.add(new Match<>(idx, end, c.entry));
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

    /**
     * Map smart/curly quotes to their ASCII equivalents so display names like
     * "Heroes' Quest" match prose containing the typographic "Heroes’ Quest".
     * Replacement is char-for-char so match indices stay aligned with the
     * original text.
     */
    private static String normalizePunctuation(String s)
    {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '‘': case '’': case '‚': case '‛':
                case '′': case 'ʼ':
                    sb.append('\''); break;
                case '“': case '”': case '„': case '‟':
                case '″':
                    sb.append('"'); break;
                case '–': case '—': case '−':
                    sb.append('-'); break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isWordChar(char c)
    {
        return Character.isLetterOrDigit(c);
    }

    private static <T> void addCandidate(List<Candidate<T>> out, String text, T entry)
    {
        if (text == null || text.length() < 3) return;
        out.add(new Candidate<>(text, entry));
    }

    private static final class Candidate<T>
    {
        final String text;
        final String lower;
        final T entry;

        Candidate(String text, T entry)
        {
            this.text = text;
            this.lower = normalizePunctuation(text).toLowerCase();
            this.entry = entry;
        }
    }
}
