package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;

import java.util.List;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<PromptMatchable> {

    ManualMatcherDialog(AbstractMatchPage<?,?,?,?> matchPage, PromptMatchable row, List<PromptMatchable> matchableItems) {
        super(matchPage, row, matchableItems);
    }

    @Override
    protected String leftDescription() {
        return "Prompt";
    }

    @Override
    protected String rightDescription() {
        return "Audio File";
    }
}
