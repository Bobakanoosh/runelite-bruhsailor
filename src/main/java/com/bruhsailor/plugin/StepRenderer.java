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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntPredicate;

public final class StepRenderer
{
    private static final Logger log = LoggerFactory.getLogger(StepRenderer.class);

    private static final int BASE_SIZE = 18;
    private static final int SMALL_SIZE = 15;
    private static final int LARGE_SIZE = 20;
    private static final float NESTED_INDENT = 12f;
    private static final float BULLET_HANG = 10f;

    // Abbreviations whose trailing period should NOT trigger a sentence split.
    // Mirrors the upstream JS heuristic in BRUHsailer's guideDataLoader.js.
    private static final Set<String> ABBREVIATIONS = new HashSet<>(Arrays.asList(
        "e.g", "i.e", "etc", "vs", "mr", "mrs", "ms", "dr", "st", "no",
        "approx", "ca", "cf", "ft"
    ));

    private StepRenderer() {}

    /** Hit region for one bullet's checkbox glyph, used by the panel for click routing. */
    public static final class BulletHit
    {
        public final int idx;
        public final int glyphStart;
        public final int glyphEnd;

        BulletHit(int idx, int glyphStart, int glyphEnd)
        {
            this.idx = idx;
            this.glyphStart = glyphStart;
            this.glyphEnd = glyphEnd;
        }
    }

    public static final class Rendered
    {
        public final JComponent component;
        public final List<BulletHit> bullets;

        Rendered(JComponent component, List<BulletHit> bullets)
        {
            this.component = component;
            this.bullets = bullets;
        }
    }

    /** Backwards-compat: renders without checkbox state. No bullets ever appear "checked". */
    public static JComponent render(Step step)
    {
        return render(step, idx -> false).component;
    }

    public static Rendered render(Step step, IntPredicate isChecked)
    {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        // JTextPane has a default ~3px margin on every side; reclaim it for prose width.
        pane.setMargin(new java.awt.Insets(0, 0, 0, 0));
        Font base = FontManager.getRunescapeFont();
        if (base != null)
        {
            pane.setFont(base);
        }

        StyledDocument doc = pane.getStyledDocument();
        BulletCounter counter = new BulletCounter();

        if (step.content != null)
        {
            renderContentArray(doc, step.content, 0f, isChecked, counter);
        }

        if (step.nestedContent != null)
        {
            for (Step.NestedBlock block : step.nestedContent)
            {
                if (block == null || block.content == null) continue;
                ensureParagraphBreak(doc);
                float indent = NESTED_INDENT * Math.max(1, block.level);
                renderContentArray(doc, block.content, indent, isChecked, counter);
            }
        }

        pane.setCaretPosition(0);
        return new Rendered(pane, counter.bullets);
    }

    private static final class BulletCounter
    {
        int next = 0;
        final List<BulletHit> bullets = new ArrayList<>();
    }

    /**
     * Render a content array as either plain inline fragments (single
     * sentence) or as a per-sentence checkbox-bulleted list (multiple sentences).
     */
    private static void renderContentArray(StyledDocument doc, List<ContentFragment> content,
                                           float indent, IntPredicate isChecked, BulletCounter counter)
    {
        List<List<Piece>> groups = splitContentBySentences(content);
        if (groups.size() <= 1)
        {
            for (ContentFragment f : content)
            {
                appendFragment(doc, f, indent);
            }
            return;
        }
        for (List<Piece> group : groups)
        {
            int idx = counter.next++;
            BulletHit hit = appendBulletParagraph(doc, group, indent, isChecked.test(idx));
            counter.bullets.add(new BulletHit(idx, hit.glyphStart, hit.glyphEnd));
        }
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

    /**
     * Append one sentence as a checkbox-prefixed paragraph with hanging indent.
     * Returns the [glyphStart, glyphEnd) range of the checkbox glyph so the
     * panel can route clicks to it.
     */
    private static BulletHit appendBulletParagraph(StyledDocument doc, List<Piece> pieces,
                                                   float indent, boolean checked)
    {
        // Ensure we start on a fresh paragraph.
        int existing = doc.getLength();
        if (existing > 0)
        {
            try
            {
                String last = doc.getText(existing - 1, 1);
                if (!"\n".equals(last))
                {
                    doc.insertString(existing, "\n", null);
                }
            }
            catch (BadLocationException ignored) {}
        }

        int paraStart = doc.getLength();

        SimpleAttributeSet glyphAttrs = new SimpleAttributeSet();
        StyleConstants.setForeground(glyphAttrs, checked ? CHECKBOX_DONE_FG : ColorScheme.LIGHT_GRAY_COLOR);
        StyleConstants.setFontSize(glyphAttrs, BASE_SIZE);
        String glyph = checked ? "☑ " : "☐ ";
        int glyphStart = doc.getLength();
        try
        {
            doc.insertString(glyphStart, glyph, glyphAttrs);
        }
        catch (BadLocationException ignored) {}
        int glyphEnd = doc.getLength();

        for (Piece p : pieces)
        {
            if (p.text.isEmpty()) continue;
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            try
            {
                applyFormatting(attrs, p.source, 0f); // paragraph indent applied below
            }
            catch (RuntimeException e)
            {
                attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, ColorScheme.LIGHT_GRAY_COLOR);
            }
            if (checked)
            {
                StyleConstants.setStrikeThrough(attrs, true);
                StyleConstants.setForeground(attrs, CHECKBOX_DONE_FG);
            }
            try
            {
                doc.insertString(doc.getLength(), p.text, attrs);
            }
            catch (BadLocationException ignored) {}
        }

        int paraEnd = doc.getLength();

        SimpleAttributeSet paraAttrs = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(paraAttrs, indent + BULLET_HANG);
        StyleConstants.setFirstLineIndent(paraAttrs, -BULLET_HANG);
        StyleConstants.setSpaceBelow(paraAttrs, 4f);
        doc.setParagraphAttributes(paraStart, Math.max(1, paraEnd - paraStart), paraAttrs, false);

        return new BulletHit(-1, glyphStart, glyphEnd);
    }

