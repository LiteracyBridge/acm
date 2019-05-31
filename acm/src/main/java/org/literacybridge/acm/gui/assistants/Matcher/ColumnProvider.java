package org.literacybridge.acm.gui.assistants.Matcher;

public interface ColumnProvider<T> {

    int getColumnCount();

    String getColumnName(int columnIndex);

    Class getColumnClass(int columnIndex);

    Object getValueAt(T data, int columnIndex);

}
