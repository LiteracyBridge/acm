package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.ContentImport.MatcherTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * Filters rows in a MatcherTable<T>, based on a client supplied predicate.
 * @param <T> The type of the MatcherTable.
 */
class MatcherFilter<T extends MatchableItem<?,?>> extends RowFilter<MatcherTableModel, Integer> {
    private final MatcherTableModel model;
    private boolean includeMatched = true;

    private Predicate<T> predicate;

    MatcherFilter(MatcherTableModel model) {
        this.model = model;
    }

    void setPredicate(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean include(Entry<? extends MatcherTableModel, ? extends Integer> entry) {
        if (predicate == null) return true;
        int rowIx = entry.getIdentifier();
        T row = (T)model.getRowAt(rowIx);
        return predicate.test(row);
    }
}
