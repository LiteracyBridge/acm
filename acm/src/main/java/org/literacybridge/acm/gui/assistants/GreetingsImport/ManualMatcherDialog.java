package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableGreeting;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<MatchableGreeting> {

    @Override
    protected String leftDescription() {
        return "Recipient";
    }

    @Override
    protected String rightDescription() {
        return "Greeting";
    }
}
