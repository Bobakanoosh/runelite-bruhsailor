package com.bruhsailor.plugin;

import java.util.Objects;

public final class QuestEntry
{
    private final String enumName;
    private final String displayName;

    public QuestEntry(String enumName, String displayName)
    {
        this.enumName = Objects.requireNonNull(enumName, "enumName");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    public String enumName() { return enumName; }
    public String displayName() { return displayName; }

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
