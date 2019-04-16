package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * Filters rows in a MatcherTable<MatchableImportableAudio>, based on a client supplied predicate.
 */
class MatcherFilter extends RowFilter<MatcherTableModel, Integer> {
    private final MatcherTableModel model;
    private boolean includeMatched = true;

    private Predicate<MatchableImportableAudio> predicate;

    MatcherFilter(MatcherTableModel model) {
        this.model = model;
    }

    void setPredicate(Predicate<MatchableImportableAudio> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean include(Entry<? extends MatcherTableModel, ? extends Integer> entry) {
        if (predicate == null) return true;
        int rowIx = entry.getIdentifier();
        MatchableImportableAudio row = model.getRowAt(rowIx);
        return predicate.test(row);
    }
}
