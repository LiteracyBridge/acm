package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;

import java.util.List;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<GreetingMatchable> {

    ManualMatcherDialog(AbstractMatchPage<?,?,?,?> matchPage, GreetingMatchable row, List<GreetingMatchable> matchableItems)
    {
        super(matchPage, row, matchableItems);
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
