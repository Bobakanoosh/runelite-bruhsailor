package com.bruhsailor.plugin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
    name = "BRUHsailer Guide",
    description = "BRUHsailer ironman guide in a side panel",
    tags = {"ironman", "guide", "bruhsailer"}
)
public class BruhsailorPlugin extends Plugin
{
    @Inject private ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    @Inject private EventBus eventBus;
    @Inject private PluginManager pluginManager;

    private NavigationButton navButton;
    private BruhsailorPanel panel;

    @Override
    protected void startUp() throws Exception
    {
        GuideRepository repo;
        try
        {
            repo = GuideRepository.loadBundled();
        }
        catch (RuntimeException e)
        {
            log.error("Failed to load BRUHsailer guide data", e);
            installErrorPanel("Failed to load BRUHsailer guide data");
            return;
        }

        StepMappings mappings;
        try
        {
            mappings = StepMappings.loadBundled();
        }
        catch (RuntimeException e)
        {
            log.error("Failed to load step_mappings.json; chips disabled", e);
            mappings = StepMappings.empty();
        }
        QuestRegistry questRegistry = QuestRegistry.create(pluginManager);
        QuestHelperBridge questBridge = new QuestHelperBridge(pluginManager);

        GuideStateService state = new GuideStateService(repo, configManager, eventBus);
        panel = new BruhsailorPanel(repo, state, eventBus, mappings, questRegistry, questBridge);

        BufferedImage icon = loadIcon();
        navButton = NavigationButton.builder()
            .tooltip("BRUHsailer Guide")
            .priority(7)
            .icon(icon)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        if (panel != null)
        {
            panel.unregister();
            panel = null;
        }
    }

    private BufferedImage loadIcon()
    {
        try
        {
            return ImageUtil.loadImageResource(getClass(), "/icon.png");
        }
        catch (Exception e)
        {
            log.warn("BRUHsailer icon missing; using blank placeholder", e);
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private void installErrorPanel(String message)
    {
        net.runelite.client.ui.PluginPanel error = new net.runelite.client.ui.PluginPanel(false) {};
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        error.add(label);
        BufferedImage icon = loadIcon();
        navButton = NavigationButton.builder()
            .tooltip("BRUHsailer Guide (error)")
            .priority(7)
            .icon(icon)
            .panel(error)
            .build();
        clientToolbar.addNavigation(navButton);
    }
}
