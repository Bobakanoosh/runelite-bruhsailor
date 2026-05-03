package com.bruhsailor.plugin;

import org.junit.Test;
import static org.junit.Assert.*;

public class StepIdTest
{
    @Test
    public void parseAndFormatRoundtrip()
    {
        StepId id = StepId.parse("1.2.3");
        assertEquals(1, id.chapter());
        assertEquals(2, id.section());
        assertEquals(3, id.step());
        assertEquals("1.2.3", id.toString());
    }

    @Test
    public void orderingAcrossBoundaries()
    {
        assertTrue(StepId.parse("1.9.99").compareTo(StepId.parse("2.1.1")) < 0);
        assertTrue(StepId.parse("1.1.2").compareTo(StepId.parse("1.1.10")) < 0);
        assertEquals(0, StepId.parse("1.1.1").compareTo(StepId.parse("1.1.1")));
    }

    @Test
    public void equalsAndHashCode()
    {
        assertEquals(StepId.parse("1.1.1"), StepId.parse("1.1.1"));
        assertEquals(StepId.parse("1.1.1").hashCode(), StepId.parse("1.1.1").hashCode());
        assertNotEquals(StepId.parse("1.1.1"), StepId.parse("1.1.2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedTwoParts()
    {
        StepId.parse("1.2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonNumeric()
    {
        StepId.parse("1.a.3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositive()
    {
        StepId.parse("0.1.1");
    }
}
