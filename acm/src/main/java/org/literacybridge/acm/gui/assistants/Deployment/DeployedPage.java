package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.Assistant.ProblemReviewDialog;
import org.literacybridge.acm.gui.assistants.util.AcmContent.AudioItemNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.LanguageNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.PlaylistNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.Version;
import org.literacybridge.core.spec.Deployment;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Calendar.YEAR;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class DeployedPage extends AssistantPage<DeploymentContext> {

    private final JLabel publishCommand;
    private final Box publishNotification;
    private final JLabel summary;
    private final JLabel statusLabel;
    private final JButton viewErrorsButton;
    private final JLabel currentState;
    private DeploymentContext context;

    private List<Exception> errors = new ArrayList<>();

    DeployedPage(PageHelper<DeploymentContext> listener) {
        super(listener);
        context = getContext();
        context = getContext();
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

        publishNotification = Box.createHorizontalBox();
        publishCommand = makeBoxedLabel();
        publishNotification.add(new JLabel("<html>The Deployment was <u>not</u> published. Use </html>"));
        publishNotification.add(Box.createHorizontalStrut(5));
        publishNotification.add(publishCommand);
        publishNotification.add(new JLabel(" to publish."));
        publishNotification.add(Box.createHorizontalGlue());
        GridBagConstraints tmpGbc = (GridBagConstraints) gbc.clone();
        // Need these fill & anchor values because there are HTML labels in the Box.
        tmpGbc.fill=GridBagConstraints.NONE;
        tmpGbc.anchor=GridBagConstraints.LINE_START;
        add(publishNotification, tmpGbc);
        publishNotification.setVisible(false);

        // The status line. Could be updated as the deployment progresses.
        hbox = Box.createHorizontalBox();
        summary = new JLabel();
        summary.setText(String.format("Creating Deployment #%d as %s.",
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

        add(new JLabel("Click \"Close\" to return to the ACM."), gbc);

        // Absorb any vertical space.
        gbc.weighty = 1.0;
        add(new JLabel(), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        performUpdate();
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

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(
            ZoneId.systemDefault());
        String message = "<html>The following error(s) occurred when attempting to create the Deployment. " +
            "If the problem persists, contact Amplio technical support. The button below will send this report to Amplio."+
            "</html>";
        String reportHeading = String.format("Project %s, User %s (%s), Computer %s%nCreate Deployment at %s%n" +
                "Deployment %d%n"+
                "ACM Version %s, built %s%n",
            dbConfig.getProjectName(),
            ACMConfiguration.getInstance().getUserName(),
            ACMConfiguration.getInstance().getUserContact(),
            computerName,
            formatter.format(LocalDateTime.now()),
            context.deploymentNo,
            Constants.ACM_VERSION, Version.buildTimestamp);

        ProblemReviewDialog dialog = new ProblemReviewDialog(Application.getApplication(), "Errors While Importing");
        DefaultMutableTreeNode issues = new DefaultMutableTreeNode();
        context.issues.addToTree(issues);
        dialog.showProblems(message, reportHeading, issues, errors);
    }


    private void performUpdate() {
        if (!makeDirectories()) {
            // error already recorded
            return;
        }

        // Create the files in TB-Loaders/packages to match exporting playlists.
        Map<String, String> pkgs = exportLists();

        try {
            String acmName = ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
            TBBuilder tbb = new TBBuilder(ACMConfiguration.getInstance().getCurrentDB(), this::reportState);
            final List<String> args = new ArrayList<>();
            args.add("CREATE");
            args.add(acmName);
            args.add(deploymentName());
            pkgs.forEach((key, value) -> {
                args.add(value);
                args.add(key);
                args.add(key);
            });
            tbb.createDeployment(args.toArray(new String[0]));

            if (!context.noPublish) {
                args.clear();
                args.add(deploymentName());
                tbb.publishDeployment(args);
            } else {
                publishCommand.setText(publishCommand());
                publishNotification.setVisible(true);
            }
        } catch (Exception e) {
            errors.add(new DeploymentException("Exception creating deployment", e));
            e.printStackTrace();
        }

        if (ACMConfiguration.isTestData()) {
            // Fake error for testing.
            errors.add(new DeploymentException("Just kidding", null));
        }

        if (errors.size() > 0) {
            setStatus("Finished, but with errors.");
            statusLabel.setForeground(Color.red);
            summary.setText(String.format("There were errors creating Deployment #%d.", context.deploymentNo));
            viewErrorsButton.setVisible(true);
        } else {
            summary.setText(String.format("Deployment #%d successfully created as %s.",
                context.deploymentNo, deploymentName()));
            setStatus("Finished.");
        }

        setComplete();

    }

    /**
     * Reports status & state from the TB-Builder to the user.
     * @param state to be reported.
     */
    void reportState(String state) {
        UIUtils.setLabelText(currentState, state);
    }

    private String publishCommand() {
        String acmName = ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
        return String.format("<html><span style='font-family:Lucida Console'>TB-Builder publish %s %s</span></html>",
            ACMConfiguration.cannonicalProjectName(acmName),
            deploymentName());
    }

    /**
     * Builds the lists of content, and the list of lists. Returns a map
     * of language to package name. Will extract any content prompts to a
     * packages/${package}/prompts directory.
     * @return {language : pkgName}
     */
    private Map<String, String> exportLists() {
        Map<String, String> result = new HashMap<>();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        File packagesDir = new File(tbLoadersDir, "packages");

        Enumeration langEnumeration = context.playlistRootNode.children();
        while (langEnumeration.hasMoreElements()) {
            LanguageNode languageNode = (LanguageNode)langEnumeration.nextElement();
            String language = languageNode.languagecode;

            // The directories were just created.
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
                Enumeration playlistEnumeration = languageNode.children();
                while (playlistEnumeration.hasMoreElements()) {
                    PlaylistNode playlistNode = (PlaylistNode)playlistEnumeration.nextElement();
                    Playlist playlist = playlistNode.playlist;

                    title = undecoratedPlaylistName(playlist.getName());
                    PlaylistPrompts prompts = playlistsPrompts.get(title);
                    int promptIx = new ArrayList<>(playlistsPrompts.keySet()).indexOf(title);

                    String promptCat = getPromptCat(prompts, promptIx, promptsDir);
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
            String language = languageNode.languagecode;

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

    private void createListFile(PlaylistNode playlistNode, String promptCat, File listsDir)
        throws FileNotFoundException
    {
        File listFile = new File(listsDir, promptCat + ".txt");
        try (PrintWriter listWriter = new PrintWriter(listFile)) {
            Enumeration audioItemEnumeration = playlistNode.children();
            while (audioItemEnumeration.hasMoreElements()) {
                AudioItemNode audioItemNode = (AudioItemNode)audioItemEnumeration.nextElement();
                String audioItemId = audioItemNode.item.getUuid();

                listWriter.println(audioItemId);
            }
        }
    }

    /**
     * Gets the prompt category to be used in the _activeLists.txt file, and if necessary
     * extracts prompts from the ACM.
     * @param prompts The prompts that were found earlier, if any.
     * @param promptIx The index of the playlist in the Deployment. Used to synthesize the
     *                 category name when there isn't one already existing.
     * @param promptsDir The directory to which any content prompts should be extracted.
     * @return the category, as a String.
     * @throws IOException if the audio file can't be written.
     * @throws BaseAudioConverter.ConversionException If the audio file can't be converted
     *          to .a18 format.
     */
    private String getPromptCat(PlaylistPrompts prompts, int promptIx, File promptsDir)
        throws IOException, BaseAudioConverter.ConversionException
    {
        // If there is a categoryId, that's the "prompt category".
        String promptCat;
        if (prompts.categoryId != null) {
            promptCat = prompts.categoryId;
            // For the intro message, we don't actually need or want a prompt file.
            if (promptCat.equals(Constants.CATEGORY_INTRO_MESSAGE)) return promptCat;
        } else {
            promptCat = String.format("100-0-%d", promptIx);
        }

        AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
        // If there is not a pre-defined set of prompt files, we'll need to extract the
        // content audio to the promptsDir.
        if (prompts.shortPromptFile == null && prompts.shortPromptItem != null) {
            if (!promptsDir.exists()) {
                if (!promptsDir.mkdirs()) errors.add(new MakeDirectoryException("prompts", promptsDir));
            }
            repository.exportA18WithMetadataToFile(prompts.shortPromptItem,
                new File(promptsDir, promptCat+".a18"));
        }
        if (prompts.longPromptFile == null && prompts.longPromptItem != null) {
            if (!promptsDir.exists()) {
                if (!promptsDir.mkdirs()) errors.add(new MakeDirectoryException("prompts", promptsDir));
            }
            repository.exportA18WithMetadataToFile(prompts.shortPromptItem,
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
