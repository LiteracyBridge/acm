package org.literacybridge.acm.gui.assistants.util;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Class to hold information about the structure of content in playlists, in a deployment,
 * as defined in the Program Specification.
 */
public class PSContent {

    private static void fillTreeForDeploymentAndLanguage(DefaultMutableTreeNode languageNode,
        Content content,
        int deploymentNo,
        String language)
    {
        List<Content.Playlist> playlists = content.getPlaylists(deploymentNo, language);
        for (Content.Playlist playlist : playlists) {
            PlaylistNode playlistNode = new PlaylistNode(playlist);
            languageNode.add(playlistNode);

            List<Content.Message> messages = playlist.getMessages();
            for (Content.Message message : messages) {
                MessageNode messageNode = new MessageNode(message);
                playlistNode.add(messageNode);
            }
        }

    }

    /**
     * Given a ProgramSpec, returns a tree of MutableTreeNodes for a given deployment.
     * @param DefaultMutableTreeNode to be filled with playlist data.
     * @param programSpec  with content data.
     * @param deploymentNo for which the playlist tree is desired.
     * @param languageCode for which the playlist tree is desired.
     * @return a tree of languageCode / playlist / message
     */
    public static void fillTreeForDeployment(DefaultMutableTreeNode root,
        ProgramSpec programSpec,
        int deploymentNo,
        String languageCode)
    {
        Content content = programSpec.getContent();
        Set<String> languageCodes = programSpec.getLanguagesForDeployment(deploymentNo);
        for (String language : languageCodes) {
            if (!language.equalsIgnoreCase(languageCode)) continue;
            LanguageNode languageNode = new LanguageNode(language);
            root.add(languageNode);

            fillTreeForDeploymentAndLanguage(languageNode, content, deploymentNo, language);
        }
    }

    public static void fillTreeForDeployment(DefaultMutableTreeNode root,
        ProgramSpec programSpec,
        int deploymentNo)
    {
        Content content = programSpec.getContent();
        Set<String> languageCodes = programSpec.getLanguagesForDeployment(deploymentNo);
        for (String language : languageCodes) {
            LanguageNode languageNode = new LanguageNode(language);
            root.add(languageNode);
            fillTreeForDeploymentAndLanguage(languageNode, content, deploymentNo, language);
        }
    }

    /**
     * Node class for a Language. One or more languages in a Deployment. These can't be
     * re-arranged (because it makes no difference on a TB).
     */
    public static class LanguageNode extends DefaultMutableTreeNode {
        final String languagecode;
        final String languagename;

        public LanguageNode(String languagecode) {
            this.languagecode = languagecode;
            this.languagename = ACMConfiguration
                .getInstance()
                .getCurrentDB()
                .getLanguageLabel(new Locale(languagecode));
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
        final Content.Playlist playlist;

        public PlaylistNode(Content.Playlist playlist) {
            this.playlist = playlist;
        }

        public String toString() {
            return playlist.getPlaylistTitle();
        }
    }

    /**
     * Node class for an Audio Item. One or more Audio Items in a playlist. These can
     * be re-arranged within their playlist, and can also be moved to a different playlist
     * within the language.
     */
    public static class MessageNode extends DefaultMutableTreeNode {
        final Content.Message item;

        public MessageNode(Content.Message item) {
            this.item = item;
        }

        public String toString() {
            return item.title;
        }
    }
}
