package org.literacybridge.acm.gui.assistants.util;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Class to hold information about the structure of content in playlists, in a deployment,
 * as deduced from the playlists in the ACM.
 */
public class AcmContent {
    public static class AcmRootNode extends DefaultMutableTreeNode {
        @SuppressWarnings("unchecked")
        public Enumeration<LanguageNode> children() {
            return (Enumeration<LanguageNode>)super.children();
        }
        public LanguageNode find(String languagecode) {
            Enumeration<LanguageNode> children = children();
            while (children.hasMoreElements()) {
                LanguageNode languageNode = children.nextElement();
                if (languageNode.getLanguageCode().equals(languagecode))
                    return languageNode;
            }
            return null;
        }

        public List<LanguageNode> getLanguages() {
            if (super.children == null) {
                return new ArrayList<>();
            } else {
                //noinspection unchecked
                return (List<LanguageNode>)super.children;
            }
        }
    }

    /**
     * Node class for a Language. One or more languages in a Deployment. These can't be
     * re-arranged (because it makes no difference on a TB).
     */
    public static class LanguageNode extends DefaultMutableTreeNode {
        final String languagename;
        public LanguageNode(String languagecode) {
            super(languagecode);
            this.languagename = ACMConfiguration
                .getInstance().getCurrentDB().getLanguageLabel(new Locale(languagecode));
        }
        public String getLanguageCode() {
            return (String)getUserObject();
        }
        public String toString() {
            return String.format("%s (%s)", languagename, getLanguageCode());
        }

        @SuppressWarnings("unchecked")
        public Enumeration<PlaylistNode> children() {
            return (Enumeration<PlaylistNode>)super.children();
        }
        public PlaylistNode find(String title) {
            Enumeration<PlaylistNode> children = children();
            while (children.hasMoreElements()) {
                PlaylistNode playlistNode = children.nextElement();
                if (playlistNode.getTitle().equals(title))
                    return playlistNode;
            }
            return null;
        }

        public List<PlaylistNode> getPlaylists() {
            if (super.children == null) {
                return new ArrayList<>();
            } else {
                //noinspection unchecked
                return (List<PlaylistNode>)super.children;
            }
        }
    }

    /**
     * Node class for a Playlist. One or more playlists in a language. These can be
     * re-arranged within their language.
     */
    public static class PlaylistNode extends DefaultMutableTreeNode {
        public PlaylistNode(Playlist playlist) {
            super(playlist);
        }
        public Playlist getPlaylist() {
            return (Playlist)getUserObject();
        }
        public String toString() {
            return AssistantPage.undecoratedPlaylistName(getPlaylist().getName());
        }
        public String getTitle() {
            return AssistantPage.undecoratedPlaylistName(getPlaylist().getName());
        }
        public String getDecoratedTitle() {
            return getPlaylist().getName();
        }

        @SuppressWarnings("unchecked")
        public Enumeration<AudioItemNode> children() {
            return (Enumeration<AudioItemNode>)super.children();
        }

        public List<AudioItemNode> getAudioItems() {
            if (super.children == null) {
                return new ArrayList<>();
            } else {
                //noinspection unchecked
                return (List<AudioItemNode>)super.children;
            }
        }
    }

    /**
     * Node class for an Audio Item. One or more Audio Items in a playlist. These can
     * be re-arranged within their playlist, and can also be moved to a different playlist
     * within the language.
     */
    public static class AudioItemNode extends DefaultMutableTreeNode {
        public AudioItemNode(AudioItem item) {
            super(item);
        }
        public AudioItem getAudioItem() {
            return (AudioItem)getUserObject();
        }
        public String toString() {
            return getAudioItem().getTitle();
        }
    }
}
