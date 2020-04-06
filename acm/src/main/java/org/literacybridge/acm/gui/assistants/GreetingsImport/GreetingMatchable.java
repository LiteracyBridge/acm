package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableFileItem;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.core.spec.Recipient;

public class GreetingMatchable extends MatchableFileItem<GreetingTarget, GreetingFile> {

    GreetingMatchable(GreetingTarget left, GreetingFile right) {
        super(left, right);
    }

    private GreetingMatchable(GreetingTarget left, GreetingFile right, MATCH match)
    {
        super(left, right, match);
    }

    boolean containsText(String filterText) {
        if (getRight() != null && getRight().getTitle().toLowerCase().contains(filterText))
            return true;
        if (getLeft() != null) {
            Recipient recipient = getLeft().getRecipient();
            return recipient.communityname.toLowerCase().contains(filterText) ||
                recipient.groupname.toLowerCase().contains(filterText) ||
                recipient.agent.toLowerCase().contains(filterText);
        }
        return false;
    }

    @Override
    public GreetingMatchable disassociate() {
        GreetingMatchable disassociated = new GreetingMatchable(null, getRight(), MATCH.RIGHT_ONLY);
        setRight(null);
        setMatch(MATCH.LEFT_ONLY);
        setScore(0);
        return disassociated;
    }



}
