package org.literacybridge.acm.gui.assistants.Deployment;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;

public class ValidationPage extends AssistantPage<DeploymentContext> {

    private final JLabel deployment;
    private final JCheckBox deployWithWarnings;
    private final JCheckBox deployWithErrors;

    private DeploymentContext context;

    private MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
    private final JTree issuesTree;
    private final DefaultMutableTreeNode issuesTreeRoot;
    private final DefaultTreeModel issuesTreeModel;

    ValidationPage(PageHelper<DeploymentContext> listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Validation</span>" + "</ul>"
                + "<br/>Examine any issues, and click \"Next\" if you wish to proceed. "

                + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        deployment = makeBoxedLabel();
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        // TODO: needed?
        add(hbox, gbc);

        deployWithWarnings = new JCheckBox("Create Deployment with warnings. This may not conform to the Program Spec.");
        add(deployWithWarnings, gbc);
        deployWithWarnings.addActionListener(this::onSelection);
        deployWithErrors = new JCheckBox("<html>Create Deployment with errors. <em>This will probably fail on some Talking Books</em>.</html>");
        add(deployWithErrors, gbc);
        deployWithErrors.addActionListener(this::onSelection);

        issuesTreeRoot = new DefaultMutableTreeNode();
        issuesTreeModel = new DefaultTreeModel(issuesTreeRoot);
        issuesTree = new JTree(issuesTreeModel);
        issuesTree.setRootVisible(false);
        JScrollPane issuesScroller = new JScrollPane(issuesTree);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(issuesScroller, gbc);
    }

    /**
     * Called when a selection changes.
     *
     * @param actionEvent is ignored.
     */
    @SuppressWarnings("unused")
    private void onSelection(ActionEvent actionEvent) {
        boolean ok = !context.issues.hasError() || deployWithErrors.isSelected();
        ok = ok && (!context.issues.hasWarning() || deployWithWarnings.isSelected());
        setComplete(ok);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        deployment.setText(Integer.toString(context.deploymentNo));

        if (progressing) {
            // Reset issues when we enter with a (possibly) new deployment number.
            context.issues.clear();
            collectDeploymentInformation(context.deploymentNo);
            deployWithWarnings.setSelected(false);
            deployWithErrors.setSelected(false);
        }

        validateDeployment(context.deploymentNo);

        issuesTreeRoot.removeAllChildren();
        context.issues.addToTree(issuesTreeRoot);
        issuesTreeModel.reload();
        for (int ix=0; ix<issuesTree.getRowCount(); ix++) {
            issuesTree.expandRow(ix);
        }

        if (ACMConfiguration.isTestData()) {
            deployWithErrors.setSelected(true);
            deployWithWarnings.setSelected(true);
            setComplete();
        }
    }

    @Override
    protected String getTitle() {
        return "Validate the Deployment";
    }

    private void collectDeploymentInformation(int deploymentNo) {
        RecipientList recipients = context.programSpec.getRecipientsForDeployment(deploymentNo);
        context.languages = recipients.stream().map(r -> r.language).collect(Collectors.toSet());

        context.allProgramSpecPlaylists = getProgramSpecPlaylists(deploymentNo, context.languages);
        context.allAcmPlaylists = getAcmPlaylists(deploymentNo, context.languages);

    }

    private void validateDeployment(int deploymentNo) {
        context.issues.clear();

        // Get the languages, and playlists for each language.
        // Check that all languages are a language in the ACM.
        validateDeploymentLanguages(context.languages);

        // Check that we have all messages in all playlists; check if anything was added.
        validatePlaylists(deploymentNo, context.languages);

        // Check that we have all system prompts in all languages.
        validateSystemPrompts(context.languages);

        // Check that we have all category prompts for categories in each language.
        validatePlaylistPrompts(context.languages);

        // Check that we have all recipient prompts for recipients in the deployment.
        validateRecipients(deploymentNo);

        deployWithWarnings.setVisible(context.issues.hasWarning());
        deployWithErrors.setVisible(context.issues.hasError());

        onSelection(null);
    }

    private void validateDeploymentLanguages(Collection<String> languages) {
        List<Locale> locales = ACMConfiguration.getInstance().getCurrentDB().getAudioLanguages();
        for (String language : languages) {
            Locale locale = new Locale(language);
            if (!locales.contains(locale)) {
                context.issues.add(Issues.Severity.ERROR,
                    Issues.Area.LANGUAGES,
                    "Language '%s' is used in the Deployment, but is not defined in the ACM.",
                    language);
            }
        }
    }

