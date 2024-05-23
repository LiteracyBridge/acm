package org.literacybridge.acm.tbbuilder;

import org.amplio.csm.CsmData;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.cloud.UfKeyHelper;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.acm.deployment.DeploymentInfo.PackageInfo;
import org.literacybridge.acm.deployment.DeploymentInfo.PackageInfo.PlaylistInfo;
import org.literacybridge.acm.deployment.DeploymentInfo.PromptInfo;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.acm.tbbuilder.survey.Survey;
import org.literacybridge.acm.utils.ExternalCommandRunner;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;
import org.literacybridge.core.tbloader.PackagesData;
import org.literacybridge.core.tbloader.PackagesData.PackageData;
import org.literacybridge.core.tbloader.PackagesData.PackageData.PlaylistData;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.BELL_SOUND_V2;
import static org.literacybridge.acm.Constants.CUSTOM_GREETING_V1;
import static org.literacybridge.acm.Constants.SILENCE_V2;

/*
 * Builds a TBv1 deployment.
 * 

* Given a program 'DEMO', a deployment 'DEMO-21-1', and a package 'DEMO-1-en-c'
*
* a "tree.txt" file with empty directories to be created in each image.
*   inbox
*   log
*   log-archive
*   statistics

DEMO/content/DEMO-21-1/images/DEMO-1-en-c/
*
* DEMO/content/DEMO-21-1/
├── firmware.v2
├── recipients
├── shaodowFiles
├── images.v2
│   ├── DEMO-1-en
│   │   ├── content
│   │   │   ├── prompts
│   │   │   │   └── en
│   │   │   │       ├── 0.mp3
│   │   │   │       ├── 1.mp3
│   │   │   │        . . .
│   │   │   │       ├── 9.mp3
│   │   │   │       ├── 9-0.mp3
│   │   │   │       ├── LB-2_kkg8lufhqr_jp.mp3
│   │   │   │        . . .
│   │   │   │       └── iLB-2_uzz71upxwm_vn.mp3
│   │   │   ├── messages
│   │   │   │   ├── LB-2_uzz71upxwm_vg.mp3
│   │   │   │    . . .
│   │   │   │   └── LB-2_uzz71upxwm_zd.mp3
│   │   │   └── packages_data.txt
│   │   └── system
│   │       ├── DEMO-1-en-c.pkg     <<-- zero-byte marker file
│   │       ├── c.grp               <<-- zero-byte marker file
│   │       ├── config.txt          <<-- fairly constant across programs
│   │       └── profiles.txt        <<-- "DEMO-1-EN-C,en,1,menu"
│   ├── DEMO-1-en-c
│   . . .
└── programspec
    ├── content.csv
    ├── content.json                <<-- remove
    ├── deployment.properties
    ├── deployment_spec.csv
    ├── deployments.csv             <<-- remove
    ├── etags.properties
    ├── pending_spec.xlsx           <<-- remove?
    ├── program_spec.xlsx           <<-- remove
    ├── recipients.csv
    └── recipients_map.csv

Format of the package_data.txt file:
1 # format version, currently "1"
${deployment_name} # name of the deployment, like TEST-21-4
${num_path_directories} # number of directories containing audio. Their ordinals are used to make paths
${directory_1} # First path directory; referred to as "1"
${directory_2} # Second path directory
. . .
${num_packages} # number of packages that follow
#- - - - - - - - - - - - start of a package
${package_name_1} # name of the first package
  ${path_ordinal} ${package_announcement} # ordinal of the path that contains the ${package_announcement}
  ${prompts_path} # up to 10 path ordinals, separated by semicolons. Searched in order for system prompts.
  ${num_playlists} # number of playlists that follow
  ${playlist_name_1} # name of the first playlist. Only used for logging purposes.
    ${path_ordinal} ${playlist_announcement_1} # path ordinal and file name of short prompt for playlist 1
    ${path_ordianl} ${playlist_invitation_2} # path ordinal and file name of invitation for playlist 1
    ${num_messages} # number of messages that follow
    ${path_ordinal} ${message_1} # path ordinal and file name of first message
    ${path_ordianl} ${message_2} # path ordinal and file name of second message
    . . .
  ${playlist_name_2} # name of second playlist
  . . .
#- - - - - - - - - - - - end of previous package, start of next package
* ${package_name_2} # name of the second package
. . .

Each image will contain a 1-package package_data.txt. To combine several on a single Talking Book,
concatenate the path lines, and adjust the path ordinals in the audio file lines. Concatenate the
resulting package sections

 *
 */

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public
class CreateForV2 extends CreateFromDeploymentInfo {
    private final AudioItemRepository.AudioFormat audioFormat;
    private final PackagesData allPackagesData;
    private final File imagesDir;

