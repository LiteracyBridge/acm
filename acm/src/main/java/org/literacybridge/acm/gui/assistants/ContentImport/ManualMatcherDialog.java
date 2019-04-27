package org.literacybridge.acm.gui.assistants.ContentImport;

import javafx.collections.ObservableList;
import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableGreeting;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<MatchableAudio> {

    @Override
    protected String leftDescription() {
        return "Audio Item";
    }

    @Override
    protected String rightDescription() {
        return "File";
    }
}
