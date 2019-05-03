package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.core.spec.RecipientList.RecipientAdapter;

import javax.swing.table.AbstractTableModel;

/**
 * Abstract base class for a Table Model with a RecipientAdapter as the first one or more columns.
 */
abstract class AbstractRecipientsModel extends AbstractTableModel {
    /**
     * A RecipientAdapter object has a "getValue(ix)" method to return values of the
     * hierarchy. This is used by super classes to provide names the levels of the
     * hierarchy, despite having no knowledge of the actual data.
     *
     * The values of ix correspond to the values of NAMES in the RecipientList.
     *
     * We take advantage of that here by determining the levels in the hierarchy, and
     * calling getNameOfLevel() for each level, then pass the list of level names to
     * a helper that returns the value indices. Thus we know the names of the relevant
     * levels in the hierarchy, which we use as column headers. And we know the indices
     * for those columns, which we use to get the values for the cells.
     *
     * The constructor takes an optional set of "columns of interest"; if one is passed,
     * only names in that collection are added to the model.
     */

    private GreetingsImportContext.RecipientColumnProvider recipientColumnProvider;

    AbstractRecipientsModel(GreetingsImportContext.RecipientColumnProvider recipientColumnProvider) {
        this.recipientColumnProvider = recipientColumnProvider;
    }

    @Override
    public int getColumnCount() {
        return recipientColumnProvider.getColumnCount();
    }

    @Override
    public String getColumnName(int column) {
        return recipientColumnProvider.getColumnName(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RecipientAdapter target = getRecipientAt(rowIndex);
        return recipientColumnProvider.getValueAt(target, columnIndex);
    }

    /**
     * Sub-classes override this to provide the RecipientAdapter for a given row.
     * @param rowIndex for which the Recipient is needed.
     * @return the recipient.
     */
    abstract RecipientAdapter getRecipientAt(int rowIndex);
}

