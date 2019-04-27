package org.literacybridge.acm.gui.assistants.Matcher;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * This class provides support for dragging and dropping between "right" and "left" columns
 * of a JTable of MatchableItems. The intent is that an unmatched left can be dropped onto
 * an unmatched right, or vice-versa. It is up to the client class what that actually means,
 * but the simple expectation is that the drop creates a match.
 * @param <T> The type of the MatchableItem sub-class.
 */
@SuppressWarnings("unchecked")
public abstract class MatcherTableTransferHandler<T extends MatchableItem> extends TransferHandler {
    private DataFlavor matchableFlavor = new DataFlavor(MatchableItem.class,"MatchableItem");

    private class MatchableSelection implements Transferable {
        final T matchable;
        MatchableSelection(T matchable) {
            this.matchable = matchable;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { matchableFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(matchableFlavor);
        }

        @Override
        public T getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException
        {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return matchable;
        }
    }


    private final JTable table;
    private final IMatcherTableModel<T> model;

    protected MatcherTableTransferHandler(JTable table, IMatcherTableModel<T> model) {
        super();
        this.table = table;
        this.model = model;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY | LINK | MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        T row = selectedRow();
        if (row == null) return null;
        int viewCol = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        int modelCol = table.convertColumnIndexToModel(viewCol);

        // Can only drag from a left-ish column of a left-only row or a right-ish column of a right-only row.
        if (isColumnUnmatchedInRow(modelCol, row)) {
            return new MatchableSelection(row);
        }
        return null;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        // This basically means "is drop ok".
        T sourceRow = null;
        boolean can = false;
        try {
            // If the transfer isn't what we expect, we'll catch the exception and reject the drop.
            sourceRow = (T) support.getTransferable().getTransferData(matchableFlavor);
        } catch (Exception ignored) {}
        // If from ourself, do not accept the drop.
        if (sourceRow == null) {
            support.setShowDropLocation(false);
            return false;
        }
        // Has to be dropping onto ourself...
        if (support.getDropLocation() instanceof JTable.DropLocation) {
            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            if (dropLocation.getRow() >= 0 && dropLocation.getColumn() >= 0) {
                int targetModelRow = table.convertRowIndexToModel(dropLocation.getRow());
                T targetRow = model.getRowAt(targetModelRow);
                // Non null and one left-only & one right-only?
                if (sourceRow.isMatchableWith(targetRow)) {
                    int targetModelColumn = table.convertColumnIndexToModel(dropLocation.getColumn());
                    // Does the drop column match the drop row left/right - ness?
                    if (isColumnUnmatchedInRow(targetModelColumn, targetRow)) {
                        can = true;
                    }
                }
            }
        }
        support.setShowDropLocation(can);
        return can;
    }

    @Override
    public boolean importData(TransferSupport support) {
        T sourceRow;
        try {
            sourceRow = (T) support.getTransferable().getTransferData(matchableFlavor);
        } catch (Exception ignored) {
            return false;
        }
        JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
        int targetModelRow = table.convertRowIndexToModel(dropLocation.getRow());
        T targetRow = model.getRowAt(targetModelRow);
        onMatched(sourceRow, targetRow);
        return true;
    }

    /**
     * Is a column a left-ish column in a LEFT_ONLY row, or a right-ish column in a RIGHT_ONLY? This
     * is a bit more complicated because there can be multiple columns on the left or right (think
     * of a Recipient, with community, group, and agent columns).
     * @param column the model column number.
     * @param row The row in question.
     * @return true if the column is the only data carrying column in the row.
     */
    private boolean isColumnUnmatchedInRow(int column, T row) {
        return (model.isLeftColumn(column) && row.getMatch() == MATCH.LEFT_ONLY
             || model.isRightColumn(column) && row.getMatch() == MATCH.RIGHT_ONLY);
    }

    /**
     * Helper to get the row currently selected in the table.
     * @return the row.
     */
    private T selectedRow() {
        T row = null;
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            row = model.getRowAt(modelRow);
        }
        return row;
    }

    /**
     * Called to notify the client that a drop has been performed.
     * @param sourceRow The row being dragged.
     * @param targetRow Where it was dropped.
     */
    public abstract void onMatched(T sourceRow, T targetRow);

}
