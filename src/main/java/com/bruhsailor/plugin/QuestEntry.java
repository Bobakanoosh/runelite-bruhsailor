package com.bruhsailor.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class QuestEntry
{
    private final String enumName;
    private final String displayName;
    private final List<String> aliases;

    public QuestEntry(String enumName, String displayName)
    {
        this(enumName, displayName, Collections.emptyList());
    }

    public QuestEntry(String enumName, String displayName, List<String> aliases)
    {
        this.enumName = Objects.requireNonNull(enumName, "enumName");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.aliases = (aliases == null) ? Collections.emptyList() : Collections.unmodifiableList(aliases);
    }

    public String enumName() { return enumName; }
    public String displayName() { return displayName; }
    public List<String> aliases() { return aliases; }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof QuestEntry)) return false;
        QuestEntry other = (QuestEntry) o;
        return enumName.equals(other.enumName) && displayName.equals(other.displayName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(enumName, displayName);
    }

    @Override
    public String toString()
    {
        return "QuestEntry{" + enumName + " -> " + displayName + "}";
    }
}
