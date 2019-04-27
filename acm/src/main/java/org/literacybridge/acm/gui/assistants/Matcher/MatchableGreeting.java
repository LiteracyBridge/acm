package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.core.spec.Recipient;

public class MatchableGreeting extends MatchableItem<GreetingsTarget, ImportableFile> {

    public MatchableGreeting(GreetingsTarget left, ImportableFile right) {
        super(left, right);
    }

    public MatchableGreeting(GreetingsTarget left,
        ImportableFile right,
        MATCH match)
    {
        super(left, right, match);
    }

    public MatchableGreeting(GreetingsTarget left,
        ImportableFile right,
        MATCH match,
        int score)
    {
        super(left, right, match, score);
    }

    public boolean containsText(String filterText) {
        if (getRight() != null && getRight().getFile().getName().toLowerCase().contains(filterText))
            return true;
        if (getLeft() != null) {
            Recipient recipient = getLeft().getRecipient();
            if (recipient.communityname.toLowerCase().contains(filterText) ||
                recipient.groupname.toLowerCase().contains(filterText) ||
                recipient.supportentity.toLowerCase().contains(filterText))
                return true;
        }
        return false;
    }

    @Override
    public MatchableGreeting disassociate() {
        MatchableGreeting disassociated = new MatchableGreeting(null, getRight(), MATCH.RIGHT_ONLY);
        setRight(null);
        setMatch(MATCH.LEFT_ONLY);
        setScore(0);
        return disassociated;
    }



}
