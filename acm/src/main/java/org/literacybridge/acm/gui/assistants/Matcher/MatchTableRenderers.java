package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.gui.assistants.ContentImport.AudioPlaylistTarget;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class MatchTableRenderers<T extends MatchableItem> {
    private static Color selectionColor = new Color(0xFA8072);
    private static Color exactColor = new Color(0xffffe0);
    private static Color fuzzyColor = new Color(0xfff0ff);
    private static Color tokenColor = new Color(0xe8ffff);
    private static Color leftColor = new Color(0xFFC0CB);
    private static Color rightColor = new Color(0xADD8E6);

    public static boolean colorCodeMatches = false;
    public static boolean isColorCoded = true;

    private JTable table;
    private IMatcherTableModel<T> model;

    public MatchTableRenderers(JTable table, IMatcherTableModel<T> model) {
        this.table = table;
        this.model = model;
    }

    public MatcherRenderer getMatcherRenderer() {
        return new MatcherRenderer();
    }
    public AudioItemRenderer getAudioItemRenderer() {
        return new AudioItemRenderer();
    }
    public StatusRenderer getStatusRenderer() {
        return new StatusRenderer();
    }
    public UpdatableBooleanRenderer getUpdatableBooleanRenderer() {
        return new UpdatableBooleanRenderer();
    }

    protected Color getBG(int viewRow, int viewColumn, boolean isSelected) {
        Color bg = (viewRow%2 == 0) ? AcmAssistantPage.bgColor : AcmAssistantPage.bgAlternateColor;
        Color custom = null;
        if (!isColorCoded) {
            if (isSelected) bg = AcmAssistantPage.bgSelectionColor;
//                bg = isSelected ? bgSelectionColor : bgColor;
//                if (viewRow % 2 == 1 && !isSelected) bg = bgAlternateColor; // darken(bg);
        } else {
            int selectedViewRow = table.getSelectionModel().getLeadSelectionIndex();
            int selectedViewColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int selectedColumn = table.convertColumnIndexToModel(selectedViewColumn);
            int column = table.convertColumnIndexToModel(viewColumn);
            if (selectedViewRow == viewRow && (selectedColumn == column
                                              || model.isLeftColumn(selectedColumn) && model.isLeftColumn(column)
                                              || model.isRightColumn(selectedColumn) && model.isRightColumn(column))) {
                isSelected = true;
            }

            if (isSelected) {
                bg = selectionColor;
            } else {
                int row = table.convertRowIndexToModel(viewRow);
                T item = model.getRowAt(row);
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
                    if (model.isLeftColumn(column) && !item.getLeft().targetExists()) custom = leftColor;
                    break;
                case RIGHT_ONLY:
                    if (model.isRightColumn(column)) custom = rightColor;
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

        private final Font normalFont;
        private final Font italicFont;

        AudioItemRenderer() {
            super();
            normalFont = getFont();
            italicFont = new Font(normalFont.getName(),
                normalFont.getStyle()|Font.ITALIC,
                normalFont.getSize());
        }
        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            int modelRow = table.convertRowIndexToModel(row);
            T item = model.getRowAt(modelRow);
            JLabel comp = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus, row, column);
            ImageIcon icon = null;
            if (item != null && item.getLeft() != null) {
                if (item.getLeft().targetExists() || item.getMatch().isMatch()) {
                    icon = AcmAssistantPage.soundImage;
                } else {
                    icon = AcmAssistantPage.noSoundImage;
                }
                setFont(item.getLeft() instanceof AudioPlaylistTarget ? italicFont : normalFont);
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
            T item = model.getRowAt(modelRow);
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

    public class UpdatableBooleanRenderer extends JCheckBox implements TableCellRenderer {
        private final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        // When we don't want to display a checkbox, we return the dummy label instead of "this" checkbox.
        private JLabel dummy;

        UpdatableBooleanRenderer() {
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
            T item = MatchTableRenderers.this.model.getRowAt(modelRow);
            boolean editable = item != null && item.getLeft() != null && item.getLeft().targetExists()
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
