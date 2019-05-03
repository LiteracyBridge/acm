package org.literacybridge.acm.gui.assistants.Matcher;

public interface ColumnProvider<T> {

    public abstract int getColumnCount();

    public abstract String getColumnName(int columnIndex);

    public Class getColumnClass(int columnIndex);

    public abstract Object getValueAt(T data, int columnIndex);

}
