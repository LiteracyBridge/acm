package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.core.spec.RecipientList;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

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

    private int[] recipientColumnsMap;
    int maxRecipientColumn;
    int numRecipientColumns;

    List<String> columnNames = new ArrayList<>();

    AbstractRecipientsModel(RecipientList recipients) {
        this(recipients, null);
    }

    AbstractRecipientsModel(RecipientList recipients, final Collection<String> columnsOfInterest) {
        String[] recipientColumnNames = IntStream.rangeClosed(0, recipients.getMaxLevel())
            .mapToObj(recipients::getNameOfLevel)
            .filter(it -> columnsOfInterest == null || columnsOfInterest.contains(it))
            .toArray(String[]::new);
        recipientColumnsMap = recipients.getAdapterIndicesForValues(recipientColumnNames);
        numRecipientColumns = recipientColumnNames.length;
        maxRecipientColumn = numRecipientColumns-1;

        columnNames.addAll(Arrays.asList(recipientColumnNames));
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RecipientList.RecipientAdapter recipient = getRecipientAt(rowIndex);
        if (recipient == null) return "";
        return recipient.getValue(recipientColumnsMap[columnIndex]);
    }

    /**
     * Sub-classes override this to provide the RecipientAdapter for a given row.
     * @param rowIndex for which the Recipient is needed.
     * @return the recipient.
     */
    abstract RecipientList.RecipientAdapter getRecipientAt(int rowIndex);
}

