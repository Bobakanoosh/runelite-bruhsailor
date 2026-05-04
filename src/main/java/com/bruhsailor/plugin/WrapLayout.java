package com.bruhsailor.plugin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * FlowLayout that actually wraps when it runs out of horizontal room.
 * Stock FlowLayout reports its preferred size as a single row, which means
 * a parent (or a JScrollPane) sized to that preferred width never has to
 * wrap; this subclass computes the preferred height for the actual width
 * being given to the layout, so the row breaks correctly.
 *
 * Pattern adapted from the well-known FlowLayout-wrapping recipe.
 */
final class WrapLayout extends FlowLayout
{
    WrapLayout(int align, int hgap, int vgap)
    {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target)
    {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target)
    {
        Dimension min = layoutSize(target, false);
        min.width -= getHgap() + 1;
        return min;
    }

    private Dimension layoutSize(Container target, boolean preferred)
    {
        synchronized (target.getTreeLock())
        {
            int targetWidth = target.getWidth();
            if (targetWidth == 0)
            {
                Container scrollAncestor = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
                targetWidth = (scrollAncestor != null) ? scrollAncestor.getWidth() : Integer.MAX_VALUE;
            }
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + hgap * 2;
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            for (int i = 0, n = target.getComponentCount(); i < n; i++)
            {
                Component c = target.getComponent(i);
                if (!c.isVisible()) continue;
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                if (rowWidth + d.width > maxWidth)
                {
                    addRow(dim, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) rowWidth += hgap;
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;

            // When inside a JScrollPane viewport, align width with the viewport
            // so the layout uses the visible width on the next pass.
            Container scrollAncestor = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollAncestor != null && target.isValid())
            {
                dim.width -= hgap + 1;
            }

            return dim;
        }
    }

    private void addRow(Dimension dim, int rowWidth, int rowHeight)
    {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) dim.height += getVgap();
        dim.height += rowHeight;
    }
}
