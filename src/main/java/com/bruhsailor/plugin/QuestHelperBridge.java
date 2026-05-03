package com.bruhsailor.plugin;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;

public final class QuestHelperBridge
{
    private static final Logger log = LoggerFactory.getLogger(QuestHelperBridge.class);
    private static final String QH_PLUGIN_CLASS = "com.questhelper.QuestHelperPlugin";
    private static final String QH_QUEST_ENUM = "com.questhelper.questhelpers.QuestHelperQuest";

    private final PluginManager pluginManager;
    private volatile boolean reflectionDisabled;
    private volatile Boolean cachedInstalled;

    public QuestHelperBridge(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public boolean isInstalled()
    {
        Boolean cached = cachedInstalled;
        if (cached != null) return cached;
        boolean found = locateQHPlugin() != null;
        cachedInstalled = found;
        return found;
    }

    public boolean open(QuestEntry quest)
    {
        if (quest == null) return false;
        if (reflectionDisabled) return false;
        Plugin qh = locateQHPlugin();
        if (qh == null) return false;
        try
        {
            return invokeDisplayPanel(qh, quest);
        }
        catch (Throwable t)
        {
            log.warn("Quest Helper open failed for {}; disabling reflection", quest.enumName(), t);
            reflectionDisabled = true;
            return false;
        }
    }

    private Plugin locateQHPlugin()
    {
        if (pluginManager == null) return null;
        Collection<Plugin> plugins;
        try
        {
            plugins = pluginManager.getPlugins();
        }
        catch (Throwable t)
        {
            log.debug("PluginManager.getPlugins threw", t);
            return null;
        }
        if (plugins == null) return null;
        for (Plugin p : plugins)
        {
            if (p == null) continue;
            String cls = p.getClass().getName();
            if (cls.equals(QH_PLUGIN_CLASS) || cls.endsWith(".QuestHelperPlugin"))
            {
                return p;
            }
        }
        return null;
    }

    private boolean invokeDisplayPanel(Plugin qh, QuestEntry quest) throws ReflectiveOperationException
    {
        ClassLoader cl = qh.getClass().getClassLoader();

        // Preferred: displayPanel(QuestHelperQuest)
        Class<?> enumClass = null;
        try
        {
            enumClass = cl.loadClass(QH_QUEST_ENUM);
        }
        catch (ClassNotFoundException ignored)
        {
            // fall through to String overload
        }

        if (enumClass != null)
        {
            Method m = findMethod(qh.getClass(), "displayPanel", enumClass);
            if (m != null)
            {
                Object enumValue = enumValueOf(enumClass, quest.enumName());
                if (enumValue != null)
                {
                    m.invoke(qh, enumValue);
                    return true;
                }
            }
        }

        // Fallback: displayPanel(String) — accepts the display name.
        Method strMethod = findMethod(qh.getClass(), "displayPanel", String.class);
        if (strMethod != null)
        {
            strMethod.invoke(qh, quest.displayName());
            return true;
        }

        // Last resort: a no-arg displayPanel() that just brings the panel up.
        Method noArg = findMethod(qh.getClass(), "displayPanel");
        if (noArg != null)
        {
            noArg.invoke(qh);
            return true;
        }

        log.warn("Quest Helper has no recognised displayPanel method; reflection disabled.");
        reflectionDisabled = true;
        return false;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... paramTypes)
    {
        try
        {
            return cls.getMethod(name, paramTypes);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private static Object enumValueOf(Class<?> enumClass, String name)
    {
        try
        {
            for (Object c : enumClass.getEnumConstants())
            {
                if (((Enum<?>) c).name().equals(name)) return c;
            }
        }
        catch (Throwable t)
        {
            // ignore — return null
        }
        return null;
    }
}
