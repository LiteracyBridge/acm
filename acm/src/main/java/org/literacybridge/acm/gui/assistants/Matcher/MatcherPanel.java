package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableModel.Columns;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher.MatchStats;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class MatcherPanel extends JPanel {
    public static int DEFAULT_THRESHOLD = 90;
    public static int MINIMUM_THRESHOLD = 80;

    private JCheckBox hideMatched;
    private JCheckBox colorCoded;
    private JCheckBox showLeft;
    private JCheckBox showRight;
    private JCheckBox showExact;
    private JCheckBox showFuzzy;
    private JCheckBox showToken;

    private MatcherTableModel model;
    private MatcherTable /*JTable*/ table;
    private SpinnerNumberModel thresholdModel;
    private JLabel statusBar;

    private Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher;
    private List<ImportableAudioItem> leftList;
    private List<ImportableFile> rightList;

    public MatcherPanel() {
        super();

        createComponents();
    }

    public void setData(Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher, List<ImportableAudioItem> left, List<ImportableFile> right) {
        // We have these so that we can implement "Reset".
        leftList = new ArrayList(left);
        rightList = new ArrayList(right);
        
        boolean first = this.matcher == null;
        this.matcher = matcher;
        model.setData(matcher.matchableItems);
    }
    public void reset() {
        onResetAction(null);
    }

    /**
     * Performs a sequence of exact/fuzzy/token matches (using the current value of threshold),
     * and then sorts the contents.
     * @return a MatchStats describing how many matches were made.
     */
    public MatchStats autoMatch() {
        MatchStats result = new MatchStats();
        if (matcher != null) {
            int threshold = (int) this.thresholdModel.getValue();
            result.add(matcher.findExactMatches());
            result.add(matcher.findFuzzyMatches(threshold));
            result.add(matcher.findTokenMatches(threshold));
            matcher.sort();
            model.fireTableDataChanged();
            statusBar.setText(result.toString());
        }
        return result;
    }

    private void createComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        makeToolbar();
        makeVisualFilters();
        makeTable();

        // Status bar
        Box hbox = Box.createHorizontalBox();
        statusBar = new JLabel(" ");
        hbox.add(statusBar);
        hbox.add(Box.createHorizontalGlue());
        add(hbox);

    }

    /**
     * Creates the table and sets the model and various renderers, filters, etc.
     */
    private void makeTable() {
        table = new MatcherTable();
        model = table.getModel();

        table.setFilter(this::filter);

        MatcherRenderer mr = new MatcherRenderer();
        table.setRenderer(Columns.Left, mr);
//        table.setRenderer(Columns.Match, new MatchRenderer());
//        table.setRenderer(Columns.Score, new ScoreRenderer());
        table.setRenderer(Columns.Status, new StatusRenderer());
        table.setRenderer(Columns.Right, mr);

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(224, 224, 224));
        table.setGridColor(Color.RED);

        add(scrollPane);
    }

    /**
     * Make the toolbar, with the function buttons.
     */
    private void makeToolbar() {
        Box hbox = Box.createHorizontalBox();
        JButton btn = new JButton("Reset");
        btn.addActionListener(this::onResetAction);
        hbox.add(btn);
        btn = new JButton("Exact");
        btn.addActionListener(this::onExactAction);
        hbox.add(btn);
        btn = new JButton("Fuzzy");
        btn.addActionListener(this::onFuzzyAction);
        hbox.add(btn);
        btn = new JButton("Tokens");
        btn.addActionListener(this::onTokensAction);
        hbox.add(btn);
        btn = new JButton("Sort");
        btn.addActionListener(this::onSortAction);
        hbox.add(btn);
        hbox.add(Box.createHorizontalGlue());
        add(hbox);
    }

    /**
     * Make the checkboxes for the visual filters.
     */
    private void makeVisualFilters() {
        Box hbox = Box.createHorizontalBox();
        hideMatched = new JCheckBox("Hide matched");
        colorCoded = new JCheckBox("Color coded", null, false);
        hbox.add(hideMatched);
        hbox.add(colorCoded);
        hbox.add(Box.createHorizontalGlue());
        add(hbox);

        hbox = Box.createHorizontalBox();
        showLeft = new JCheckBox("Left", null, true);
        showRight = new JCheckBox("Right", null, true);
        showExact = new JCheckBox("Exact", null, true);
        showFuzzy = new JCheckBox("Fuzzy", null, true);
        showToken = new JCheckBox("Token", null, true);

        hideMatched.addActionListener(this::onHideMatched);
        colorCoded.addActionListener(actionEvent -> model.fireTableDataChanged());

        showLeft.addActionListener(actionEvent -> model.fireTableDataChanged());
        showRight.addActionListener(actionEvent -> model.fireTableDataChanged());
        showExact.addActionListener(actionEvent -> model.fireTableDataChanged());
        showFuzzy.addActionListener(actionEvent -> model.fireTableDataChanged());
        showToken.addActionListener(actionEvent -> model.fireTableDataChanged());

        // Spinner to set the fuzzy threshold between MINIMUM_THRESHOLD and 100.
        JSpinner threshold = new JSpinner();
        Dimension size = threshold.getPreferredSize();
        thresholdModel = new SpinnerNumberModel(DEFAULT_THRESHOLD, MINIMUM_THRESHOLD, 100, 1);
        threshold.setModel(thresholdModel);
        size.width = 75;
        threshold.setPreferredSize(size);
        threshold.setMaximumSize(size);

        hbox.add(new JLabel("Show: "));
        hbox.add(showLeft);
        hbox.add(showRight);
        hbox.add(showExact);
        hbox.add(showFuzzy);
        hbox.add(showToken);
        hbox.add(threshold);
        hbox.add(Box.createHorizontalGlue());
        add(hbox);
    }

    /**
     * When the "show/hide matched" is toggled, this propagates the setting to the individual
     * show/hide matched toggles.
     *
     * @param actionEvent is unused.
     */
    private void onHideMatched(ActionEvent actionEvent) {
        showExact.setSelected(!hideMatched.isSelected());
        showFuzzy.setSelected(!hideMatched.isSelected());
        showToken.setSelected(!hideMatched.isSelected());
        model.fireTableDataChanged();
    }

    /**
     * The filter for showing and hiding matched columns.
     *
     * @param t   The item to be filtered.
     * @param <T> Type of the item, some subclass of MatchableItem.
     * @return true if the item shoudl be shown.
     */
    private <T extends MatchableItem<?, ?>> boolean filter(T t) {
        MATCH match = t.getMatch();
        switch (match) {
        case EXACT:
            return showExact.isSelected();
        case FUZZY:
            return showFuzzy.isSelected();
        case TOKEN:
            return showToken.isSelected();
        case LEFT_ONLY:
            return showLeft.isSelected();
        case RIGHT_ONLY:
            return showRight.isSelected();
        default:
            return false;
        }
    }

    /**
     * Handles the sort button.
     *
     * @param actionEvent ignored.
     */
    private void onSortAction(ActionEvent actionEvent) {
        matcher.sort();
        model.fireTableDataChanged();
    }

    /**
     * Handles the tokens button.
     *
     * @param actionEvent ignored.
     */
    private void onTokensAction(ActionEvent actionEvent) {
        int threshold = (int) this.thresholdModel.getValue();
        MatchStats result = matcher.findTokenMatches(threshold);
        model.fireTableDataChanged();
        statusBar.setText(result.toString());
    }

    /**
     * Handles the fuzzy button.
     *
     * @param actionEvent ignored.
     */
    private void onFuzzyAction(ActionEvent actionEvent) {
        int threshold = (int) this.thresholdModel.getValue();
        MatchStats result = matcher.findFuzzyMatches(threshold);
        model.fireTableDataChanged();
        statusBar.setText(result.toString());
    }

    /**
     * Handles the exact button.
     *
     * @param actionEvent ignored.
     */
    private void onExactAction(ActionEvent actionEvent) {
        MatchStats result = matcher.findExactMatches();
        model.fireTableDataChanged();
        statusBar.setText(result.toString());
    }

    /**
     * Handles the reset button. Resets the data to the unmatched state.
     *
     * @param actionEvent ignored.
     */
    private void onResetAction(ActionEvent actionEvent) {
        matcher.setData(leftList, rightList, MatchableImportableAudio::new);
        model.setData(matcher.matchableItems);
    }

    private static Color selectionColor = new Color(0xFA8072);
    private static Color exactColor = new Color(0xffffe0);
    private static Color fuzzyColor = new Color(0xfff0ff);
    private static Color tokenColor = new Color(0xe8ffff);
    private static Color leftColor = new Color(0xFFC0CB);
    private static Color rightColor = new Color(0xADD8E6);

    /**
     * General renderer for matchable items. Optionally performs color coding based on
     * the match state of the data.
     */
    private class MatcherRenderer extends DefaultTableCellRenderer {
        private Color bgColor;
        private Color bgSelectionColor;
        private Color bgAlternateColor;

        MatcherRenderer() {
            super();
            bgColor = Color.white; // table.getBackground();
            bgSelectionColor = table.getSelectionForeground();
            bgAlternateColor = new Color(235, 245, 252);
        }

        private void setBackground(int viewRow, int viewColumn, boolean isSelected) {
            Color bg = (viewRow%2 == 0) ? bgColor : bgAlternateColor;
            if (!colorCoded.isSelected()) {
                if (isSelected) bg = bgSelectionColor;
//                bg = isSelected ? bgSelectionColor : bgColor;
//                if (viewRow % 2 == 1 && !isSelected) bg = bgAlternateColor; // darken(bg);
            } else {
                if (isSelected) bg = selectionColor;
                else {
                    int row = table.convertRowIndexToModel(viewRow);
                    int column = table.convertColumnIndexToModel(viewColumn);
                    MatchableImportableAudio item = model.getRowAt(row);
                    switch (item.getMatch()) {
                    case EXACT:
                        bg = exactColor;
                        break;
                    case FUZZY:
                        bg = fuzzyColor;
                        break;
                    case TOKEN:
                        bg = tokenColor;
                        break;
                    case LEFT_ONLY:
                        if (column == Columns.Left.ordinal()) bg = leftColor;
                        break;
                    case RIGHT_ONLY:
                        if (column == Columns.Right.ordinal()) bg = rightColor;
                        break;
                    }
                }
                if (viewRow % 2 == 1 && !isSelected) bg = lighten(bg);
            }
            setBackground(bg);
        }

        private Color darken(Color c) {
            double FACTOR = 0.96;
            return new Color(Math.max((int) (c.getRed() * FACTOR), 0),
                Math.max((int) (c.getGreen() * FACTOR), 0),
                Math.max((int) (c.getBlue() * FACTOR), 0),
                c.getAlpha());
        }

        private Color lighten(Color c) {
            double FACTOR = 1.04;
            return new Color(Math.min((int) (c.getRed() * FACTOR), 255),
                Math.min((int) (c.getGreen() * FACTOR), 255),
                Math.min((int) (c.getBlue() * FACTOR), 255),
                c.getAlpha());
        }

        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(row, column, isSelected);
            return this;
        }
    }

    /**
     * Renderer for the Score -- only prints the score for Token or Fuzzy matches.
     */
    private class ScoreRenderer extends MatcherRenderer {
        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int viewRow,
            int viewColumn)
        {
            int row = table.convertRowIndexToModel(viewRow);
            MatchableImportableAudio item = model.getRowAt(row);
            if (item.getMatch() != MATCH.FUZZY && item.getMatch() != MATCH.TOKEN) {
                value = "";
            }
            return super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                viewRow,
                viewColumn);
        }
    }

    private class MatchRenderer extends MatcherRenderer {
        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int viewRow,
            int viewColumn)
        {
            int row = table.convertRowIndexToModel(viewRow);
            MatchableImportableAudio item = model.getRowAt(row);
            switch (item.getMatch()) {

            case NONE:
                break;
            case EXACT:
                value = "Exact";
                break;
            case FUZZY:
                value = "Fuzzy";
                break;
            case TOKEN:
                value = "Token";
                break;
            case LEFT_ONLY:
                value = "Content";
                break;
            case RIGHT_ONLY:
                value = "File";
                break;
            }
            return super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                viewRow,
                viewColumn);
        }
    }

    private class StatusRenderer extends MatcherRenderer {
        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int viewRow,
            int viewColumn)
        {
            int row = table.convertRowIndexToModel(viewRow);
            MatchableImportableAudio item = model.getRowAt(row);
            String tooltip = null;
            switch (item.getMatch()) {
            case NONE:
                break;
            case EXACT:
                value = "Import from";
                tooltip = "Exact match";
                break;
            case FUZZY:
                value = "Import from";
                tooltip = "Fuzzy match @" + item.getScore();
                break;
            case TOKEN:
                value = "Import from";
                tooltip = "Token match @" + item.getScore();
                break;
            case LEFT_ONLY:
                value = "Missing Audio";
                tooltip = "Message is missing audio content";
                break;
            case RIGHT_ONLY:
                value = "Audio File";
                tooltip = "Audio file has no matching message";
                break;
            }
            JLabel label = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                viewRow,
                viewColumn);
            label.setToolTipText(tooltip);
            return label;
        }
    }
}
