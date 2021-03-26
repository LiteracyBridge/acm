package org.literacybridge.acm.gui.assistants.Matcher;

import java.util.Objects;

/**
 * The MatchableItem has a right item, a left item, and a match score.
 *
 * @param <L>
 * @param <R>
 */
public class MatchableItem<L extends Target, R> implements Comparable<MatchableItem<L,R>> {
    private L left;

    public L getLeft() {
        return left;
    }

    void setLeft(L left) {
        this.left = left;
    }

    private R right;

    public R getRight() {
        return right;
    }

    public void setRight(R right) {
        this.right = right;
    }

    private MATCH match = MATCH.NONE;

    public MATCH getMatch() {
        return match;
    }

    public void setMatch(MATCH match) {
        this.match = match;
    }

    private int score = 0;

    int getScore() {
        return score;
    }

    protected void setScore(int score) {
        this.score = score;
    }

    public String getScoredMatch() {
        StringBuilder sb = new StringBuilder(getMatch().toString());
        if (getMatch().isMatch() && getMatch() != MATCH.EXACT)
            sb.append('@').append(getScore());
        return sb.toString();
    }

    protected MatchableItem(L left, R right) {
        this(left, right, left == null ? MATCH.RIGHT_ONLY : MATCH.LEFT_ONLY);
    }

    protected MatchableItem(L left, R right, MATCH match) {
        this.setLeft(left);
        this.setRight(right);
        this.setMatch(match);
    }

    protected MatchableItem(L left, R right, MATCH match, int score) {
        this.setLeft(left);
        this.setRight(right);
        this.setMatch(match);
        this.setScore(score);
    }

    public MatchableItem<L,R> disassociate() {
        MatchableItem<L,R> disassociated = new MatchableItem<>(null, getRight(), MATCH.RIGHT_ONLY);
        setRight(null);
        setMatch(MATCH.LEFT_ONLY);
        setScore(0);
        return disassociated;
    }

    boolean isMatchableWith(MatchableItem<?, ?> other) {
        return other != null && this.getMatch().isUnmatched() && other.getMatch().isUnmatched()
            && this.getMatch() != other.getMatch();
    }

    public boolean isExactMatch(MatchableItem<L,R> other) {
        if (getMatch() == MATCH.LEFT_ONLY && other.getMatch() == MATCH.RIGHT_ONLY) {
            // we have a left and a right. Compare them, and then move on.
            return getLeft().toString().equals(other.getRight().toString());
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchableItem)) return false;
        @SuppressWarnings("unchecked")
        MatchableItem<L,R> matchableItem = (MatchableItem<L,R>) o;
        return Objects.equals(left, matchableItem.left) && Objects.equals(right,
            matchableItem.right) && match == matchableItem.match;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, match);
    }

    @Override
    public int compareTo(MatchableItem o) {
        if (this == o) return 0;
        String l = this.toString();
        String r = o.toString();
        int rslt = l.compareTo(r);
        // If strings are equal, LEFT_ONLY is less than RIGHT_ONLY
        if (rslt == 0 && this.getMatch() == MATCH.RIGHT_ONLY && o.getMatch() != MATCH.RIGHT_ONLY)
            rslt = 1;
        else if (rslt == 0 && this.getMatch() != MATCH.RIGHT_ONLY
            && o.getMatch() == MATCH.RIGHT_ONLY) rslt = -1;
        return rslt;
    }

    @Override
    public String toString() {
        if (this.getMatch() == MATCH.RIGHT_ONLY) return right.toString();
        else if (this.getMatch() == MATCH.LEFT_ONLY || this.getMatch().isMatch()) return left.toString();
        else return "";
    }

    public String description() {
        return this.toString();
    }

    public String getOperation() {
        String status;
        if (getMatch().isMatch()) {
            if (!getLeft().targetExists()) {
                status = "Import";
            } else if (getLeft().isReplaceOk()) {
                status = "Update";
            } else {
                status = "Keep";
            }
        } else if (getLeft() != null) {
            if (getLeft().targetExists()) {
                status = "Imported";
            } else {
                status = "Missing";
            }
        } else {
            status = "Extra File";
        }
        return status;
    }

}
