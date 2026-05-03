package com.bruhsailor.plugin;

import lombok.Value;

@Value
public class GuideStateChanged
{
    StepId previousCurrent;
    StepId newCurrent;
    boolean completionChanged;
}
