package org.literacybridge.acm.gui.assistants.ContentImport;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * Filters rows in a MatcherTable<AudioMatchable>, based on a client supplied predicate.
 */
class MatcherFilter extends RowFilter<MatcherTableModel, Integer> {
    private final MatcherTableModel model;

    private Predicate<AudioMatchable> predicate;

    MatcherFilter(MatcherTableModel model) {
        this.model = model;
    }

    void setPredicate(Predicate<AudioMatchable> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean include(Entry<? extends MatcherTableModel, ? extends Integer> entry) {
        if (predicate == null) return true;
        int rowIx = entry.getIdentifier();
        AudioMatchable row = model.getRowAt(rowIx);
        return predicate.test(row);
    }
}
