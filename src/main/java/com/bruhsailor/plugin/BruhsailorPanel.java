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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public class BruhsailorPanel extends PluginPanel
{
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

    private final javax.swing.DefaultListModel<ListRow> listModel = new javax.swing.DefaultListModel<>();
    private final javax.swing.JList<ListRow> stepList = new javax.swing.JList<>(listModel);
    private JScrollPane stepScroll;

    public BruhsailorPanel(GuideRepository repo, GuideStateService state, EventBus bus,
                           StepMappings stepMappings, QuestRegistry questRegistry)
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

        currentStepHolder.setLayout(new BoxLayout(currentStepHolder, BoxLayout.Y_AXIS));
        currentStepHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        currentStepHolder.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        currentStepHolder.setAlignmentX(Component.LEFT_ALIGNMENT);

        stepScroll = new JScrollPane(currentStepHolder,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        stepScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepScroll.setBorder(BorderFactory.createEmptyBorder());
        stepScroll.getVerticalScrollBar().setUnitIncrement(16);
        // The step block expands to fill the top half of the split (sized below).
        stepScroll.setMinimumSize(new Dimension(0, 80));

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
        prevButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        nextButton.setMargin(new java.awt.Insets(0, 0, 0, 0));

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
            state.setComplete(state.getCurrent(), completeToggle.isSelected()));

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
        stepList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            ListRow row = stepList.getSelectedValue();
            if (row instanceof StepRow)
            {
                state.setCurrent(((StepRow) row).id);
            }
        });
        stepList.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane listScroll = new JScrollPane(stepList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, root, listScroll);
        split.setResizeWeight(0.65);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        split.setDividerSize(6);
        // Apply the 65/35 split as soon as the panel has its real height.
        SwingUtilities.invokeLater(() -> {
            int h = getHeight();
            if (h > 0) split.setDividerLocation((int) (h * 0.65));
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
        if (step == null) return;

        chapterLabel.setText(repo.chapterTitleFor(id));
        sectionLabel.setText(repo.sectionTitleFor(id));

        currentStepHolder.removeAll();

        // Render rich-text first so we know the document text for inline
        // matching against quest names.
        StepRenderer.Rendered renderResult = StepRenderer.render(step,
            idx -> state.isBulletComplete(id, idx));
        JComponent rendered = renderResult.component;
        // Budget: 4+4 holder padding, 0 textpane margin, ~16 scrollbar, ~4 safety.
        int contentWidth = Math.max(120, PluginPanel.PANEL_WIDTH - 28);
        rendered.setSize(new Dimension(contentWidth, Short.MAX_VALUE));

        if (rendered instanceof javax.swing.JTextPane)
        {
            javax.swing.JTextPane pane = (javax.swing.JTextPane) rendered;
            installBulletHandlers(pane, id, renderResult.bullets);
            annotateInlineLinks(pane, id);
        }

        // Recompute preferred size AFTER annotation (styling can change line wrapping).
        rendered.setSize(new Dimension(contentWidth, Short.MAX_VALUE));
        Dimension pref = rendered.getPreferredSize();
        rendered.setPreferredSize(new Dimension(contentWidth, pref.height));
        rendered.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        rendered.setAlignmentX(Component.LEFT_ALIGNMENT);

        currentStepHolder.add(rendered);

        currentStepHolder.revalidate();
        currentStepHolder.repaint();
        // Newly-rendered content has its caret at end; force the surrounding
        // viewport back to the top so users always see the start of the step.
        SwingUtilities.invokeLater(() -> {
            stepScroll.getVerticalScrollBar().setValue(0);
            stepScroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
        });

        metadataLabel.setText(formatMetadata(step));

        int idx = repo.indexOf(id);
        prevButton.setEnabled(idx > 0);
        nextButton.setEnabled(idx >= 0 && idx < repo.steps().size() - 1);

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

    private static final java.awt.Color DONE_BG = new java.awt.Color(0x3F7A3F);
    private static final java.awt.Color DONE_FG = new java.awt.Color(0xE6FFE6);

    private void installBulletHandlers(javax.swing.JTextPane pane, StepId id,
                                       java.util.List<StepRenderer.BulletHit> bullets)
    {
        if (bullets == null || bullets.isEmpty()) return;
        pane.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                int pos = pane.viewToModel2D(e.getPoint());
                for (StepRenderer.BulletHit hit : bullets)
                {
                    if (pos >= hit.glyphStart && pos < hit.glyphEnd)
                    {
                        state.toggleBullet(id, hit.idx);
                        return;
                    }
                }
            }
        });
        pane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e)
            {
                int pos = pane.viewToModel2D(e.getPoint());
                for (StepRenderer.BulletHit hit : bullets)
                {
                    if (pos >= hit.glyphStart && pos < hit.glyphEnd)
                    {
                        pane.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                        return;
                    }
                }
                // Don't reset cursor here — the link hover handler manages it for non-bullet positions.
            }
        });
    }

    /** A linkified span in the rendered step. Click action and color depend on the kind. */
    private static final class InlineLink
    {
        final int start;
        final int end;
        final java.awt.Color color;
        final Runnable onClick;

        InlineLink(int start, int end, java.awt.Color color, Runnable onClick)
        {
            this.start = start;
            this.end = end;
            this.color = color;
            this.onClick = onClick;
        }
    }

    /**
     * Find quest / NPC / location mentions inside the given pane's text, style
     * them as blue underlined "links", and install click + hover handlers.
     * Quest, NPC, and location links all open the OSRS wiki page in the user's browser.
     */
    private void annotateInlineLinks(javax.swing.JTextPane pane, StepId id)
    {
        java.util.Optional<StepMapping> mappingOpt = stepMappings.findById(id);
        if (!mappingOpt.isPresent()) return;
        StepMapping mapping = mappingOpt.get();

        javax.swing.text.StyledDocument doc = pane.getStyledDocument();
        String text;
        try
        {
            text = doc.getText(0, doc.getLength());
        }
        catch (javax.swing.text.BadLocationException e)
        {
            return;
        }

        java.util.List<InlineLink> links = new java.util.ArrayList<>();

        if (mapping.questIds != null && !mapping.questIds.isEmpty())
        {
            java.util.List<QuestEntry> resolved = new java.util.ArrayList<>();
            for (String enumName : mapping.questIds)
            {
                questRegistry.resolve(enumName).ifPresent(resolved::add);
            }
            for (InlineMatcher.Match<QuestEntry> m : InlineMatcher.findFirstPerQuest(text, resolved))
            {
                final QuestEntry q = m.entry;
                links.add(new InlineLink(m.start, m.end, QUEST_LINK_BLUE, () -> WikiLink.open(q.displayName())));
            }
        }

        addWikiLinks(text, mapping.npcs, NPC_LINK_COLOR, links);
        addWikiLinks(text, mapping.locations, LOCATION_LINK_COLOR, links);

        if (links.isEmpty()) return;

        // Sort by start to keep behaviour deterministic when spans somehow overlap
        // across the three matchers (they don't share a `consumed` set).
        links.sort((a, b) -> Integer.compare(a.start, b.start));

        // Preserve existing character attributes (font size set per-fragment by
        // StepRenderer, plus the pane's font family) when applying link styling.
        // Without this, Swing falls back to a default font family for the styled
        // run, which is wider than the pixel RuneScape font and overflows the
        // panel width, visually collapsing spaces around the link.
        for (InlineLink link : links)
        {
            javax.swing.text.Element el = doc.getCharacterElement(link.start);
            javax.swing.text.SimpleAttributeSet attrs = new javax.swing.text.SimpleAttributeSet(el.getAttributes());
            javax.swing.text.StyleConstants.setFontFamily(attrs, pane.getFont().getFamily());
            javax.swing.text.StyleConstants.setForeground(attrs, link.color);
            javax.swing.text.StyleConstants.setUnderline(attrs, true);
            doc.setCharacterAttributes(link.start, link.end - link.start, attrs, true);
        }

        java.util.List<InlineLink> finalLinks = links;
        pane.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                int pos = pane.viewToModel2D(e.getPoint());
                for (InlineLink link : finalLinks)
                {
                    if (pos >= link.start && pos < link.end)
                    {
                        link.onClick.run();
                        return;
                    }
                }
            }
        });
        pane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e)
            {
                int pos = pane.viewToModel2D(e.getPoint());
                boolean over = false;
                for (InlineLink link : finalLinks)
                {
                    if (pos >= link.start && pos < link.end) { over = true; break; }
                }
                pane.setCursor(java.awt.Cursor.getPredefinedCursor(
                    over ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
            }
        });
    }

    private static void addWikiLinks(String text, java.util.List<String> names,
                                     java.awt.Color color,
                                     java.util.List<InlineLink> out)
    {
        if (names == null || names.isEmpty()) return;
        for (InlineMatcher.Match<String> m : InlineMatcher.findFirstPerString(text, names))
        {
            final String pageTitle = m.entry;
            out.add(new InlineLink(m.start, m.end, color, () -> WikiLink.open(pageTitle)));
        }
    }

    private static final java.awt.Color QUEST_LINK_BLUE = new java.awt.Color(0x4FB3FF);
    private static final java.awt.Color NPC_LINK_COLOR = new java.awt.Color(0xFFB347);      // warm amber — distinct from quest blue
    private static final java.awt.Color LOCATION_LINK_COLOR = new java.awt.Color(0x9CE37D); // soft green

    private static String formatMetadata(Step step)
    {
        if (step.metadata == null) return "";
        StringBuilder sb = new StringBuilder("<html>");
        appendMeta(sb, "gp", step.metadata.gp_stack);
        appendMeta(sb, "time", step.metadata.total_time);
        appendMeta(sb, "skills", step.metadata.skills_quests_met);
        sb.append("</html>");
        return sb.toString();
    }

    private static void appendMeta(StringBuilder sb, String label, String value)
    {
        if (value == null || value.isEmpty()) return;
        if (sb.length() > "<html>".length()) sb.append(" &middot; ");
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
                listModel.addElement(new HeaderRow(repo.chapterTitleFor(id), true));
                lastChapter = id.chapter();
                lastSection = null;
            }
            if (lastSection == null || id.section() != lastSection)
            {
                listModel.addElement(new HeaderRow(repo.sectionTitleFor(id), false));
                lastSection = id.section();
            }
            listModel.addElement(new StepRow(id, id + "  " + firstLineSnippet(step)));
        }
    }

    private static String firstLineSnippet(Step step)
    {
        if (step.content == null) return "";
        StringBuilder sb = new StringBuilder();
        for (com.bruhsailor.plugin.model.ContentFragment f : step.content)
        {
            if (f == null || f.text == null) continue;
            sb.append(f.text);
            if (sb.length() >= 60) break;
        }
        String s = sb.toString().replace('\n', ' ').trim();
        if (s.length() > 50) s = s.substring(0, 50) + "…";
        return s;
    }

    private final class RowRenderer extends javax.swing.JLabel
        implements javax.swing.ListCellRenderer<ListRow>
    {
        RowRenderer()
        {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
            javax.swing.JList<? extends ListRow> list,
            ListRow value, int index, boolean isSelected, boolean cellHasFocus)
        {
            if (value instanceof HeaderRow)
            {
                HeaderRow h = (HeaderRow) value;
                int wrapWidth = Math.max(120, list.getWidth() - 24);
                if (h.isChapter)
                {
                    setText(html(escape(h.text), wrapWidth));
                    setFont(FontManager.getRunescapeBoldFont().deriveFont(17f));
                    setForeground(ColorScheme.BRAND_ORANGE);
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    setBorder(BorderFactory.createEmptyBorder(12, 8, 6, 8));
                }
                else
                {
                    setText(html(escape(h.text), wrapWidth));
                    setFont(FontManager.getRunescapeBoldFont().deriveFont(17f));
                    setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    setBackground(ColorScheme.DARK_GRAY_COLOR);
                    setBorder(BorderFactory.createEmptyBorder(12, 8, 6, 8));
                }
            }
            else
            {
                StepRow r = (StepRow) value;
                boolean isCurrent = r.id.equals(state.getCurrent());
                boolean done = state.isComplete(r.id);
                String text = done
                    ? "<html><strike>" + escape(r.label) + "</strike></html>"
                    : r.label;
                setText(text);
                setFont(FontManager.getRunescapeFont().deriveFont(16f));
                setForeground(done ? ColorScheme.LIGHT_GRAY_COLOR.darker() : ColorScheme.LIGHT_GRAY_COLOR);
                setBackground(isCurrent ? ColorScheme.BRAND_ORANGE.darker() : ColorScheme.DARK_GRAY_COLOR);
                setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 8));
            }
            return this;
        }

        private String escape(String s)
        {
            return s.replace("&", "&amp;").replace("<", "&lt;");
        }

        private String html(String inner, int widthPx)
        {
            return "<html><div style='width:" + widthPx + "px'>" + inner + "</div></html>";
        }
    }

    private interface ListRow {}

    private static final class HeaderRow implements ListRow
    {
        final String text;
        final boolean isChapter;
        HeaderRow(String text, boolean isChapter) { this.text = text; this.isChapter = isChapter; }
    }

    private static final class StepRow implements ListRow
    {
        final StepId id;
        final String label;
        StepRow(StepId id, String label) { this.id = id; this.label = label; }
    }
}
