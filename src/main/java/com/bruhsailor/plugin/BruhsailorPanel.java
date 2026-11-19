package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.Step;
import com.bruhsailor.plugin.model.StepMapping;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class BruhsailorPanel extends PluginPanel
{
    private static final Color DONE_BG = new Color(0x3F7A3F);
    private static final Color DONE_FG = new Color(0xE6FFE6);
    private static final Color QUEST_LINK_BLUE = new Color(0x4FB3FF);
    private static final Color NPC_LINK_COLOR = new Color(0xFFB347);
    private static final Color LOCATION_LINK_COLOR = new Color(0x9CE37D);

    private final GuideRepository repo;
    private final GuideStateService state;
    private final EventBus bus;
    private final StepMappings stepMappings;
    private final QuestRegistry questRegistry;

    private final JLabel chapterLabel = new JLabel();
    private final JLabel sectionLabel = new JLabel();
    private final JPanel currentStepHolder = new JPanel();
    private final JLabel metadataLabel = new JLabel();
    private final JButton prevButton = new JButton("◀");
    private final JButton nextButton = new JButton("▶");
    private final JToggleButton completeToggle = new JToggleButton("Done");

    private final DefaultListModel<ListRow> listModel = new DefaultListModel<>();
    private final JList<ListRow> stepList = new JList<>(listModel);

    private JScrollPane stepScroll;
    private int renderedWidth = -1;
    private StepId renderedStepId;
    private long refreshVersion;
    private Point pendingScrollPosition;

    private StepRenderer.Rendered displayedRender;
    private JTextPane displayedPane;
    private Step displayedStep;

    public BruhsailorPanel(
            GuideRepository repo,
            GuideStateService state,
            EventBus bus,
            StepMappings stepMappings,
            QuestRegistry questRegistry)
    {
        super(false);
        this.repo = repo;
        this.state = state;
        this.bus = bus;
        this.stepMappings = stepMappings;
        this.questRegistry = questRegistry;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(ColorScheme.DARK_GRAY_COLOR);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        Font headerFont = FontManager.getRunescapeBoldFont().deriveFont(15f);
        chapterLabel.setFont(headerFont);
        chapterLabel.setForeground(ColorScheme.BRAND_ORANGE);
        chapterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sectionLabel.setFont(FontManager.getRunescapeFont().deriveFont(13f));
        sectionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        currentStepHolder.setLayout(
                new BoxLayout(currentStepHolder, BoxLayout.Y_AXIS)
        );
        currentStepHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        currentStepHolder.setBorder(
                BorderFactory.createEmptyBorder(6, 4, 6, 4)
        );
        currentStepHolder.setAlignmentX(Component.LEFT_ALIGNMENT);

        stepScroll = new JScrollPane(
                currentStepHolder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        stepScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepScroll.setBorder(BorderFactory.createEmptyBorder());
        stepScroll.getVerticalScrollBar().setUnitIncrement(16);
        stepScroll.setMinimumSize(new Dimension(0, 80));
        stepScroll.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                if (stepContentWidth() != renderedWidth)
                {
                    refreshAll();
                }
            }
        });

        metadataLabel.setFont(FontManager.getRunescapeFont());
        metadataLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        metadataLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        Font arrowFont = FontManager.getRunescapeBoldFont().deriveFont(16f);
        prevButton.setFont(arrowFont);
        nextButton.setFont(arrowFont);
        completeToggle.setFont(FontManager.getRunescapeFont());
        completeToggle.setFocusPainted(false);
        completeToggle.setOpaque(true);

        Dimension arrowSize = new Dimension(36, 28);
        prevButton.setPreferredSize(arrowSize);
        prevButton.setMaximumSize(arrowSize);
        nextButton.setPreferredSize(arrowSize);
        nextButton.setMaximumSize(arrowSize);
        prevButton.setMargin(new Insets(0, 0, 0, 0));
        nextButton.setMargin(new Insets(0, 0, 0, 0));

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        controls.add(prevButton);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(completeToggle);
        controls.add(Box.createHorizontalGlue());
        controls.add(nextButton);

        prevButton.addActionListener(e -> state.prev());
        nextButton.addActionListener(e -> state.next());
        completeToggle.addActionListener(e ->
        {
            StepId current = state.getCurrent();
            boolean complete = completeToggle.isSelected();

            state.setComplete(current, complete);

            if (complete)
            {
                int index = repo.indexOf(current);

                if (index >= 0 && index < repo.steps().size() - 1)
                {
                    state.next();
                }
            }
        });

        root.add(chapterLabel);
        root.add(Box.createVerticalStrut(2));
        root.add(sectionLabel);
        root.add(Box.createVerticalStrut(8));
        root.add(stepScroll);
        root.add(Box.createVerticalStrut(6));
        root.add(metadataLabel);
        root.add(Box.createVerticalStrut(8));
        root.add(controls);

        buildListModel();
        stepList.setCellRenderer(new RowRenderer());
        stepList.addListSelectionListener(e ->
        {
            if (e.getValueIsAdjusting())
            {
                return;
            }

            ListRow row = stepList.getSelectedValue();
            if (row instanceof StepRow)
            {
                state.setCurrent(((StepRow) row).id);
            }
        });
        stepList.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane listScroll = new JScrollPane(
                stepList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        listScroll.setBorder(
                BorderFactory.createEmptyBorder(8, 0, 0, 0)
        );

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                root,
                listScroll
        );
        split.setResizeWeight(0.65);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        split.setDividerSize(6);

        SwingUtilities.invokeLater(() ->
        {
            int height = getHeight();
            if (height > 0)
            {
                split.setDividerLocation((int) (height * 0.65));
            }
        });

        add(split, BorderLayout.CENTER);

        bus.register(this);
        refreshAll();
    }

    public void unregister()
    {
        bus.unregister(this);
    }

    @Subscribe
    public void onGuideStateChanged(GuideStateChanged e)
    {
        SwingUtilities.invokeLater(this::refreshAll);
    }

    private void refreshAll()
    {
        StepId id = state.getCurrent();
        Step step = repo.findById(id).orElse(null);
        if (step == null)
        {
            return;
        }

        boolean sameStep = id.equals(renderedStepId);
        int contentWidth = stepContentWidth();

        Point savedPosition;
        if (!sameStep)
        {
            savedPosition = new Point(0, 0);
        }
        else if (pendingScrollPosition != null)
        {
            savedPosition = new Point(pendingScrollPosition);
        }
        else
        {
            savedPosition = stepScroll.getViewport().getViewPosition();
        }

        chapterLabel.setText(repo.chapterTitleFor(id));
        sectionLabel.setText(repo.sectionTitleFor(id));

        StepRenderer.Rendered renderResult = StepRenderer.render(
                step,
                idx -> state.isBulletComplete(id, idx)
        );

        JComponent rendered = renderResult.component;
        rendered.setSize(new Dimension(contentWidth, Short.MAX_VALUE));

        JTextPane newPane = null;
        List<InlineLink> links = Collections.emptyList();

        if (rendered instanceof JTextPane)
        {
            newPane = (JTextPane) rendered;
            disableCaretScrolling(newPane);
            links = annotateInlineLinks(newPane, id);
        }

        boolean updatedInPlace = sameStep
                && displayedStep == step
                && contentWidth == renderedWidth
                && displayedPane != null
                && displayedRender != null
                && newPane != null
                && updateDisplayedStep(
                displayedPane,
                displayedRender,
                newPane,
                renderResult
        );

        if (!updatedInPlace)
        {
            renderedStepId = id;
            renderedWidth = contentWidth;
            displayedStep = step;
            displayedRender = renderResult;
            displayedPane = newPane;

            pendingScrollPosition = new Point(savedPosition);
            long version = ++refreshVersion;

            if (newPane != null)
            {
                installBulletHandlers(
                        newPane,
                        id,
                        renderResult.bullets,
                        links
                );
            }

            fitToWidth(rendered, contentWidth);

            currentStepHolder.removeAll();

            JTextPane items = buildItemsNeeded(step);
            if (items != null)
            {
                disableCaretScrolling(items);
                fitToWidth(items, contentWidth);
                currentStepHolder.add(items);
            }

            currentStepHolder.add(rendered);
            currentStepHolder.revalidate();
            currentStepHolder.repaint();

            SwingUtilities.invokeLater(() ->
            {
                if (version != refreshVersion)
                {
                    return;
                }

                stepScroll.validate();

                JViewport viewport = stepScroll.getViewport();
                Dimension viewSize = viewport.getViewSize();
                Dimension extent = viewport.getExtentSize();

                int maxX = Math.max(0, viewSize.width - extent.width);
                int maxY = Math.max(0, viewSize.height - extent.height);

                viewport.setViewPosition(new Point(
                        Math.max(0, Math.min(savedPosition.x, maxX)),
                        Math.max(0, Math.min(savedPosition.y, maxY))
                ));

                pendingScrollPosition = null;
            });
        }

        metadataLabel.setText(formatMetadata(step));

        int idx = repo.indexOf(id);
        prevButton.setEnabled(idx > 0);
        nextButton.setEnabled(
                idx >= 0 && idx < repo.steps().size() - 1
        );

        boolean complete = state.isComplete(id);
        completeToggle.setSelected(complete);

        if (complete)
        {
            completeToggle.setText("Done ✓");
            completeToggle.setBackground(DONE_BG);
            completeToggle.setForeground(DONE_FG);
        }
        else
        {
            completeToggle.setText("Done");
            completeToggle.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            completeToggle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        }

        stepList.repaint();
    }

    private static boolean updateDisplayedStep(
            JTextPane displayedPane,
            StepRenderer.Rendered displayedRender,
            JTextPane newPane,
            StepRenderer.Rendered newRender)
    {
        StyledDocument displayedDoc = displayedPane.getStyledDocument();
        StyledDocument newDoc = newPane.getStyledDocument();

        if (displayedDoc.getLength() != newDoc.getLength()
                || displayedRender.bullets.size() != newRender.bullets.size()
                || !(displayedDoc instanceof AbstractDocument))
        {
            return false;
        }

        try
        {
            String displayedText = displayedDoc.getText(
                    0,
                    displayedDoc.getLength()
            );
            String newText = newDoc.getText(0, newDoc.getLength());

            char[] normalizedDisplayed = displayedText.toCharArray();
            char[] normalizedNew = newText.toCharArray();

            for (int i = 0; i < displayedRender.bullets.size(); i++)
            {
                StepRenderer.BulletHit oldHit =
                        displayedRender.bullets.get(i);
                StepRenderer.BulletHit newHit =
                        newRender.bullets.get(i);

                if (oldHit.idx != newHit.idx
                        || oldHit.glyphStart != newHit.glyphStart
                        || oldHit.glyphEnd != newHit.glyphEnd
                        || oldHit.glyphStart < 0
                        || oldHit.glyphEnd <= oldHit.glyphStart
                        || oldHit.glyphEnd > displayedText.length())
                {
                    return false;
                }

                Arrays.fill(
                        normalizedDisplayed,
                        oldHit.glyphStart,
                        oldHit.glyphEnd,
                        '\0'
                );
                Arrays.fill(
                        normalizedNew,
                        newHit.glyphStart,
                        newHit.glyphEnd,
                        '\0'
                );
            }

            if (!Arrays.equals(normalizedDisplayed, normalizedNew))
            {
                return false;
            }

            AbstractDocument mutableDoc = (AbstractDocument) displayedDoc;

            for (StepRenderer.BulletHit hit : newRender.bullets)
            {
                String oldGlyph = displayedText.substring(
                        hit.glyphStart,
                        hit.glyphEnd
                );
                String newGlyph = newText.substring(
                        hit.glyphStart,
                        hit.glyphEnd
                );

                if (!oldGlyph.equals(newGlyph))
                {
                    SimpleAttributeSet attrs = new SimpleAttributeSet(
                            newDoc.getCharacterElement(
                                    hit.glyphStart
                            ).getAttributes()
                    );

                    mutableDoc.replace(
                            hit.glyphStart,
                            hit.glyphEnd - hit.glyphStart,
                            newGlyph,
                            attrs
                    );
                }
            }

            copyChangedCharacterStyles(displayedDoc, newDoc);
            return true;
        }
        catch (BadLocationException e)
        {
            return false;
        }
    }

    private static void copyChangedCharacterStyles(
            StyledDocument target,
            StyledDocument source)
    {
        int offset = 0;
        int length = source.getLength();

        while (offset < length)
        {
            Element sourceRun = source.getCharacterElement(offset);
            Element targetRun = target.getCharacterElement(offset);

            int end = Math.min(
                    length,
                    Math.min(
                            sourceRun.getEndOffset(),
                            targetRun.getEndOffset()
                    )
            );

            if (!targetRun.getAttributes().isEqual(
                    sourceRun.getAttributes()))
            {
                SimpleAttributeSet attrs = new SimpleAttributeSet(
                        sourceRun.getAttributes()
                );

                target.setCharacterAttributes(
                        offset,
                        end - offset,
                        attrs,
                        true
                );
            }

            offset = end;
        }
    }

    private int stepContentWidth()
    {
        int scrollbar =
                stepScroll.getVerticalScrollBar().getPreferredSize().width;
        Insets holder = currentStepHolder.getInsets();
        int outer = stepScroll.getWidth();

        if (outer <= 0)
        {
            outer = PluginPanel.PANEL_WIDTH - 16;
        }

        Insets scrollInsets = stepScroll.getInsets();
        int width = outer
                - scrollInsets.left
                - scrollInsets.right
                - scrollbar
                - holder.left
                - holder.right;

        return Math.max(120, width);
    }

    private static void disableCaretScrolling(JTextPane pane)
    {
        if (pane.getCaret() instanceof DefaultCaret)
        {
            DefaultCaret caret = (DefaultCaret) pane.getCaret();
            caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }

        pane.setFocusable(false);
    }

    private void installBulletHandlers(
            JTextPane pane,
            StepId id,
            List<StepRenderer.BulletHit> bullets,
            List<InlineLink> links)
    {
        pane.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (!SwingUtilities.isLeftMouseButton(e)
                        || e.getClickCount() != 1)
                {
                    return;
                }

                int pos = pane.viewToModel2D(e.getPoint());
                if (pos < 0 || pos >= pane.getDocument().getLength())
                {
                    return;
                }

                InlineLink link = findInlineLink(links, pos);
                if (link != null)
                {
                    link.onClick.run();
                    return;
                }

                StepRenderer.BulletHit bullet =
                        findBullet(pane, bullets, pos);

                if (bullet != null)
                {
                    state.toggleBullet(id, bullet.idx);
                }
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                pane.setCursor(Cursor.getDefaultCursor());
            }
        });

        pane.addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                int pos = pane.viewToModel2D(e.getPoint());

                boolean clickable = pos >= 0
                        && pos < pane.getDocument().getLength()
                        && (findInlineLink(links, pos) != null
                        || findBullet(pane, bullets, pos) != null);

                pane.setCursor(Cursor.getPredefinedCursor(
                        clickable
                                ? Cursor.HAND_CURSOR
                                : Cursor.DEFAULT_CURSOR
                ));
            }
        });
    }

    private static InlineLink findInlineLink(
            List<InlineLink> links,
            int pos)
    {
        if (links == null)
        {
            return null;
        }

        for (InlineLink link : links)
        {
            if (pos >= link.start && pos < link.end)
            {
                return link;
            }
        }

        return null;
    }

    private static StepRenderer.BulletHit findBullet(
            JTextPane pane,
            List<StepRenderer.BulletHit> bullets,
            int pos)
    {
        if (bullets == null || bullets.isEmpty() || pos < 0)
        {
            return null;
        }

        StyledDocument doc = pane.getStyledDocument();

        for (StepRenderer.BulletHit hit : bullets)
        {
            Element paragraph = doc.getParagraphElement(hit.glyphStart);

            int start = hit.glyphStart;
            int end = Math.max(hit.glyphEnd, paragraph.getEndOffset());
            end = Math.min(end, doc.getLength());

            for (StepRenderer.BulletHit other : bullets)
            {
                if (other.glyphStart > start && other.glyphStart < end)
                {
                    end = other.glyphStart;
                }
            }

            if (pos >= start && pos < end)
            {
                return hit;
            }
        }

        return null;
    }

    private static final class InlineLink
    {
        final int start;
        final int end;
        final Color color;
        final Runnable onClick;

        InlineLink(int start, int end, Color color, Runnable onClick)
        {
            this.start = start;
            this.end = end;
            this.color = color;
            this.onClick = onClick;
        }
    }

    private List<InlineLink> annotateInlineLinks(JTextPane pane, StepId id)
    {
        Optional<StepMapping> mappingOpt = stepMappings.findById(id);
        if (!mappingOpt.isPresent())
        {
            return Collections.emptyList();
        }

        StepMapping mapping = mappingOpt.get();
        StyledDocument doc = pane.getStyledDocument();
        String text;

        try
        {
            text = doc.getText(0, doc.getLength());
        }
        catch (BadLocationException e)
        {
            return Collections.emptyList();
        }

        List<InlineLink> links = new ArrayList<>();

        if (mapping.questIds != null && !mapping.questIds.isEmpty())
        {
            List<QuestEntry> resolved = new ArrayList<>();

            for (String enumName : mapping.questIds)
            {
                questRegistry.resolve(enumName).ifPresent(resolved::add);
            }

            for (InlineMatcher.Match<QuestEntry> match :
                    InlineMatcher.findFirstPerQuest(text, resolved))
            {
                final QuestEntry quest = match.entry;

                links.add(new InlineLink(
                        match.start,
                        match.end,
                        QUEST_LINK_BLUE,
                        () -> WikiLink.open(quest.displayName())
                ));
            }
        }

        addWikiLinks(text, mapping.npcs, NPC_LINK_COLOR, links);
        addWikiLinks(text, mapping.locations, LOCATION_LINK_COLOR, links);

        if (links.isEmpty())
        {
            return links;
        }

        links.sort(Comparator.comparingInt(link -> link.start));

        for (InlineLink link : links)
        {
            Element element = doc.getCharacterElement(link.start);
            SimpleAttributeSet attrs =
                    new SimpleAttributeSet(element.getAttributes());

            StyleConstants.setFontFamily(
                    attrs,
                    pane.getFont().getFamily()
            );
            StyleConstants.setForeground(attrs, link.color);
            StyleConstants.setUnderline(attrs, true);

            doc.setCharacterAttributes(
                    link.start,
                    link.end - link.start,
                    attrs,
                    true
            );
        }

        return links;
    }

    private static void addWikiLinks(
            String text,
            List<String> names,
            Color color,
            List<InlineLink> out)
    {
        if (names == null || names.isEmpty())
        {
            return;
        }

        for (InlineMatcher.Match<String> match :
                InlineMatcher.findFirstPerString(text, names))
        {
            final String pageTitle = match.entry;

            out.add(new InlineLink(
                    match.start,
                    match.end,
                    color,
                    () -> WikiLink.open(pageTitle)
            ));
        }
    }

    private static String formatMetadata(Step step)
    {
        if (step.metadata == null)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder("<html>");
        appendMeta(sb, "gp", step.metadata.gp_stack);
        appendMeta(sb, "time", step.metadata.total_time);
        appendMeta(sb, "skills", step.metadata.skills_quests_met);
        sb.append("</html>");

        return sb.toString();
    }

    private static JTextPane buildItemsNeeded(Step step)
    {
        if (step.metadata == null || step.metadata.items_needed == null)
        {
            return null;
        }

        String raw = step.metadata.items_needed.trim();
        if (raw.isEmpty())
        {
            return null;
        }

        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(false);
        pane.setMargin(new Insets(0, 0, 0, 0));
        pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        pane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        SimpleAttributeSet header = new SimpleAttributeSet();
        StyleConstants.setBold(header, true);
        StyleConstants.setForeground(header, ColorScheme.BRAND_ORANGE);

        SimpleAttributeSet body = new SimpleAttributeSet();
        StyleConstants.setForeground(body, ColorScheme.LIGHT_GRAY_COLOR);

        String text = raw;

        if (raw.equalsIgnoreCase("tbd"))
        {
            text = "Not listed in the guide yet";
            StyleConstants.setItalic(body, true);
        }
        else if (raw.equalsIgnoreCase("none"))
        {
            text = "None";
        }

        StyledDocument doc = pane.getStyledDocument();

        try
        {
            doc.insertString(0, "Items needed: ", header);
            doc.insertString(doc.getLength(), text, body);
        }
        catch (BadLocationException e)
        {
            throw new IllegalStateException(
                    "Unable to render items needed",
                    e
            );
        }

        return pane;
    }

    private static void fitToWidth(JComponent component, int width)
    {
        component.setSize(new Dimension(width, Short.MAX_VALUE));

        Dimension size = new Dimension(
                width,
                component.getPreferredSize().height
        );

        component.setPreferredSize(size);
        component.setMaximumSize(size);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void appendMeta(
            StringBuilder sb,
            String label,
            String value)
    {
        if (value == null || value.isEmpty())
        {
            return;
        }

        if (sb.length() > "<html>".length())
        {
            sb.append(" &middot; ");
        }

        sb.append(label).append(": ").append(value);
    }

    private void buildListModel()
    {
        Integer lastChapter = null;
        Integer lastSection = null;

        for (Step step : repo.steps())
        {
            StepId id = step.id;

            if (lastChapter == null || id.chapter() != lastChapter)
            {
                listModel.addElement(
                        new HeaderRow(repo.chapterTitleFor(id), true)
                );
                lastChapter = id.chapter();
                lastSection = null;
            }

            if (lastSection == null || id.section() != lastSection)
            {
                listModel.addElement(
                        new HeaderRow(repo.sectionTitleFor(id), false)
                );
                lastSection = id.section();
            }

            listModel.addElement(
                    new StepRow(id, id + "  " + firstLineSnippet(step))
            );
        }
    }

    private static String firstLineSnippet(Step step)
    {
        if (step.content == null)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (com.bruhsailor.plugin.model.ContentFragment fragment :
                step.content)
        {
            if (fragment == null || fragment.text == null)
            {
                continue;
            }

            sb.append(fragment.text);

            if (sb.length() >= 60)
            {
                break;
            }
        }

        String snippet = sb.toString().replace('\n', ' ').trim();

        if (snippet.length() > 50)
        {
            snippet = snippet.substring(0, 50) + "…";
        }

        return snippet;
    }

    private final class RowRenderer extends JLabel
            implements ListCellRenderer<ListRow>
    {
        RowRenderer()
        {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends ListRow> list,
                ListRow value,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
        {
            if (value instanceof HeaderRow)
            {
                HeaderRow header = (HeaderRow) value;
                int wrapWidth = Math.max(120, list.getWidth() - 24);

                setText(html(escape(header.text), wrapWidth));
                setFont(
                        FontManager.getRunescapeBoldFont().deriveFont(17f)
                );
                setBorder(
                        BorderFactory.createEmptyBorder(12, 8, 6, 8)
                );

                if (header.isChapter)
                {
                    setForeground(ColorScheme.BRAND_ORANGE);
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
                else
                {
                    setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    setBackground(ColorScheme.DARK_GRAY_COLOR);
                }
            }
            else
            {
                StepRow row = (StepRow) value;
                boolean isCurrent = row.id.equals(state.getCurrent());
                boolean done = state.isComplete(row.id);

                String text = done
                        ? "<html><strike>" + escape(row.label)
                        + "</strike></html>"
                        : row.label;

                setText(text);
                setFont(
                        FontManager.getRunescapeFont().deriveFont(16f)
                );
                setForeground(
                        done
                                ? ColorScheme.LIGHT_GRAY_COLOR.darker()
                                : ColorScheme.LIGHT_GRAY_COLOR
                );
                setBackground(
                        isCurrent
                                ? ColorScheme.BRAND_ORANGE.darker()
                                : ColorScheme.DARK_GRAY_COLOR
                );
                setBorder(
                        BorderFactory.createEmptyBorder(6, 12, 6, 8)
                );
            }

            return this;
        }

        private String escape(String text)
        {
            return text.replace("&", "&amp;").replace("<", "&lt;");
        }

        private String html(String inner, int widthPx)
        {
            return "<html><div style='width:" + widthPx + "px'>"
                    + inner + "</div></html>";
        }
    }

    private interface ListRow
    {
    }

    private static final class HeaderRow implements ListRow
    {
        final String text;
        final boolean isChapter;

        HeaderRow(String text, boolean isChapter)
        {
            this.text = text;
            this.isChapter = isChapter;
        }
    }

    private static final class StepRow implements ListRow
    {
        final StepId id;
        final String label;

        StepRow(StepId id, String label)
        {
            this.id = id;
            this.label = label;
        }
    }
}
