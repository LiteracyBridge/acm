package org.literacybridge.acm.gui.assistants.Matcher;

/**
 * The information that a column provider must provide. Used to compose the columns from multiple
 * objects when an object may provide multiple columns.
 *
 * @param <T> The type of object providing these columns.
 */
public interface ColumnProvider<T> {

    int getColumnCount();

    String getColumnName(int columnIndex);

    Class getColumnClass(int columnIndex);

    Object getValueAt(T data, int columnIndex);

}
