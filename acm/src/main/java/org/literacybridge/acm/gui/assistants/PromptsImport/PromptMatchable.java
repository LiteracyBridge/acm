package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableFileItem;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;

import java.util.regex.Matcher;

public class PromptMatchable extends MatchableFileItem<PromptTarget, ImportableFile> {

    PromptMatchable(PromptTarget left, ImportableFile right) {
        super(left, right);
    }

    private PromptMatchable(PromptTarget left, ImportableFile right, MATCH match)
    {
        super(left, right, match);
    }

    boolean containsText(String filterText) {
        if (getRight() != null && getRight().getTitle().toLowerCase().contains(filterText))
            return true;
        if (getLeft() != null) {
            PromptTarget target = getLeft();
            String definition = target.getPromptText().toLowerCase();
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
    public boolean isExactMatch(MatchableItem<PromptTarget, ImportableFile> o) {
        PromptsInfo.PromptInfo promptInfo = this.getLeft().getPromptInfo();
        String filename = FilenameUtils.getBaseName(o.getRight().getFile().getName());
        return promptInfo.getId().equals(filename) || promptInfo.getFilename().equalsIgnoreCase(filename);
    }

    /**
     * Compare one PromptMatchable to another.
     * @param o The other PromptMatchable.
     * @return -1, 0, +1
     */
    @Override
    public int compareTo(MatchableItem o) {
        int rslt;
        if (getLeft() != null) {
            // If this item has a "left", a PromptTarget...
            if (o.getLeft() != null) {
                // ...and the other item also has a PromptTarget, then compare the targets.
                rslt = compareTargets(getLeft(), (PromptTarget) o.getLeft());
            } else {
                // ...and the other item doesn't, make a mixed comparison.
                rslt = compareMixed(getLeft(), o.getRight().toString());
            }
        } else {
            // This item does not have a "left", a PromptTarget, so compare this item's File...
            if (o.getLeft() != null) {
                // ...and the other item does have a PromptTarget, so make a mixed comparison.
                // The comparison is designed for the comparands in the other order.
                rslt = -compareMixed((PromptTarget) o.getLeft(), getRight().toString());
            } else {
                // ANd the other side also does not have a PromptTarget, so compare File to File.
                rslt = compareNames(getRight().toString(), o.getRight().toString());
            }
        }

        // If strings are equal, LEFT_ONLY is less than RIGHT_ONLY
        if (rslt == 0 && this.getMatch() == MATCH.RIGHT_ONLY && o.getMatch() != MATCH.RIGHT_ONLY)
            rslt = 1;
        else if (rslt == 0 && this.getMatch() != MATCH.RIGHT_ONLY
            && o.getMatch() == MATCH.RIGHT_ONLY) rslt = -1;
        return rslt;
    }

    /**
     * Compare two PromptTargets. If they're both non-prompts, compare the ids. If a mixed set,
     * the non-prompt is less. And if both prompts, compare by category.
     * @param lTarget The PromptTarget from the left side of the comparison.
     * @param rTarget The PromptTarget from the right side of the comparison.
     * @return <0, 0, >0
     */
    private static int compareTargets(PromptTarget lTarget, PromptTarget rTarget) {
        boolean lPrompt = lTarget.getPromptInfo().isPlaylistPrompt();
        boolean rPrompt = rTarget.getPromptInfo().isPlaylistPrompt();
        if (lPrompt && rPrompt) {
            // Both are prompts
            return compareCategories(lTarget, rTarget);
        } else if (!lPrompt && !rPrompt) {
            // Neither prompts, compare the ids, as numeric.
            int l = Integer.parseInt(lTarget.getPromptId());
            int r = Integer.parseInt(rTarget.getPromptId());
            return l - r;
        } else if (lPrompt) {
            // Only one prompt, and it's the left side, so it is larger.
            return 1;
        }
        return -1;
    }

    /**
     * Compare two items by names. This happens when they're both ImportableFile items.
     * @param l The name of the left side of the comparison.
     * @param r The name of the right side of hte comparison.
     * @return <0, 0, >0
     */
    private static int compareNames(String l, String  r) {
        boolean lPrompt = PromptsInfo.playlistPromptPattern.matcher(l).matches();
        boolean rPrompt = PromptsInfo.playlistPromptPattern.matcher(r).matches();
        if (lPrompt && rPrompt) {
            // Both are prompts
            return compareCategories(l, r);
        } else if (!lPrompt && !rPrompt) {
            // Neither prompts, just compare as strings
            return l.compareTo(r);
        } else if (lPrompt) {
            // Only one prompt, and it's the left side, so it is larger.
            return 1;
        }
        return -1;
    }

    /**
     * Compare a PromptTarget with a file name. The file may match a category prompt, may
     * match a system prompt id (numeric), or may match a name.
     * This can compare a file on the left with a PromptTarget on the right by inverting the
     * result.
     * @param lTarget The PromptTarget from the left side of the comparison.
     * @param r The name of the file from the right side of the comparison.
     * @return <0, 0, >0
     */
    private static int compareMixed(PromptTarget lTarget, String r) {
        boolean lPrompt = lTarget.getPromptInfo().isPlaylistPrompt();
        boolean rPrompt = PromptsInfo.playlistPromptPattern.matcher(r).matches();
        if (lPrompt && rPrompt) {
            // Both are category ids.
            String l = lTarget.getPromptId();
            return compareCategories(l, r);
        } else if (!lPrompt && !rPrompt) {
            // Neither is a category. If the filename looks like an id, compare as ids. Otherwise,
            // sort the filename after the match target.
            try {
                int R = Integer.parseInt(r);
                String l = lTarget.getPromptId();
                int L = Integer.parseInt(l);
                return L - R;
            } catch (NumberFormatException ignored) {
                return -1;
            }
        } else if (lPrompt) {
            // Only one prompt, and it's the left side, so it is larger.
            return 1;
        }
        return -1;
    }

    /**
     * Compare categories. If either is an "invitation" item (starts with 'i'), extract just the
     * category part. If the result is then "equal", the non-invitation sorts lower.
     * @param l the PromptTarget from the left side of the comparison.
     * @param r the PromptTarget from the right side of the comparison.
     * @return <0, 0, >0
     */
    private static int compareCategories(PromptTarget l, PromptTarget r) {
        int rslt;
        boolean lInvitation = l.getPromptInfo().isPlaylistInvitation();
        boolean rInvitation = r.getPromptInfo().isPlaylistInvitation();
        if (lInvitation == rInvitation) {
            // Both invitations, or neither is invitation, just compare the ids as strings.
            rslt = l.getPromptId().compareTo(r.getPromptId());
        } else {
            // One is, one isn't. Compare the taxonomy values.
            rslt = l.getPromptId().substring(lInvitation?1:0).compareTo(r.getPromptId().substring(rInvitation?1:0));
            if (rslt == 0) {
                // Taxonomy values are the same. non-invitation is less
                rslt = lInvitation?1:-1;
            }
        }
        return rslt;
    }

    /**
     * Compare categories. If either is an "invitation" item (starts with 'i'), extract just the
     * category part. If the result is then "equal", the non-invitation sorts lower.
     *
     * This is like the other compareCategories, but works on names.
     * @param l the file name from the left side of the comparison.
     * @param r the file name from the right side of the comparison.
     * @return <0, 0, >0
     */
    private static int compareCategories(String l, String r) {
        int rslt;
        boolean lInvitation = l.charAt(0) == 'i';
        boolean rInvitation = r.charAt(0) == 'i';
        if (lInvitation == rInvitation) {
            // Both invitations, or neither is invitation, just compare the strings.
            rslt = l.compareTo(r);
        } else {
            // One is, one isn't. Compare the taxonomy values.
            rslt = l.substring(lInvitation?1:0).compareTo(r.substring(rInvitation?1:0));
            if (rslt == 0) {
                // Taxonomy values are the same. non-invitation is less
                rslt = lInvitation?1:-1;
            }
        }
        return rslt;
    }

}
