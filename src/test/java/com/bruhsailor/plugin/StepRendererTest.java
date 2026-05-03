package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.ContentFragment;
import com.bruhsailor.plugin.model.Step;
import org.junit.Test;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class StepRendererTest
{
    private Step stepWith(List<ContentFragment> content)
    {
        Step s = new Step();
        s.id = StepId.of(1, 1, 1);
        s.content = content;
        s.nestedContent = new ArrayList<>();
        return s;
    }

    private ContentFragment frag(String text, Boolean bold, Double size, Double r, Double g, Double b)
    {
        ContentFragment f = new ContentFragment();
        f.text = text;
        f.formatting = new ContentFragment.Formatting();
        f.formatting.bold = bold;
        f.formatting.fontSize = size;
        if (r != null)
        {
            f.formatting.color = new ContentFragment.Color();
            f.formatting.color.r = r;
            f.formatting.color.g = g;
            f.formatting.color.b = b;
        }
        return f;
    }

    @Test
    public void emitsTextInOrder()
    {
        List<ContentFragment> content = new ArrayList<>();
        content.add(frag("Hello ", null, null, null, null, null));
        content.add(frag("world", null, null, null, null, null));
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        assertTrue(pane.getText().contains("Hello world"));
    }

    @Test
    public void boldFragmentHasBoldAttribute()
    {
        List<ContentFragment> content = new ArrayList<>();
        content.add(frag("plain ", null, null, null, null, null));
        content.add(frag("bold", true, null, null, null, null));
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        StyledDocument doc = pane.getStyledDocument();

        Element boldElem = doc.getCharacterElement("plain ".length());
        AttributeSet attrs = boldElem.getAttributes();
        assertTrue(StyleConstants.isBold(attrs));
    }

    @Test
    public void coloredFragmentPreservesExactColor()
    {
        List<ContentFragment> content = new ArrayList<>();
        content.add(frag("red", null, null, 1.0, 0.0, 0.0));
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        StyledDocument doc = pane.getStyledDocument();

        AttributeSet attrs = doc.getCharacterElement(0).getAttributes();
        Color c = StyleConstants.getForeground(attrs);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    public void nestedContentAppearsAfterMainContent()
    {
        Step s = new Step();
        s.id = StepId.of(1, 1, 1);
        s.content = new ArrayList<>();
        s.content.add(frag("outer", null, null, null, null, null));
        s.nestedContent = new ArrayList<>();
        Step.NestedBlock block = new Step.NestedBlock();
        block.level = 1;
        block.content = new ArrayList<>();
        block.content.add(frag("inner", null, null, null, null, null));
        s.nestedContent.add(block);

        JTextPane pane = (JTextPane) StepRenderer.render(s);
        String all = pane.getText();
        assertTrue(all.contains("outer"));
        assertTrue(all.contains("inner"));
        assertTrue(all.indexOf("outer") < all.indexOf("inner"));
    }

    @Test
    public void fragmentWithNullFormattingRendersAsPlain()
    {
        ContentFragment f = new ContentFragment();
        f.text = "no formatting object";
        f.formatting = null;
        List<ContentFragment> content = new ArrayList<>();
        content.add(f);
        JTextPane pane = (JTextPane) StepRenderer.render(stepWith(content));
        assertTrue(pane.getText().contains("no formatting object"));
    }
}
