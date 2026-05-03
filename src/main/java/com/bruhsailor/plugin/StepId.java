package com.bruhsailor.plugin;

import java.util.Objects;

public final class StepId implements Comparable<StepId>
{
    private final int chapter;
    private final int section;
    private final int step;

    private StepId(int chapter, int section, int step)
    {
        this.chapter = chapter;
        this.section = section;
        this.step = step;
    }

    public static StepId of(int chapter, int section, int step)
    {
        if (chapter < 1 || section < 1 || step < 1)
        {
            throw new IllegalArgumentException("StepId components must be >= 1");
        }
        return new StepId(chapter, section, step);
    }

    public static StepId parse(String s)
    {
        String[] parts = s.split("\\.");
        if (parts.length != 3)
        {
            throw new IllegalArgumentException("Expected chapter.section.step, got: " + s);
        }
        try
        {
            return of(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Non-numeric component in: " + s, e);
        }
    }

    public int chapter() { return chapter; }
    public int section() { return section; }
    public int step() { return step; }

    @Override
    public int compareTo(StepId o)
    {
        int c = Integer.compare(chapter, o.chapter);
        if (c != 0) return c;
        c = Integer.compare(section, o.section);
        if (c != 0) return c;
        return Integer.compare(step, o.step);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof StepId)) return false;
        StepId other = (StepId) o;
        return chapter == other.chapter && section == other.section && step == other.step;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(chapter, section, step);
    }

    @Override
    public String toString()
    {
        return chapter + "." + section + "." + step;
    }
}
