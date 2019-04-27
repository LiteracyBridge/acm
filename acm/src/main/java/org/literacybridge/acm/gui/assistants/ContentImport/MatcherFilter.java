package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.MatchableAudio;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * Filters rows in a MatcherTable<MatchableAudio>, based on a client supplied predicate.
 */
class MatcherFilter extends RowFilter<MatcherTableModel, Integer> {
    private final MatcherTableModel model;
    private boolean includeMatched = true;

    private Predicate<MatchableAudio> predicate;

    MatcherFilter(MatcherTableModel model) {
        this.model = model;
    }

    void setPredicate(Predicate<MatchableAudio> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean include(Entry<? extends MatcherTableModel, ? extends Integer> entry) {
        if (predicate == null) return true;
        int rowIx = entry.getIdentifier();
        MatchableAudio row = model.getRowAt(rowIx);
        return predicate.test(row);
    }
}
