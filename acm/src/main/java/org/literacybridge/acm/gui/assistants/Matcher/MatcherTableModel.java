package org.literacybridge.acm.gui.assistants.Matcher;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class MatcherTableModel extends AbstractTableModel {
    public enum Columns {
        Left(String.class),
        Update(Boolean.class),
        Status(String.class),
        Right(String.class);

        Columns(Class theClass) { this.theClass = theClass; }
        Class<?> theClass;

        static String[] names = {Left.name(), Update.name(), Status.name(), Right.name()};
        static Class[] classes = {Left.theClass, Update.theClass, Status.theClass, Right.theClass};
    };
    private static final String[] columnNames = {"Content Title", "Update?", "Status", "Audio File"};
    private static final Class[] columnClasses = {String.class, Boolean.class, String.class, String.class};

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
        if (row == null)
            return null;
        switch (columnIndex) {
        case 0: return row.getLeft();
        case 1: return row.getDoUpdate();
        case 2: return row.getScoredMatch();
        case 3: return row.getRight();
        default: return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != 1) return false;
        MatchableImportableAudio row = getRowAt(rowIndex);
        if (row == null) return false;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        return (row.isDoReplaceEditable());
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 1) return;
        MatchableImportableAudio row = getRowAt(rowIndex);
        if (row == null) return;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        if (!(row.isDoReplaceEditable())) return;
        boolean v = (aValue != null && ((Boolean)aValue).booleanValue());
        row.setDoUpdate(v);
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

    void setData(List<MatchableImportableAudio> data) {
        this.data = data;
        fireTableDataChanged();
        table.sizeColumns();
    }
}
