package org.literacybridge.acm.gui.assistants.GreetingsImport;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;

import java.util.function.BiFunction;

/**
 * Class to match custom greetings with recipients. Provides a specialized scoring function
 */
public class GreetingsMatcher extends Matcher<GreetingTarget, ImportableFile, GreetingMatchable> {

    @Override
    protected int scoreMatch(GreetingMatchable l, GreetingMatchable r, boolean tokens) {
        RecipientAdapter recipient = l.getLeft().getRecipient();
        ImportableFile file = r.getRight();

        // Which compare function are we using?
        BiFunction<String, String, Integer> fn = tokens ? FuzzySearch::tokenSortRatio : FuzzySearch::ratio;

        String fileName = FilenameUtils.removeExtension(file.getFile().getName());
        int score;

        if (StringUtils.isEmpty(recipient.groupname) && StringUtils.isEmpty(recipient.agent)) {
            score = fn.apply(recipient.communityname, fileName);
        } else {
            // There is either a group name, or an agent name, or both. We will try combinations
            // of community name, group name, agent
            StringBuilder testName = new StringBuilder(recipient.communityname).append(' ');
            if (StringUtils.isNotEmpty(recipient.groupname)) {
                // There is a group name, and maybe/maybe not an agent.
                testName.append(recipient.groupname);
                score = fn.apply(testName.toString(), fileName);

                if (StringUtils.isNotEmpty(recipient.agent)) {
                    testName.append(' ').append(recipient.agent);
                    score = Math.max(score, fn.apply(testName.toString(), fileName));
                }

                testName.delete(0, testName.length()).append(recipient.groupname).append(' ').append(recipient.communityname);
                score = Math.max(score, fn.apply(testName.toString(), fileName));

                if (StringUtils.isNotEmpty(recipient.agent)) {
                    testName.append(' ').append(recipient.agent);
                    score = Math.max(score, fn.apply(testName.toString(), fileName));
                }
            } else {
                // There is an agent, but no group name.
                testName.append(recipient.agent);
                score = fn.apply(testName.toString(), fileName);

                testName.delete(0, testName.length()).append(recipient.agent).append(' ').append(recipient.communityname);
                score = Math.max(score, fn.apply(testName.toString(), fileName));
            }
        }

        return score;
    }
}