    /**
     * Validate the Playlists in a Deployment. Each language is evaluated separately, both because
     * playlists can legitimately differ between languages, and because there will be different
     * errors between languages.
     *
     * @param deploymentNo deployment # to be validated.
     * @param languages    list of languages in the deployment.
     */
    private void validatePlaylists(int deploymentNo, Collection<String> languages) {
        // For each language in the Deployment...
        for (String language : languages) {
            // Get the Program Spec playlists, and the ACM playlists (that match by pattern).
            List<ContentSpec.PlaylistSpec> programSpecPlaylistSpecs = context.allProgramSpecPlaylists.get(language);
            List<Playlist> acmPlaylists = context.allAcmPlaylists.get(language);

            // For all the playlists in the Program Spec...
            Set<Playlist> foundPlaylists = new HashSet<>();
            for (ContentSpec.PlaylistSpec programSpecPlaylistSpec : programSpecPlaylistSpecs) {
                // Get the corresponding ACM playlist, if it exists.
                String qualifiedPlaylistName = decoratedPlaylistName(programSpecPlaylistSpec.getPlaylistTitle(),
                    deploymentNo,
                    language);
                Playlist playlist = acmPlaylists.stream()
                    .filter(p -> p.getName().equals(qualifiedPlaylistName))
                    .findFirst()
                    .orElse(null);
                // If the ACM playlist is missing, note the issue.
                if (playlist == null) {
                    context.issues.add(Issues.Severity.WARNING,
                        Issues.Area.PLAYLISTS,
                        "Playlist '%s' is missing in the ACM.",
                        qualifiedPlaylistName);
                } else {
                    // ...otherwise, we have the playlist, so check the contents.
                    foundPlaylists.add(playlist);
                    Set<AudioItem> foundItems = new HashSet<>();
                    // For the messages in the Program Spec...
                    for (ContentSpec.MessageSpec messageSpec : programSpecPlaylistSpec.getMessageSpecs()) {
                        // Get the corresponding ACM AudioItem.
                        AudioItem audioItem = playlist.getAudioItemList()
                            .stream()
                            .map(store::getAudioItem)
                            .filter(it -> it.getTitle().equals(messageSpec.title))
                            .findFirst()
                            .orElse(null);
                        // If the ACM AudioItem is missing, note the issue.
                        if (audioItem == null) {
                            context.issues.add(Issues.Severity.WARNING,
                                Issues.Area.CONTENT,
                                "Title '%s' is missing in playlist '%s' in the ACM.",
                                messageSpec.title,
                                playlist.getName());
                        } else {
                            foundItems.add(audioItem);
                        }
                    }
                    // We've looked in the ACM for messages in the Program Spec.
                    // Now see if there are messages added to the ACM playlist.
                    playlist.getAudioItemList()
                        .stream()
                        .map(store::getAudioItem)
                        .filter(i -> !foundItems.contains(i))
                        .forEach(i -> context.issues.add(Issues.Severity.INFO,
                            Issues.Area.CONTENT,
                            "Title '%s' was added to playlist '%s' in the ACM.",
                            i.getTitle(),
                            playlist.getName()));
                }
            }
            // Report the playlists in the ACM list but not the Program Specification list.
            acmPlaylists.stream()
                .filter(p -> !foundPlaylists.contains(p))
                .forEach(p -> context.issues.add(Issues.Severity.INFO,
                    Issues.Area.PLAYLISTS,
                    "Playlist '%s' was added to the ACM.",
                    p.getName()));
        }

    }

