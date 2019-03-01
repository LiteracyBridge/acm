package org.literacybridge.acm.gui.assistants.Matcher;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class MatcherTableModel extends AbstractTableModel {
    public enum Columns {
        Left(String.class),
//        Match(MATCH.class),
//        Score(Integer.class),
        Status(String.class),
        Right(String.class);

        Columns(Class theClass) { this.theClass = theClass; }
        Class<?> theClass;

//        static String[] names = {Left.name(), Match.name(), Score.name(), Right.name()};
//        static Class[] classes = {Left.theClass, Match.theClass, Score.theClass, Right.theClass};
        static String[] names = {Left.name(), Status.name(), Right.name()};
        static Class[] classes = {Left.theClass, Status.theClass, Right.theClass};
    };
//    private static final String[] columnNames = {"Left", "Match", "Score", "Right"};
//    private static final Class[] columnClasses = {String.class, MATCH.class, Integer.class, String.class};
    private static final String[] columnNames = {"Content Title", "Status", "Audio File"};
    private static final Class[] columnClasses = {String.class, String.class, String.class};

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
//        case 1: return row.getMatch();
//        case 2: return row.getScore();
        case 1: return row.getScoredMatch();
        case 2: return row.getRight();
        default: return null;
        }
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
