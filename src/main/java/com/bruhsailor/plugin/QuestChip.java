package com.bruhsailor.plugin;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class QuestChip
{
    private static final String ARROW = "↗";
    // Quest-Helper-blue accent so the chips read as "quest" affordances.
    private static final Color QUEST_BLUE = new Color(0x4FB3FF);
    private static final Color QUEST_BLUE_DIM = new Color(0x2A6280);

    private QuestChip() {}

    public static JComponent create(QuestEntry quest, QuestHelperBridge bridge)
    {
        boolean installed = bridge.isInstalled();
        JLabel label = new JLabel("<html><span style='color:#4FB3FF;'>?</span> "
            + escape(quest.displayName()) + " " + ARROW + "</html>");
        label.setOpaque(true);
        label.setFont(FontManager.getRunescapeFont().deriveFont(14f));
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
        Border line = BorderFactory.createLineBorder(installed ? QUEST_BLUE : QUEST_BLUE_DIM, 1);
        Border pad = BorderFactory.createEmptyBorder(4, 8, 4, 8);
        label.setBorder(BorderFactory.createCompoundBorder(line, pad));

        if (installed)
        {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setToolTipText("Open in Quest Helper");
        }
        else
        {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            label.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
            label.setToolTipText("Quest Helper plugin not installed");
        }

        label.addMouseListener(new MouseAdapter()
        {
            private boolean disabled;

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (disabled) return;
                if (!bridge.isInstalled()) return;
                boolean ok = bridge.open(quest);
                if (!ok)
                {
                    label.setToolTipText("Couldn't open Quest Helper - see logs");
                    disabled = true;
                }
            }
        });

        return label;
    }

    private static String escape(String s)
    {
        return s.replace("&", "&amp;").replace("<", "&lt;");
    }
}