    /**
     * Determines whether we have all required system prompts in all languages for the Deployment.
     *
     * @param languages to be checked.
     */
    private void validateSystemPrompts(Collection<String> languages) {
        // Get from config? From Prog Spec?
        boolean hasUserFeedback = true;
        @SuppressWarnings("ConstantConditions")
        String[] required_messages = hasUserFeedback ?
                                     TBBuilder.REQUIRED_SYSTEM_MESSAGES_UF :
                                     TBBuilder.REQUIRED_SYSTEM_MESSAGES_NO_UF;

        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(tbLoadersDir, languagesPath);

        for (String language : languages) {
            File languageDir = IOUtils.FileIgnoreCase(languagesDir, language);
            List<Integer> missing = new ArrayList<>();

            for (String prompt : required_messages) {
                File p1 = new File(languageDir, prompt + ".a18");
                if (!p1.exists()) {
                    // We know they're short integers.
                    missing.add(Integer.parseInt(prompt));
                }
            }
            if (!missing.isEmpty()) {
                /*
                 * Some system prompts are missing. Build a minimal description of which ones,
                 * by combining runs of consecutive missing numbers, formatted for the
                 * issues pane.
                 */
                StringBuilder msg = new StringBuilder();
                missing.sort(Integer::compareTo);
                boolean combining = false;
                int prevN = Short.MIN_VALUE;
                for (int n : missing) {
                    if (n == prevN + 1) {
                        combining = true;
                    } else if (combining) {
                        // We were combining, but this one is not prev+1, so close out the previous run.
                        msg.append('-').append(prevN).append(',').append(n);
                        combining = false;
                    } else {
                        if (msg.length() > 0) msg.append(',');
                        msg.append(n);
                        combining = false;
                    }
                    prevN = n;
                }
                msg.insert(0, "System prompts are missing for language %s: ");
                context.issues.add(Issues.Severity.ERROR,
                    Issues.Area.SYSTEM_PROMPTS,
                    msg.toString(),
                    language);
            }
        }
    }

    /**
     * Validates that the playlist prompts exist (short and long) for all playlists in all
     * languages.
     *
     * @param languages to check.
     */
    private void validatePlaylistPrompts(Collection<String> languages) {
        for (String language : languages) {
            Map<String, PlaylistPrompts> promptsMap = new HashMap<>();
            context.prompts.put(language, promptsMap);

            // Here, we care about the actual playlists for the Deployment.
            List<Playlist> acmPlaylists = context.allAcmPlaylists.get(language);
            for (Playlist playlist : acmPlaylists) {
                String title = WelcomePage.undecoratedPlaylistName(playlist.getName());
                PlaylistPrompts prompts = new PlaylistPrompts(title, language);
                prompts.findPrompts();
                promptsMap.put(title, prompts);

                if (!prompts.hasBothPrompts()) {
                    StringBuilder msg = new StringBuilder();
                    if (!prompts.hasShortPrompt() && prompts.hasLongPrompt())
                        msg.append("The short prompt is");
                    else if (prompts.hasShortPrompt() && !prompts.hasLongPrompt())
                        msg.append("The long prompt is");
                    else msg.append("Both the short and long prompts are");

                    msg.append(" missing in language '%s' for playlist '%s'.");

                    context.issues.add(Issues.Severity.ERROR,
                        Issues.Area.PLAYLISTS,
                        msg.toString(),
                        language,
                        title);

                } else if (prompts.hasEitherPromptAmbiguity()) {
                    StringBuilder msg = new StringBuilder(
                        "There is both an ACM message and a category ");
                    if (prompts.hasShortPromptAmbiguity() && !prompts.hasLongPromptAmbiguity())
                        msg.append("short ");
                    else if (!prompts.hasShortPromptAmbiguity() && prompts.hasLongPromptAmbiguity())
                        msg.append("long ");
                    else msg.append("short and long ");

                    msg.append(
                        " prompt in language '%s' for playlist '%s'. The ACM message will be used.");

                    context.issues.add(Issues.Severity.WARNING,
                        Issues.Area.PLAYLISTS,
                        msg.toString(),
                        language,
                        title);

                } else if (prompts.hasMixedPrompts()) {
                    context.issues.add(Issues.Severity.INFO,
                        Issues.Area.PLAYLISTS,
                        "One ACM message and one category prompt was found in language '%s' for playlist '%s'.",
                        language,
                        title);
                }

            }
        }
    }

