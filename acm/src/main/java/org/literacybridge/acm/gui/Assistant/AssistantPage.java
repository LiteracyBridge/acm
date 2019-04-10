package org.literacybridge.acm.gui.Assistant;

import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The abstract base class for Assistant pages. Provides access to Assistant properties.
 *
 * Derived classes must implement 'OnPageEnterer'.
 */
public abstract class AssistantPage<Context> extends JPanel {
    // This is because the SeaGlass L&F makes combo boxes too small
    // for the content. To work around, we measure the size of an
    // empty ComboBox, and add a 5px buffer.in a page measure the size of the longest string,
    // and add a 5px buffer.
    /**
     * This is because the SeaGlass L&F makes combo boxes too small for the content. To work
     * around, we measure the size of an empty ComboBox, and add a 5px buffer.
     *
     * In each page, for each combo box, measure the size of the longest string, and set the
     * combo preferred size (and, optionally, min or max size) to the string + baseComboWidth
     */
    public static final int baseComboWidth = new JComboBox().getPreferredSize().width + 5;
    public static void setComboWidth(JComboBox cb, String string) {
        int textWidth = new JLabel("").getFontMetrics(cb.getFont()).stringWidth(string);
        Dimension size = cb.getPreferredSize();
        size.width = baseComboWidth + textWidth;
        cb.setPreferredSize(size);
    }

    protected static final Border greenBorder = new LineBorder(Color.green); //new LineBorder(new Color(0xf0f0f0));
    protected static final Border redBorder = new LineBorder(Color.RED, 1, true);
    protected static final Border blankBorder = new LineBorder(new Color(0, 0, 0, 0), 1, true);
    protected static final Border parameterBorder = new CompoundBorder(greenBorder, new EmptyBorder(2,3,2,4));

    public static JLabel parameterText() { return parameterText(null); }
    public static JLabel parameterText(String text) {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(Color.white);
        label.setBorder(parameterBorder);
        if (!StringUtils.isEmpty(text)) label.setText(text);
        return label;
    }

    /**
     * Method to size "small" header columns. This is useful for columns with fairly consistent
     * and fairly small data. Sizes the column to show every item.
     * @param table to be sized.
     * @param columnValues A Map of Integer -> Stream<String> where the integer is the
     *                     column number, and the Stream is all the items in the column.
     */
    public static void sizeColumns(JTable table, Map<Integer, Stream<Object>> columnValues) {
        sizeColumns(table, columnValues, 0);
    }
    public static void sizeColumns(JTable table, Map<Integer, Stream<Object>> columnValues, int margin) {
        TableModel model = table.getModel();
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        for (Map.Entry<Integer, Stream<Object>> e : columnValues.entrySet()) {
            final int columnNo = e.getKey();
            TableColumn column = table.getColumnModel().getColumn(columnNo);

            int headerWidth = headerRenderer.getTableCellRendererComponent(null,
                column.getHeaderValue(),
                false,
                false,
                0,
                0).getPreferredSize().width;

            int cellWidth = e.getValue()
                .map(item -> table.getDefaultRenderer(model.getColumnClass(columnNo))
                    .getTableCellRendererComponent(table, item, false, false, 0, columnNo)
                    .getPreferredSize().width)
                .max(Integer::compareTo)
                .orElse(1);

            int w = Math.max(headerWidth, cellWidth) + 2;
            column.setMaxWidth(w + margin + 40);
            column.setPreferredWidth(w + margin);
            column.setWidth(w + margin);
        }
    }

    private final Assistant.PageHelper<Context> pageHelper;
    private boolean isComplete = false;

    public AssistantPage(Assistant.PageHelper pageHelper) {
        super();
        this.pageHelper = pageHelper;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(pageHelper.getAssistant().factory.background);
    }

    protected abstract void onPageEntered(boolean progressing);
    protected void onPageLeaving(boolean progressing) {}

    protected String getTitle() { return ""; }
    protected boolean isSummaryPage() { return false; }

    /**
     * Sets the completed state of the page. The Assistant uses this state to enable/disable
     * Next / Finish buttons.
     */
    protected void setComplete() { setComplete(true); }
    protected void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
        pageHelper.onComplete(isComplete);
    }
    boolean isComplete() { return isComplete; }

    protected final Context getContext() { return pageHelper.getContext(); }
}
