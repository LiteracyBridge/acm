package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.literacybridge.acm.Constants.CATEGORY_TB_CATEGORIES;

class DeploymentContext {

    int deploymentNo = -1;
    String deploymentName;
    ProgramSpec programSpec;
    Set<String> languages;

    Map<String, List<Content.Playlist>> allProgramSpecPlaylists;
    Map<String, List<Playlist>> allAcmPlaylists;

    // Should these be per-contentpackage properties?
    boolean includeUfCategory;
    boolean includeTbCategory;

    // Don't publish, only create.
    boolean noPublish;

    // { language : { playlisttitle : prompts } }
    Map<String, Map<String, PlaylistPrompts>> prompts = new HashMap<>();

    // List of issues found in the Deployment.
    Issues issues = new Issues();

    // This is the root of a Deployment's languages / playlists / messages.
    DefaultMutableTreeNode playlistRootNode;


    /**
     * Node class for a Language. One or more languages in a Deployment. These can't be
     * re-arranged (because it makes no difference on a TB).
     */
    public static class LanguageNode extends DefaultMutableTreeNode {
        final String languagecode;
        final String languagename;
        LanguageNode(String languagecode) {
            this.languagecode = languagecode;
            this.languagename = ACMConfiguration.getInstance().getCurrentDB().getLanguageLabel(new Locale(languagecode));
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
        final Playlist playlist;
        PlaylistNode(Playlist playlist) {
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
        final AudioItem item;
        public AudioItemNode(AudioItem item) {
            this.item = item;
        }
        public String toString() {
            return item.getTitle();
        }
    }

}
