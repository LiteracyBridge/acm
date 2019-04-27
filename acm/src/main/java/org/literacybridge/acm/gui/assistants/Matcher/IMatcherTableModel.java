package org.literacybridge.acm.gui.assistants.Matcher;

public interface IMatcherTableModel<T> {

    T getRowAt(int rowIndex);

    boolean isLeftColumn(int columnIndex);

    boolean isRightColumn(int columnIndex);
}
