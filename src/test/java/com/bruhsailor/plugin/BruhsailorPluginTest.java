package com.bruhsailor.plugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BruhsailorPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(BruhsailorPlugin.class);
        RuneLite.main(args);
    }
}
