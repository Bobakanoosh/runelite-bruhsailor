package com.bruhsailor.plugin;

import net.runelite.client.util.LinkBrowser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Opens an OSRS wiki article for an NPC or location in the user's preferred browser.
 * Wiki page titles use underscores for spaces and percent-encode other reserved chars.
 */
public final class WikiLink
{
    private static final String BASE = "https://oldschool.runescape.wiki/w/";

    private WikiLink() {}

    public static String urlFor(String pageTitle)
    {
        String slug = pageTitle.trim().replace(' ', '_');
        StringBuilder out = new StringBuilder(BASE.length() + slug.length() + 8);
        out.append(BASE);
        for (int i = 0; i < slug.length(); i++)
        {
            char c = slug.charAt(i);
            if (c == '_' || c == '/' || c == ':' || c == '-' || c == '.' || c == '\''
                || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
            {
                out.append(c);
            }
            else
            {
                out.append(URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8));
            }
        }
        return out.toString();
    }

    public static void open(String pageTitle)
    {
        LinkBrowser.browse(urlFor(pageTitle));
    }
}
