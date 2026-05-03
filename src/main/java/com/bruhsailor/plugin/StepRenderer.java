package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.ContentFragment;
import com.bruhsailor.plugin.model.Step;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

public final class StepRenderer
{
    private static final Logger log = LoggerFactory.getLogger(StepRenderer.class);

    private static final int BASE_SIZE = 18;
    private static final int SMALL_SIZE = 15;
    private static final int LARGE_SIZE = 20;
    private static final float NESTED_INDENT = 16f;

    private StepRenderer() {}

    public static JComponent render(Step step)
    {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        Font base = FontManager.getRunescapeFont();
        if (base != null)
        {
            pane.setFont(base);
        }

        StyledDocument doc = pane.getStyledDocument();

        if (step.content != null)
        {
            for (ContentFragment f : step.content)
            {
                appendFragment(doc, f, 0f);
            }
        }

        if (step.nestedContent != null)
        {
            for (Step.NestedBlock block : step.nestedContent)
            {
                if (block == null || block.content == null) continue;
                ensureParagraphBreak(doc);
                float indent = NESTED_INDENT * Math.max(1, block.level);
                for (ContentFragment f : block.content)
                {
                    appendFragment(doc, f, indent);
                }
            }
        }

        pane.setCaretPosition(0);
        return pane;
    }

    private static void ensureParagraphBreak(StyledDocument doc)
    {
        try
        {
            doc.insertString(doc.getLength(), "\n", null);
        }
        catch (BadLocationException e)
        {
            // unreachable: appending at end
        }
    }

    private static void appendFragment(StyledDocument doc, ContentFragment frag, float indent)
    {
        if (frag == null || frag.text == null) return;
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        try
        {
            applyFormatting(attrs, frag, indent);
        }
        catch (RuntimeException e)
        {
            log.warn("Bad formatting on fragment, falling back to plain", e);
            attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, ColorScheme.LIGHT_GRAY_COLOR);
        }
        try
        {
            doc.insertString(doc.getLength(), frag.text, attrs);
        }
        catch (BadLocationException e)
        {
            // unreachable: appending at end
        }
    }

    private static void applyFormatting(SimpleAttributeSet attrs, ContentFragment frag, float indent)
    {
        StyleConstants.setForeground(attrs, ColorScheme.LIGHT_GRAY_COLOR);
        StyleConstants.setFontSize(attrs, BASE_SIZE);
        if (indent > 0f)
        {
            StyleConstants.setLeftIndent(attrs, indent);
        }

        ContentFragment.Formatting f = frag.formatting;
        if (f == null) return;

        if (Boolean.TRUE.equals(f.bold))
        {
            StyleConstants.setBold(attrs, true);
        }

        if (f.fontSize != null)
        {
            double s = f.fontSize;
            if (s <= 11.0) StyleConstants.setFontSize(attrs, SMALL_SIZE);
            else if (s >= 14.0) StyleConstants.setFontSize(attrs, LARGE_SIZE);
            else StyleConstants.setFontSize(attrs, BASE_SIZE);
        }

        if (f.color != null)
        {
            float rr = clamp01((float) f.color.r);
            float gg = clamp01((float) f.color.g);
            float bb = clamp01((float) f.color.b);
            // Source guide is authored on a light background; near-black text is
            // unreadable on our dark theme, so swap it out for the default.
            if (Math.max(rr, Math.max(gg, bb)) < 0.25f)
            {
                StyleConstants.setForeground(attrs, ColorScheme.LIGHT_GRAY_COLOR);
            }
            else
            {
                StyleConstants.setForeground(attrs, new Color(rr, gg, bb));
            }
        }
    }

    private static float clamp01(float v)
    {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
