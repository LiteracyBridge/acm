package org.literacybridge.acm.tbbuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.acm.deployment.DeploymentInfo.PackageInfo;
import org.literacybridge.acm.deployment.DeploymentInfo.PackageInfo.PlaylistInfo;
import org.literacybridge.acm.deployment.DeploymentInfo.PromptInfo;
import org.literacybridge.core.tbloader.PackagesData;
import org.literacybridge.core.tbloader.PackagesData.PackageData;
import org.literacybridge.core.tbloader.PackagesData.PackageData.PlaylistData;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

import static org.literacybridge.acm.Constants.CUSTOM_GREETING;

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
  ${playlist_name_2} # name of secon playlist
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
    private final AudioItemRepository.AudioFormat audioFormat = AudioItemRepository.AudioFormat.WAV;
    private final PackagesData allPackagesData;
    private final File imagesDir;

    public static final List<String> requiredFirmwareFiles = Arrays.asList("TBookRev2b.hex", "firmware_built.txt");
    public static final List<String> requiredCSMFiles = Arrays.asList("control_def.txt", "csm_data.txt");

    public final static List<String> TUTORIAL_V2_MESSAGES = Arrays.asList(
            // 22 and 23 are speed control, not implemented in v2
            // Note not in numeric order.
            "17", "16", "28", "26", "20", "21", /*"22", "23",*/ "19", "25", "54"
    );


    public static boolean isValidFirmwareSource(File maybeFirmware) {
        if (maybeFirmware == null || !maybeFirmware.isDirectory()) return false;
        Set<String> foundFiles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        foundFiles.addAll(Arrays.asList(Objects.requireNonNull(maybeFirmware.list())));
        return foundFiles.containsAll(requiredFirmwareFiles);
    }

    public static boolean isValidCSMSource(File maybeCSM) {
        if (maybeCSM == null || !maybeCSM.isDirectory()) return false;
        Set<String> foundFiles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        foundFiles.addAll(Arrays.asList(Objects.requireNonNull(maybeCSM.list())));
        return foundFiles.containsAll(requiredCSMFiles);
    }

    CreateForV2(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext, DeploymentInfo deploymentInfo) {
        super(tbBuilder, builderContext, deploymentInfo);
        allPackagesData = new PackagesData(builderContext.deploymentName);
        imagesDir = new File(builderContext.stagedDeploymentDir, "images.v2");
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
        return tbBuilder.getAcceptableFirmwareVersions(deploymentInfo.isUfHidden());
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
            String targetFilename = FilenameUtils.removeExtension(CUSTOM_GREETING) + '.' + getAudioFormat().getFileExtension();
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
         * @throws BaseAudioConverter.ConversionException         if an audio file can't be converted to the required format
         *                                                        (eg, .a18, .mp3)
         * @throws AudioItemRepository.UnsupportedFormatException if the requested or found audio format is not supported
         *                                                        (hmm, "Apple Lossless" maybe? ffmpeg supports many formats.)
         */
        private void addPlaylistContentToImage(PlaylistInfo playlistInfo, PlaylistData playlistData)
            throws
            IOException,
            BaseAudioConverter.ConversionException,
            AudioItemRepository.UnsupportedFormatException {
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
                    repository.exportAudioFileWithFormat(audioItem, exportFile, getAudioFormat());
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
        }

        private void addSystemDirFilesToImage() throws IOException {
            // Look for an override in the program's TB_Options directory.
            File sourceSystemDir = new File(builderContext.sourceTbOptionsDir, "system.v2");
            if (!isValidCSMSource(sourceSystemDir)) {
                // Fall back to the system default, ~/Amplio/ACM/system.v2
                sourceSystemDir = new File(AmplioHome.getAppSoftwareDir(), "system.v2");
            }

            File targetSystemDir = new File(imageDir, "system");

            // The csm_data.txt file. Control file for the TB.
            File csmData = new File(sourceSystemDir, "csm_data.txt");
            FileUtils.copyFileToDirectory(csmData, targetSystemDir);

            // Optionally, the source for csm_data.txt file. 
            File controlDefFile = new File(sourceSystemDir, "control_def.txt");
            if (controlDefFile.exists()) {
                FileUtils.copyFileToDirectory(controlDefFile, targetSystemDir);
            }
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
            File sourceTutorialTxt = new File(sourceLanguageDir, Constants.CATEGORY_TUTORIAL + ".txt");
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

}
