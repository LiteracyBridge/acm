package org.literacybridge.acm.gui.assistants.Matcher;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.xdrop.fuzzywuzzy.FuzzySearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Matcher<L, R, T extends MatchableItem<L, R>> {

    public ObservableList<T> matchableItems = FXCollections.observableArrayList(item -> new Observable[] {
        item.leftProperty(), item.matchProperty(), item.scoreProperty(), item.rightProperty() });

    public Matcher() {

    }

    public void setData(Collection<L> left, Collection<R> right, BiFunction<L, R, T> factory)
    {
        matchableItems.clear();
        Set<L> testL = new HashSet<>();
        for (L leftItem : left) {
            if (testL.contains(leftItem)) {
                throw new IllegalArgumentException("Duplicates not allowed in left");
            }
            testL.add(leftItem);
            T li = factory.apply(leftItem, null);
            matchableItems.add(li);
        }
        Set<R> testR = new HashSet<>();
        for (R rightItem : right) {
            if (testR.contains(rightItem)) {
                throw new IllegalArgumentException("Duplicates not allowed in right");
            }
            testR.add(rightItem);
            T ri = factory.apply(null, rightItem);
            matchableItems.add(ri);
        }

        Collections.sort(matchableItems);
    }

    public MatchStats findExactMatches() {
        MatchStats result = new MatchStats();
        // The array is sorted, so matching strings will already be adjacent. And the "left"
        // strings sort earlier than the "right" strings. So, all we need to do is walk the
        // list, and compare every "left" item to the next item.
        int ix = 0;
        while (ix < matchableItems.size() - 1) {
            MatchableItem item1 = matchableItems.get(ix);
            if (item1.getMatch() == MATCH.LEFT_ONLY) {
                MatchableItem item2 = matchableItems.get(ix + 1);
                if (item2.getMatch() == MATCH.RIGHT_ONLY) {
                    result.comparisons++;
                    // we have a left and a right. Compare them, and then move on.
                    if (item1.getLeft().equals(item2.getRight())) {
                        result.matches++;
                        recordMatch(item1, item2, MATCH.EXACT, 0);
                    }
                    ix += 2;
                } else if (item2.getMatch() == MATCH.LEFT_ONLY) {
                    // the second item is another LEFT; we want to look at it next time,
                    // so advance only one.
                    ix++;
                } else {
                    // the second item is not a RIGHT, so we don't need to test it. It's also
                    // not a LEFT, so we don't need to see it again. Advance two.
                    ix += 2;
                }
            } else {
                ix++;
            }
        }

        squash();
        return result;
    }

    public MatchStats findTokenMatches(int threshold) {
        return fuzzyMatchWorker(threshold, true);
    }

    public MatchStats findFuzzyMatches(int threshold) {
        return fuzzyMatchWorker(threshold, false);
    }

    private MatchStats fuzzyMatchWorker(int threshold, boolean tokens) {
        MatchStats result = new MatchStats();
        // Items are sorted. To the extent that the fuzzily matching strings are similar in their
        // prefixes, they're likely close together. So, first try to match adjacent items, then
        // move on to the full o(n^2) matching.
//        int ix = 0;
//        while (ix < matchableItems.size() - 1) {
//            MatchableItem item1 = matchableItems.get(ix);
//            if (item1.getMatch().isSingle()) {
//                MatchableItem item2 = matchableItems.get(ix + 1);
//                if (!item2.getMatch().isSingle()) {
//                    // The second item won't match anything. Skip on past it.
//                    ix += 2;
//                } else {
//                    result.comparisons++;
//                    if (doMatch(item1, item2, threshold, tokens)) {
//                        result.matches++;
//                        ix += 2;
//                    } else {
//                        ix++;
//                    }
//                }
//            } else {
//                ix++;
//            }
//        }
//        squash();
        result.add(matrixMatch(threshold, tokens));

        squash();

        return result;
    }

    private boolean doMatch(MatchableItem item1, MatchableItem item2, int threshold, boolean tokens)
    {
        // We need one left and one right in order to match.
        if (!item1.getMatch().isSingle() || !item2.getMatch().isSingle()
            || item1.getMatch() == item2.getMatch()) {
            return false;
        }
        // we have a left and a right. See which is which, compare them, and then move on.
        MatchableItem l = item1.getMatch() == MATCH.LEFT_ONLY ? item1 : item2;
        MatchableItem r = item1.getMatch() == MATCH.RIGHT_ONLY ? item1 : item2;

        int score = scoreMatch(l, r, tokens);
        if (score >= threshold) {
            // Yay!
            recordMatch(l, r, tokens ? MATCH.TOKEN : MATCH.FUZZY, score);
            return true;
        }
        return false;
    }

    private void recordMatch(MatchableItem l, MatchableItem r, MATCH match, int score) {
        l.setRight(r.getRight());
        l.setMatch(match);
        l.setScore(score);
        r.setMatch(MATCH.NONE);
        if (score != 100) {
            System.out.println(String.format("Matched %s%n     to %s%n   with %s (%d)",
                l.getLeft(),
                l.getRight(),
                match,
                score));
        }
    }

    private int scoreMatch(MatchableItem l, MatchableItem r, boolean tokens) {
        int score = 0;
        if (tokens) score = FuzzySearch.tokenSortPartialRatio(l.getLeft().toString(),
            r.getRight().toString());
        else score = FuzzySearch.ratio(l.getLeft().toString(), r.getRight().toString());
        return score;
    }

    private static class Comparison {
        MatchableItem leftItem;
        MatchableItem rightItem;
        int score;

        public Comparison(MatchableItem leftItem, MatchableItem rightItem, int score) {
            this.leftItem = leftItem;
            this.rightItem = rightItem;
            this.score = score;
        }
    }

    private MatchStats matrixMatch(int threshold, boolean tokens) {
        MatchStats result = new MatchStats();
        List<MatchableItem> leftList = new ArrayList<>();
        List<MatchableItem> rightList = new ArrayList<>();
        for (MatchableItem item : matchableItems) {
            if (item.getMatch() == MATCH.LEFT_ONLY) leftList.add(item);
            else if (item.getMatch() == MATCH.RIGHT_ONLY) rightList.add(item);
        }
        // Anything?
        if (leftList.size() == 0 || rightList.size() == 0) return result;
        // Perform all comparisons, then pick the best ones.
        List<Comparison> comparisons = new ArrayList<>();
        for (int iLeft = 0; iLeft < leftList.size(); iLeft++) {
            for (int iRight = 0; iRight < rightList.size(); iRight++) {
                result.comparisons++;
                int score = scoreMatch(leftList.get(iLeft), rightList.get(iRight), tokens);
                comparisons.add(new Comparison(leftList.get(iLeft), rightList.get(iRight), score));
            }
        }
        // Sort the best ones to the start of the list.
        comparisons.sort((a,b)->b.score-a.score);
        MATCH matchFlavor = tokens ? MATCH.TOKEN : MATCH.FUZZY;
        for (Comparison mc : comparisons) {
            // Stop when the matches aren't good enough.
            if (mc.score < threshold) break;
            // If we haven't already matched this item, match it now.
            if (!mc.leftItem.getMatch().isMatch()) {
                result.matches++;
                recordMatch(mc.leftItem, mc.rightItem, matchFlavor, mc.score);
            }
        }

        return result;
    }

    public void sort() {
//        Collections.sort(matchableItems);
        Collections.sort(matchableItems, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                MATCH m1 = o1.getMatch();
                MATCH m2 = o2.getMatch();
                // Compare unmatched items against each other.
                // Compare same-matches against each other.
                if ((!m1.isMatch() && !m2.isMatch()) || m1==m2) {
                    return o1.compareTo(o2);
                }
                // Compare unmatched as less than matched.
                if (m1.isMatch() && !m2.isMatch()) {
                    return 1;
                } else if (!m1.isMatch() && m2.isMatch()) {
                    return -1;
                }
                // Otherwise compare by match type. Let the looser matches sort lower.
                return m2.ordinal() - m1.ordinal();
            }
        });
    }

    private void squash() {
        List<MatchableItem> toRemove = matchableItems.stream()
            .filter(m -> m.getMatch() == MATCH.NONE)
            .collect(Collectors.toList());
        matchableItems.removeAll(toRemove);
    }

    private static int[][] removeRow(int[][] array, int rowIx)
    {
        int[][] newArray = new int[array.length - 1][];

        System.arraycopy(array, 0, newArray, 0, rowIx);
        System.arraycopy(array, rowIx + 1, newArray, rowIx, array.length - rowIx - 1);

        return newArray;
    }

    private static int[][] removeSlice(int[][] array, int sliceIx)
    {
        for (int iRow = 0; iRow < array.length; iRow++) {
            int[] newArray = new int[array[iRow].length - 1];

            System.arraycopy(array[iRow], 0, newArray, 0, sliceIx);
            System.arraycopy(array[iRow],
                sliceIx + 1,
                newArray,
                sliceIx,
                array.length - sliceIx - 1);

            array[iRow] = newArray;
        }
        return array;
    }

    public static class MatchStats {
        private long start = System.nanoTime();
        int comparisons;
        int matches;

        public boolean matched() { return matches > 0; }

        public MatchStats add(MatchStats other) {
            this.comparisons += other.comparisons;
            this.matches += other.matches;
            this.start = Math.min(this.start, other.start);
            return this;
        }

        public String toString() {
            long uSec = (System.nanoTime() - this.start) / 1000;
            return String.format("cmp:%d, match:%d, time:%,d \u03bcs", comparisons, matches, uSec);
        }
    }

}