    private static final Color CHECKBOX_DONE_FG = new Color(0x9AA39A);

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

    /** A piece of content carved out of a fragment by the sentence splitter. */
    static final class Piece
    {
        final ContentFragment source;
        final String text;

        Piece(ContentFragment source, String text)
        {
            this.source = source;
            this.text = text;
        }
    }

    /**
     * Split a content array into per-sentence groups. Preserves each fragment's
     * formatting; never splits inside a fragment except at sentence boundaries.
     * Ports the upstream BRUHsailer JS heuristic. Returns one group per sentence;
     * empty/whitespace-only groups are dropped.
     */
    static List<List<Piece>> splitContentBySentences(List<ContentFragment> content)
    {
        if (content == null || content.isEmpty())
        {
            return new ArrayList<>();
        }

        StringBuilder flatBuf = new StringBuilder();
        List<int[]> ranges = new ArrayList<>();
        List<ContentFragment> items = new ArrayList<>();
        for (ContentFragment f : content)
        {
            if (f == null || f.text == null || f.text.isEmpty()) continue;
            int start = flatBuf.length();
            flatBuf.append(f.text);
            ranges.add(new int[]{start, flatBuf.length()});
            items.add(f);
        }
        String flat = flatBuf.toString();
        if (flat.isEmpty())
        {
            return new ArrayList<>();
        }

        TreeSet<Integer> boundarySet = findSentenceBoundaries(flat);

        List<List<Piece>> groups = new ArrayList<>();
        groups.add(new ArrayList<>());

        for (int s = 0; s < items.size(); s++)
        {
            int segStart = ranges.get(s)[0];
            int segEnd = ranges.get(s)[1];
            ContentFragment item = items.get(s);
            int cursor = segStart;
            for (int b : boundarySet)
            {
                if (b <= segStart) continue;
                if (b >= segEnd) break;
                pushPiece(groups, item, flat.substring(cursor, b));
                closeGroup(groups);
                cursor = b;
            }
            pushPiece(groups, item, flat.substring(cursor, segEnd));
            if (boundarySet.contains(segEnd)) closeGroup(groups);
        }

        // Trim leading whitespace on first piece and trailing on last piece per group.
        // Drop any pieces that become empty and any groups that end up empty.
        List<List<Piece>> cleaned = new ArrayList<>();
        for (List<Piece> g : groups)
        {
            if (g.isEmpty()) continue;
            List<Piece> trimmed = new ArrayList<>(g.size());
            for (int i = 0; i < g.size(); i++)
            {
                Piece p = g.get(i);
                String t = p.text;
                if (i == 0) t = t.replaceAll("^\\s+", "");
                if (i == g.size() - 1) t = t.replaceAll("\\s+$", "");
                if (!t.isEmpty()) trimmed.add(new Piece(p.source, t));
            }
            boolean hasContent = false;
            for (Piece p : trimmed) { if (!p.text.trim().isEmpty()) { hasContent = true; break; } }
            if (hasContent) cleaned.add(trimmed);
        }
        return cleaned;
    }

    private static TreeSet<Integer> findSentenceBoundaries(String flat)
    {
        TreeSet<Integer> boundarySet = new TreeSet<>();
        int n = flat.length();
        int i = 0;
        while (i < n)
        {
            char ch = flat.charAt(i);

            if (ch == '\n')
            {
                boundarySet.add(i + 1);
                i++;
                continue;
            }

            if (ch != '.' && ch != '!' && ch != '?')
            {
                i++;
                continue;
            }

            int j = i;
            while (j + 1 < n && isPunct(flat.charAt(j + 1))) j++;
            if (j + 1 >= n) break;
            if (!Character.isWhitespace(flat.charAt(j + 1)))
            {
                i = j + 1;
                continue;
            }

            int k = j + 1;
            while (k < n && Character.isWhitespace(flat.charAt(k))) k++;
            if (k >= n) break;

            if (ch == '.' && isAbbreviationBefore(flat, i))
            {
                i = j + 1;
                continue;
            }

            boundarySet.add(k);
            i = k;
        }
        return boundarySet;
    }

    private static boolean isPunct(char c)
    {
        return c == '.' || c == '!' || c == '?';
    }

    private static boolean isAbbreviationBefore(String text, int periodIdx)
    {
        int start = periodIdx - 1;
        while (start >= 0)
        {
            char c = text.charAt(start);
            if (Character.isLetter(c) || c == '.') start--;
            else break;
        }
        String token = text.substring(start + 1, periodIdx).toLowerCase();
        return ABBREVIATIONS.contains(token);
    }

    private static void pushPiece(List<List<Piece>> groups, ContentFragment source, String text)
    {
        if (text.isEmpty()) return;
        groups.get(groups.size() - 1).add(new Piece(source, text));
    }

    private static void closeGroup(List<List<Piece>> groups)
    {
        if (!groups.get(groups.size() - 1).isEmpty())
        {
            groups.add(new ArrayList<>());
        }
    }
}
