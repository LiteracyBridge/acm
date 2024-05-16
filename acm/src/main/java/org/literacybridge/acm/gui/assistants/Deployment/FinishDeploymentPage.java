package org.literacybridge.acm.gui.assistants.Deployment;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.ProblemReviewDialog;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AcmContent.AudioItemNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.LanguageNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.PlaylistNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.EmailHelper;
import org.literacybridge.acm.utils.EmailHelper.TD;
import org.literacybridge.acm.utils.EmailHelper.TH;
import org.literacybridge.acm.utils.EmailHelper.TR;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.Version;
import org.literacybridge.core.spec.ContentSpec.DeploymentSpec;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;
import org.literacybridge.core.spec.ContentSpec.PlaylistSpec;
import org.literacybridge.core.spec.Deployment;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Calendar.YEAR;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import static org.literacybridge.acm.utils.EmailHelper.pinkZebra;

@SuppressWarnings({"JavadocBlankLines", "FieldCanBeLocal"})
public class FinishDeploymentPage extends AcmAssistantPage<DeploymentContext> {

    private final JLabel publishNotification;
    private final JLabel statusLabel;
    private final JButton viewErrorsButton;
    private final JLabel currentState;
    private final JLabel revisionText;

    private final DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
    private final List<Exception> errors = new ArrayList<>();
    private final DateTimeFormatter localDateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
        .withZone(ZoneId.systemDefault());
    private StringBuilder summaryMessage;

    private final String publishNotificationText = "<html>The deployment was <u>not</u> published. To publish, start the "
                                             +"Deployment Assistant again, and do <u>not</u> select the 'Do not publish the deployment.' option.</html>";
    private final String publishErrorNotificationText = "<html>The deployment was <u>not</u> published, due to errors. Correct the errors and run the "
            +"Deployment Assistant again. If you need assistance, please click on 'View Errors', and choose "
            +"'Send to Amplio'.</html>";

    FinishDeploymentPage(PageHelper<DeploymentContext> listener) {
        super(listener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Finished creating deployment</span>"
                + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        JLabel deployment = makeBoxedLabel(Integer.toString(context.deploymentNo));
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        publishNotification = new JLabel(publishNotificationText);
        add(publishNotification, gbc);
        publishNotification.setVisible(false);

        // The status line. Could be updated as the deployment progresses.
        hbox = Box.createHorizontalBox();
        JLabel summary = new JLabel();
        summary.setText(String.format("Creating deployment %d as %s",
            context.deploymentNo, deploymentName()));
        hbox.add(summary);
        revisionText = new JLabel();
        revisionText.setVisible(false);
        hbox.add(revisionText);
        hbox.add(new JLabel("."));
        hbox.add(Box.createHorizontalStrut(5));
        viewErrorsButton = new JButton("View Errors");
        viewErrorsButton.setVisible(false);
        viewErrorsButton.addActionListener(this::onViewErrors);
        hbox.add(viewErrorsButton);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        // Current item being imported.
        currentState = new JLabel("...");
        add(currentState, gbc);

        // Working / Finished
        statusLabel = new JLabel("");
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        add(statusLabel, gbc);
        setStatus("Working...");

        // Absorb any vertical space.
        gbc.weighty = 1.0;
        add(new JLabel(), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        String programId = ACMConfiguration.getInstance().getCurrentDB().getProgramId();
        Calendar startdate = Calendar.getInstance();
        Deployment depl = context.getProgramSpec().getDeployment(context.deploymentNo);
        startdate.setTime(depl.startdate);
        context.deploymentInfo = new DeploymentInfo(programId, context.deploymentNo, startdate);
        context.deploymentInfo.setUfPublic(context.userFeedbackPublic);
        context.deploymentInfo.setTutorial(context.includeTbTutorial);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                summaryMessage = new StringBuilder("<html>");
                summaryMessage.append(String.format("<h1>Deployment %d for Program %s</h1>",
                    context.deploymentNo, dbConfig.getFriendlyName()));
                summaryMessage.append(String.format("<h3>Created on %s</h3>", localDateFormatter.format(LocalDateTime.now())));

                try {
                    createDeployment();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return 0;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                summaryMessage.append("</html>");
                EmailHelper.sendNotificationEmail("Deployment Created", summaryMessage.toString());

                setComplete();

                if (errors.size() > 0) {
                    setStatus("Finished, but with errors.");
                    statusLabel.setForeground(Color.red);
                    viewErrorsButton.setVisible(true);
                } else {
                    setStatus("Finished.");
                }

                setComplete();

                UIUtils.setLabelText(currentState, "Click \"Close\" to return to the ACM.");
            }
        };

        worker.execute();
        setComplete(false);
    }

    @Override
    protected String getTitle() {
        return "Created Deployment";
    }

    @Override
    protected boolean isSummaryPage() { return true; }

    private void setStatus(String status) {
        UIUtils.setLabelText(statusLabel, "<html>" + "<span style='font-size:2.5em'>"+status+"</span>" + "</html>");
    }

    /**
     * Handle the "view errors" button. Opens a dialog that presents the errors. The dialog
     * has a button to send this error report to Amplio.
     * @param actionEvent is ignored.
     */
    private void onViewErrors(@SuppressWarnings("unused") ActionEvent actionEvent) {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        String computerName;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }

        String message = "<html>The following error(s) occurred when attempting to create the deployment. " +
            "If the problem persists, contact Amplio technical support. The button below will send this report to Amplio."+
            "</html>";
        String reportHeading = String.format("Error report from Deployment Assistant%n%n" +
                "Project %s (%s), User %s (%s), Computer %s%nCreate Deployment at %s%n" +
                "Deployment %d%n"+
                "ACM Version %s, built %s%n",
            dbConfig.getFriendlyName(),
            dbConfig.getFriendlyName(),
            ACMConfiguration.getInstance().getUserName(),
            ACMConfiguration.getInstance().getUserContact(),
            computerName,
            localDateFormatter.format(LocalDateTime.now()),
            context.deploymentNo,
            Constants.ACM_VERSION, Version.buildTimestamp);

        ProblemReviewDialog dialog = new ProblemReviewDialog(Application.getApplication(),
            "Errors While Deploying",
            "Error report from Deployment Assistant");
        DefaultMutableTreeNode issues = new DefaultMutableTreeNode();
        context.issues.fillNodeFromList(issues);
        dialog.showProblems(message, reportHeading, issues, errors);
    }

