package com.bruhsailor.plugin.model;

import java.util.List;

public class StepMapping
{
    public String contentHash;
    public String title;
    public List<String> questIds;
    public List<Item> items;
    public List<String> abstractItems;
    public List<String> unresolvedItems;
    public boolean verified;
    public double verifierConfidence;
    public List<String> verifierFlags;
    public String verifierNotes;
}
