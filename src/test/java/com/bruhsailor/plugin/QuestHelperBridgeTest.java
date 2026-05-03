package com.bruhsailor.plugin;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuestHelperBridgeTest
{
    @Test
    public void noQHInstalledReportsNotInstalled()
    {
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugins()).thenReturn(Collections.emptyList());
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.isInstalled());
    }

    @Test
    public void openWithoutQHReturnsFalse()
    {
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugins()).thenReturn(Collections.emptyList());
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.open(new QuestEntry("COOKS_ASSISTANT", "Cook's Assistant")));
    }

    @Test
    public void unrelatedPluginsDoNotCountAsQH()
    {
        PluginManager pm = mock(PluginManager.class);
        Plugin other = mock(Plugin.class, "OtherPlugin");
        // Mockito-mocked Plugin's class is a generated subclass; its name will
        // not match "QuestHelperPlugin", so isInstalled() must stay false.
        when(pm.getPlugins()).thenReturn(Arrays.asList(other));
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.isInstalled());
    }

    @Test
    public void nullArgsAreSafe()
    {
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugins()).thenReturn(Collections.emptyList());
        QuestHelperBridge bridge = new QuestHelperBridge(pm);
        assertFalse(bridge.open(null));
    }
}
