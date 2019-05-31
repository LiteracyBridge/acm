package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;

import java.util.List;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<PromptMatchable> {

    ManualMatcherDialog(PromptMatchable row, List<PromptMatchable> matchableItems) {
        super(row, matchableItems);
    }

    @Override
    protected String leftDescription() {
        return "Prompt";
    }

    @Override
    protected String rightDescription() {
        return "Audio";
    }
}
