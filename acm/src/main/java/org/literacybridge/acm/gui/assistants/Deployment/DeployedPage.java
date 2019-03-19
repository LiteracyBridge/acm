package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage;
import org.literacybridge.acm.gui.assistants.Deployment.DeploymentContext.AudioItemNode;
import org.literacybridge.acm.gui.assistants.Deployment.DeploymentContext.LanguageNode;
import org.literacybridge.acm.gui.assistants.Deployment.DeploymentContext.PlaylistNode;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Deployment;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
    private final JTextPane summary;
    private DeploymentContext context;

    public DeployedPage(PageHelper listener) {
        super(listener);
        context = getContext();
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,15,0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);


        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Finished Creating Deployment</span>"
                + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        JLabel deployment = parameterText(Integer.toString(context.deploymentNo));
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        publishNotification = Box.createHorizontalBox();
        publishCommand = parameterText();
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

        summary = new JTextPane();
        tmpGbc.fill=GridBagConstraints.HORIZONTAL;
        summary.setEditable(false);
        summary.setBackground(null);
        summary.setContentType("text/html");

        add(summary, tmpGbc);

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

    private void performUpdate() {
        // Create the files in TB-Loaders/packages to match exporting playlists.
        Map<String, String> pkgs = exportLists();

        try {
            String acmName = ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
            TBBuilder tbb = new TBBuilder(ACMConfiguration.getInstance().getCurrentDB());
            final List<String> args = new ArrayList<>();
            args.add("CREATE");
            args.add(acmName);
            args.add(deploymentName());
            pkgs.entrySet().forEach(e->{
                args.add(e.getValue());
                args.add(e.getKey());
                args.add(e.getKey());
            });
            tbb.apiCreate(args.toArray(new String[0]));

            if (!context.noPublish) {
                args.clear();
                args.add(deploymentName());
                tbb.publish(args);
            } else {
                publishCommand.setText(publishCommand());
                publishNotification.setVisible(true);
            }
            summary.setText(String.format("Deployment #%d successfully created as %s.",
                context.deploymentNo, deploymentName()));
        } catch (Exception e) {
            String message = "An error occurred creating the Deployment:\n"
                +e.getMessage();
            String title = "Error creating Deployment";
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            String summaryMessage = String.format("<html><span style='font-size:1.5em'>An error occurred creating the Deployment:</span>"
                + "<br/><i>%s</i>", e.getMessage());
            summary.setText(summaryMessage);
        }
        setComplete();

    }

    private String publishCommand() {
        String acmName = ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
        String cmd = String.format("<html><span style='font-family:Lucida Console'>TB-Builder publish %s %s</span></html>",
            ACMConfiguration.cannonicalProjectName(acmName),
            deploymentName());
        return cmd;
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
        packagesDir.mkdirs();

        Enumeration langEnumeration = context.playlistRootNode.children();
        while (langEnumeration.hasMoreElements()) {
            LanguageNode languageNode = (LanguageNode)langEnumeration.nextElement();
            String language = languageNode.languagecode;

            // Create the directories.
            String packageName = packageName(language);
            result.put(language, packageName);
            File packageDir = new File(packagesDir, packageName);
            IOUtils.deleteRecursive(packageDir);
            packageDir.mkdirs();
            File listsDir = new File(packageDir, "messages"+File.separator+"lists"+File.separator+"1");
            listsDir.mkdirs();
            File promptsDir = new File(packageDir, "prompts"+File.separator+language+File.separator+"cat");

            // Create the list files, and copy the non-predefined prompts.
            File activeLists = new File(listsDir, "_activeLists.txt");
            try (PrintWriter activeListsWriter = new PrintWriter(activeLists)) {
                List<Playlist> acmPlaylists = context.allAcmPlaylists.get(language);
                Map<String, PlaylistPrompts> playlistsPrompts = context.prompts.get(language);
                Enumeration playlistEnumeration = languageNode.children();
                while (playlistEnumeration.hasMoreElements()) {
                    PlaylistNode playlistNode = (PlaylistNode)playlistEnumeration.nextElement();
                    Playlist playlist = playlistNode.playlist;

                    String title = WelcomePage.basePlaylistName(playlist.getName());
                    title = title.replaceAll("_", " ");
                    PlaylistPrompts prompts = playlistsPrompts.get(title);
                    int promptIx = new ArrayList<String>(playlistsPrompts.keySet()).indexOf(title);

                    String promptCat = (String) getPromptCat(prompts, promptIx, promptsDir);
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
                e.printStackTrace();
            }
        }
        return result;
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
        String promptCat = null;
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
            if (!promptsDir.exists()) promptsDir.mkdirs();
            repository.exportA18WithMetadataToFile(prompts.shortPromptItem,
                new File(promptsDir, promptCat+".a18"));
        }
        if (prompts.longPromptFile == null && prompts.longPromptItem != null) {
            if (!promptsDir.exists()) promptsDir.mkdirs();
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

}
