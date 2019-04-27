package org.literacybridge.acm.gui.assistants.Matcher;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;

public class GreetingsMatcher extends
        Matcher<GreetingsTarget, ImportableFile, MatchableGreeting> {

    @Override
    protected int scoreMatch(MatchableGreeting l, MatchableGreeting r, boolean tokens) {
        RecipientAdapter recipient = l.getLeft().getRecipient();
        ImportableFile file = r.getRight();

        String fileName = FilenameUtils.removeExtension(file.getFile().getName());
        int score = 0; // so far

        String recipName = recipient.communityname;
        if (tokens) {
            score = FuzzySearch.tokenSortRatio(recipName, fileName);
        } else {
            score = FuzzySearch.ratio(recipName, fileName);
        }

        int test;
        if (StringUtils.isNotEmpty(recipient.groupname)) {
            recipName = recipient.communityname + " - " + recipient.groupname;
            if (tokens) {
                test = FuzzySearch.tokenSortRatio(recipName, fileName);
            } else {
                test = FuzzySearch.ratio(recipName, fileName);
            }
            score = Math.max(test, score);
            recipName = recipient.groupname + " - " + recipient.communityname;
            if (tokens) {
                test = FuzzySearch.tokenSortRatio(recipName, fileName);
            } else {
                test = FuzzySearch.ratio(recipName, fileName);
            }
            score = Math.max(test, score);
        }

        return score;
    }
}
