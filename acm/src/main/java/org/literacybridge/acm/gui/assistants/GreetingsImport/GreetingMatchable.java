package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.core.spec.Recipient;

public class GreetingMatchable extends MatchableItem<GreetingTarget, ImportableFile> {

    public GreetingMatchable(GreetingTarget left, ImportableFile right) {
        super(left, right);
    }

    public GreetingMatchable(GreetingTarget left,
        ImportableFile right,
        MATCH match)
    {
        super(left, right, match);
    }

    public GreetingMatchable(GreetingTarget left,
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
                recipient.agent.toLowerCase().contains(filterText))
                return true;
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
