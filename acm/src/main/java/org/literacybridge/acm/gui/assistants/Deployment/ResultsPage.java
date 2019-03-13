package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.ContentImport.ContentImportContext;
import org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Deployment;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Calendar.YEAR;
import static org.literacybridge.acm.Constants.CATEGORY_GENERAL_OTHER;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class ResultsPage extends AssistantPage<DeploymentContext> {

    private DeploymentContext context;

    public ResultsPage(PageHelper listener) {
        super(listener);
        context = getContext();
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,20,0);
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


        add(new JLabel("Click \"Close\" to return to the ACM."), gbc);

        // Absorb any vertical space.
        gbc.weighty = 1.0;
        add(new JLabel(), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        if (progressing) performUpdate();

        setComplete();
    }

    @Override
    protected String getTitle() {
        return "Created Deployment";
    }

    @Override
    protected boolean isSummaryPage() { return true; }

    private void performUpdate() {
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

            args.clear();
            args.add(deploymentName());
            tbb.publish(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds the lists of content, and the list of lists. Returns a map
     * of language to package name.
     * @return {language : pkgName}
     */
    private Map<String, String> exportLists() {
        Map<String, String> result = new HashMap<>();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        File packagesDir = new File(tbLoadersDir, "packages");
        packagesDir.mkdirs();
        for (String language : context.languages) {
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
                for (Playlist playlist : acmPlaylists) {
                    String title = WelcomePage.basePlaylistName(playlist.getName());
                    title = title.replaceAll("_", " ");
                    PlaylistPrompts prompts = playlistsPrompts.get(title);
                    int promptIx = new ArrayList<String>(playlistsPrompts.keySet()).indexOf(title);

                    String promptCat = (String) getPromptCat(prompts, promptIx, promptsDir);
                    if (!promptCat.equals(Constants.CATEGORY_INTRO_MESSAGE)) {
                        activeListsWriter.println("!"+promptCat);
                    }
                    createListFile(playlist, promptCat, listsDir);
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

    private void createListFile(Playlist playlist, String promptCat, File listsDir)
        throws FileNotFoundException
    {
        File listFile = new File(listsDir, promptCat + ".txt");
        try (PrintWriter listWriter = new PrintWriter(listFile)) {
            for (String audioItemId : playlist.getAudioItemList()) {
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
                new File(promptsDir, promptCat+".i18"));
        }
        if (prompts.longPromptFile == null && prompts.longPromptItem != null) {
            if (!promptsDir.exists()) promptsDir.mkdirs();
            repository.exportA18WithMetadataToFile(prompts.shortPromptItem,
                new File(promptsDir, "i"+promptCat+".i18"));
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
