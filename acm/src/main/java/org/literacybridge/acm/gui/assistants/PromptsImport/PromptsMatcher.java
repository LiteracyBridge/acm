package org.literacybridge.acm.gui.assistants.PromptsImport;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;

import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class PromptsMatcher extends Matcher<PromptTarget, ImportableFile, PromptMatchable> {
    // Some programs have named the prompts with both the ID and the descriptive name, like
    // "10 welcome" or "i9-0 user feedback invitation". This recognizes most such formatting
    // and splits apart the ID and descriptive name, which are then matched independently.
    static final Pattern promptPattern = Pattern.compile("(?i)^((?:i?\\W*)?\\d+(?:-\\d+)?)\\s*(.+)$");

    @Override
    protected int scoreMatch(PromptMatchable l, PromptMatchable r, boolean tokens) {
        PromptTarget prompt = l.getLeft();
        ImportableFile file = r.getRight();

        // Which compare function are we using?
        BiFunction<String, String, Integer> fn = tokens ?
                                                 FuzzySearch::tokenSortRatio :
                                                 FuzzySearch::ratio;

        String fileName = FilenameUtils.removeExtension(file.getFile().getName());
        String idPart = null;
        String descriptivePart = null;
        java.util.regex.Matcher matcher = promptPattern.matcher(fileName);
        if (matcher.matches()) {
            idPart = matcher.group(1);
            descriptivePart = matcher.group(2);
        }

        int score = 0;
        try {
            // Try to match with both the prompt id and definition.
            score = fn.apply(prompt.getPromptId(), fileName);
            score = Math.max(score, fn.apply(prompt.getPromptFilename(), fileName));
            score = Math.max(score, fn.apply(prompt.getPromptText(), fileName));
            if (StringUtils.isNotEmpty(idPart)) {
                score = Math.max(score, fn.apply(prompt.getPromptId(), idPart));
            }
            if (StringUtils.isNotEmpty(descriptivePart)) {
                score = Math.max(score, fn.apply(prompt.getPromptFilename(), descriptivePart));
            }
        } catch (Exception ignored) {
            // Ignore and take whatever we got.
        }

        return score;
    }

}