    private void validateRecipients(int deploymentNo) {
        RecipientList recipients = context.programSpec.getRecipientsForDeployment(deploymentNo);
        Map<String, String> recipientsMap = context.programSpec.getRecipientsMap();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        File communitiesDir = IOUtils.FileIgnoreCase(tbLoadersDir, "communities");
        if (!communitiesDir.exists() || !communitiesDir.isDirectory()) {
            context.issues.add(Issues.Severity.ERROR, Issues.Area.CUSTOM_GREETINGS, "The 'communities' directory is missing in this project.");
            return;
        }

        for (Recipient recipient : recipients) {
            String dirName = recipientsMap.get(recipient.recipientid);
            File recipientDir = (dirName==null)?null:IOUtils.FileIgnoreCase(communitiesDir, dirName);
            if (recipientDir == null || !recipientDir.exists() || !recipientDir.isDirectory()) {
                context.issues.add(Issues.Severity.WARNING, Issues.Area.CUSTOM_GREETINGS, "Missing directory for recipient '%s'.", recipName(recipient));
            } else {
                File languagesDir = IOUtils.FileIgnoreCase(recipientDir, "languages");
                File languageDir = IOUtils.FileIgnoreCase(languagesDir, recipient.language);
                File promptFile = IOUtils.FileIgnoreCase(languageDir, "10.a18");
                if (!languageDir.exists() || !languageDir.isDirectory()) {
                    context.issues.add(Issues.Severity.WARNING, Issues.Area.CATEGORY_PROMPTS, "Missing 'languages/%s' directory for recipient '%s'.", recipient.language, recipName(recipient));
                } else if (!promptFile.exists()) {
                    context.issues.add(Issues.Severity.WARNING, Issues.Area.CATEGORY_PROMPTS, "No custom greeting for recipient '%s'.", recipName(recipient));
                }
                
                File systemDir = new File(recipientDir, "system");
                File[] groupDirs = systemDir.listFiles((dir, name) -> name.toLowerCase().endsWith(GROUP_FILE_EXTENSION));
                if (!systemDir.exists() || !systemDir.isDirectory()) {
                    context.issues.add(Issues.Severity.WARNING, Issues.Area.CATEGORY_PROMPTS, "Missing 'system' directory for recipient '%s'.", recipName(recipient));
                } else if (groupDirs==null || groupDirs.length==0) {
                    context.issues.add(Issues.Severity.WARNING, Issues.Area.CATEGORY_PROMPTS, "No '.grp' file for recipient '%s'.", recipName(recipient));
                }
            }

        }
    }

    private String recipName(Recipient recipient) {
        StringBuilder result = new StringBuilder(recipient.communityname);
        if (StringUtils.isNotEmpty(recipient.groupname))
            result.append('-').append(recipient.groupname);
        if (StringUtils.isEmpty(recipient.groupname) && StringUtils.isNotEmpty(recipient.agent))
            result.append('-').append(recipient.agent);
        return result.toString();
    }

    /**
     * Gets the playlists defined in the Program Spec for a given Deployment. Note that playlists
     * may be different between languages, due to missing content in some languages.
     *
     * @param deploymentNo of the Deployment.
     * @param languages    of all the Recipients in the Deployment.
     * @return a map of { language : [ContentSpec.PlaylistSpec ] }
     */
    private Map<String, List<ContentSpec.PlaylistSpec>> getProgramSpecPlaylists(int deploymentNo,
        Set<String> languages)
    {
        ContentSpec contentSpec = context.programSpec.getContentSpec();
        ContentSpec.DeploymentSpec deploymentSpec = contentSpec.getDeployment(deploymentNo);
        Map<String, List<ContentSpec.PlaylistSpec>> programSpecPlaylists = new HashMap<>();
        for (String language : languages) {
            programSpecPlaylists.put(language, deploymentSpec.getPlaylistSpecs(language));
        }
        return programSpecPlaylists;
    }

    /**
     * Gets the playlists defined in the ACM for a given Deployment. If all content was imported,
     * and playlists were not manually edited, these will completely match the programSpec playlists.
     * Additional playlists may be present, if there were any created with the pattern #-pl-lang.
     *
     * @param deploymentNo of the Deployment.
     * @param languages    of all the Recipients in the Deployment.
     * @return a map of { language : [ Playlist ] }
     */
    private Map<String, List<Playlist>> getAcmPlaylists(int deploymentNo, Set<String> languages) {
        Map<String, List<Playlist>> acmPlaylists = new LinkedHashMap<>();
        Collection<Playlist> playlists = store.getPlaylists();
        for (String language : languages) {
            List<Playlist> langPlaylists = new ArrayList<>();
            // Look for anything matching the pattern, whether from the Program Spec or not.
            Pattern pattern = Pattern.compile(String.format("%d-.*-%s", deploymentNo, language));
            for (Playlist pl : playlists) {
                Matcher plMatcher = pattern.matcher(pl.getName());
                if (plMatcher.matches()) {
                    langPlaylists.add(pl);
                }
            }
            acmPlaylists.put(language, langPlaylists);
        }
        return acmPlaylists;
    }

}
