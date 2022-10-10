package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.assistants.common.AssistantContext;
import org.literacybridge.acm.gui.assistants.util.AcmContent;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.core.spec.ContentSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DeploymentContext implements AssistantContext {

    int deploymentNo = -1;
    Set<String> languageCodes;

    // Map of {languagecode : [playlistspec, ...], ...}
    Map<String, List<ContentSpec.PlaylistSpec>> allProgramSpecPlaylists;
    // Map of {languagecode : [Playlist, ...], ...}
    Map<String, List<Playlist>> allAcmPlaylists;

    // Should these be per-contentpackage properties?
    // "User Feedback..."
    boolean includeUfCategory = !ACMConfiguration.getInstance().getCurrentDB().isUserFeedbackHidden();
    // "Talking Book. To learn about this device, press the tree."
    boolean includeTbTutorial;

    // Don't publish, only create.
    private boolean noPublish;
    void setNoPublish(boolean noPublish) {
        this.noPublish = noPublish;
    }
    /**
     * Note that this is the reverse of the boolean, and of the setter!
     * @return true if the deployment should be published.
     */
    boolean isPublish() {
        return !noPublish;
    }
    boolean isNoPublishDefault() {
        return ACMConfiguration.isTestData() || ACMConfiguration.isSandbox();
    }

    // Information about packages, playlists, and other deployment information is gathered here.
    DeploymentInfo deploymentInfo;

    // { language : { playlisttitle : prompts } }
    Map<String, Map<String, PlaylistPrompts>> prompts = new HashMap<>();

    // List of issues found in the Deployment.
    Issues issues = new Issues();

    // This is the root of a Deployment's languages / playlists / messages.
    AcmContent.AcmRootNode playlistRootNode;

}
