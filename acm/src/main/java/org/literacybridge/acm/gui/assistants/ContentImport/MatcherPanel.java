package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTable;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableModel.Columns;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher.MatchStats;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MatcherPanel extends JPanel {
    public static int DEFAULT_THRESHOLD = 80;
    public static int MINIMUM_THRESHOLD = 60;

    private ContentImportContext context;

    private JCheckBox hideMatched;
    private JCheckBox colorCoded;
    private JCheckBox showLeft;
    private JCheckBox showRight;
    private JCheckBox showExact;
    private JCheckBox showFuzzy;
    private JCheckBox showToken;
    private JCheckBox showManual;

    private MatcherTableModel model;
    private MatcherTable /*JTable*/ table;
    private SpinnerNumberModel thresholdModel;
    private JLabel statusBar;

    private Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher;
    private List<ImportableAudioItem> leftList;
    private List<ImportableFile> rightList;
    private JButton unMatch;
    private JButton manualMatch;
    private JButton importAsIs;

    private int fuzzyThreshold = DEFAULT_THRESHOLD;

    public MatcherPanel(ContentImportContext context) {
        super();
        this.context = context;

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
            result.add(matcher.findExactMatches());
            result.add(matcher.findFuzzyMatches(fuzzyThreshold));
            result.add(matcher.findTokenMatches(fuzzyThreshold));
            matcher.sort();
            model.fireTableDataChanged();
            statusBar.setText(result.toString());
        }
        return result;
    }

    private void createComponents() {
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0, 0, 15, 0);
        Insets tight = new Insets(0, 0, 5, 0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.LINE_START,
            GridBagConstraints.BOTH,
            tight,
            1,
            1);

        JComponent toolbar = makeToolbar();
        JComponent filters = makeVisualFilters();
        if (context.debug) {
            add(toolbar, gbc);
            add(filters, gbc);
        }

        add(makeThresholdControl(), gbc);

        gbc.weighty = 1.0;
        add(makeTable(), gbc);
        gbc.weighty = 0;

        add(makeManualMatchButtons(), gbc);

        // Status bar
        Box hbox = Box.createHorizontalBox();
        statusBar = new JLabel(" ");
        hbox.add(statusBar);
        hbox.add(Box.createHorizontalGlue());
        if (context.debug) {
            add(hbox, gbc);
        }

        bgColor = Color.white; // table.getBackground();
        bgSelectionColor = table.getSelectionBackground();
        bgAlternateColor = new Color(235, 245, 252);
    }

    private Box makeManualMatchButtons() {
        // Control buttons
        Box hbox = Box.createHorizontalBox();
        unMatch = new JButton("Unmatch");
        unMatch.addActionListener(this::onUnMatch);
        unMatch.setToolTipText("Unmatch the Audio Item from the File.");
        hbox.add(unMatch);
        hbox.add(Box.createHorizontalStrut(5));
        manualMatch = new JButton("Manual Match");
        manualMatch.addActionListener(this::onManualMatch);
        manualMatch.setToolTipText("Manually match this file with an message.");
        hbox.add(manualMatch);
        hbox.add(Box.createHorizontalStrut(5));
        importAsIs = new JButton("Import File");
        importAsIs.addActionListener(this::onImportAsIs);
        importAsIs.setToolTipText("Import the file as its name.");
        hbox.add(importAsIs);

        unMatch.setEnabled(false);
        manualMatch.setEnabled(false);

        hbox.add(Box.createHorizontalGlue());

        table.getSelectionModel().addListSelectionListener(this::listSelectionListener);

        return hbox;
    }

    private void listSelectionListener(ListSelectionEvent listSelectionEvent) {
        enableButtons();
    }

    private MatchableImportableAudio selectedRow() {
        MatchableImportableAudio row = null;
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            row = model.getRowAt(modelRow);
        }
        return row;
    }

    private void onUnMatch(ActionEvent actionEvent) {
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            MatchableImportableAudio row = model.getRowAt(modelRow);
            if (row != null && row.getMatch().isMatch()) {
                matcher.unMatch(modelRow);
                model.fireTableDataChanged();
            }
        }
    }

    private void onManualMatch(ActionEvent actionEvent) {
        MatchableImportableAudio row = selectedRow();
        File audioFile = row.getRight().getFile();
        Map<String, MatchableImportableAudio> unmatchedItems = new LinkedHashMap<>();
        matcher.matchableItems
            .stream()
            .filter(m->m.getMatch()== MATCH.LEFT_ONLY)
            .forEach(m->unmatchedItems.put(m.getLeft().getTitle(), m));
        String[] unmatchedTitles = unmatchedItems.keySet().toArray(new String[0]);

        JList<String> list = new JList<>(unmatchedTitles);
        String message = new String("<html>Choose the Audio item for file '<i>"+audioFile.getName()+"</i>'.</html>");
        AudioItemSelectionDialog dialog = new AudioItemSelectionDialog("Choose Message For Import Target",
            message, list);

        String chosenTitle = dialog.show();
        if (chosenTitle != null) {
            MatchableImportableAudio chosenItem = unmatchedItems.get(chosenTitle);

            matcher.setMatch(chosenItem, row);
            model.fireTableDataChanged();
        }
    }

    private void onImportAsIs(ActionEvent actionEvent) {
        MatchableImportableAudio row = selectedRow();
    }

    private void enableButtons() {
        MatchableImportableAudio row = selectedRow();
        unMatch.setEnabled(row != null && row.getMatch().isMatch());
        manualMatch.setEnabled(row != null && row.getMatch()==MATCH.RIGHT_ONLY);
        importAsIs.setEnabled(false); // row != null && row.getMatch()==MATCH.RIGHT_ONLY);
    }

    /**
     * Creates the table and sets the model and various renderers, filters, etc.
     */
    private Component makeTable() {
        table = new MatcherTable();
        model = table.getModel();

        table.setFilter(this::filter);

        MatcherRenderer mr = new MatcherRenderer();
        table.setRenderer(Columns.Left, new AudioItemRenderer());
//        table.setRenderer(Columns.Match, new MatchRenderer());
//        table.setRenderer(Columns.Score, new ScoreRenderer());
        table.setRenderer(Columns.Update, new BooleanRenderer());
        table.setRenderer(Columns.Status, new StatusRenderer());
        table.setRenderer(Columns.Right, mr);

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(224, 224, 224));
        table.setGridColor(Color.RED);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        return scrollPane;
    }

    /**
     * Make the toolbar, with the function buttons.
     */
    private Box makeToolbar() {
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
        return hbox;
    }

    /**
     * Make the checkboxes for the visual filters.
     */
    private Box makeVisualFilters() {
        Box vbox = Box.createVerticalBox();
        Box hbox = Box.createHorizontalBox();
        hideMatched = new JCheckBox("Hide matched");
        colorCoded = new JCheckBox("Color coded", null, false);
        hbox.add(hideMatched);
        hbox.add(colorCoded);
        hbox.add(Box.createHorizontalGlue());
        vbox.add(hbox);

        hbox = Box.createHorizontalBox();
        showLeft = new JCheckBox("Left", null, true);
        showRight = new JCheckBox("Right", null, true);
        showExact = new JCheckBox("Exact", null, true);
        showFuzzy = new JCheckBox("Fuzzy", null, true);
        showToken = new JCheckBox("Token", null, true);
        showManual = new JCheckBox("Manual", null, true);

        hideMatched.addActionListener(this::onHideMatched);
        colorCoded.addActionListener(actionEvent -> model.fireTableDataChanged());

        showLeft.addActionListener(actionEvent -> model.fireTableDataChanged());
        showRight.addActionListener(actionEvent -> model.fireTableDataChanged());
        showExact.addActionListener(actionEvent -> model.fireTableDataChanged());
        showFuzzy.addActionListener(actionEvent -> model.fireTableDataChanged());
        showToken.addActionListener(actionEvent -> model.fireTableDataChanged());
        showManual.addActionListener(actionEvent -> model.fireTableDataChanged());

        // Spinner to set the fuzzy threshold between MINIMUM_THRESHOLD and 100.
        JSpinner threshold = new JSpinner();
        Dimension size = threshold.getPreferredSize();
        thresholdModel = new SpinnerNumberModel(DEFAULT_THRESHOLD, MINIMUM_THRESHOLD, 100, 1);
        threshold.setModel(thresholdModel);
        threshold.addChangeListener(e -> {
            if ((int) this.thresholdModel.getValue() != fuzzyThreshold) {
                fuzzyThreshold = (int) this.thresholdModel.getValue();
            }
        });
        size.width = 75;
        threshold.setPreferredSize(size);
        threshold.setMaximumSize(size);

        hbox.add(new JLabel("Show: "));
        hbox.add(showLeft);
        hbox.add(showRight);
        hbox.add(showExact);
        hbox.add(showFuzzy);
        hbox.add(showToken);
        hbox.add(showManual);
        hbox.add(threshold);
        hbox.add(Box.createHorizontalGlue());
        vbox.add(hbox);
        return vbox;
    }

    private JComponent makeThresholdControl() {
        final int STEP = 5;
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Match strictness: Permissive"));
        hbox.add(Box.createHorizontalStrut(5));

        JSlider slider = new JSlider(JSlider.HORIZONTAL, MINIMUM_THRESHOLD/STEP, 100/STEP, DEFAULT_THRESHOLD/STEP);
        slider.setMinorTickSpacing(1);
        slider.setMajorTickSpacing(2);
        slider.setPaintTicks(true);
        slider.addChangeListener(e -> {
            int newValue = slider.getValue() * STEP;
            if (newValue != fuzzyThreshold) {
                int oldValue = fuzzyThreshold;
                fuzzyThreshold = newValue;
                onThresholdChanged(oldValue);
            }
        });
        hbox.add(slider);

        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(new JLabel("Strict"));

        // Hack to always have the "color coded" control.
        if (!context.debug) {
            hbox.add(Box.createHorizontalStrut(20));
            hbox.add(colorCoded);
        }

        hbox.add(Box.createHorizontalGlue());

        return hbox;
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
        showManual.setSelected(!hideMatched.isSelected());
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
        case MANUAL:
            return showManual.isSelected();
        case LEFT_ONLY:
            return showLeft.isSelected();
        case RIGHT_ONLY:
            return showRight.isSelected();
        default:
            return false;
        }
    }

    private void onThresholdChanged(int oldValue) {
        // TODO: handle more-strict differently from less-strict.
        if (oldValue < fuzzyThreshold) {
            // Got more strict. Unmatch the fuzzy matches that no longer meet the criteria.
            List<MatchableImportableAudio> toUnmatch = matcher.matchableItems.stream().filter(i->{
                return i.getMatch().isNonStrictMatch() &&
                    i.getScore() < fuzzyThreshold;
            }).collect(Collectors.toList());
            toUnmatch.forEach(matcher::unMatch);
            if (!toUnmatch.isEmpty()) {
                model.fireTableDataChanged();
            }
        } else {
            // Got less strict
            autoMatch();
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
        MatchStats result = matcher.findTokenMatches(fuzzyThreshold);
        model.fireTableDataChanged();
        statusBar.setText(result.toString());
    }

    /**
     * Handles the fuzzy button.
     *
     * @param actionEvent ignored.
     */
    private void onFuzzyAction(ActionEvent actionEvent) {
        MatchStats result = matcher.findFuzzyMatches(fuzzyThreshold);
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

    private Color bgColor;
    private Color bgSelectionColor;
    private Color bgAlternateColor;

    private Color getBG(int viewRow, int viewColumn, boolean isSelected) {
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
                case MANUAL:
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
        return bg;
    }
    private Color lighten(Color c) {
        double FACTOR = 1.04;
        return new Color(Math.min((int) (c.getRed() * FACTOR), 255),
            Math.min((int) (c.getGreen() * FACTOR), 255),
            Math.min((int) (c.getBlue() * FACTOR), 255),
            c.getAlpha());
    }

    /**
     * General renderer for matchable items. Optionally performs color coding based on
     * the match state of the data.
     */
    private class MatcherRenderer extends DefaultTableCellRenderer {
        MatcherRenderer() {
            super();
        }

        private void setBackground(int viewRow, int viewColumn, boolean isSelected) {
            Color bg = getBG(viewRow, viewColumn, isSelected);
            setBackground(bg);
        }

        private Color darken(Color c) {
            double FACTOR = 0.96;
            return new Color(Math.max((int) (c.getRed() * FACTOR), 0),
                Math.max((int) (c.getGreen() * FACTOR), 0),
                Math.max((int) (c.getBlue() * FACTOR), 0),
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

    ImageIcon soundImage = new ImageIcon(
        UIConstants.getResource("sound-1.png"));
    ImageIcon newSoundImage = new ImageIcon(
        UIConstants.getResource("sound-2.png"));
    ImageIcon noSoundImage = new ImageIcon(
        UIConstants.getResource("sound-3.png"));
    private class AudioItemRenderer extends MatcherRenderer {

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
            JLabel comp = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                viewRow,
                viewColumn);
            ImageIcon icon = null;
            if (item != null && item.getLeft() != null) {
                if (item.getLeft().hasAudioItem()) {
                    icon = item.isUpdate() ? newSoundImage : soundImage;
                } else {
                    icon = item.getMatch().isMatch() ? newSoundImage : noSoundImage;
                }
            }
            comp.setIcon(icon);
            return comp;
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
            case MANUAL:
                value = "Manual";
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
            value = item.getOperation();
            String tooltip = null;
            switch (item.getMatch()) {
            case NONE:
                break;
            case EXACT:
                tooltip = "Exact match";
                break;
            case FUZZY:
                tooltip = "Fuzzy match @" + item.getScore();
                break;
            case TOKEN:
                tooltip = "Token match @" + item.getScore();
                break;
            case MANUAL:
                tooltip = "User match";
                break;
            case LEFT_ONLY:
                tooltip = "Message is missing audio content";
                break;
            case RIGHT_ONLY:
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

    private class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        private final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        private JLabel dummy;

        public BooleanRenderer() {
            super();
            dummy = new JLabel();
            dummy.setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
            setBorderPainted(true);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int viewRow, int viewColumn) {

            int row = table.convertRowIndexToModel(viewRow);
            int column = table.convertColumnIndexToModel(viewColumn);
            MatchableImportableAudio item = MatcherPanel.this.model.getRowAt(row);
            Component comp = (item.isDoReplaceEditable()) ? this : dummy;

            if (isSelected) {
                comp.setForeground(table.getSelectionForeground());
            }
            else {
                comp.setForeground(table.getForeground());
            }
            setSelected((value != null && ((Boolean)value).booleanValue()));

            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
                setBorder(noFocusBorder);
            }

            comp.setBackground(getBG(viewRow, viewColumn, isSelected));
            return comp;
        }
    }
}
