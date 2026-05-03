package com.bruhsailor.plugin;

import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QuestChipTest
{
    private final QuestEntry cooks = new QuestEntry("COOKS_ASSISTANT", "Cook's Assistant");

    @Test
    public void labelTextEndsWithArrow()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);

        JComponent chip = QuestChip.create(cooks, bridge);
        JLabel label = (JLabel) chip;
        assertTrue(label.getText().contains("Cook's Assistant"));
        assertTrue(label.getText().contains("↗"));
    }

    @Test
    public void installedChipUsesHandCursor()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);

        JComponent chip = QuestChip.create(cooks, bridge);
        assertEquals(Cursor.HAND_CURSOR, chip.getCursor().getType());
        assertTrue(chip.getToolTipText().toLowerCase().contains("open"));
    }

    @Test
    public void uninstalledChipUsesDefaultCursorAndExplainTooltip()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(false);

        JComponent chip = QuestChip.create(cooks, bridge);
        assertEquals(Cursor.DEFAULT_CURSOR, chip.getCursor().getType());
        assertTrue(chip.getToolTipText().toLowerCase().contains("not installed"));
    }

    @Test
    public void clickOnInstalledChipInvokesBridgeOpen()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);
        when(bridge.open(cooks)).thenReturn(true);

        JComponent chip = QuestChip.create(cooks, bridge);
        for (java.awt.event.MouseListener ml : chip.getMouseListeners())
        {
            ml.mouseClicked(new MouseEvent(chip, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        }
        verify(bridge).open(cooks);
    }

    @Test
    public void clickOnUninstalledChipDoesNotInvokeBridgeOpen()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(false);

        JComponent chip = QuestChip.create(cooks, bridge);
        for (java.awt.event.MouseListener ml : chip.getMouseListeners())
        {
            ml.mouseClicked(new MouseEvent(chip, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        }
        verify(bridge, never()).open(any());
    }

    @Test
    public void failedOpenUpdatesTooltip()
    {
        QuestHelperBridge bridge = mock(QuestHelperBridge.class);
        when(bridge.isInstalled()).thenReturn(true);
        when(bridge.open(cooks)).thenReturn(false);

        JComponent chip = QuestChip.create(cooks, bridge);
        for (java.awt.event.MouseListener ml : chip.getMouseListeners())
        {
            ml.mouseClicked(new MouseEvent(chip, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        }
        assertTrue(chip.getToolTipText().toLowerCase().contains("couldn"));
    }
}
