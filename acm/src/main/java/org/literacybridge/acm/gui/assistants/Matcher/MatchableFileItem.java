package org.literacybridge.acm.gui.assistants.Matcher;

public class MatchableFileItem<L extends Target> extends MatchableItem<L, ImportableFile> {

    protected MatchableFileItem(L left, ImportableFile right) {
        super(left, right);
    }

    protected MatchableFileItem(L left, ImportableFile right, MATCH match) {
        super(left, right, match);
    }

    protected MatchableFileItem(L left, ImportableFile right, MATCH match, int score) {
        super(left, right, match, score);
    }
}
