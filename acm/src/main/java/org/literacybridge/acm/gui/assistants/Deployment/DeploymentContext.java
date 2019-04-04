package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DeploymentContext {

    int deploymentNo = -1;
    String deploymentName;
    ProgramSpec programSpec;
    Set<String> languages;

    Map<String, List<ContentSpec.PlaylistSpec>> allProgramSpecPlaylists;
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
