package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MatcherTableModel extends AbstractTableModel {
    public enum Columns {
        Left(ImportableAudioItem.class, "Audio Item"),
        Right(ImportableFile.class, "File"),
        Status(String.class, "Status"),
        Update(Boolean.class, "Update?");

        Columns(Class theClass, String heading) { this.theClass = theClass; this.heading = heading;}
        private Class<?> theClass;
        private String heading;
        public String heading() { return this.heading; }
        public Class<?> theClass() { return this.theClass; }
    }
    private static final String[] columnNames = Arrays
        .stream(Columns.values())
        .map(Columns::heading)
        .collect(Collectors.toList())
        .toArray(new String[0]);
    private static final Class[] columnClasses = Arrays
        .stream(Columns.values())
        .map(Columns::theClass)
        .collect(Collectors.toList())
        .toArray(new Class<?>[0]);


    private MatcherTable table;
    private List<MatchableImportableAudio> data;

    MatcherTableModel(MatcherTable table) {
        this.table = table;
    }

    @Override
    public int getRowCount() {
        if (data == null) return 0;
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MatchableImportableAudio row = getRowAt(rowIndex);
        if (row == null || columnIndex<0 || columnIndex>=Columns.values().length)
            return null;
        Columns which = Columns.values()[columnIndex];
        switch (which) {
        case Left: return row.getLeft();
        case Update: return row.getLeft()!=null && row.getLeft().isReplaceOk();
        case Status: return row.getScoredMatch();
        case Right: return row.getRight();
        default: return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != Columns.Update.ordinal()) return false;
        MatchableImportableAudio row = getRowAt(rowIndex);
        if (row == null) return false;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        return row.getMatch().isMatch() && row.getLeft().hasAudioItem();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != Columns.Update.ordinal()) return;
        MatchableImportableAudio row = getRowAt(rowIndex);
        if (row == null) return;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        if (!(row.getMatch().isMatch() && row.getLeft().hasAudioItem())) return;
        boolean v = (aValue != null && (Boolean) aValue);
        row.getLeft().setReplaceOk(v);
        super.fireTableRowsUpdated(rowIndex, rowIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    @Override
    public void fireTableDataChanged() {
        super.fireTableDataChanged();
    }

    MatchableImportableAudio getRowAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= data.size())
            return null;
        return data.get(rowIndex);
    }

    public void setData(List<MatchableImportableAudio> data) {
        this.data = data;
        fireTableDataChanged();
        table.sizeColumns();
    }

    /**
     * Set appropriate comparators on rows. The TableSorter will otherwise *cast* the items to
     * be compared to Strings, which (of course) leads to IllegalCast exceptions.
     * @param sorter on which to set the comparators.
     */
    void setupSorter(TableRowSorter<MatcherTableModel> sorter) {
        sorter.setComparator(Columns.Left.ordinal(), (ImportableAudioItem o1, ImportableAudioItem o2) -> {
            if (o1==null) return 1;
            if (o2==null) return -1;
            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
        });
        sorter.setComparator(Columns.Right.ordinal(), (ImportableFile o1, ImportableFile o2) -> {
            if (o1==null) return 1;
            if (o2==null) return -1;
            return o1.getFile().getName().compareToIgnoreCase(o2.getFile().getName());
        });
        sorter.setComparator(Columns.Update.ordinal(), (Boolean o1, Boolean o2) -> {
            if (o1==null) return 1;
            if (o2==null) return -1;
            return o1.compareTo(o2);
        });

    }


}
