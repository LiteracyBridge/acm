package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;

class AudioMatcher extends Matcher<AudioTarget, ImportableFile, AudioMatchable> {
    /**
     * Set this to true to sort unmatched files to the end of the list. Set to false to sort
     * unmatched files intermingled by name with the program spec items.
     */
    private static boolean filesAtEnd = true;

    void sortByProgramSpecification() {
        matchableItems.sort((o1, o2) -> {
            AudioTarget t1 = o1.getLeft();
            AudioTarget t2 = o2.getLeft();

            // If both have a program spec position, compare by that.
            if (t1 != null && t2 != null) {
                // Both are matched. Compare by ProgramSpec order.
                int cmp = o1.getLeft().getPlaylistSpecOrdinal() - o2.getLeft().getPlaylistSpecOrdinal();
                if (cmp == 0)
                    cmp = o1.getLeft().getMessageSpecOrdinal() - o2.getLeft().getMessageSpecOrdinal();
                return cmp;
            }

            if (filesAtEnd) {
                // If neither has a program spec, compare by file names.
                if (t1 == null && t2 == null) {
                    String s1 = o1.getRight().toString();
                    String s2 = o2.getRight().toString();
                    return s1.compareToIgnoreCase(s2);
                }

                // Whichever one has no program spec can sort as smaller, towards the top of the list.
                if (t1 == null)
                    return 1;
                return -1;

            } else {
                // At least one is only a file. Compare by names.
                String s1 = t1 != null ? o1.getLeft().toString() : o1.getRight().toString();
                String s2 = t2 != null ? o2.getLeft().toString() : o2.getRight().toString();

                return s1.compareToIgnoreCase(s2);
            }
        });
    }
}