    /**
     * Creates the Deployment, and publishes if configured to do so.
     */
    private void createDeployment() {
        makeDeploymentReport();

        if (!makeDirectories()) {
            // error already recorded
            return;
        }

        // Create the files in TB-Loaders/packages to match exporting playlists.
        Map<String, Map<String, String>> pkgs = exportPackages();

        // New style, to support creating multiple package flavors.
        gatherDeploymentInfo();
        Map<String, Map<String, String>> newPkgs = exportV1Packages();
        assert(newPkgs.equals(pkgs));

        try {
            TBBuilder tbBuilder = new TBBuilder(ACMConfiguration.getInstance().getCurrentDB(), context.deploymentNo, deploymentName(), this::reportState, this::logException);

            // Create.
            final List<String> args = new ArrayList<>();
            pkgs.forEach((language, values) -> values.forEach((variant, pkg) ->{
                if (StringUtils.isBlank(variant)) {
                    variant = language;
                }
                args.add(pkg); // package
                args.add(language);   // language
                args.add(variant);   // group
            }));
            // Old style deployment creation, driven by files in "TB-Loaders/packages"
            tbBuilder.createDeployment(args);
            saveDeploymentInfoToProgramSpec(tbBuilder, pkgs);

            // New style, driven by deploymentInfo
            tbBuilder.createDeployment(context.deploymentInfo);

            // Publish.
            if (context.isPublish() && errors.size()==0) {
                args.clear();
                args.add(deploymentName());
                tbBuilder.publishDeployment(args);
                UIUtils.setLabelText(revisionText, String.format(" (revision '%s')", tbBuilder.getRevision()));
                UIUtils.setVisible(revisionText, true);
            } else {
                if (context.isPublish()) {
                    context.issues.add(Issues.Severity.FATAL, Issues.Area.DEPLOYMENT, "Deployment not published due to errors.", (Object)null);
                    errors.add(new DeploymentException("Deployment not published due to errors.", null));
                    publishNotification.setText(publishErrorNotificationText);
                }
                publishNotification.setVisible(true);
            }
        } catch (Exception e) {
            errors.add(new DeploymentException("Exception creating deployment", e));
            e.printStackTrace();
        }

        if (ACMConfiguration.isTestData()) {
            // Fake error for testing.
            errors.add(new DeploymentException("Simulated error for testing.", null));
        }
    }

