package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;

import java.util.List;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<AudioMatchable> {

    ManualMatcherDialog(AbstractMatchPage<?,?,?,?> matchPage,
            AudioMatchable row,
            List<AudioMatchable> matchableItems) {
        super(matchPage, row, matchableItems);
    }

    @Override
    protected String leftDescription() {
        return "Title";
    }

    @Override
    protected String rightDescription() {
        return "Audio File";
    }
}
