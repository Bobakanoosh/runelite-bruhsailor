package com.bruhsailor.plugin;

import com.bruhsailor.plugin.model.Step;
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
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

public class BruhsailorPanel extends PluginPanel
{
    private final GuideRepository repo;
    private final GuideStateService state;
    private final EventBus bus;

    private final JLabel chapterLabel = new JLabel();
    private final JLabel sectionLabel = new JLabel();
    private final JPanel currentStepHolder = new JPanel(new BorderLayout());
    private final JLabel metadataLabel = new JLabel();
    private final JButton prevButton = new JButton("Prev");
    private final JButton nextButton = new JButton("Next");
    private final JToggleButton completeToggle = new JToggleButton("Mark complete");

    private final javax.swing.DefaultListModel<ListRow> listModel = new javax.swing.DefaultListModel<>();
    private final javax.swing.JList<ListRow> stepList = new javax.swing.JList<>(listModel);

    public BruhsailorPanel(GuideRepository repo, GuideStateService state, EventBus bus)
    {
        super(false);
        this.repo = repo;
        this.state = state;
        this.bus = bus;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(ColorScheme.DARK_GRAY_COLOR);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        chapterLabel.setFont(FontManager.getRunescapeBoldFont());
        chapterLabel.setForeground(ColorScheme.BRAND_ORANGE);
        chapterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionLabel.setFont(FontManager.getRunescapeFont());
        sectionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        currentStepHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        currentStepHolder.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        currentStepHolder.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane stepScroll = new JScrollPane(currentStepHolder);
        stepScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepScroll.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 16, 240));
        stepScroll.setBorder(BorderFactory.createEmptyBorder());

        metadataLabel.setFont(FontManager.getRunescapeSmallFont());
        metadataLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        metadataLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(prevButton);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(completeToggle);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(nextButton);

        prevButton.addActionListener(e -> state.prev());
        nextButton.addActionListener(e -> state.next());
        completeToggle.addActionListener(e ->
            state.setComplete(state.getCurrent(), completeToggle.isSelected()));

        root.add(chapterLabel);
        root.add(sectionLabel);
        root.add(Box.createVerticalStrut(8));
        root.add(stepScroll);
        root.add(Box.createVerticalStrut(4));
        root.add(metadataLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(controls);

        add(root, BorderLayout.NORTH);

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

        JScrollPane listScroll = new JScrollPane(stepList);
        listScroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(listScroll, BorderLayout.CENTER);

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
        JComponent rendered = (JComponent) StepRenderer.render(step);
        currentStepHolder.add(rendered, BorderLayout.CENTER);
        currentStepHolder.revalidate();
        currentStepHolder.repaint();

        metadataLabel.setText(formatMetadata(step));

        int idx = repo.indexOf(id);
        prevButton.setEnabled(idx > 0);
        nextButton.setEnabled(idx >= 0 && idx < repo.steps().size() - 1);

        completeToggle.setSelected(state.isComplete(id));
        stepList.repaint();
    }

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
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }

        @Override
        public Component getListCellRendererComponent(
            javax.swing.JList<? extends ListRow> list,
            ListRow value, int index, boolean isSelected, boolean cellHasFocus)
        {
            if (value instanceof HeaderRow)
            {
                HeaderRow h = (HeaderRow) value;
                setText(h.text);
                setFont(h.isChapter ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeFont());
                setForeground(h.isChapter ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
                setBackground(ColorScheme.DARKER_GRAY_COLOR);
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
                setFont(FontManager.getRunescapeSmallFont());
                setForeground(done ? ColorScheme.LIGHT_GRAY_COLOR.darker() : ColorScheme.LIGHT_GRAY_COLOR);
                setBackground(isCurrent ? ColorScheme.BRAND_ORANGE.darker() : ColorScheme.DARK_GRAY_COLOR);
            }
            return this;
        }

        private String escape(String s)
        {
            return s.replace("&", "&amp;").replace("<", "&lt;");
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
