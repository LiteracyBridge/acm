package org.literacybridge.acm.gui.assistants.Deployment;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.store.IOContext;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptImportContext;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptTarget;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AcmContent;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.tbbuilder.CreateForV2;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.BELL_SOUND;
import static org.literacybridge.acm.gui.assistants.common.AcmAssistantPage.getLanguageAndName;
import static org.literacybridge.acm.tbbuilder.TBBuilder.MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;

public class ValidationPage extends AssistantPage<DeploymentContext> {
    /**
     * In the adjustments page the user had an opportunity to rearrange playlists, and titles
     * within playlists, and to remove titles or entire playlists. The results of that arrangement
     * is in a tree rooted in
     *     context.playlistRootNode
     * The tree structure is
     *   AcmContent.AcmRootNode
     *     AcmContent.LanguageNode              user object is languagecode
     *       AcmContent.PlaylistNode            user object is ACM Playlist
     *         AcmContent.AudioItemNode         user object is ACM AudioItem
     * Those are the playlists and messages that we need to validate.
     */

    private final JLabel deployment;
    private final JCheckBox deployWithWarnings;
    private final JCheckBox deployWithErrors;
    private final CardLayout issuesLayout;

    private final DeploymentContext context;

    private final JTree issuesTree;
    private final DefaultMutableTreeNode issuesTreeRoot;
    private final DefaultTreeModel issuesTreeModel;
    private final JPanel issuesBillboard;

    @SuppressWarnings("FieldCanBeLocal")
    private final String fatalIssuesWelcome =
        "<html>" + "<span style='font-size:2.5em'>Validation</span>" + "</ul>"
            + "<br/>Severe errors were found. The deployment can not be created. Click \"Cancel\" to exit."
            + "</html>";
    @SuppressWarnings("FieldCanBeLocal")
    private final String issuesWelcome =
        "<html>" + "<span style='font-size:2.5em'>Validation</span>" + "</ul>"
            + "<br/>Examine any issues, and click \"Next\" if you wish to proceed. " + "</html>";
    @SuppressWarnings("FieldCanBeLocal")
    private final String noIssuesWelcome =
        "<html>" + "<span style='font-size:2.5em'>Validation</span>" + "</ul>"
            + "<br/>Click \"Finish\" to proceed. " + "</html>";
    private final JLabel welcomeLabel;

    ValidationPage(PageHelper<DeploymentContext> listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        welcomeLabel = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Validate the Deployment</span>" + "</ul>"
                + "<br/>Examine any issues, and click \"Finish\" if you wish to proceed. "

                + "</html>");
        add(welcomeLabel, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        deployment = makeBoxedLabel();
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        deployWithWarnings = new JCheckBox(
            "Create deployment despite warnings. This may not conform to the Program Specification.");
        add(deployWithWarnings, gbc);
        deployWithWarnings.addActionListener(this::onSelection);
        deployWithErrors = new JCheckBox(
            "<html>Create deployment despite errors. <em>This will probably fail on some Talking Books</em>.</html>");
        add(deployWithErrors, gbc);
        deployWithErrors.addActionListener(this::onSelection);

        JPanel issuesCard = new JPanel(new BorderLayout());
        issuesTreeRoot = new DefaultMutableTreeNode();
        issuesTreeModel = new DefaultTreeModel(issuesTreeRoot);
        issuesTree = new JTree(issuesTreeModel);
        issuesTree.setRootVisible(false);
        issuesTree.setShowsRootHandles(true);
        JScrollPane issuesScroller = new JScrollPane(issuesTree);
        issuesCard.add(issuesScroller, BorderLayout.CENTER);

        // Show "No issues..." instead of an empty issues tree.
        JPanel noIssuesCard = new JPanel(new BorderLayout());
        Box noIssuesLabel = Box.createVerticalBox();
        noIssuesLabel.add(new JLabel(
            "<html><span style='font-size:2.5em'>No issues or differences found.</span></html>"));
        noIssuesLabel.add(Box.createVerticalGlue());
        noIssuesCard.add(noIssuesLabel, BorderLayout.CENTER);

        issuesLayout = new CardLayout();
        issuesBillboard = new JPanel(issuesLayout);
        issuesBillboard.add(issuesCard, "ISSUES");
        issuesBillboard.add(noIssuesCard, "NOISSUES");

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(issuesBillboard, gbc);

    }

