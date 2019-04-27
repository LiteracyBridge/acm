package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.core.spec.ContentSpec;

import java.util.Comparator;

public class AudioMatcher extends Matcher<AudioTarget, ImportableFile, MatchableAudio>  {
    public void sortByProgramSpecification() {
        matchableItems.sort((o1, o2) -> {
            MATCH m1 = o1.getMatch();
            MATCH m2 = o2.getMatch();
            // Compare unmatched items against each other.
            if (!m1.isMatch() && !m2.isMatch()) {
                return o1.compareTo(o2);
            }
            // Compare unmatched as less than matched.
            if (m1.isMatch() && !m2.isMatch()) {
                return 1;
            } else if (!m1.isMatch() && m2.isMatch()) {
                return -1;
            }
            // Both are matched. Compare by ProgramSpec order.
            ContentSpec.MessageSpec messageSpec1 = o1.getLeft().getMessage();
            ContentSpec.MessageSpec messageSpec2 = o2.getLeft().getMessage();
            ContentSpec.PlaylistSpec playlistSpec1 = messageSpec1.getPlaylist();
            ContentSpec.PlaylistSpec playlistSpec2 = messageSpec2.getPlaylist();
            if (!playlistSpec1.equals(playlistSpec2)) {
                return playlistSpec1.getOrdinal() - playlistSpec2.getOrdinal();
            }
            return messageSpec1.getOrdinal() - messageSpec2.getOrdinal();
        });
    }
}
