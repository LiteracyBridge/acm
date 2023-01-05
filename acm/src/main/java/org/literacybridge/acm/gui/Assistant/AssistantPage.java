package org.literacybridge.acm.gui.Assistant;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.MetadataStore;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The abstract base class for Assistant pages. Provides access to Assistant properties.
 *
 * Various pages of an Assistant can communicate with each other through their shared Context
 * object.
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
    private static final int baseComboWidth = new JComboBox<String>().getPreferredSize().width + 6;

    public static void setComboWidth(JComboBox<String> cb, String... strings) {
        List<String> stringsList = Arrays.asList(strings);
        setComboWidth(cb, stringsList);
    }
    protected static void setComboWidth(JComboBox<String> cb, Collection<String> strings, String string) {
        Collection<String> allStrings = new HashSet<>(strings);
        allStrings.add(string);
        setComboWidth(cb, allStrings);
    }
    protected static void setComboWidth(JComboBox<String> cb, String string) {
        setComboWidth(cb, Collections.singleton(string));
    }
    private static void setComboWidth(JComboBox<String> cb, Collection<String> strings) {
        int textWidth = getMaxWidthForWidget(cb, strings);
        Dimension size = cb.getPreferredSize();
        size.width = baseComboWidth + textWidth;
        cb.setPreferredSize(size);
    }

    public static int getMaxWidthForWidget(Container c, Collection<String> strings) {
        FontMetrics m = new JLabel("").getFontMetrics(c.getFont());
        int textWidth = 0;
        for (String string : strings) {
            textWidth = Math.max(textWidth, m.stringWidth(string));
        }
        return textWidth;
    }

    private static final Border greenBorder = new LineBorder(Color.green); //new LineBorder(new Color(0xf0f0f0));
    public static final Border redBorder = new RoundedLineBorder(Color.RED, 1, 8);
    protected static final Border blankBorder = new RoundedLineBorder(new Color(0, 0, 0, 0), 1, 4);
    private static final Border parameterBorder = new CompoundBorder(greenBorder, new EmptyBorder(2,3,2,4));

    protected static JLabel makeBoxedLabel() { return makeBoxedLabel(null); }
    protected static JLabel makeBoxedLabel(String text) {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(Color.white);
        label.setBorder(parameterBorder);
        if (!StringUtils.isEmpty(text)) label.setText(text);
        return label;
    }

    public static class SizingParams {
        public static final int IGNORE = Integer.MIN_VALUE;

        public SizingParams() {
            this(-1);
        }
        SizingParams(int modelColumn) {
            this.modelColumn = modelColumn;
            this.minPadding = IGNORE;
            this.preferredPadding = 2;
            this.maxPadding = 42;
        }
        public SizingParams(int modelColumn,
            int minPadding,
            int preferredPadding,
            int maxPadding)
        {
            this.modelColumn = modelColumn;
            this.minPadding = minPadding;
            this.preferredPadding = preferredPadding;
            this.maxPadding = maxPadding;
        }
        public SizingParams(int minPadding, int preferredPadding, int maxPadding) {
            this.modelColumn = -1;
            this.minPadding = minPadding;
            this.preferredPadding = preferredPadding;
            this.maxPadding = maxPadding;
        }

        public List<SizingParams> forColumns(Integer... columnNos) {
            @SuppressWarnings("UnnecessaryLocalVariable")
            List<SizingParams> params = Arrays.stream(columnNos)
                .map(n -> new SizingParams(n, minPadding, preferredPadding, maxPadding))
                .collect(Collectors.toList());
            return params;
        }

        int modelColumn;
        int minPadding;
        int preferredPadding;
        int maxPadding;
    }
    /**
     * Method to size "small" header columns. This is useful for columns with fairly consistent
     * and fairly small data. Sizes the column to show every item.
     * @param table to be sized.
     * @param columns a list of model column numbers to be sized.
     */
    public static Dimension sizeColumns(JTable table, Integer... columns) {
        List<SizingParams> params = Arrays.stream(columns).map(SizingParams::new).collect(
            Collectors.toList());
        return sizeColumns(table, params);
    }

    public static Dimension sizeColumns(JTable table, List<SizingParams> params) {
        TableModel model = table.getModel();
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        int totalPreferredWidth = 0;
        int[] cellHeight = new int[]{0};

        for (SizingParams param : params) {
            // To get the column class from the model we need to use the model column index. But to
            // get the render component we need to use the view index.
            final int modelColumnNo = param.modelColumn;
            final int viewColumnNo = table.convertColumnIndexToView(modelColumnNo);
            TableColumn tableColumn = table.getColumnModel().getColumn(viewColumnNo);

            int headerWidth = headerRenderer.getTableCellRendererComponent(null,
                tableColumn.getHeaderValue(),
                false,
                false,
                0,
                0).getPreferredSize().width;

            // Determine which table cell renderer is expected.
            Class<?> columnClass = model.getColumnClass(modelColumnNo);
            TableCellRenderer columnClassRenderer = table.getDefaultRenderer(columnClass);
            TableCellRenderer columnRenderer = tableColumn.getCellRenderer();
            TableCellRenderer columnActualRenderer = columnRenderer != null ? columnRenderer : columnClassRenderer;

            // For each row, render the value, and get the width. Take the largest width.
            int cellWidth = IntStream.range(0, table.getRowCount())
                .mapToObj(rowNo ->
                    columnActualRenderer
                        .getTableCellRendererComponent(table, table.getValueAt(rowNo, viewColumnNo), false, false, rowNo, viewColumnNo)
                        .getPreferredSize())
                .map(d->{cellHeight[0]=Math.max(d.height, cellHeight[0]);return d.width;})
                .max(Integer::compareTo)
                .orElse(0);

            int w = Math.max(headerWidth, cellWidth) + 2;
            totalPreferredWidth += w;
            if (param.minPadding != SizingParams.IGNORE) tableColumn.setMinWidth(w + param.minPadding);
            if (param.maxPadding != SizingParams.IGNORE) tableColumn.setMaxWidth(w + param.maxPadding);
            if (param.preferredPadding != SizingParams.IGNORE) {
                tableColumn.setPreferredWidth(w + param.preferredPadding);
                totalPreferredWidth += param.preferredPadding;
            }
        }
        return new Dimension(totalPreferredWidth, cellHeight[0]);
    }

    /**
     * Creates "standard" GridBagConstraints for a grid-based Assistant page. Optimized for
     * one column, so each item is it's own row, but is a good starting point 
     * @return a "standard" GridBagConstraints object.
     */
    public static GridBagConstraints getGBC() {
        Insets insets = new Insets(0,0,15,0);
        @SuppressWarnings("UnnecessaryLocalVariable")
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);
        return gbc;
    }

    protected MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

    protected static DateTimeFormatter localDateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(ZoneId.systemDefault());

    private final Assistant.PageHelper<Context> pageHelper;
    private boolean isComplete = false;

    public AssistantPage(Assistant.PageHelper<Context> pageHelper) {
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

    protected void goToLastPage() {
        pageHelper.goToLastPage();
    }

    protected void cancelAssistant() {
        pageHelper.cancelAssistant();
    }

    // Helpers to convert between decorated and un-decorated playlist names.

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<T>() {
            public T next() {
                return e.nextElement();
            }

            public boolean hasNext() {
                return e.hasMoreElements();
            }
        }, Spliterator.ORDERED), false);
    }



}
