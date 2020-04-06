package org.literacybridge.acm.gui.assistants.Matcher;

public class MatchableFileItem<L extends Target, R extends ImportableFile> extends MatchableItem<L, R> {

    protected MatchableFileItem(L left, R right) {
        super(left, right);
    }

    protected MatchableFileItem(L left, R right, MATCH match) {
        super(left, right, match);
    }

    protected MatchableFileItem(L left, R right, MATCH match, int score) {
        super(left, right, match, score);
    }
}
