package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;

import java.util.List;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<AudioMatchable> {

    ManualMatcherDialog(AudioMatchable row, List<AudioMatchable> matchableItems) {
        super(row, matchableItems);
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
