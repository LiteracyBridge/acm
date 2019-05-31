package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;

import java.util.List;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<GreetingMatchable> {

    ManualMatcherDialog(GreetingMatchable row, List<GreetingMatchable> matchableItems)
    {
        super(row, matchableItems);
    }

    @Override
    protected String leftDescription() {
        return "Recipient";
    }

    @Override
    protected String rightDescription() {
        return "Greeting";
    }
}