    public static final List<String> requiredFirmwareFiles = Arrays.asList("TBookRev2b.hex", "firmware_built.txt");

    public final static List<String> TUTORIAL_V2_MESSAGES = Arrays.asList(
            // 22 and 23 are speed control, not implemented in v2
            // Note not in numeric order.
            "17", "16", "28", "26", "20", "21", /*"22", "23",*/ "19", "25", "54"
    );

    /**
     * A directory is a valid source of firmware if it contains both the firmware itself and the
     * file that provides the label (the firmware writes its label to the system directory at
     * every startup, so we can tell what firmware most recently booted).
     * @param maybeFirmware A directory that might, or might not be a firmware directory.
     * @return True if is (appears to be) a valid firmware directory.
     */
    public static boolean isValidFirmwareSource(File maybeFirmware) {
        if (maybeFirmware == null || !maybeFirmware.isDirectory()) return false;
        Set<String> foundFiles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        foundFiles.addAll(Arrays.asList(Objects.requireNonNull(maybeFirmware.list())));
        return foundFiles.containsAll(requiredFirmwareFiles);
    }

    /**
     * A directory is a valid source of system files if it is a directory and contains files.
     * @param maybeSystem A directory that might, or might not, be a source of system files.
     * @return True if it is (appears to be) a valid source of system files.
     */
    public static boolean isValidSystemSource(File maybeSystem) {
        if (maybeSystem == null || !maybeSystem.isDirectory()) return false;
        String[] files = maybeSystem.list();
        return files != null && files.length != 0;
    }

    CreateForV2(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext, DeploymentInfo deploymentInfo) {
        super(tbBuilder, builderContext, deploymentInfo);
        allPackagesData = new PackagesData(builderContext.deploymentName);
        imagesDir = new File(builderContext.stagedDeploymentDir, "images.v2");
        audioFormat = AudioItemRepository.AudioFormat.MP3;
    }


    @Override
    public AudioItemRepository.AudioFormat getAudioFormat() {
        return audioFormat;
    }

    @Override
    public String getPackageName(PackageInfo packageInfo) {
        return packageInfo.getShortName();
    }

    /**
     * Adds a Content Package to a Deployment. Copies the files to the staging directory.
     *
     * @param packageInfo Information about the package: name, language, groups.
     * @throws Exception if there is an error creating or reading a file.
     */
    @Override
    protected void addImageForPackage(PackageInfo packageInfo) throws Exception {
        new ImageCreator(packageInfo).addImage();
    }

    @Override
    protected void finalizeDeployment() throws Exception {
        super.finalizeDeployment();
        exportGreetings();

        File of = new File(imagesDir, PackagesData.PACKAGES_DATA_TXT);
        try (FileOutputStream fos = new FileOutputStream(of)) {
            allPackagesData.exportPackageDataFile(fos);
        }

        createMp3FrameOffsetsFiles();
    }

    private void createMp3FrameOffsetsFiles() throws IOException {
        if (getAudioFormat() != AudioItemRepository.AudioFormat.MP3) {
            return;
        }
        Deque<File> directoriesToSearch = new LinkedList<>();
        directoriesToSearch.add(builderContext.stagedDeploymentDir);
        while (!directoriesToSearch.isEmpty()) {
            File dir = directoriesToSearch.remove();
            File[] ofInterest = dir.listFiles(contained -> contained.isDirectory() || contained.getName().toLowerCase().endsWith(".mp3"));
            if (ofInterest != null) {
                // Add the subdirectories to the list to be searched.
                Arrays.stream(ofInterest).filter(File::isDirectory).forEach(directoriesToSearch::add);
                // Create .m3t files for any audio files.
                createMp3FrameOffsetsForDirectory(dir, Arrays.stream(ofInterest).filter(File::isFile).collect(Collectors.toList()));
            }
        }
    }

    private void createMp3FrameOffsetsForDirectory(File directory, List<File> files) throws IOException {
        List<File> toConvert = new ArrayList<>();
        for (File file : files) {
            if (file.length() == 0) {
                // Zero-byte marker file. The real file will be created elsewhere.
                File markerFile = new File(directory, FilenameUtils.removeExtension(file.getName())+".m3t");
                markerFile.createNewFile();
            } else {
                toConvert.add(file);
            }
        }
        if (!toConvert.isEmpty()) {
            new Mp3FrameWrapper(directory, toConvert).go();
        }
    }

