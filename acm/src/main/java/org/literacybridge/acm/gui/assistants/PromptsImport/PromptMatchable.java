package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;

public class PromptMatchable extends MatchableItem<PromptTarget, ImportableFile> {

    PromptMatchable(PromptTarget left, ImportableFile right) {
        super(left, right);
    }

    private PromptMatchable(PromptTarget left, ImportableFile right, MATCH match)
    {
        super(left, right, match);
    }

    boolean containsText(String filterText) {
        if (getRight() != null && getRight().getFile().getName().toLowerCase().contains(filterText))
            return true;
        if (getLeft() != null) {
            PromptTarget target = getLeft();
            String definition = target.getPromptDefinition().toLowerCase();
            // TODO: add fuzzy matching.
            return definition.contains(filterText) ||
                target.getPromptId().contains(filterText);
        }
        return false;
    }

    @Override
    public PromptMatchable disassociate() {
        PromptMatchable disassociated = new PromptMatchable(null, getRight(), MATCH.RIGHT_ONLY);
        setRight(null);
        setMatch(MATCH.LEFT_ONLY);
        setScore(0);
        return disassociated;
    }

    @Override
    public int compareTo(MatchableItem o) {
        if (this == o) return 0;
        String l = comparableStringFor(this);
        String r = comparableStringFor(o);
        int rslt = l.compareTo(r);
        // If strings are equal, LEFT_ONLY is less than RIGHT_ONLY
        if (rslt == 0 && this.getMatch() == MATCH.RIGHT_ONLY && o.getMatch() != MATCH.RIGHT_ONLY)
            rslt = 1;
        else if (rslt == 0 && this.getMatch() != MATCH.RIGHT_ONLY
            && o.getMatch() == MATCH.RIGHT_ONLY) rslt = -1;
        return rslt;
    }

    private static String comparableStringFor(MatchableItem it) {
        if (it instanceof PromptMatchable) {
            String result = "";
            PromptMatchable pit = (PromptMatchable)it;
            if (pit.getLeft() != null) result = pit.getLeft().getPromptId();
            else if (pit.getRight() != null) result = FilenameUtils.removeExtension(pit.getRight().getFile().getName());
            if (result.length() < 2) result = "0" + result;
            return result;
        }
        return it.toString();
    }
}
