package org.literacybridge.acm.gui.assistants.Matcher;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

/**
 * The MatchableItem has a right item, a left item, and a match score.
 * @param <L>
 * @param <R>
 */
public class MatchableItem<L extends Target, R> implements Comparable<MatchableItem> {
    SimpleObjectProperty<L> left = new SimpleObjectProperty<>();

    public L getLeft() {
        return left.get();
    }

    // These properties must be public for the TableView to display them.
    public SimpleObjectProperty<L> leftProperty() {
        return left;
    }

    void setLeft(L left) {
        this.left.set(left);
    }

    SimpleObjectProperty<R> right = new SimpleObjectProperty<>();

    public R getRight() {
        return right.get();
    }

    // These properties must be public for the TableView to display them.
    public SimpleObjectProperty<R> rightProperty() {
        return right;
    }

    public void setRight(R right) {
        this.right.set(right);
    }

    SimpleObjectProperty<MATCH> match = new SimpleObjectProperty<>(MATCH.NONE);

    public MATCH getMatch() {
        return match.get();
    }

    public SimpleObjectProperty<MATCH> matchProperty() {
        return match;
    }

    public void setMatch(MATCH match) {
        this.match.set(match);
    }

    SimpleIntegerProperty score = new SimpleIntegerProperty(0);

    public int getScore() {
        return score.get();
    }

    public SimpleIntegerProperty scoreProperty() {
        return score;
    }

    public void setScore(int score) {
        this.score.set(score);
    }

    public String getScoredMatch() {
        StringBuilder sb = new StringBuilder(getMatch().toString());
        if (getMatch().isMatch() && getMatch() != MATCH.EXACT)
            sb.append('@').append(Integer.toString(getScore()));
        return sb.toString();
    }

    protected MatchableItem(L left, R right) {
        this(left, right, left==null ? MATCH.RIGHT_ONLY : MATCH.LEFT_ONLY);
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

    public MatchableItem disassociate() {
        MatchableItem disassociated = new MatchableItem(null, getRight(), MATCH.RIGHT_ONLY);
        setRight(null);
        setMatch(MATCH.LEFT_ONLY);
        setScore(0);
        return disassociated;
    }

    public boolean isMatchableWith(MatchableItem other) {
        return other != null &&
            this.getMatch().isUnmatched() && other.getMatch().isUnmatched() &&
            this.getMatch() != other.getMatch();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchableItem)) return false;
        MatchableItem matchableItem = (MatchableItem) o;
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
        if (this.getMatch() == MATCH.RIGHT_ONLY) return right.getValue().toString();
        else if (this.getMatch() == MATCH.LEFT_ONLY) return left.getValue().toString();
        else return "";
    }

    public String description() {
        return this.toString();
    }

    public String getOperation() {
        String status = "";
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
