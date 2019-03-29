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

}
