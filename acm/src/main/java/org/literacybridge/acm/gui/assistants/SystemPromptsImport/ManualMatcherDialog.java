package org.literacybridge.acm.gui.assistants.SystemPromptsImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<PromptMatchable> {

    @Override
    protected String leftDescription() {
        return "Prompt";
    }

    @Override
    protected String rightDescription() {
        return "Audio";
    }
}
