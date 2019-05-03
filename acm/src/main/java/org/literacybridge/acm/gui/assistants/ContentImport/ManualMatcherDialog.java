package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<AudioMatchable> {

    @Override
    protected String leftDescription() {
        return "Audio Item";
    }

    @Override
    protected String rightDescription() {
        return "File";
    }
}
