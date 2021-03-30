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
    public enum PlDisposition {
        IGNORE,             // Do not add the playlist to the tree.
        MESSAGES_ONLY,      // Add the playlist content to the tree.
        BOTH,               // Add the playlist content and prompts to the tree.
        PROMPTS_ONLY;       // Add only the playlist prompts to the tree.

        boolean isaAddSomething() {
            return this != IGNORE;
        }
        boolean isAddContent() {
            return this == MESSAGES_ONLY || this == BOTH;
        }
        boolean isAddPrompt() {
            return this == BOTH || this == PROMPTS_ONLY;
        }
    }
    public static class PlaylistFilter {
        public PlDisposition filter(ContentSpec.PlaylistSpec playlistSpec) {
            return PlDisposition.BOTH;
        }
    }

    /**
     * Given a tree node for a language, add playlist nodes and prompt and/or message sub-nodes.
     * @param languageNode A DefaultMutableTreeNode that will contain any found playists.
     * @param contentSpec of the content in the deployment, per the program spec.
     * @param deploymentNo of interest.
     * @param language of interest.
     * @param playlistFilter a filter that is given a chance to examine each playlist to determine if
     *                       the playlist should be included, and, if so, whether to include prompts,
     *                       content messages, or both.
     */
    private static void fillTreeForDeploymentAndLanguage(DefaultMutableTreeNode languageNode,
        ContentSpec contentSpec,
        int deploymentNo,
        String language,
        PlaylistFilter playlistFilter)
    {
        ContentSpec.DeploymentSpec deploymentSpec = contentSpec.getDeployment(deploymentNo);
        if (deploymentSpec == null) return;

        List<ContentSpec.PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecsForLanguage(language);
        for (ContentSpec.PlaylistSpec playlistSpec : playlistSpecs) {
            // If the caller wants this playlist...
            PlDisposition disposition = playlistFilter.filter(playlistSpec);
            if (disposition.isaAddSomething()) {
                PlaylistNode playlistNode = new PlaylistNode(playlistSpec);
                languageNode.add(playlistNode);

                if (disposition.isAddPrompt()) {
                    playlistNode.add(new PromptNode(playlistSpec, false));
                    playlistNode.add(new PromptNode(playlistSpec, true));
                }

                if (disposition.isAddContent()) {
                    List<ContentSpec.MessageSpec> messageSpecs = playlistSpec.getMessageSpecs();
                    for (ContentSpec.MessageSpec messageSpec : messageSpecs) {
                        playlistNode.add(new MessageNode(messageSpec));
                    }
                }
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
        String languageCode,
        PlaylistFilter playlistFilter)
    {
        ContentSpec contentSpec = programSpec.getContentSpec();
        Set<String> languageCodes = programSpec.getLanguagesForDeployment(deploymentNo);
        for (String language : languageCodes) {
            if (!language.equalsIgnoreCase(languageCode)) continue;
            LanguageNode languageNode = new LanguageNode(language);
            root.add(languageNode);

            fillTreeForDeploymentAndLanguage(languageNode, contentSpec, deploymentNo, language, playlistFilter);
        }
    }

    public static void fillTreeForDeployment(DefaultMutableTreeNode root,
        ProgramSpec programSpec,
        int deploymentNo,
        PlaylistFilter playlistFilter)
    {
        ContentSpec contentSpec = programSpec.getContentSpec();
        Set<String> languageCodes = programSpec.getLanguagesForDeployment(deploymentNo);
        for (String language : languageCodes) {
            LanguageNode languageNode = new LanguageNode(language);
            root.add(languageNode);
            fillTreeForDeploymentAndLanguage(languageNode, contentSpec, deploymentNo, language, playlistFilter);
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
