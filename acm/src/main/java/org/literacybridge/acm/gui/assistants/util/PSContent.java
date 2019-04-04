package org.literacybridge.acm.gui.assistants.util;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Class to hold information about the structure of content in playlists, in a deployment,
 * as defined in the Program Specification.
 */
public class PSContent {

    private static void fillTreeForDeploymentAndLanguage(DefaultMutableTreeNode languageNode,
        ContentSpec contentSpec,
        int deploymentNo,
        String language)
    {
        List<ContentSpec.PlaylistSpec> playlistSpecs = contentSpec.getPlaylists(deploymentNo, language);
        for (ContentSpec.PlaylistSpec playlistSpec : playlistSpecs) {
            PlaylistNode playlistNode = new PlaylistNode(playlistSpec);
            languageNode.add(playlistNode);

            List<ContentSpec.MessageSpec> messageSpecs = playlistSpec.getMessageSpecs();
            for (ContentSpec.MessageSpec messageSpec : messageSpecs) {
                MessageNode messageNode = new MessageNode(messageSpec);
                playlistNode.add(messageNode);
            }
        }

    }

    /**
     * Given a ProgramSpec, returns a tree of MutableTreeNodes for a given deployment.
     * @param DefaultMutableTreeNode to be filled with playlistSpec data.
     * @param programSpec  with content data.
     * @param deploymentNo for which the playlistSpec tree is desired.
     * @param languageCode for which the playlistSpec tree is desired.
     * @return a tree of languageCode / playlistSpec / message
     */
    public static void fillTreeForDeployment(DefaultMutableTreeNode root,
        ProgramSpec programSpec,
        int deploymentNo,
        String languageCode)
    {
        ContentSpec contentSpec = programSpec.getContentSpec();
        Set<String> languageCodes = programSpec.getLanguagesForDeployment(deploymentNo);
        for (String language : languageCodes) {
            if (!language.equalsIgnoreCase(languageCode)) continue;
            LanguageNode languageNode = new LanguageNode(language);
            root.add(languageNode);

            fillTreeForDeploymentAndLanguage(languageNode, contentSpec, deploymentNo, language);
        }
    }

    public static void fillTreeForDeployment(DefaultMutableTreeNode root,
        ProgramSpec programSpec,
        int deploymentNo)
    {
        ContentSpec contentSpec = programSpec.getContentSpec();
        Set<String> languageCodes = programSpec.getLanguagesForDeployment(deploymentNo);
        for (String language : languageCodes) {
            LanguageNode languageNode = new LanguageNode(language);
            root.add(languageNode);
            fillTreeForDeploymentAndLanguage(languageNode, contentSpec, deploymentNo, language);
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
     * Node class for a PlaylistSpec. One or more playlists in a language. These can be
     * re-arranged within their language.
     */
    public static class PlaylistNode extends DefaultMutableTreeNode {
        final ContentSpec.PlaylistSpec playlistSpec;

        public PlaylistNode(ContentSpec.PlaylistSpec playlistSpec) {
            this.playlistSpec = playlistSpec;
        }

        public String toString() {
            return playlistSpec.getPlaylistTitle();
        }
    }

    /**
     * Node class for an Audio Item. One or more Audio Items in a playlistSpec. These can
     * be re-arranged within their playlist, and can also be moved to a different playlist
     * within the language.
     */
    public static class MessageNode extends DefaultMutableTreeNode {
        final ContentSpec.MessageSpec item;

        public MessageNode(ContentSpec.MessageSpec item) {
            this.item = item;
        }

        public String toString() {
            return item.title;
        }
    }
}
