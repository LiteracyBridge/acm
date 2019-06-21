package org.literacybridge.acm.gui.assistants.util;

import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
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
        ContentSpec.DeploymentSpec deploymentSpec = contentSpec.getDeployment(deploymentNo);
        if (deploymentSpec == null) return;

        List<ContentSpec.PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecs(language);
        for (ContentSpec.PlaylistSpec playlistSpec : playlistSpecs) {
            PlaylistNode playlistNode = new PlaylistNode(playlistSpec);
            languageNode.add(playlistNode);

            playlistNode.add(new PromptNode(playlistSpec, false));
            playlistNode.add(new PromptNode(playlistSpec, true));

            List<ContentSpec.MessageSpec> messageSpecs = playlistSpec.getMessageSpecs();
            for (ContentSpec.MessageSpec messageSpec : messageSpecs) {
                MessageNode messageNode = new MessageNode(messageSpec);
                playlistNode.add(messageNode);
            }
        }

    }

    /**
     * Given a ProgramSpec, returns a tree of MutableTreeNodes for a given deployment.
     * @param root to be filled with playlistSpec data.
     * @param programSpec  with content data.
     * @param deploymentNo for which the playlistSpec tree is desired.
     * @param languageCode for which the playlistSpec tree is desired.
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

    /**
     * Given a ProgramSpec, returns a tree of MutableTreeNodes for a given deployment.
     * @param root to be filled with playlistSpec data.
     * @param programSpec  with content data.
     * @param deploymentNo for which the playlistSpec tree is desired.
     * @param languageCode for which the playlistSpec tree is desired.
     */
    public static void fillTreeWithPlaylistPromptsForDeployment(DefaultMutableTreeNode root,
        ProgramSpec programSpec,
        int deploymentNo,
        String languageCode)
    {
        ContentSpec contentSpec = programSpec.getContentSpec();
        LanguageNode languageNode = null;

        ContentSpec.DeploymentSpec deploymentSpec = contentSpec.getDeployment(deploymentNo);
        if (deploymentSpec == null) return;

        List<ContentSpec.PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecs(languageCode);
        for (ContentSpec.PlaylistSpec playlistSpec : playlistSpecs) {
            if (languageNode == null) {
                languageNode = new LanguageNode(languageCode);
                root.add(languageNode);
            }

            PlaylistNode playlistNode = new PlaylistNode(playlistSpec);
            languageNode.add(playlistNode);

            playlistNode.add(new PromptNode(playlistSpec, false));
            playlistNode.add(new PromptNode(playlistSpec, true));
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
        LanguageNode(String languagecode) {
            super(languagecode);
        }

        public String toString() {
            return AcmAssistantPage.getLanguageAndName((String)getUserObject());
        }
    }

    /**
     * Node class for a PlaylistSpec. One or more playlists in a language. These can be
     * re-arranged within their language.
     */
    public static class PlaylistNode extends DefaultMutableTreeNode {
        PlaylistNode(ContentSpec.PlaylistSpec playlistSpec) {
            super(playlistSpec);
        }

        public String toString() {
            return ((ContentSpec.PlaylistSpec)getUserObject()).getPlaylistTitle();
        }
    }

    /**
     * Node class for an Audio Item. One or more Audio Items in a playlistSpec. These can
     * be re-arranged within their playlist, and can also be moved to a different playlist
     * within the language.
     */
    public static class MessageNode extends DefaultMutableTreeNode {
        public ContentSpec.MessageSpec getItem() { return (ContentSpec.MessageSpec)getUserObject(); }

        MessageNode(ContentSpec.MessageSpec item) {
            super(item);
        }

        public String toString() {
            return getItem().title;
        }
    }

    /**
     * Node class for an Audio Item for a playlist prompt. These can't be moved; they're implicit
     * to the Playlist.
     */
    public static class PromptNode extends DefaultMutableTreeNode {
        boolean isLongPrompt;

        PromptNode(ContentSpec.PlaylistSpec playlistSpec, boolean isLongPrompt) {
            super(playlistSpec);
            this.isLongPrompt = isLongPrompt;
        }

        public ContentSpec.PlaylistSpec getPlaylist() {
            return (ContentSpec.PlaylistSpec) getUserObject();
        }
        public boolean isLongPrompt() {
            return this.isLongPrompt;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append('"');
            if (isLongPrompt) result.append("...");
            result.append(getPlaylist().getPlaylistTitle());
            if (isLongPrompt) result.append("...");
            return result.append('"').toString();
        }
    }

}