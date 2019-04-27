package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.IMatcherTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableGreeting;
import org.literacybridge.core.spec.RecipientList;

import java.util.Arrays;

class GreetingsMatchModel extends AbstractRecipientsModel implements
                                                          IMatcherTableModel<MatchableGreeting> {
    private GreetingsMatchPage greetingsMatchPage;
    int statusColumnNo;
    int replaceColumnNo;
    private int fileColumnNo;

    GreetingsMatchModel(GreetingsMatchPage greetingsMatchPage, String[] columnsOfInterest) {
        super(greetingsMatchPage.getRecipients(), Arrays.asList(columnsOfInterest));
        this.greetingsMatchPage = greetingsMatchPage;

        statusColumnNo = maxRecipientColumn+1;
        replaceColumnNo = statusColumnNo+1;
        fileColumnNo = replaceColumnNo+1;

        String[] NAMES = { "Status", "Replace?", "File" };
        columnNames.addAll(Arrays.asList(NAMES));
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == replaceColumnNo) {
            return Boolean.class;
        }
        return super.getColumnClass(columnIndex);
    }

    @Override
    public boolean isLeftColumn(int columnIndex) {
        return columnIndex <= maxRecipientColumn;
    }

    @Override
    public boolean isRightColumn(int columnIndex) {
        return columnIndex == fileColumnNo;
    }

    @Override
    public int getRowCount() {
        return greetingsMatchPage.getMatchableItems().size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MatchableGreeting item = getRowAt(rowIndex);

        // One of ours?
        if (columnIndex > maxRecipientColumn) {
            columnIndex -= numRecipientColumns;
            switch (columnIndex) {
            case 0:
                return item.getOperation();
            case 1:
                return item.getLeft()!=null && item.getLeft().isReplaceOk();
            case 2:
                if (item.getMatch() == MATCH.LEFT_ONLY) return "";
                return item.getRight().toString();
            }
            return "???";
        }
        // Anything for the base class to even get?
        if (item.getMatch() == MATCH.RIGHT_ONLY) return "";
        return super.getValueAt(rowIndex, columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != replaceColumnNo) return false;
        MatchableGreeting row = getRowAt(rowIndex);
        if (row == null) return false;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        return row.getMatch().isMatch() && row.getLeft().targetExists();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != replaceColumnNo) return;
        MatchableGreeting row = getRowAt(rowIndex);
        if (row == null) return;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        if (!(row.getMatch().isMatch() && row.getLeft().targetExists())) return;
        boolean v = (aValue != null && (Boolean) aValue);
        row.getLeft().setReplaceOk(v);
        super.fireTableRowsUpdated(rowIndex, rowIndex);
    }

    @Override
    public MatchableGreeting getRowAt(int rowIndex) {
        return greetingsMatchPage.getMatchableItems().get(rowIndex);
    }

    @Override
    RecipientList.RecipientAdapter getRecipientAt(int rowIndex) {
        return getRowAt(rowIndex).getLeft().getRecipient();
    }
}
