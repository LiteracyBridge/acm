package org.literacybridge.acm.gui.assistants.Matcher;

public class MatchableStrings extends MatchableItem<String, String> {
    public MatchableStrings(String left, String right, MATCH match, int score) {
        super(left, right, match, score);
    }

    public MatchableStrings(String left, String right) {
        super(left, right, left==null ? MATCH.RIGHT_ONLY : MATCH.LEFT_ONLY);
    }
    MatchableStrings(String left, String right, MATCH match) {
        super(left, right, match);
    }

    @Override
    public String description() {
        StringBuilder sb = new StringBuilder();
        switch (this.getMatch()) {
        case LEFT_ONLY:
            sb.append("Left: ");
            break;
        case RIGHT_ONLY:
            sb.append("Right: ");
            break;
        }
        return sb.append(this.toString()).toString();
    }
};

