package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
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

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Calendar.YEAR;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import static org.literacybridge.acm.utils.EmailHelper.pinkZebra;

public class FinishDeploymentPage extends AcmAssistantPage<DeploymentContext> {

    private final JLabel publishNotification;
    private final JLabel summary;
    private final JLabel statusLabel;
    private final JButton viewErrorsButton;
    private final JLabel currentState;

    private DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
    private List<Exception> errors = new ArrayList<>();
    private DateTimeFormatter localDateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
        .withZone(ZoneId.systemDefault());
    private StringBuilder summaryMessage;

    FinishDeploymentPage(PageHelper<DeploymentContext> listener) {
        super(listener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Finished Creating Deployment</span>"
                + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        JLabel deployment = makeBoxedLabel(Integer.toString(context.deploymentNo));
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        publishNotification = new JLabel("<html>The Deployment was <u>not</u> published. To publish, start the "
            +"Deployment Assistant again, and do <u>not</u> select the 'Do not publish the Deployment.' option.</html>");
        add(publishNotification, gbc);
        publishNotification.setVisible(false);

        // The status line. Could be updated as the deployment progresses.
        hbox = Box.createHorizontalBox();
        summary = new JLabel();
        summary.setText(String.format("Creating Deployment %d as %s.",
            context.deploymentNo, deploymentName()));
        hbox.add(summary);
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
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                summaryMessage = new StringBuilder("<html>");
                summaryMessage.append(String.format("<h1>Deployment %d for Project %s</h1>",
                    context.deploymentNo, dbConfig.getProjectName()));
                summaryMessage.append(String.format("<h3>Created on %s</h3>", localDateFormatter.format(LocalDateTime.now())));

                createDeployment();
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

        String message = "<html>The following error(s) occurred when attempting to create the Deployment. " +
            "If the problem persists, contact Amplio technical support. The button below will send this report to Amplio."+
            "</html>";
        String reportHeading = String.format("Error report from Deployment Assistant%n%n" +
                "Project %s, User %s (%s), Computer %s%nCreate Deployment at %s%n" +
                "Deployment %d%n"+
                "ACM Version %s, built %s%n",
            dbConfig.getProjectName(),
            ACMConfiguration.getInstance().getUserName(),
            ACMConfiguration.getInstance().getUserContact(),
            computerName,
            localDateFormatter.format(LocalDateTime.now()),
            context.deploymentNo,
            Constants.ACM_VERSION, Version.buildTimestamp);

        ProblemReviewDialog dialog = new ProblemReviewDialog(Application.getApplication(), "Errors While Importing");
        DefaultMutableTreeNode issues = new DefaultMutableTreeNode();
        context.issues.addToTree(issues);
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
        Map<String, String> pkgs = exportLists();

        try {
            String acmName = ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
            TBBuilder tbb = new TBBuilder(acmName, deploymentName(), this::reportState);

            // Create.
            final List<String> args = new ArrayList<>();
            pkgs.forEach((key, value) -> {
                args.add(value); // package
                args.add(key);   // language
                args.add(key);   // group
            });
            tbb.createDeployment(args);

            // Publish.
            if (context.isPublish()) {
                args.clear();
                args.add(deploymentName());
                tbb.publishDeployment(args);
            } else {
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
     * Reports status & state from the TB-Builder to the user.
     * @param state to be reported.
     */
    private void reportState(String state) {
        UIUtils.setLabelText(currentState, state);
    }

    /**
     * Builds the lists of content, and the list of lists. Returns a map
     * of language to package name. Will extract any content prompts to a
     * packages/${package}/prompts directory.
     * @return {language : pkgName}
     */
    private Map<String, String> exportLists() {
        Map<String, String> result = new LinkedHashMap<>();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        File packagesDir = new File(tbLoadersDir, "packages");

        Enumeration<LanguageNode> langEnumeration = context.playlistRootNode.children();
        while (langEnumeration.hasMoreElements()) {
            LanguageNode languageNode = langEnumeration.nextElement();
            String language = languageNode.getLanguageCode();

            // one entry in the language : package-name map.
            String packageName = packageName(language);
            result.put(language, packageName);
            File packageDir = new File(packagesDir, packageName);
            File listsDir = new File(packageDir, "messages"+File.separator+"lists"+File.separator+"1");
            File promptsDir = new File(packageDir, "prompts"+File.separator+language+File.separator+"cat");

            // Create the list files, and copy the non-predefined prompts.
            File activeLists = new File(listsDir, "_activeLists.txt");
            String title = null;
            try (PrintWriter activeListsWriter = new PrintWriter(activeLists)) {
                Map<String, PlaylistPrompts> playlistsPrompts = context.prompts.get(language);
                Enumeration<PlaylistNode> playlistEnumeration = languageNode.children();
                while (playlistEnumeration.hasMoreElements()) {
                    PlaylistNode playlistNode = playlistEnumeration.nextElement();
                    title = playlistNode.getTitle();
                    PlaylistPrompts prompts = playlistsPrompts.get(title);
                    int promptIx = new ArrayList<>(playlistsPrompts.keySet()).indexOf(title);

                    String promptCat = getPromptCategoryAndFiles(prompts, promptIx, promptsDir);
                    if (!promptCat.equals(Constants.CATEGORY_INTRO_MESSAGE)) {
                        activeListsWriter.println("!"+promptCat);
                    }
                    createListFile(playlistNode, promptCat, listsDir);
                }

                if (context.includeUfCategory)
                    activeListsWriter.println(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                if (context.includeTbCategory)
                    activeListsWriter.println("$"+ Constants.CATEGORY_TB_INSTRUCTIONS);
            } catch (IOException | BaseAudioConverter.ConversionException e) {
                errors.add(new DeploymentException(String.format("Error exporting playlist '%s'", title), e));
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Creates the directories that are needed to create the deployment.
     * @return true if the directories were created successfully.
     */
    private boolean makeDirectories() {
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        File packagesDir = new File(tbLoadersDir, "packages");
        if ((packagesDir.exists() && !packagesDir.isDirectory()) || (!packagesDir.exists() && !packagesDir.mkdirs())) {
            // Can't make packages directory.
            errors.add(new MakeDirectoryException("packages", packagesDir));
            return false;
        }

        Enumeration langEnumeration = context.playlistRootNode.children();
        while (langEnumeration.hasMoreElements()) {
            LanguageNode languageNode = (LanguageNode)langEnumeration.nextElement();
            String language = languageNode.getLanguageCode();

            // Create the directories.
            String packageName = packageName(language);
            File packageDir = new File(packagesDir, packageName);
            // Clean any old data.
            IOUtils.deleteRecursive(packageDir);
            if (packageDir.exists()) {
                // Couldn't clean up.
                errors.add(new RemoveDirectoryException(packageDir));
                return false;
            }
            if (!packageDir.mkdirs()) {
                // Can't make package directory
                errors.add(new MakeDirectoryException("package", packageDir));
                return false;
            }
            File listsDir = new File(packageDir, "messages"+File.separator+"lists"+File.separator+"1");
            if (!listsDir.mkdirs()) {
                // Can't make lists directory
                errors.add(new MakeDirectoryException("lists", listsDir));
                return false;
            }
            // This directory won't be created until it is needed. But if we're able to get here, we're almost
            // certainly able to create the prompts directory.
            //File promptsDir = new File(packageDir, "prompts"+File.separator+language+File.separator+"cat");

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
        File listFile = new File(listsDir, promptCat + ".txt");
        try (PrintWriter listWriter = new PrintWriter(listFile)) {
            Enumeration audioItemEnumeration = playlistNode.children();
            while (audioItemEnumeration.hasMoreElements()) {
                AudioItemNode audioItemNode = (AudioItemNode)audioItemEnumeration.nextElement();
                String audioItemId = audioItemNode.getAudioItem().getId();

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
     * @param promptIx The index of the playlist in the Deployment. Used to synthesize the
     *                 category name when there isn't one already existing.
     * @param promptsDir The directory to which any content prompts should be extracted.
     * @return the category, as a String.
     * @throws IOException if the audio file can't be written.
     * @throws BaseAudioConverter.ConversionException If the audio file can't be converted
     *          to .a18 format.
     */
    private String getPromptCategoryAndFiles(PlaylistPrompts prompts, int promptIx, File promptsDir)
        throws IOException, BaseAudioConverter.ConversionException
    {
        // If there is a categoryId, that's the "prompt category".
        String promptCat;
        if (prompts.categoryId != null) {
            promptCat = prompts.categoryId;
            // For the intro message, we don't actually need or want a prompt file.
            if (promptCat.equals(Constants.CATEGORY_INTRO_MESSAGE)) return promptCat;
        } else {
            // If we weren't able to find a category id, then this must have been a prompt from 
            // content. But if we can't find the category item, just make up a category id.
            AudioItem promptITem = prompts.getShortItem();
            if (promptITem != null) {
                promptCat = prompts.getShortItem().getId();
            } else {
                promptCat = String.format("100-0-%d-%d", context.deploymentNo, promptIx);
            }
        }

        AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
        // If there is not a pre-defined set of prompt files, we'll need to extract the
        // content audio to the promptsDir.
        if (/*prompts.shortPromptFile == null &&*/ prompts.shortPromptItem != null) {
            if (!promptsDir.exists()) {
                if (!promptsDir.mkdirs()) errors.add(new MakeDirectoryException("prompts", promptsDir));
            }
            repository.exportA18WithMetadataToFile(prompts.shortPromptItem,
                new File(promptsDir, promptCat+".a18"));
        }
        if (/*prompts.longPromptFile == null &&*/ prompts.longPromptItem != null) {
            if (!promptsDir.exists()) {
                if (!promptsDir.mkdirs()) errors.add(new MakeDirectoryException("prompts", promptsDir));
            }
            repository.exportA18WithMetadataToFile(prompts.longPromptItem,
                new File(promptsDir, "i"+promptCat+".a18"));
        }

        return promptCat;
    }

    private String packageName(String languagecode) {
        return String.format("%s-%s", deploymentName(), languagecode.toLowerCase());
    }

    private String deploymentName() {
        String project = ACMConfiguration.cannonicalProjectName(ACMConfiguration.getInstance().getCurrentDB().getSharedACMname());
        Deployment depl = context.programSpec.getDeployment(context.deploymentNo);
        Calendar start = Calendar.getInstance();
        start.setTime(depl.startdate);
        int year = start.get(YEAR) % 100;
        return String.format("%s-%d-%d", project, year, context.deploymentNo);
    }

    private void makeDeploymentReport() {
        EmailHelper.HtmlTable summaryTable = new EmailHelper.HtmlTable().withStyle("font-family:sans-serif;border-collapse:collapse;border:2px solid #B8C4CC;width:100%");
        String msg;

        // For each language in the deployment
        for (String language : context.languages) {
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
            DeploymentSpec deploymentSpec = context.programSpec.getContentSpec().getDeployment(context.deploymentNo);
            List<PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecs();
            // Build a list of the playlists in the progspec that have messages in this language.
            List<String> specifiedPlaylists = new ArrayList<>();
            for (PlaylistSpec playlistSpec : playlistSpecs) {
                List<MessageSpec> msgs = playlistSpec.getMessagesForLanguage(language);
                if (msgs.size() > 0)
                    specifiedPlaylists.add(playlistSpec.getPlaylistTitle());
            }
            // Keep track of which playlists we find, so we can report on the ones missing.
            Set<String> foundPlaylists = new HashSet<>();

            int playlistIx = -1; // To keep track of the ordinal position of each playlist.
            // For each ACM playlist
            for (Enumeration<PlaylistNode> playlistEnumeration=languageNode.children(); playlistEnumeration.hasMoreElements(); ) {
                playlistIx++;
                PlaylistNode playlistNode = playlistEnumeration.nextElement();
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
                for (Enumeration<AudioItemNode> audioItemEnumeration=playlistNode.children(); audioItemEnumeration.hasMoreElements(); ) {
                    audioItemIx++;
                    AudioItemNode audioItemNode = audioItemEnumeration.nextElement();
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
