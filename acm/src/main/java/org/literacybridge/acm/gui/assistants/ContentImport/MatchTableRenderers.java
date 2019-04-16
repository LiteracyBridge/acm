package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

class MatchTableRenderers {
    private static Color selectionColor = new Color(0xFA8072);
    private static Color exactColor = new Color(0xffffe0);
    private static Color fuzzyColor = new Color(0xfff0ff);
    private static Color tokenColor = new Color(0xe8ffff);
    private static Color leftColor = new Color(0xFFC0CB);
    private static Color rightColor = new Color(0xADD8E6);

    static boolean colorCodeMatches = false;
    static boolean isColorCoded = true;

    private MatcherTable table;
    private MatcherTableModel model;

    MatchTableRenderers(MatcherTable table) {
        this.table = table;
        model = this.table.getModel();
    }

    MatcherRenderer getMatcherRenderer() {
        return new MatcherRenderer();
    }
    AudioItemRenderer getAudioItemRenderer() {
        return new AudioItemRenderer();
    }
    StatusRenderer getStatusRenderer() {
        return new StatusRenderer();
    }
    UpdatableRenderer getUpdatableRenderer() {
        return new UpdatableRenderer();
    }

    private Color getBG(int viewRow, int viewColumn, boolean isSelected) {
        Color bg = (viewRow%2 == 0) ? ContentImportPage.bgColor : ContentImportPage.bgAlternateColor;
        Color custom = null;
        if (!isColorCoded) {
            if (isSelected) bg = ContentImportPage.bgSelectionColor;
//                bg = isSelected ? bgSelectionColor : bgColor;
//                if (viewRow % 2 == 1 && !isSelected) bg = bgAlternateColor; // darken(bg);
        } else {
            int selectedRow = table.getSelectionModel().getLeadSelectionIndex();
            int selectedColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            if (selectedRow == viewRow && selectedColumn == viewColumn) isSelected = true;

            if (isSelected) {
                bg = selectionColor;
            } else {
                int row = table.convertRowIndexToModel(viewRow);
                int column = table.convertColumnIndexToModel(viewColumn);
                MatchableImportableAudio item = model.getRowAt(row);
                if (item == null) {
                    return bg;
                }
                switch (item.getMatch()) {
                case EXACT:
                case MANUAL:
                    if (colorCodeMatches)
                        custom = exactColor;
                    break;
                case FUZZY:
                    if (colorCodeMatches)
                        custom = fuzzyColor;
                    break;
                case TOKEN:
                    if (colorCodeMatches)
                        custom = tokenColor;
                    break;
                case LEFT_ONLY:
                    if (column == MatcherTableModel.Columns.Left.ordinal()) custom = leftColor;
                    break;
                case RIGHT_ONLY:
                    if (column == MatcherTableModel.Columns.Right.ordinal()) custom = rightColor;
                    break;
                }
            }
            if (viewRow % 2 == 1 && !isSelected) custom = alternateColor(custom);
        }
        return custom != null ? custom : bg;
    }
    private Color alternateColor(Color color) {
        if (color == null) return null;
        int r=color.getRed();
        int g=color.getGreen();
        int b=color.getBlue();
        //int a=color.getAlpha();
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        if (cmax > 128) return lighten(color);
        else return darken(color);
    }
    private Color lighten(Color color) {
        double FACTOR = 1.04;
        return new Color(Math.min((int) (color.getRed() * FACTOR), 255),
            Math.min((int) (color.getGreen() * FACTOR), 255),
            Math.min((int) (color.getBlue() * FACTOR), 255),
            color.getAlpha());
    }

    private Color darken(Color color) {
        double FACTOR = 0.96;
        return new Color(Math.max((int) (color.getRed() * FACTOR), 0),
            Math.max((int) (color.getGreen() * FACTOR), 0),
            Math.max((int) (color.getBlue() * FACTOR), 0),
            color.getAlpha());
    }


    /**
     * General renderer for matchable items. Optionally performs color coding based on
     * the match state of the data.
     */
    public class MatcherRenderer extends DefaultTableCellRenderer {
        MatcherRenderer() {
            super();
        }

        private void setBackground(int viewRow, int viewColumn, boolean isSelected) {
            Color bg = getBG(viewRow, viewColumn, isSelected);
            setBackground(bg);
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

    public class AudioItemRenderer extends MatcherRenderer {

        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            int modelRow = table.convertRowIndexToModel(row);
            MatchableImportableAudio item = model.getRowAt(modelRow);
            JLabel comp = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus, row, column);
            ImageIcon icon = null;
            if (item != null && item.getLeft() != null) {
                if (item.getLeft().hasAudioItem() || item.getMatch().isMatch()) {
                    icon = ContentImportPage.soundImage;
                } else {
                    icon = ContentImportPage.noSoundImage;
                }
            }
            comp.setIcon(icon);
            return comp;
        }

    }

    public class StatusRenderer extends MatcherRenderer {
        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            int modelRow = table.convertRowIndexToModel(row);
            MatchableImportableAudio item = model.getRowAt(modelRow);
            String tooltip = null;
            if (item != null) {
                value = item.getOperation();
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
            }
            JLabel label = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus, row, column);
            label.setToolTipText(tooltip);
            return label;
        }
    }

    public class UpdatableRenderer extends JCheckBox implements TableCellRenderer {
        private final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        // When we don't want to display a checkbox, we return the dummy label instead of "this" checkbox.
        private JLabel dummy;

        UpdatableRenderer() {
            super();
            dummy = new JLabel();
            dummy.setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
            setBorderPainted(true);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

            int modelRow = table.convertRowIndexToModel(row);
            MatchableImportableAudio item = MatchTableRenderers.this.model.getRowAt(modelRow);
            boolean editable = item != null && item.getLeft() != null && item.getLeft().hasAudioItem()
                && item.getMatch() != null && item.getMatch().isMatch();
            Component comp = editable ? this : dummy;

            if (isSelected) {
                comp.setForeground(table.getSelectionForeground());
            }
            else {
                comp.setForeground(table.getForeground());
            }
            setSelected((value != null && (Boolean) value));

            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
                setBorder(noFocusBorder);
            }

            comp.setBackground(getBG(row, column, isSelected));
            return comp;
        }
    }
}