    /**
     * Expoort the correct firmware to the image. In general, "correct" means "latest".
     *
     * @throws IOException if the file can't be copied.
     */
    @Override
    protected void exportFirmware() throws IOException {
        // Look for an override in the program's TB_Options directory.
        File sourceFirmwareDir = new File(builderContext.sourceTbOptionsDir, "firmware.v2");
        if (!isValidFirmwareSource(sourceFirmwareDir)) {
            // Fall back to the system default, ~/Amplio/ACM/firmware.v2
            sourceFirmwareDir = new File(AmplioHome.getAppSoftwareDir(), "firmware.v2");
        }
        File stagedFirmwareDir = new File(builderContext.stagedDeploymentDir, "firmware.v2");
        IOUtils.deleteRecursive(stagedFirmwareDir);
        stagedFirmwareDir.mkdirs();
        FileUtils.copyDirectory(sourceFirmwareDir, stagedFirmwareDir);
    }

    @Override
    protected List<String> getAcceptableFirmwareVersions() {
        return tbBuilder.getAcceptableFirmwareVersions(deploymentInfo.isUfPublic());
    }


    /**
     * Export the greetings for the recipients of the Deployment.
     *
     * @throws IOException                            If a greeting file can't be read or written.
     * @throws BaseAudioConverter.ConversionException if a greeting file can't be converted.
     */
    private void exportGreetings() throws
                                   IOException,
                                   BaseAudioConverter.ConversionException, TBBuilder.TBBuilderException {

        File sourceCommunitiesDir = new File(builderContext.sourceTbLoadersDir, "communities");
        File stagedCommunitiesDir = new File(builderContext.stagedDeploymentDir, "communities");
        File sourceRecipientsDir = new File(builderContext.sourceHomeDir, "recipients");
        File stagedRecipientsDir = new File(builderContext.stagedDeploymentDir, "recipients");
        for (File f : new File[]{sourceCommunitiesDir, sourceRecipientsDir, stagedCommunitiesDir, stagedRecipientsDir}) {
            if (!f.exists() && !f.mkdirs()) {
                throw (new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", f)));
            }
        }

        RecipientList recipients = builderContext.programSpec.getRecipientsForDeployment(deploymentInfo.getDeploymentNumber());
        exportCommunityFiles(recipients, sourceCommunitiesDir, stagedCommunitiesDir);
        exportRecipientFiles(recipients, sourceRecipientsDir, stagedRecipientsDir);
    }

    private void exportRecipientFiles(RecipientList recipients, File sourceRecipientsDir, File stagedRecipientsDir)
        throws BaseAudioConverter.ConversionException, IOException {
        for (Recipient recipient : recipients) {
            File[] sourceFiles = sourceRecipientsDir.listFiles(pathname -> (pathname.isFile() && FilenameUtils.getBaseName(
                pathname.getName()).equals(recipient.recipientid)));
            if (sourceFiles != null && sourceFiles.length > 0) {
                File targetFile = new File(stagedRecipientsDir, recipient.recipientid + '.' + getAudioFormat());
                repository.exportFileWithFormat(sourceFiles[0], targetFile, getAudioFormat());
            }
        }
    }

    private void exportCommunityFiles(RecipientList recipients, File sourceCommunitiesDir, File stagedCommunitiesDir)
        throws BaseAudioConverter.ConversionException, IOException {
        for (Recipient recipient : recipients) {
            exportCommunityFilesForRecipient(recipient, sourceCommunitiesDir, stagedCommunitiesDir, getAudioFormat());
        }
    }


    /**
     * Exports the greeting file for one recipient.
     *
     * @param recipient            The recipient for which to export the greeting.
     * @param sourceCommunitiesDir Where greetings come from ( {program}/TB-Loaders/communities )
     * @param stagedCommunitiesDir Where greetings go to.
     * @param audioFormat          Audio format for the greeting.
     * @throws IOException                            if a greeting can't be read or written.
     * @throws BaseAudioConverter.ConversionException If a greeting can't be converted.
     */
    private void exportCommunityFilesForRecipient(Recipient recipient,
        File sourceCommunitiesDir,
        File stagedCommunitiesDir,
        AudioItemRepository.AudioFormat audioFormat) throws
                                                     IOException,
                                                     BaseAudioConverter.ConversionException {
        String communityDirName = builderContext.programSpec.getRecipientsMap()
            .getOrDefault(recipient.recipientid, recipient.recipientid);
        File sourceCommunityDir = new File(sourceCommunitiesDir, communityDirName);
        File targetCommunityDir = new File(stagedCommunitiesDir, communityDirName);
        File sourceLanguageDir = new File(sourceCommunityDir, "languages" + File.separator + recipient.languagecode);
        File targetLanguageDir = new File(targetCommunityDir, "languages" + File.separator + recipient.languagecode);
        File targetSystemDir = new File(targetCommunityDir, "system");
        // Copy the greeting in the recipient's language, if there is one. Create the vestigal ".grp" file as well.
        if (sourceLanguageDir.exists() && sourceLanguageDir.isDirectory() && Objects.requireNonNull(sourceLanguageDir.listFiles()).length > 0) {
            targetLanguageDir.mkdirs();
            targetSystemDir.mkdirs();
            String targetFilename = FilenameUtils.removeExtension(CUSTOM_GREETING_V1) + '.' + getAudioFormat().getFileExtension();
            File targetFile = new File(targetLanguageDir, targetFilename);
            repository.exportGreetingWithFormat(sourceLanguageDir, targetFile, audioFormat);
            File groupFile = new File(targetSystemDir, recipient.languagecode + ".grp");
            groupFile.createNewFile();
        }
    }

    private class ImageCreator {
        final PackageInfo packageInfo;
        final TBBuilder.BuilderContext builderContext;
        private final File imageDir;
        private final File contentDir;
        private final File messagesDir;
        private final File promptsDir;
        private final File shadowMessagesDir;
        private final File shadowPromptsDir;
        private final PackagesData packagesData;


        ImageCreator(PackageInfo packageInfo) {
            this.packageInfo = packageInfo;
            this.builderContext = CreateForV2.this.builderContext;

            imageDir = new File(imagesDir, getPackageName(packageInfo));

            contentDir = new File(imageDir, "content");
            File shadowContentDir = new File(builderContext.stagedShadowDir, "content");
            messagesDir = new File(contentDir, "messages");
            shadowMessagesDir = new File(shadowContentDir, "messages");

            // TODO: if we want prompts-per-variant, split them out here, languagecode-variant
            promptsDir = new File(contentDir, "prompts" + File.separator + packageInfo.getLanguageCode());
            shadowPromptsDir = new File(shadowContentDir,
                "prompts" + File.separator + packageInfo.getLanguageCode());

            packagesData = new PackagesData(this.builderContext.deploymentName);
        }

        /**
         * This is the main worker for adding the content, prompts, firmware, and miscelaneous files to an image.
         *
         * @throws Exception if something goes wrong.
         */
        void addImage() throws Exception {
            builderContext.reportStatus("%n%nExporting package %s%n", getPackageName(packageInfo));

            IOUtils.deleteRecursive(imageDir);
            imageDir.mkdirs();

            for (File f : new File[]{messagesDir, promptsDir}) {
                if (!f.exists() && !f.mkdirs()) {
                    throw (new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", f)));
                }
            }
            if (builderContext.deDuplicateAudio) {
                for (File f : new File[]{shadowMessagesDir, shadowPromptsDir}) {
                    if (!f.exists() && !f.mkdirs()) {
                        throw (new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", f)));
                    }
                }
            }

            // Create the empty directory structure
            PackageData packageData = packagesData.addPackage(getPackageName(packageInfo))
                .withPromptPath(makePath(promptsDir));
            addPackageContentToImage(packageData);

            addSystemPromptsToImage();
            addSystemDirFilesToImage();
            if (packageInfo.hasTutorial()) {
                addTutorialToImage(packageData);
            }
            if (packageInfo.isUfPublic()) {
                addUfToImage(packageData);
            }

            allPackagesData.addPackagesData(packagesData);
            File of = new File(contentDir, PackagesData.PACKAGES_DATA_TXT);
            try (FileOutputStream fos = new FileOutputStream(of)) {
                packagesData.exportPackageDataFile(fos, getPackageName(packageInfo));
            }

            builderContext.reportStatus(
                String.format("Done with adding image for %s and %s.%n",
                    getPackageName(packageInfo),
                    packageInfo.getLanguageCode()));
        }

        private Path makePath(File file) {
            return imageDir.toPath().relativize(file.toPath());
        }

        /**
         * Adds the content for the given package to the given image files. Does not include the
         * playlist prompts, nor any tutorial.
         *
         * @param packageData description of the package to be added to the image.
         */
        private void addPackageContentToImage(PackageData packageData)
            throws
            IOException,
            BaseAudioConverter.ConversionException,
            AudioItemRepository.UnsupportedFormatException {

            for (PlaylistInfo playlistInfo : packageInfo.getPlaylists()) {
                // Copy files, add PlaylistData to PackageData.
                PlaylistData playlistData = packageData.addPlaylist(playlistInfo.getTitle());
                addPlaylistContentToImage(playlistInfo, playlistData);
                addPlaylistPromptsToImage(playlistInfo, playlistData);
                // If there is a survey for this playlist, add it.
                if (playlistInfo.isSurvey()) {
                    addPlaylistSurveyToImage(playlistInfo, playlistData);
                }
            }

            // Export the intro, if there is one.
            if (packageInfo.hasIntro()) {
                AudioItem audioItem = packageInfo.getIntro();
                String filename = repository.getAudioFilename(audioItem, getAudioFormat());

                // Export the audio file.
                File exportFile = determineShadowFile(messagesDir, filename, shadowMessagesDir);
                if (!exportFile.exists()) {
                    repository.exportAudioFileWithFormat(audioItem, exportFile, getAudioFormat());
                }
                // Add audio item to the package_data.txt.
                Path exportPath = makePath(new File(messagesDir, filename));
                packageData.withAnnouncement(exportPath);
            } else {
                // No intro, so export a dummy file of silence.
                String filename = "7" + '.' + getAudioFormat().getFileExtension();
                File exportFile = new File(promptsDir, filename);
                Path exportPath = makePath(exportFile);
                packageData.withAnnouncement(exportPath);
            }
        }

        /**
         * Adds the content for a playlist to the given image's files.
         *
         * @param playlistInfo The playlist to be added to the image.
         * @param playlistData To create the package_data.txt file.
         * @throws IOException                                    if a file can't be read or written
         */
        private void addPlaylistContentToImage(PlaylistInfo playlistInfo, PlaylistData playlistData)
            throws
            IOException {
            for (String audioItemId : playlistInfo.getAudioItemIds()) {
                // Export the audio item.
                AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB()
                    .getMetadataStore().getAudioItem(audioItemId);
                builderContext.reportStatus(String.format("    Exporting audioitem %s to %s%n",
                    audioItemId,
                    messagesDir));
                String filename = repository.getAudioFilename(audioItem, getAudioFormat());

                // Export the audio file.
                File exportFile = determineShadowFile(messagesDir, filename, shadowMessagesDir);
                if (!exportFile.exists()) {
                    try {
                        repository.exportAudioFileWithFormat(audioItem, exportFile, getAudioFormat());
                    } catch (Exception ex) {
                        builderContext.logException(ex);
                    }
                }
                // Add audio item to the package_data.txt.
                Path exportPath = makePath(new File(messagesDir, filename));
                playlistData.addMessage(audioItem.getTitle(), exportPath);
            }
        }

        private void addPlaylistPromptsToImage(PlaylistInfo playlistInfo, PlaylistData playlistData) throws
                                                                                                     IOException,
                                                                                                     BaseAudioConverter.ConversionException,
                                                                                                     AudioItemRepository.UnsupportedFormatException {
            PlaylistPrompts playlistPrompts = playlistInfo.getPlaylistPrompts();
            String categoryId = playlistPrompts.getCategoryId();

            // Export anouncement.
            PromptInfo promptInfo = playlistInfo.getAnnouncement();
            exportPrompt(promptInfo, categoryId, playlistInfo.getTitle(), playlistData::withShortPrompt);
            // Invitation
            promptInfo = playlistInfo.getInvitation();
            exportPrompt(promptInfo, 'i' + categoryId, playlistInfo.getTitle(), playlistData::withLongPrompt);
        }

        private void exportPrompt(PromptInfo promptInfo,
            String categoryId,
            String title,
            BiConsumer<String, Path> withPrompt) throws
                                                 IOException,
                                                 BaseAudioConverter.ConversionException,
                                                 AudioItemRepository.UnsupportedFormatException {
            File exportFile;
            if (promptInfo.audioItem != null) {
                // export from content database to messagesDir
                String promptFilename = promptInfo.audioItem.getId() + '.' + getAudioFormat().getFileExtension();
                exportFile = determineShadowFile(messagesDir, promptFilename, shadowMessagesDir);
                if (!exportFile.exists()) {
                    repository.exportAudioFileWithFormat(promptInfo.audioItem, exportFile, getAudioFormat());
                }
            } else {
                // export from languages/{language}/cat/{categoryId} to promptsDir
                String promptFilename = categoryId + '.' + getAudioFormat().getFileExtension();
                exportFile = determineShadowFile(promptsDir, promptFilename, shadowPromptsDir);
                if (!exportFile.getParentFile().exists()) exportFile.getParentFile().mkdirs();
                if (!exportFile.exists()) {
                    try {
                        repository.exportFileWithFormat(promptInfo.audioFile, exportFile, getAudioFormat());
                    } catch (Exception ex) {
                        // Keep going after failing to export a prompt.
                        builderContext.logException(ex);
                    }
                }
            }
            withPrompt.accept(title, makePath(exportFile));

        }

        /**
         * If this playlist is a survey, look for a ".survey" file from which to create the custom
         * survey script. These are language (and variant) specific, so they go into the content/prompts/${language}
         * directory in the deployment.
         * @param playlistInfo Information about the playlist from the program spec.
         * @param playlistData More information about the playlist, from the content database.
         */
        @SuppressWarnings("ReassignedVariable")
        private void addPlaylistSurveyToImage(PlaylistInfo playlistInfo, PlaylistData playlistData) {
            // Look for a ${playlist}.survey in the program spec directory.
            String fname = playlistInfo.getTitle() + ".survey";
            File surveyFile = new File(builderContext.sourceProgramspecDir, fname);
            if (!surveyFile.isFile()) {
                fname = fname.replace(' ', '_');
                surveyFile = new File(builderContext.sourceProgramspecDir, fname);
            }
            if (!surveyFile.isFile()) return; // no survey definition

            Survey survey;
            try {
                survey = Survey.loadDefinition(surveyFile);
                Collection<String> prompts = survey.getPromptList();
                Map<String,String> promptsToItemIds = getPromptMap(prompts);
                survey.setPromptMap(promptsToItemIds);
            } catch (IOException e) {
                // Log the exception, to fail the deployment, and keep going.
                builderContext.logException(e);
                return;
            }
            CsmData csmData = survey.generateCsm();
            List<String> errors = new ArrayList<>();
            csmData.fillErrors(errors);

            // Create the survey's script.
            String csmFname = FilenameUtils.removeExtension(fname.replace(' ', '_')) + ".csm";
            File csmFile = new File(promptsDir, csmFname);
            try (FileOutputStream fos = new FileOutputStream(csmFile)) {
                // Here we write the binary CSM data.
                csmData.emit(fos);
                // This tells the TBv2 to execute the survey script when the "playlist" is entered.
                playlistData.withScript(csmFname);
            } catch (IOException e) {
                // Log the exception, to fail the deployment, and keep going.
                builderContext.logException(e);
            }
            // Create a .yaml source that corresponds to the survey's script.
            File yamlFile = new File(promptsDir, FilenameUtils.removeExtension(csmFname)+".yaml");
            try (FileOutputStream fos = new FileOutputStream(yamlFile);
                 PrintStream ps = new PrintStream(fos, true)) {
                ps.println(csmData);
            } catch (IOException e) {
                // Log the exception, to fail the deployment, and keep going.
                builderContext.logException(e);
            }

        }

        /**
         * Given the list of prompts from a survey (the prolog, questions, and epilog), find the message IDs for
         * the language (and variant) specific content.
         * @param prompts A list of strings that are the prompts.
         * @return a map of {prompt:messageId}
         */
        private Map<String, String> getPromptMap(Collection<String> prompts) {
            Map<String, String> result = new HashMap<>();
            MetadataStore store = builderContext.dbConfig.getMetadataStore();
            List<Locale> localeList = Collections.singletonList(new RFC3066LanguageCode(packageInfo.getLanguageCode()).getLocale());
            for (String prompt : prompts) {
                SearchResult searchResult = store.search(prompt, null, localeList);
                List<AudioItem> items = searchResult.getAudioItems()
                        .stream()
                        .map(store::getAudioItem)
                        .collect(Collectors.toList());
                // Hopefully there is one and no more than one. If more, take first one, randomly.
                if (!items.isEmpty()) {
                    result.put(prompt, items.get(0).getId());
                    if (items.size() > 1) {
                        System.out.printf("Multiple items found for '%s': %s\n", prompt, items.stream().map(AudioItem::getId).collect(Collectors.toList()));
                    }
                } else {
                    System.out.printf("Could not find audio item for survey item '%s'\n", prompt);
                }
            }
            return result;
        }

        private void addSystemPromptsToImage() throws IOException {
            // Copy the system prompt files from TB_Options. (0, 1, 2, ...)
            for (String prompt : TBBuilder.getRequiredSystemMessages(packageInfo.hasTutorial())) {
                String promptFilename = prompt + '.' + getAudioFormat().getFileExtension();

                File exportFile = determineShadowFile(promptsDir, promptFilename, shadowPromptsDir);
                if (!exportFile.getParentFile().exists()) exportFile.getParentFile().mkdirs();
                if (!exportFile.exists()) {
                    try {
                        repository.exportSystemPromptFileWithFormat(prompt, exportFile, packageInfo.getLanguageCode(),
                            getAudioFormat());
                    } catch (Exception ex) {
                        // Keep going after failing to export a prompt.
                        builderContext.logException(ex);
                    }
                }
            }
            // Copy the boilerplate files, $0-1.txt (tutorial list), 0.a18 (cha-ching), 7.a18 (silence)
            addBoilerplateFile(determineShadowFile(promptsDir, BELL_SOUND_V2, shadowPromptsDir));
            addBoilerplateFile(determineShadowFile(promptsDir, SILENCE_V2, shadowPromptsDir));

        }

        private void addSystemDirFilesToImage() throws IOException {
            File targetSystemDir = new File(imageDir, "system");
            // Look for an override in the program's TB_Options directory.
            File sourceSystemDir = new File(builderContext.sourceTbOptionsDir, "system.v2");
            if (!isValidSystemSource(sourceSystemDir)) {
                // Fall back to the system default, ~/Amplio/ACM/system.v2
                sourceSystemDir = new File(AmplioHome.getAppSoftwareDir(), "system.v2");
            }
            if (isValidSystemSource(sourceSystemDir)) {
                // Copy whatever is in the source dir to the target.
                FileUtils.copyDirectory(sourceSystemDir, targetSystemDir);
            }

            // If UF is hidden in the deployment, obtain and copy the appropriate 'uf.der' file to 'system/'
            if (deploymentInfo.isUfHidden()) {
                UfKeyHelper ufKeyHelper = new UfKeyHelper(builderContext.project);
                byte[] publicKey = ufKeyHelper.getPublicKeyFor(builderContext.deploymentNo);
                File derFile = new File(targetSystemDir, "uf.der");
                try (FileOutputStream fos = new FileOutputStream(derFile);
                     DataOutputStream dos = new DataOutputStream(fos)){
                    dos.write(publicKey);
                }
            }

            //
            Integer volumeThreshold = ACMConfiguration.getInstance().getCurrentDB().getVolumeThreshold();
            System.out.print("system dir : ");
            System.out.println(targetSystemDir.toString());

            if (volumeThreshold != null) {
                // Get directory containing config files
                File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
                String configPathString = "TB_Options" + File.separator + "config_files" + File.separator + "config.txt";
                File configPath = new File(tbLoadersDir, configPathString);

                String text = "VOLUME_THRESHOLD:" + volumeThreshold.toString();

                // Write volume threshold value to config file
                BufferedWriter out = null;
                try {
                    FileWriter fstream = new FileWriter(configPath, true); // true tells to append data.
                    out = new BufferedWriter(fstream);
                    out.write(text);
                }
                catch (IOException e) {
                    //
                }
                finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        }

        private void addUfToImage(PackageData packageData)
        throws BaseAudioConverter.ConversionException, AudioItemRepository.UnsupportedFormatException, IOException {
            File sourceLanguageDir = new File(builderContext.sourceTbOptionsDir,
                "languages" + File.separator + packageInfo.getLanguageCode());
            File sourceTutorialPromptsDir = new File(sourceLanguageDir, "cat");
            File promptFile = new File(sourceTutorialPromptsDir,
                Constants.CATEGORY_UNCATEGORIZED_FEEDBACK + "." + getAudioFormat().getFileExtension());
            PromptInfo shortPromptInfo = new PromptInfo(null, promptFile);
            promptFile = new File(sourceTutorialPromptsDir,
                "i" + Constants.CATEGORY_UNCATEGORIZED_FEEDBACK + "." + getAudioFormat().getFileExtension());
            PromptInfo longPromptInfo = new PromptInfo(null, promptFile);

            // Playlist for UF.
            PlaylistData playlistData = packageData.addPlaylist("userfeedback");

            // Prompts for the uf.
            exportPrompt(shortPromptInfo, Constants.CATEGORY_UNCATEGORIZED_FEEDBACK, "userfeedback", playlistData::withShortPrompt);
            exportPrompt(longPromptInfo,
                'i' + Constants.CATEGORY_UNCATEGORIZED_FEEDBACK,
                "userfeedback - invitation",
                playlistData::withLongPrompt);

        }

        /**
         * Add the playlist for the tutorial.
         *
         * @param packageData to be populated with data about the tutorial playlist.
         * @throws IOException if the copy fails.
         */
        private void addTutorialToImage(PackageData packageData) throws
                                                                 IOException,
                                                                 BaseAudioConverter.ConversionException,
                                                                 AudioItemRepository.UnsupportedFormatException {
            File sourceLanguageDir = new File(builderContext.sourceTbOptionsDir,
                "languages" + File.separator + packageInfo.getLanguageCode());
            File sourceTutorialPromptsDir = new File(sourceLanguageDir, "cat");
            File promptFile = new File(sourceTutorialPromptsDir,
                Constants.CATEGORY_TUTORIAL + "." + getAudioFormat().getFileExtension());
            PromptInfo shortPromptInfo = new PromptInfo(null, promptFile);
            promptFile = new File(sourceTutorialPromptsDir,
                "i" + Constants.CATEGORY_TUTORIAL + "." + getAudioFormat().getFileExtension());
            PromptInfo longPromptInfo = new PromptInfo(null, promptFile);

            // Playlist for the tutorial.
            PlaylistData playlistData = packageData.addPlaylist("tutorial");

            for (String tutorialMessageId : TUTORIAL_V2_MESSAGES) {
                String tutorialFilename = tutorialMessageId + '.' + getAudioFormat().getFileExtension();

                String promptFilename = tutorialMessageId + '.' + getAudioFormat().getFileExtension();
                File sourcePromptFile = new File(sourceLanguageDir, tutorialFilename);
                File exportFile = determineShadowFile(promptsDir, promptFilename, shadowPromptsDir);
                if (!exportFile.getParentFile().exists()) exportFile.getParentFile().mkdirs();
                if (!exportFile.exists()) {
                    try {
                        repository.exportFileWithFormat(sourcePromptFile, exportFile, getAudioFormat());
                    } catch (Exception ex) {
                        // Keep going after failing to export a prompt.
                        builderContext.logException(ex);
                    }
                }
                // Add audio item to the package_data.txt.
                Path exportPath = makePath(new File(promptsDir, promptFilename));
                playlistData.addMessage(tutorialMessageId, exportPath);
            }
            // Prompts for the tutorial.
            exportPrompt(shortPromptInfo, Constants.CATEGORY_TUTORIAL, "tutorial", playlistData::withShortPrompt);
            exportPrompt(longPromptInfo,
                'i' + Constants.CATEGORY_TUTORIAL,
                "tutorial - invitation",
                playlistData::withLongPrompt);
        }


        /**
         * Creates a File object, either in the real directory, or in the shadow directory, depending on the value of
         * builderContext.deDuplicateAudio.
         *
         * @param realDirectory   Where the file ultimately needs to wind up; may get a zero-byte marker inside the
         *                        deployment, so that we can keep only a single copy of the file.
         * @param filename        The name of the file that will be created.
         * @param shadowDirectory A "shadow" directory, shared across all packages. One file here may ultimately
         *                        be copied to many packages.
         * @return The actual file to be written.
         * @throws IOException if there is a problem creating the zero-byte shadow file.
         */
        private File determineShadowFile(File realDirectory, String filename, File shadowDirectory) throws IOException {
            File exportFile;
            if (builderContext.deDuplicateAudio) {
                // Leave a 0-byte marker file to indicate an audio file that should be here.
                File markerFile = new File(realDirectory, filename);
                markerFile.createNewFile();
                exportFile = new File(shadowDirectory, filename);
            } else {
                // Export file to actual location.
                exportFile = new File(realDirectory, filename);
            }
            return exportFile;
        }

    }

    public static String getMp3FramesEXEPath() {
        return ACMConfiguration.getInstance().getSoftwareDir().getPath() + "/converters/lame/mp3frames.exe";
    }

    private static class Mp3FrameWrapper extends ExternalCommandRunner.CommandWrapper {
        private final File directory;
        private final List<File> targetFiles;

        Mp3FrameWrapper(File directory, List<File> targetFiles) {
            this.directory = directory;
            this.targetFiles = targetFiles;
        }

        @Override
        protected File getRunDirectory() {
            return directory;
        }

        protected String[] getCommand() {
            List<String> command = new ArrayList<>();
            command.add(getMp3FramesEXEPath());
            command.addAll(targetFiles.stream().map(File::getName).collect(Collectors.toList()));
            return command.toArray(new String[0]);
        }

        @Override
        protected List<ExternalCommandRunner.LineHandler> getLineHandlers() {
            return new ArrayList<>();
        }
    }

}