    /**
     * Called when a selection changes.
     *
     * @param actionEvent is ignored.
     */
    @SuppressWarnings("unused")
    private void onSelection(ActionEvent actionEvent) {
        boolean ok = !context.issues.hasFatalError();
        ok = ok && !context.issues.hasError() || deployWithErrors.isSelected();
        ok = ok && (!context.issues.hasWarning() || deployWithWarnings.isSelected());
        setComplete(ok);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        deployment.setText(Integer.toString(context.deploymentNo));

        if (progressing) {
            // Reset issues when we enter with a (possibly) new deployment number.
            context.issues.clear();
            deployWithWarnings.setSelected(false);
            deployWithErrors.setSelected(false);
        }

        validateDeployment(context.deploymentNo);

        issuesTreeRoot.removeAllChildren();
        context.issues.fillNodeFromList(issuesTreeRoot);
        issuesTreeModel.reload();
        for (int ix = 0; ix < issuesTree.getRowCount(); ix++) {
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

    private void validateDeployment(int deploymentNo) {
        context.issues.clear();

        // Get the languages, and playlists for each language.
        // Check that all languages are a language in the ACM.
        validateDeploymentLanguages(context.languageCodes);

        // Check that we have all messages in all playlists; check if anything was added.
        validatePlaylists(deploymentNo, context.languageCodes);

        // Check that we have all system prompts in all languages.
        validateSystemPrompts(context.languageCodes);

        // Check that we have all category prompts for categories in each language.
        validatePlaylistPrompts();

        // Check that we have all recipient prompts for recipients in the deployment.
        validateRecipients(deploymentNo);

        // If there is an Intro Message category, validate that there's only one message.
        validateIntroMessage();

        validateFirmware();

        if (context.issues.hasNoIssues()) {
            welcomeLabel.setText(noIssuesWelcome);
            issuesLayout.show(issuesBillboard, "NOISSUES");
            deployWithErrors.setVisible(false);
            deployWithWarnings.setVisible(false);
        } else if (context.issues.hasFatalError()) {
            welcomeLabel.setText(fatalIssuesWelcome);
            issuesLayout.show(issuesBillboard, "ISSUES");
            deployWithWarnings.setVisible(false);
            deployWithErrors.setVisible(false);
        } else {
            welcomeLabel.setText(issuesWelcome);
            issuesLayout.show(issuesBillboard, "ISSUES");
            deployWithWarnings.setVisible(context.issues.hasWarning());
            deployWithErrors.setVisible(context.issues.hasError());
        }

        onSelection(null);
    }

    private void validateDeploymentLanguages(Collection<String> languages) {
        List<Locale> locales = ACMConfiguration.getInstance().getCurrentDB().getAudioLanguages();
        for (String language : languages) {
            Locale locale = new Locale(language);
            if (!locales.contains(locale)) {
                context.issues.add(Issues.Severity.ERROR,
                    Issues.Area.LANGUAGES,
                    "Language '%s' is used in the deployment, but is not defined in the ACM.",
                    getLanguageAndName(language));
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
            // Get the Program Spec playlists
            List<ContentSpec.PlaylistSpec> programSpecPlaylistSpecs = context.allProgramSpecPlaylists
                .get(language);

            // Get the adjusted list of ACM playlists.
            AcmContent.LanguageNode languageNode = context.playlistRootNode.getLanguageNode(language);
            if (languageNode == null) {
                context.issues.add(Issues.Severity.WARNING, Issues.Area.LANGUAGES,
                    "There is no content in the deployment for language '%s'", getLanguageAndName(language));
                continue;
            }

            List<AcmContent.PlaylistNode> adjustedPlaylists = languageNode.getPlaylistNodes();
            Set<AcmContent.PlaylistNode> foundPlaylists = new HashSet<>();
            // Try to find all the playlists from the Program Spec in the adjusted ACM playlists...
            for (ContentSpec.PlaylistSpec programSpecPlaylistSpec : programSpecPlaylistSpecs) {
                // Get the corresponding adjusted ACM playlist, if it exists.
                String qualifiedPlaylistName = AudioUtils.decoratedPlaylistName(programSpecPlaylistSpec.getPlaylistTitle(),
                    deploymentNo,
                    language);
                AcmContent.PlaylistNode playlistNode = adjustedPlaylists.stream()
                    .filter(p -> p.getDecoratedTitle().equals(qualifiedPlaylistName))
                    .findFirst()
                    .orElse(null);

                // If the ACM playlist is missing, note the issue.
                if (playlistNode == null) {
                    context.issues.add(Issues.Severity.WARNING,
                        Issues.Area.PLAYLISTS,
                        "Playlist '%s' is missing for language '%s' in the ACM.",
                        qualifiedPlaylistName,
                        getLanguageAndName(language));
                } else {
                    // ...otherwise, we have the playlist, so add to the list of ACM playlists, and check the contents.
                    foundPlaylists.add(playlistNode);
                    Set<AcmContent.AudioItemNode> foundItems = new HashSet<>();
                    // For the messages in the Program Spec...
                    for (ContentSpec.MessageSpec messageSpec : programSpecPlaylistSpec.getMessageSpecs()) {
                        // Get the corresponding ACM AudioItem.
                        AcmContent.AudioItemNode audioItemNode = playlistNode.getAudioItemNodes()
                            .stream()
                            .filter(it -> it.toString().equals(messageSpec.title))
                            .findFirst()
                            .orElse(null);
                        // If the ACM AudioItem is missing, note the issue.
                        if (audioItemNode == null) {
                            context.issues.add(Issues.Severity.WARNING,
                                Issues.Area.CONTENT,
                                "Title '%s' is missing in playlist '%s' for language '%s' in the ACM.",
                                messageSpec.title,
                                playlistNode.getTitle(),
                                getLanguageAndName(language));
                        } else {
                            foundItems.add(audioItemNode);
                        }
                    }
                    // We've looked in the ACM for messages in the Program Spec.
                    // Now see if there are messages added to the ACM playlist.
                    playlistNode.getAudioItemNodes()
                        .stream()
                        .filter(i -> !foundItems.contains(i))
                        .forEach(i -> context.issues.add(Issues.Severity.INFO,
                            Issues.Area.CONTENT,
                            "Title '%s' was added to playlist '%s' for language '%s' in the ACM.",
                            i.toString(),
                            playlistNode.getTitle(),
                            getLanguageAndName(language)));
                }
            }
            // Report the playlists in the ACM list but not the Program Specification list.
            adjustedPlaylists.stream()
                .filter(p -> !foundPlaylists.contains(p))
                .forEach(p -> context.issues.add(Issues.Severity.INFO,
                    Issues.Area.PLAYLISTS,
                    "Playlist '%s' was added to the ACM.",
                    p.getTitle()));
        }
    }

    /**
     * Determines whether we have all required system prompts in all languages for the Deployment.
     *
     * @param languages to be checked.
     */
    private void validateSystemPrompts(Collection<String> languages) {
        List<String> required_messages = TBBuilder.getRequiredSystemMessages(context.includeTbTutorial);
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Map<String, PromptsInfo.PromptInfo> storePromptsInfoMap = store.getPromptsMap();

        // { id : description }
        PromptsInfo promptsInfo = null;

        for (String language : languages) {
            //File languageDir = IOUtils.FileIgnoreCase(languagesDir, language);
            List<Integer> missing = new ArrayList<>();

            Map<String, SystemPrompts> systemPromptsMap = new HashMap<>();
            context.systemprompts.put(language, systemPromptsMap);

            for (String prompt : required_messages) {
                if (storePromptsInfoMap.containsKey(prompt)) {
                    SystemPrompts tempSystemPrompt = new SystemPrompts(prompt, language);
                    if (!tempSystemPrompt.findPrompts()) {
                        missing.add(Integer.parseInt(prompt));
                    } else {
                        systemPromptsMap.put(prompt, tempSystemPrompt);
                    }
                } else {
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
                    }
                    prevN = n;
                }
                msg.insert(0, "System prompts are missing for language '%s': ");
                Issues.Issue issue = context.issues.add(Issues.Severity.ERROR,
                    Issues.Area.SYSTEM_PROMPTS,
                    msg.toString(),
                    getLanguageAndName(language));
                // Add details on the individual missing messages.
                if (promptsInfo == null) {
                    promptsInfo = new PromptsInfo();
                }
                for (int n : missing) {
                    PromptsInfo.PromptInfo pi = promptsInfo.getPrompt(Integer.toString(n));
                    // The 'ding' doesn't have a filename.
                    if (pi != null) {
                        issue.addDetail(String.format("(%d) %s - \"%s\"", n, pi.getFilename(), pi.getText()));
                    }
                }
            }
        }
    }

    /**
     * Validates that the playlist prompts exist (short and long) for all playlists in all
     * languages.
     */
    private void validatePlaylistPrompts() {
        // Validate prompts for each language independently.
        for (AcmContent.LanguageNode languageNode: context.playlistRootNode.getLanguageNodes()) {
            String language = languageNode.getLanguageCode();
            Map<String, PlaylistPrompts> promptsMap = new HashMap<>();
            context.prompts.put(language, promptsMap);

            // Get all the playlists with content in this language, and get those playlist titles.
            List<String> playlistTitles = languageNode.getPlaylistNodes()
                .stream()
                .map(AcmContent.PlaylistNode::getTitle)
                .collect(Collectors.toList());
            // Users can always record feedback, which will be played back to them at least once,
            // and is announced as it is played back. Thus, we always need the user feedback
            // category. Add the UF category if it is not already present.
            Category ufCategory = store.getCategory(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
            String ufTitle = ufCategory.getCategoryName();
            if (!playlistTitles.contains(ufTitle)) {
                playlistTitles.add(ufTitle);
            }

            // Determine if we have both long and short prompts for each playlist in the language.
            for (String title : playlistTitles) {
                // Look for the audio recordings of the prompts. As a side effect, this will
                // find the categoryId for the prompt.
                PlaylistPrompts prompts = new PlaylistPrompts(title, language);
                prompts.findPrompts();
                promptsMap.put(title, prompts);

                // We don't use a prompt for the Intro Message; only care about the others.
                if (!Constants.CATEGORY_INTRO_MESSAGE.equals(prompts.categoryId)) {
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
                            getLanguageAndName(language),
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
                            getLanguageAndName(language),
                            title);

                    } else if (prompts.hasMixedPrompts()) {
                        context.issues.add(Issues.Severity.ERROR,
                            Issues.Area.PLAYLISTS,
                            "One ACM message and one category prompt was found in language '%s' for playlist '%s'. A complete set in one location is required.",
                            getLanguageAndName(language),
                            title);
                    }
                }

            }
        }
    }

    /**
     * If there is an "Intro Message' category, it should have exactly one message. Warn if there
     * is such a category, but too few or too many messages. (Only the last one is used.)
     */
    private void validateIntroMessage() {
        for (AcmContent.LanguageNode languageNode : context.playlistRootNode.getLanguageNodes()) {
            String language = languageNode.getLanguageCode();
            Map<String, PlaylistPrompts> promptsMap = context.prompts.get(language);

            for (AcmContent.PlaylistNode playlistNode : languageNode.getPlaylistNodes()) {
                String plTitle = playlistNode.getTitle();

                PlaylistPrompts prompts = promptsMap.get(plTitle);

                if (Constants.CATEGORY_INTRO_MESSAGE.equals(prompts.categoryId)) {
                    // Only the last message is actually used. But "last" is just an accident
                    // of history. It could have well been "first", or "random". Since it
                    // is deterministic, we can give a better warning.
                    List<String> titles = playlistNode.getAudioItemNodes()
                        .stream()
                        .map(AcmContent.AudioItemNode::toString)
                        .collect(Collectors.toList());

                    if (titles.size() > 1) {
                        context.issues.add(Issues.Severity.INFO, Issues.Area.INTRO_MESSAGE,
                            "Only one message will be played as intro for language '%s': %s",
                            getLanguageAndName(language), titles.get(titles.size()-1));
                        for (String title : titles.subList(0, titles.size()-1)) {
                            context.issues.add(Issues.Severity.INFO, Issues.Area.INTRO_MESSAGE,
                                "This message will be skipped in intro for language '%s': %s",
                                getLanguageAndName(language), title);
                        }
                    } else if (titles.size() == 0) {
                        context.issues.add(Issues.Severity.INFO, Issues.Area.INTRO_MESSAGE,
                            "There is an 'Intro Message' category for language '%s', but it has no message.",
                            getLanguageAndName(language));
                    }
                }
            }
        }
    }

    private void validateRecipients(int deploymentNo) {
        boolean warnForMissingGreetings = ACMConfiguration.getInstance().getCurrentDB().getWarnForMissingGreetings();
        if (!warnForMissingGreetings) {
            return;
        }

        RecipientList recipients = context.getProgramSpec().getRecipientsForDeployment(deploymentNo);
        Map<String, String> recipientsMap = context.getProgramSpec().getRecipientsMap();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        File communitiesDir = IOUtils.FileIgnoreCase(tbLoadersDir, "communities");
        if (!communitiesDir.exists() || !communitiesDir.isDirectory()) {
            context.issues.add(Issues.Severity.ERROR,
                Issues.Area.CUSTOM_GREETINGS,
                "The 'communities' directory is missing in this program.");
            return;
        }

        for (Recipient recipient : recipients) {
            String dirName = recipientsMap.getOrDefault(recipient.recipientid, recipient.recipientid);
            File recipientDir = (dirName == null) ?
                                null :
                                IOUtils.FileIgnoreCase(communitiesDir, dirName);
            if (recipientDir == null || !recipientDir.exists() || !recipientDir.isDirectory()) {
                context.issues.add(Issues.Severity.WARNING,
                    Issues.Area.CUSTOM_GREETINGS,
                    "Missing directory for recipient '%s'.",
                    recipName(recipient));
            } else {
                File languagesDir = IOUtils.FileIgnoreCase(recipientDir, "languages");
                File languageDir = IOUtils.FileIgnoreCase(languagesDir, recipient.languagecode);
                File promptFile = IOUtils.FileIgnoreCase(languageDir, "10.a18");
                if (!languageDir.exists() || !languageDir.isDirectory()) {
                    context.issues.add(Issues.Severity.WARNING,
                        Issues.Area.CUSTOM_GREETINGS,
                        "Missing 'languages/%s' directory for recipient '%s'.",
                        recipient.languagecode,
                        recipName(recipient));
                } else if (!promptFile.exists()) {
                    context.issues.add(Issues.Severity.WARNING,
                        Issues.Area.CUSTOM_GREETINGS,
                        "No custom greeting for recipient '%s'.",
                        recipName(recipient));
                }

                File systemDir = new File(recipientDir, "system");
                File[] groupDirs = systemDir.listFiles((dir, name) -> name.toLowerCase()
                    .endsWith(GROUP_FILE_EXTENSION));
                if (!systemDir.exists() || !systemDir.isDirectory()) {
                    context.issues.add(Issues.Severity.WARNING,
                        Issues.Area.CUSTOM_GREETINGS,
                        "Missing 'system' directory for recipient '%s'.",
                        recipName(recipient));
                } else if (groupDirs == null || groupDirs.length == 0) {
                    context.issues.add(Issues.Severity.WARNING,
                        Issues.Area.CUSTOM_GREETINGS,
                        "No '.grp' file for recipient '%s'.",
                        recipName(recipient));
                }
            }

        }
    }

    private void validateFirmware() {
            validateV1Firmware();
        if (ACMConfiguration.getInstance().getCurrentDB().hasTbV2Devices()) {
            validateV2Firmware();
        }
    }
    private void validateV1Firmware() {
        // BE SURE that the "no firmware" message compares less than MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE
        String noFirmware = "no firmware image";
        assert (noFirmware.compareTo(MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE) < 0);

        // A firmware update may be required to support hidden user feedback. Check the
        // version currently in the project.
        if (!context.userFeedbackPublic) {
            // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
            File sourceTbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getPathProvider().getProgramTbLoadersDir();
            File sourceTbOptionsDir = new File(sourceTbLoadersDir, "TB_Options");
            File latestFirmware = null;
            File[] firmwareVersions = new File(sourceTbOptionsDir, "firmware").listFiles();
            if (firmwareVersions != null) {
                for (File f : firmwareVersions) {
                    if (latestFirmware == null) {
                        latestFirmware = f;
                    } else if (latestFirmware.getName().compareToIgnoreCase(f.getName()) < 0) {
                        latestFirmware = f;
                    }
                }
            }

            String image =
                    latestFirmware != null ? latestFirmware.getName().toLowerCase() : noFirmware;
            if (image.compareTo(MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE) < 0) {
                context.issues.add(Issues.Severity.FATAL,
                        Issues.Area.FIRMWARE,
                        "Minimum firmware image for hidden user feedback is %s, but found %s.",
                        MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE,
                        image);
            }
        }
    }

    /**
     * Validates that if there are override directories for the firmware or control state machine files,
     * those directories contain the required firmware and control state machine files. If not, a warning
     * will be generated, and the files will come from the system defaults.
     */
    private void validateV2Firmware() {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        File tbOptions = new File(dbConfig.getPathProvider().getProgramTbLoadersDir(), "TB_Options");
        File firmwareOverride = new File(tbOptions, "firmware.v2");
        validateLocalOverrideDirectory(firmwareOverride, CreateForV2::isValidFirmwareSource, "V2 firmware");
        File csmOverride = new File(tbOptions, "system.v2");
        validateLocalOverrideDirectory(csmOverride, CreateForV2::isValidCSMSource, "CSM files");
    }

    private void validateLocalOverrideDirectory(File directory, Predicate<File> isValid, String label) {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                context.issues.add(Issues.Severity.INFO,
                        Issues.Area.FIRMWARE,
                        "'%s' exists, but is not a directory. The default %s will be used.",
                        directory.getName(), label);
            } else {
                if (!isValid.test(directory)) {
                    context.issues.add(Issues.Severity.INFO,
                            Issues.Area.FIRMWARE,
                            "'%s' not contain %s files. The default %2$s will be used",
                            directory.getName(), label);
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

}
