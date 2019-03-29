package org.literacybridge.acm.gui.assistants.util;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Locale;

/**
 * Class to hold information about the structure of content in playlists, in a deployment,
 * as deduced from the playlists in the ACM.
 */
public class AcmContent {
    /**
     * Node class for a Language. One or more languages in a Deployment. These can't be
     * re-arranged (because it makes no difference on a TB).
     */
    public static class LanguageNode extends DefaultMutableTreeNode {
        public final String languagecode;
        final String languagename;
        public LanguageNode(String languagecode) {
            this.languagecode = languagecode;
            this.languagename = ACMConfiguration
                .getInstance().getCurrentDB().getLanguageLabel(new Locale(languagecode));
        }
        public String toString() {
            return String.format("%s (%s)", languagecode, languagename);
        }
    }

    /**
     * Node class for a Playlist. One or more playlists in a language. These can be
     * re-arranged within their language.
     */
    public static class PlaylistNode extends DefaultMutableTreeNode {
        public final Playlist playlist;
        public PlaylistNode(Playlist playlist) {
            this.playlist = playlist;
        }
        public String toString() {
            return playlist.getName();
        }
    }

    /**
     * Node class for an Audio Item. One or more Audio Items in a playlist. These can
     * be re-arranged within their playlist, and can also be moved to a different playlist
     * within the language.
     */
    public static class AudioItemNode extends DefaultMutableTreeNode {
        public final AudioItem item;
        public AudioItemNode(AudioItem item) {
            this.item = item;
        }
        public String toString() {
            return item.getTitle();
        }
    }
}