    /**
     * Save some information about the Deployment to the programspec directory.
     * @param tbb the TB-Builder; knows the directory into which to write the properties.
     * @param pkgs a map of {language : { variant : package}}
     */
    private void saveDeploymentInfoToProgramSpec(TBBuilder tbb,
        Map<String, Map<String, String>> pkgs) {
        Properties deploymentProperties = new Properties();
        deploymentProperties.setProperty(Constants.AUDIO_LANGUAGES, dbConfig.getConfigLanguages());
        deploymentProperties.setProperty(Constants.HAS_TBV2_DEVICES, dbConfig.hasTbV2Devices().toString());
        deploymentProperties.setProperty(TBLoaderConstants.PROGRAM_FRIENDLY_NAME_PROPERTY, dbConfig.getFriendlyName());
        deploymentProperties.setProperty(TBLoaderConstants.PROGRAM_DESCRIPTION_PROPERTY, dbConfig.getFriendlyName());
        deploymentProperties.setProperty(TBLoaderConstants.PROGRAM_ID_PROPERTY, dbConfig.getProgramId());
        deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_NUMBER, Integer.toString(context.deploymentNo));
        Date now = new Date();
        List<String> acceptableFirmwareVersions = tbb.getAcceptableFirmwareVersions(context.userFeedbackPublic);
        deploymentProperties.setProperty(TBLoaderConstants.ACCEPTABLE_FIRMWARE_VERSIONS, String.join(",", acceptableFirmwareVersions));
        DateFormat localTime = new SimpleDateFormat("HH:mm:ss a z", Locale.getDefault());
        DateFormat localDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_CREATION_TIME, localTime.format(now));
        deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_CREATION_DATE, localDate.format(now));
        String userName = ""; // TODO: replace with greeting? full name? Authenticator.getInstance().getUserName();
        String userEmail = Authenticator.getInstance().getUserEmail();
        String user = (StringUtils.isEmpty(userName)?"":'('+userName+") ") + userEmail;         
        deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_CREATION_USER, user);
        // Map of language, variant : package
        pkgs.forEach((language, values) -> values.forEach((variant, pkg) ->{
            String key = language;
            if (StringUtils.isNotBlank(variant)) key = key + "," + variant;
            deploymentProperties.put(key, pkg);
        }));

        File stagedProgramSpecDir = tbb.getStagedProgramspecDir();
        File propsFile = new File(stagedProgramSpecDir, ProgramSpec.DEPLOYMENT_INFO_PROPERTIES_NAME);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(propsFile))) {
            deploymentProperties.store(out, null);
        } catch (IOException e) {
            // Ignore and continue without deployment properties.
        }
    }

    /**
     * Reports status & state from the TB-Builder to the user.
     * @param state to be reported.
     */
    private void reportState(String state) {
        UIUtils.setLabelText(currentState, state);
    }

    private void logException(Exception exception) {
        errors.add(exception);
    }

    /**
     * Builds the lists of content, and the list of lists. Returns a map
     * of language to package name. Will extract any content prompts to a
     * packages/${package}/prompts directory.
     *
     * Every recipient may (or may not) be tagged with a single variant.
     * Every message may (or may not) be tagged with one or more variants.
     *
     * Recipients receive all un-tagged (non-variant) messages, plus any messages tagged with that recipient's variant.
     *
     * For every language,
     *   find the variants that need to be built, based on the recipients for that langauge.
     *   For every language-variant combination
     *     create packageDir = ${acmDir}/TB-Loaders/packages/${deployment}-${deploymentNumber}-${languagecode}-${variant}
     *     create listsDir = ${packageDir}/messages/lists/1
     *     create promptsDir = ${packageDir}/prompts/${languagecode}/cat
     *     create an _activeLists.txt file
     *     For every playlists in that language-variant
     *       find the content
     *       create a ${category}.txt file listing the content files
     *       possibly create a ${promptsDir}/${category}.ids file with the message ids of the short and long prompts.
     *
     *
     * @return {language : {variant: pkgName}}
     */
    private Map<String, Map<String, String>> exportPackages() {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        File packagesDir = new File(tbLoadersDir, "packages");
        DeploymentSpec deploymentSpec = context.getProgramSpec().getContentSpec().getDeployment(context.deploymentNo);

        for (LanguageNode languageNode : context.playlistRootNode.getLanguageNodes()) {
            String language = languageNode.getLanguageCode();
            Map<String, String> languageResult = new LinkedHashMap<>();
            List<PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecsForLanguage(language);
            Collection<String> variants = context.getProgramSpec().getVariantsForDeploymentAndLanguage(context.deploymentNo, language);

            for (String variant : variants) {
                // one entry in the language : package-name map.
                String packageName = packageName(language, variant);
                languageResult.put(variant, packageName);
                File packageDir = new File(packagesDir, packageName);
                File listsDir = new File(packageDir,
                    "messages" + File.separator + "lists" + File.separator + "1");
                File promptsDir = new File(packageDir,
                    "prompts" + File.separator + language + File.separator + "cat");

                // Create the list files, and copy the non-predefined prompts.
                boolean haveUserFeedbackListFile = false;
                File activeLists = new File(listsDir, "_activeLists.txt");
                String title = null;
                try (PrintWriter activeListsWriter = new PrintWriter(activeLists)) {
                    Map<String, PlaylistPrompts> playlistsPrompts = context.prompts.get(language);
                    for (PlaylistNode playlistNode : languageNode.getPlaylistNodes()) {
                        title = playlistNode.getTitle();
                        PlaylistPrompts prompts = playlistsPrompts.get(title);

                        // Filter the playlist by the variants specified in the program specification.
                        // We're not filtering by language, because this ACM playlist is already
                        // filtered by language.
                        // Fall back is the playlist node as found in the ACM.
                        PlaylistNode filteredNode = playlistNode;
                        String plTitle = title;
                        // Try to find a playlist in the spec that matches the playlist from the ACM.
                        PlaylistSpec playlistSpec = playlistSpecs.stream()
                            .filter(pls->pls.getPlaylistTitle().equals(plTitle))
                            .findFirst()
                            .orElse(null);
                        // If we found a playlist in the spec...
                        if (playlistSpec != null) {
                            // Create a new node to receive all the messages that are not
                            // filtered out by the variant.
                            PlaylistNode newNode = new PlaylistNode(playlistNode.getPlaylist());
                            // Examine the audio items in the playlist
                            for (AudioItemNode audioItemNode : playlistNode.getAudioItemNodes()) {
                                String msgTitle = audioItemNode.getAudioItem().getTitle();
                                // Try to find a message in the spec that matches the message in
                                // the ACM.
                                MessageSpec messageSpec = playlistSpec.getMessageSpecs().stream()
                                    .filter(mss->mss.getTitle().equals(msgTitle))
                                    .findFirst()
                                    .orElse(null);
                                // If the message isn't in the spec, it was added manually, so keep
                                // it. If it is in the spec, check the variant for inclusion.
                                if (messageSpec == null || messageSpec.includesVariant(variant)) {
                                    newNode.add(new AudioItemNode(audioItemNode));
                                }
                            }
                            filteredNode = newNode;
                        }

                        // If any audio items remain after filtering, create the {playlist}.txt file.
                        if (filteredNode.getAudioItemNodes().size() > 0) {
                            String promptCat = getPromptCategoryAndFiles(prompts, promptsDir, language);
                            // The intro message is handled specially, in the control file.
                            // User feedback category is added later. The best way to have feedback from
                            // users is a category named "Selected Feedback from Users" or some such.
                            if (!(promptCat.equals(Constants.CATEGORY_INTRO_MESSAGE)
                                || promptCat.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK))) {
                                activeListsWriter.println("!" + promptCat);
                            }
                            createListFile(filteredNode, promptCat, listsDir);
                            haveUserFeedbackListFile |= promptCat.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                        }
                    }

                    if (context.userFeedbackPublic) {
                        activeListsWriter.println(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                    }
                    if (context.includeTbTutorial) {
                        activeListsWriter.println('!' + Constants.CATEGORY_TUTORIAL);
                    }
                    if (!haveUserFeedbackListFile) {
                        // Create an empty "9-0.txt" file if there isn't already such a file. Needed so that the
                        // first UF message recorded can be reviewed by the user.
                        createListFile(new PlaylistNode(null), Constants.CATEGORY_UNCATEGORIZED_FEEDBACK, listsDir);
                    }
                } catch (Exception e) {
                    errors.add(new DeploymentException(String.format("Error exporting playlist '%s'",
                        title), e));
                    e.printStackTrace();
                }
            }

            if (languageResult.size() > 0) {
                result.put(language, languageResult);
            }
        }
        return result;
    }

    private void gatherDeploymentInfo() {
        DeploymentSpec deploymentSpec = context.getProgramSpec().getContentSpec().getDeployment(context.deploymentNo);

        for (LanguageNode languageNode : context.playlistRootNode.getLanguageNodes()) {
            String languagecode = languageNode.getLanguageCode();
            List<PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecsForLanguage(languagecode);
            Collection<String> variants = context.getProgramSpec().getVariantsForDeploymentAndLanguage(context.deploymentNo, languagecode);

            for (String variant : variants) {
                DeploymentInfo.PackageInfo packageInfo =  context.deploymentInfo.addPackage(languagecode, variant);

                String title = null;
                try {
                    Map<String, PlaylistPrompts> playlistsPrompts = context.prompts.get(languagecode);
                    for (PlaylistNode playlistNode : languageNode.getPlaylistNodes()) {
                        title = playlistNode.getTitle();
                        PlaylistPrompts prompts = playlistsPrompts.get(title);

                        // Filter the playlist by the variants specified in the program specification.
                        // We're not filtering by language, because this ACM playlist is already
                        // filtered by language.
                        // Fall back is the playlist node as found in the ACM.
                        PlaylistNode filteredNode = playlistNode;
                        String plTitle = title;
                        // Try to find a playlist in the spec that matches the playlist from the ACM.
                        PlaylistSpec playlistSpec = playlistSpecs.stream()
                            .filter(pls->pls.getPlaylistTitle().equals(plTitle))
                            .findFirst()
                            .orElse(null);
                        // If we found a playlist in the spec...
                        if (playlistSpec != null) {
                            // Create a new node to receive all the messages that are not
                            // filtered out by the variant.
                            PlaylistNode newNode = new PlaylistNode(playlistNode.getPlaylist());
                            // Examine the audio items in the playlist
                            for (AudioItemNode audioItemNode : playlistNode.getAudioItemNodes()) {
                                String msgTitle = audioItemNode.getAudioItem().getTitle();
                                // Try to find a message in the spec that matches the message in
                                // the ACM.
                                MessageSpec messageSpec = playlistSpec.getMessageSpecs().stream()
                                    .filter(mss->mss.getTitle().equals(msgTitle))
                                    .findFirst()
                                    .orElse(null);
                                // If the message isn't in the spec, it was added manually, so keep
                                // it. If it is in the spec, check the variant for inclusion.
                                if (messageSpec == null || messageSpec.includesVariant(variant)) {
                                    newNode.add(new AudioItemNode(audioItemNode));
                                }
                            }
                            filteredNode = newNode;
                        }

                        // If any audio items remain after filtering, create the {playlist}.txt file.
                        if (filteredNode.getAudioItemNodes().size() > 0) {
                            String promptCat = getPromptCategory(prompts, languagecode);
                            // If this is an intro message, simply note that on the packageInfo. It doesn't get
                            // an entry in the _activeLists, nor a categoryid.txt file.
                            if (promptCat.equals(Constants.CATEGORY_INTRO_MESSAGE)) {
                                packageInfo.setIntroItem(filteredNode.getAudioItemNodes().get(0).getAudioItem());
                            } else {
                                // Is this a pre-populated 9-0, Uncategorized User Feeedback, playlist?
                                // That's weird, but legal.
                                boolean isUserFeedback = promptCat.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);

                                DeploymentInfo.PackageInfo.PlaylistBuilder plPlaylistBuilder = packageInfo.new PlaylistBuilder()
                                    .withTitle(plTitle)
                                    .withPrompts(prompts)
                                    .isLocked(!isUserFeedback)
                                    .isUserFeedback(isUserFeedback);
                                DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo = packageInfo.addPlaylist(
                                    plPlaylistBuilder);
                                for (AudioItemNode audioItemNode : filteredNode.getAudioItemNodes()) {
                                    AudioItem audioItem = audioItemNode.getAudioItem();
                                    playlistInfo.addContent(audioItem);
                                }
                            }
                        }
                    }
                } catch (IllegalStateException e) {
                    errors.add(new DeploymentException(String.format("Error exporting playlist '%s'",
                        title), e));
                    e.printStackTrace();
                }
            }

        }
        context.deploymentInfo.prune();
        System.out.println(context.deploymentInfo);
    }

    private Map<String, Map<String, String>> exportV1Packages() {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        File packagesDir = new File(tbLoadersDir, "v1packages");

        for (DeploymentInfo.PackageInfo packageInfo : context.deploymentInfo.getPackages()) {
            String languageCode = packageInfo.getLanguageCode();
            String variant = packageInfo.getVariant();

            Map<String, String> languageResult = result.computeIfAbsent(languageCode, k-> new LinkedHashMap<>());

            // one entry in the language : package-name map.
            String packageName = packageInfo.getShortName();
            languageResult.put(variant, packageName);
            File packageDir = new File(packagesDir, packageName);
            File listsDir = new File(packageDir,
                "messages" + File.separator + "lists" + File.separator + "1");
            if (!listsDir.exists()) {
                if (!listsDir.mkdirs()) errors.add(new MakeDirectoryException("prompts", listsDir));
            }
            File promptsDir = new File(packageDir,
                "prompts" + File.separator + languageCode + File.separator + "cat");

            // Create the list files, and copy the non-predefined prompts.
            boolean haveUserFeedbackListFile = false;
            File activeLists = new File(listsDir, "_activeLists.txt");
            String title = null;
            try (PrintWriter activeListsWriter = new PrintWriter(activeLists)) {
                for (DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo : packageInfo.getPlaylists()) {
                    title = playlistInfo.getTitle();
                    PlaylistPrompts prompts = playlistInfo.getPlaylistPrompts();

                    String promptCat = getPromptCategoryAndFiles(prompts, promptsDir, languageCode);
                    // The intro message is handled specially, in the control file.
                    // User feedback category is added later. The best way to have feedback from
                    // users is a category named "Selected Feedback from Users" or some such.
                    if (!(promptCat.equals(Constants.CATEGORY_INTRO_MESSAGE)
                        || promptCat.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK))) {
                        activeListsWriter.println("!" + promptCat);
                    }
                    createListFile(playlistInfo.getAudioItemIds(), promptCat, listsDir);
                    haveUserFeedbackListFile |= promptCat.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);

                }
                // If the package has an intro, create a dummy 5-0.txt file.
                if (packageInfo.hasIntro()) {
                    createListFile(new ArrayList<>(Collections.singleton(packageInfo.getIntro().getId())), Constants.CATEGORY_INTRO_MESSAGE, listsDir);
                }

                if (packageInfo.hasUserFeedbackPlaylist()) {
                    activeListsWriter.println(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                }
                if (packageInfo.hasTutorial()) {
                    activeListsWriter.println('!' + Constants.CATEGORY_TUTORIAL);
                }
                if (!haveUserFeedbackListFile) {
                    // Create an empty "9-0.txt" file if there isn't already such a file. Needed so that the
                    // first UF message recorded can be reviewed by the user.
                    createListFile(new PlaylistNode(null), Constants.CATEGORY_UNCATEGORIZED_FEEDBACK, listsDir);
                }
            } catch (IllegalStateException | IOException | BaseAudioConverter.ConversionException | AudioItemRepository.UnsupportedFormatException e) {
                errors.add(new DeploymentException(String.format("Error exporting playlist '%s'",
                    title), e));
                e.printStackTrace();
            }

            if (languageResult.size() > 0) {
                result.put(languageCode, languageResult);
            }
        }
        return result;
    }

    /**
     * Creates the directories that are needed to create the deployment.
     * @return true if the directories were created successfully.
     */
    private boolean makeDirectories() {
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        File packagesDir = new File(tbLoadersDir, "packages");
        if ((packagesDir.exists() && !packagesDir.isDirectory()) || (!packagesDir.exists() && !packagesDir.mkdirs())) {
            // Can't make packages directory.
            errors.add(new MakeDirectoryException("packages", packagesDir));
            return false;
        }
        Collection<String> variants = context.getProgramSpec().getVariantsForDeployment(context.deploymentNo);

        for (LanguageNode languageNode : context.playlistRootNode.getLanguageNodes()) {
            String language = languageNode.getLanguageCode();

            for (String variant: variants) {
                // Create the directories.
                String packageName = packageName(language, variant);
                File packageDir = new File(packagesDir, packageName);
                // Clean any old data.
                IOUtils.deleteRecursive(packageDir);
                if (packageDir.exists()) {
                    System.out.printf("Couldn't delete %s, trying again%n", packageDir.getAbsolutePath());
                    // Windows sometimes fails to delete the top level directory. Wait a bit and try again.
                    try { Thread.sleep(500);} catch (InterruptedException ignored) { }
                    IOUtils.deleteRecursive(packageDir);
                    if (packageDir.exists()) {
                        // Couldn't clean up.
                        errors.add(new RemoveDirectoryException(packageDir));
                        return false;
                    }
                }
                if (!packageDir.mkdirs()) {
                    // Can't make package directory
                    errors.add(new MakeDirectoryException("package", packageDir));
                    return false;
                }
                File listsDir = new File(packageDir,
                    "messages" + File.separator + "lists" + File.separator + "1");
                if (!listsDir.mkdirs()) {
                    // Can't make lists directory
                    errors.add(new MakeDirectoryException("lists", listsDir));
                    return false;
                }
                // This directory won't be created until it is needed. But if we're able to get here, we're almost
                // certainly able to create the prompts directory.
                //File promptsDir = new File(packageDir, "prompts"+File.separator+language+File.separator+"cat");
            }

        }
        return true;
    }

    /**
     * Creates a playlist file, which is a file named for a playlist, and containing
     * the content ids of the AudioItems in the playlist.
     * @param playlistNode The source of the Audio Items.
     * @param promptCat The category by which the file will be named.
     * @param listsDir Directory into which the list file should be written.
     * @throws FileNotFoundException if the file can't be created.
     */
    private void createListFile(PlaylistNode playlistNode, String promptCat, File listsDir)
        throws FileNotFoundException
    {
        List<String> audioItemIds = playlistNode.getAudioItemNodes()
            .stream()
            .map(n -> n.getAudioItem().getId())
            .collect(Collectors.toList());
        createListFile(audioItemIds, promptCat, listsDir);
    }

    private void createListFile(Iterable<? extends String> audioItemIds, String promptCat, File listsDir)
            throws FileNotFoundException {
        File listFile = new File(listsDir, promptCat + ".txt");
        try (PrintWriter listWriter = new PrintWriter(listFile)) {
            for (String audioItemId : audioItemIds) {
                listWriter.println(audioItemId);
            }
        }
    }

    /**
     * Gets the prompt category to be used in the _activeLists.txt file, and if necessary
     * extracts prompts from the ACM.
     *
     * If there is an audio item (ie, in the ACM proper) for the prompt, that will be used
     * preferentially to the pre-defined prompt, because that is more accessible to the
     * program manager.
     * 
     * @param prompts The prompts that were found earlier, if any.
     * @param promptsDir The language-specific directory to which any content prompts should be extracted.
     * @param language The languagecode of the given prompts. Used only to give better error messages.
     * @return the category, as a String.
     * @throws IOException if the audio file can't be written.
     * @throws BaseAudioConverter.ConversionException If the audio file can't be converted
     *          to .a18 format.
     */
    private String getPromptCategoryAndFiles(PlaylistPrompts prompts,
        File promptsDir,
        String language)
        throws IOException, BaseAudioConverter.ConversionException, AudioItemRepository.UnsupportedFormatException {
        String promptCat = getPromptCategory(prompts, language);

        AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
        // If there is not a pre-defined set of prompt files, we'll need to extract the content audio to the
        // promptsDir. In that case record the extracted filenames in ${category}.ids properties file.
        Properties idProperties = null;
        if (/*prompts.shortPromptFile == null &&*/ prompts.shortPromptItem != null) {
            if (!promptsDir.exists()) {
                if (!promptsDir.mkdirs()) errors.add(new MakeDirectoryException("prompts", promptsDir));
            }
            repository.exportAudioFileWithFormat(prompts.shortPromptItem,
                new File(promptsDir, promptCat+".a18"), AudioItemRepository.AudioFormat.A18);
            idProperties = new Properties();
            idProperties.put("name", prompts.shortPromptItem.getId());
        }
        if (/*prompts.longPromptFile == null &&*/ prompts.longPromptItem != null) {
            if (!promptsDir.exists()) {
                if (!promptsDir.mkdirs()) errors.add(new MakeDirectoryException("prompts", promptsDir));
            }
            repository.exportAudioFileWithFormat(prompts.longPromptItem,
                new File(promptsDir, "i"+promptCat+".a18"), AudioItemRepository.AudioFormat.A18);
            if (idProperties == null) {
                idProperties = new Properties();
            }
            idProperties.put("invitation", prompts.longPromptItem.getId());
        }

        if (idProperties != null) {
            File propsFile = new File(promptsDir, promptCat + ".ids");
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(propsFile))) {
                idProperties.store(out, null);
            }
        }
        
        return promptCat;
    }

    /**
     * Gets the "category code" for a playlist prompt. May be a taxonomy code (eg, 2-0) or may be a semi-
     * random string (eg, LB-2_uzzqzpjzck_27d)
     * @param prompts for which the code is desired.
     * @param language in which the prompts should be recorded (for error reporting only)
     * @return the category code to use when listing the prompts.
     */
    private String getPromptCategory(PlaylistPrompts prompts, String language) {
        String promptCat;
        // If we're using a prompt from the content database, use the short prompt audio id
        // as the category id.
        if (prompts.getShortItem() != null) {
            promptCat = prompts.getShortItem().getId();
        } else {
            // If not using a prompt from the ACM, we need to have found a prompt in the languages/.../cat directory.
            // If wd did not find such file(s), categoryId will still be null.
            if (prompts.categoryId == null) {
                throw new IllegalStateException(String.format("Missing prompt id for category '%s' in language '%s'.", prompts.getTitle(), language));
            }
            promptCat = prompts.categoryId;
        }
        return promptCat;
    }

    private String packageName(String languagecode, String variant) {
        String projectStr = ACMConfiguration.getInstance().getCurrentDB().getProgramId();
        String deploymentStr = Integer.toString(context.deploymentNo);
        String variantStr = StringUtils.isNotBlank(variant) ? '-'+variant.toLowerCase() : "";
        String packageName = projectStr + '-' + deploymentStr + '-' + languagecode + variantStr;
        // If too long, eliminate hyphens.
        if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
            variantStr = StringUtils.isNotBlank(variant) ? variant.toLowerCase() : "";
            packageName = projectStr + deploymentStr + languagecode + variantStr;
        }
        // If thats still too long, eliminate vowels in project name.
        if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
            assert projectStr != null;
            projectStr = projectStr.replaceAll("[aeiouAEIOU]", "");
            packageName = projectStr + deploymentStr + languagecode + variantStr;
        }
        // If thats still too long, eliminate underscores and hyphens in project name.
        if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
            assert projectStr != null;
            projectStr = projectStr.replaceAll("[_-]", "");
            packageName = projectStr + deploymentStr + languagecode + variantStr;
        }
        // If still too long, truncate project name.
        if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
            int neededTrim = packageName.length() - Constants.MAX_PACKAGE_NAME_LENGTH;
            int keep = projectStr.length() - neededTrim;
            if (keep > 0) {
                projectStr = projectStr.substring(0, keep);
                packageName = projectStr + deploymentStr + languagecode + variantStr;
            } else {
                // This means either a very long variant or a very long language. Should never happen.
                // Use the hashcode of the string, and hope?
                // Put the vowels back
                projectStr = ACMConfiguration.getInstance().getCurrentDB().getProgramId();
                variantStr = StringUtils.isNotBlank(variant) ? '-'+variant.toLowerCase() : "";
                packageName = projectStr + deploymentStr + languagecode + variantStr;
                packageName = Integer.toHexString(packageName.hashCode());
            }
        }
        return packageName;
    }

    private String deploymentName() {
        String project = ACMConfiguration.getInstance().getCurrentDB().getProgramId();
        Deployment depl = context.getProgramSpec().getDeployment(context.deploymentNo);
        Calendar start = Calendar.getInstance();
        start.setTime(depl.startdate);
        int year = start.get(YEAR) % 100;
        return String.format("%s-%d-%d", project, year, context.deploymentNo);
    }

    private void makeDeploymentReport() {
        EmailHelper.HtmlTable summaryTable = new EmailHelper.HtmlTable().withStyle("font-family:sans-serif;border-collapse:collapse;border:2px solid #B8C4CC;width:100%");
        String msg;

        // For each language in the deployment
        for (String language : context.languageCodes) {
            // Print the language name
            LanguageNode languageNode = context.playlistRootNode.find(language);
            if (languageNode == null) {
                msg = String.format("<h2>No playlists found for language '%s (%s)'!</h2>", dbConfig.getLanguageLabel(language), language);
                summaryTable.append(new TR(new TD(msg).colspan(4)));
                continue;
            }

            msg = String.format("<h2>Content package for language '%s (%s)'.</h2>", dbConfig.getLanguageLabel(language), language);
            summaryTable.append(new TR(new TD(msg).colspan(4)));

            summaryTable.append(new TR(new TH("PL #").with("align","left"), new TH("PL / MSG #").with("align","left"), new TH("Message").with("align","left"), new TH("Notes").with("align","left")));

            // Get the ProgSpec playlists, limited to those with messages in this language.
            DeploymentSpec deploymentSpec = context.getProgramSpec().getContentSpec().getDeployment(context.deploymentNo);
            List<PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecsForLanguage(language);
            // Get the titles of the playlists.
            List<String> specifiedPlaylists = playlistSpecs.stream()
                .map(PlaylistSpec::getPlaylistTitle)
                .collect(Collectors.toList());
            // Keep track of which playlists we find, so we can report on the ones missing.
            Set<String> foundPlaylists = new HashSet<>();

            int playlistIx = -1; // To keep track of the ordinal position of each playlist.
            // For each ACM playlist
            for (PlaylistNode playlistNode : languageNode.getPlaylistNodes()) {
                playlistIx++;
                String playlistTitle = playlistNode.getTitle();
                foundPlaylists.add(playlistTitle);

                msg = "";
                // If the playlist added or moved, print that
                int specifiedOrdinal = specifiedPlaylists.indexOf(playlistTitle);
                if (specifiedOrdinal != playlistIx) {
                    if (specifiedOrdinal == -1) {
                        msg = "Added, vs the Program Specification";
                    } else {
                        msg = String.format("Moved from %d to %d, vs the Program Specification.", specifiedOrdinal+1, playlistIx+1);
                    }
                }

                summaryTable.append(new TR(new TD(playlistIx+1), new TD(playlistTitle).colspan(2), new TD(msg)));

                // If there's a PS playlist, get the PS message list.
                List<String> specifiedMessages = new ArrayList<>();
                Set<String> foundMessages = new HashSet<>();
                if (specifiedOrdinal >= 0) {
                    List<MessageSpec> messageSpecs = deploymentSpec.getPlaylist(playlistTitle).getMessagesForLanguage(language);
                    for (MessageSpec messageSpec : messageSpecs) {
                        specifiedMessages.add(messageSpec.getTitle());
                    }
                }

                msg = "";
                // For each message in the ACM message list...
                int audioItemIx = -1;
                for (AudioItemNode audioItemNode : playlistNode.getAudioItemNodes()) {
                    audioItemIx++;
                    String audioItemTitle = audioItemNode.getAudioItem().getTitle();
                    foundMessages.add(audioItemTitle);

                    // If added or moved, print that
                    int specifiedAudioOrdinal = specifiedMessages.indexOf(audioItemTitle);
                    if (specifiedAudioOrdinal != audioItemIx) {
                        if (specifiedAudioOrdinal == -1) {
                            msg = "Added, vs the Program Specification";
                        } else {
                            msg = String.format("Moved from %d to %d, vs the Program Specification.", specifiedAudioOrdinal+1, audioItemIx+1);
                        }
                    }

                    // Print the message info.
                    summaryTable.append(new TR(new TD(), new TD(audioItemIx+1), new TD(audioItemTitle), new TD(msg)));
                }
                // If there are PS messages not in ACM playlist, report them.
                for (String title : specifiedMessages) {
                    if (!foundMessages.contains(title)) {
                        summaryTable.append(new TR(new TD(), new TD('*'), new TD("<i>"+title+"</i>"), new TD("Missing!")).withStyler(pinkZebra));
                    }
                }
                summaryTable.append(new TR(new TD("&nbsp;<br/>").colspan(4))); // blank line, but with proper formatting.
            }
            // If there are PS playlists not in the ACM
            for (String title : specifiedPlaylists) {
                if (!foundPlaylists.contains(title)) {
                    summaryTable.append(new TR(new TD('*'), new TD("<i>"+title+"</i>").colspan(2), new TD("Missing!")).withStyler(pinkZebra));
                }
            }
        }

        summaryMessage.append(summaryTable.toString());
    }


    private static class MakeDirectoryException extends Exception {
        MakeDirectoryException(String description, File dir) {
            super(String.format("Can't create '%s' directory: %s ", description, dir.getPath()));
        }
    }

    private static class RemoveDirectoryException extends Exception {
        RemoveDirectoryException(File dir) {
            super("Can't remove directory: " + dir.getPath());
        }
    }

    private static class DeploymentException extends Exception {
        DeploymentException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
