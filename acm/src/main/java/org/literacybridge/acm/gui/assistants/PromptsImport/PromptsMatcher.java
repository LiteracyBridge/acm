package org.literacybridge.acm.gui.assistants.PromptsImport;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;

import java.util.function.BiFunction;

public class PromptsMatcher extends Matcher<PromptTarget, ImportableFile, PromptMatchable> {

    @Override
    protected int scoreMatch(PromptMatchable l, PromptMatchable r, boolean tokens) {
        PromptTarget prompt = l.getLeft();
        ImportableFile file = r.getRight();

        // Which compare function are we using?
        BiFunction<String, String, Integer> fn = tokens ?
                                                 FuzzySearch::tokenSortRatio :
                                                 FuzzySearch::ratio;

        String fileName = FilenameUtils.removeExtension(file.getFile().getName());

        // Try to match with both the prompt id and definition.
        int score = fn.apply(prompt.getPromptId(), fileName);
        score = Math.max(score, fn.apply(prompt.getPromptFilename(), fileName));
        score = Math.max(score, fn.apply(prompt.getPromptText(), fileName));

        if (tokens) {
            int t1 = FuzzySearch.weightedRatio(prompt.getPromptText(), fileName);
            int t2 = FuzzySearch.weightedRatio(prompt.getPromptFilename(), fileName);
            int t3 = FuzzySearch.weightedRatio(prompt.getPromptId(), fileName);
            if (t1 > 86) {
                System.out.printf("%d, %s :: %s\n", t1, fileName, prompt.getPromptText());
            }
            if (t3 > 90) {
                System.out.printf("%d, %s :: %s\n", t3, fileName, prompt.getPromptId());
            }
        }

        return score;
    }

}
