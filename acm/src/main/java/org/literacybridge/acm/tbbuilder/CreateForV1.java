package org.literacybridge.acm.tbbuilder;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.BELL_SOUND_V1;
import static org.literacybridge.acm.Constants.CUSTOM_GREETING_V1;
import static org.literacybridge.acm.Constants.SILENCE_V1;
import static org.literacybridge.acm.Constants.TUTORIAL_LIST;

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
├── basic
├── communities
├── images
│   ├── DEMO-1-en
│   │   ├── languages
│   │   │   └── en
│   │   │       ├── 0.a18
│   │   │       ├── 1.a18
│   │   │        . . .
│   │   │       ├── 9.a18
│   │   │       ├── cat
│   │   │       │   ├── 9-0.a18
│   │   │       │   ├── LB-2_kkg8lufhqr_jp.a18
│   │   │        . . .
│   │   │       │   └── iLB-2_uzz71upxwm_vn.a18
│   │   │       └── control.txt
│   │   ├── messages
│   │   │   ├── audio
│   │   │   │   ├── LB-2_uzz71upxwm_vg.a18
│   │   │        . . .
│   │   │   │   └── LB-2_uzz71upxwm_zd.a18
│   │   │   └── lists
│   │   │       └── 1
│   │   │           ├── 9-0.txt
│   │   │           ├── LB-2_kkg8lufhqr_jp.txt
│   │   │           ├── LB-2_uzz71upxwm_ve.txt
│   │   │           ├── LB-2_uzz71upxwm_vj.txt
│   │   │           ├── LB-2_uzz71upxwm_vn.txt
│   │   │           └── _activeLists.txt
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

 *
 */

@SuppressWarnings({"ResultOfMethodCallIgnored"})
class CreateForV1 extends CreateFromDeploymentInfo {

    private final AudioItemRepository.AudioFormat audioFormat = AudioItemRepository.AudioFormat.A18;
    @Override
    public AudioItemRepository.AudioFormat getAudioFormat() {
        return audioFormat;
    }
    @Override
    public String getPackageName(DeploymentInfo.PackageInfo packageInfo) {
        return packageInfo.getShortName();
    }
    @Override
    protected List<String> getAcceptableFirmwareVersions() {
        return tbBuilder.getAcceptableFirmwareVersions(deploymentInfo.isUfPublic());
    }



    CreateForV1(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext, DeploymentInfo deploymentInfo) {
        super(tbBuilder, builderContext, deploymentInfo);
    }




    /**
     * Expoort the correct firmware to the image. In general, "correct" means "latest".
     * @throws IOException if the file can't be copied.
     */
    @Override
    protected void exportFirmware() throws IOException {
        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File sourceFirmware = tbBuilder.utils.latestFirmwareImage();
        File stagedBasicDir = new File(builderContext.stagedDeploymentDir, "basic");
        FileUtils.copyFileToDirectory(sourceFirmware, stagedBasicDir);
        stagedBasicDir = new File(builderContext.stagedDeploymentDir, "firmware.v1");
        FileUtils.copyFileToDirectory(sourceFirmware, stagedBasicDir);
    }


    /**
     * Adds a Content Package to a Deployment. Copies the files to the staging directory.
     *
     * @param packageInfo Information about the package: name, language, groups.
     * @throws Exception if there is an error creating or reading a file.
     */
    protected void addImageForPackage(DeploymentInfo.PackageInfo packageInfo) throws Exception {
        builderContext.reportStatus("%n%nExporting package %s%n", getPackageName(packageInfo));
        File stagedImagesDir = new File(builderContext.stagedDeploymentDir, "images.v1");
        File stagedImageDir = new File(stagedImagesDir, getPackageName(packageInfo));

        IOUtils.deleteRecursive(stagedImageDir);
        stagedImageDir.mkdirs();
        
        File stagedMessagesDir = new File(stagedImageDir, "messages");
        File stagedAudioDir = new File(stagedMessagesDir, "audio");
        File stagedLanguagesDir = new File(stagedImageDir, "languages");
        File stagedLanguageDir = new File(stagedLanguagesDir, packageInfo.getLanguageCode());
        File shadowAudioFilesDir = new File(builderContext.stagedShadowDir, "messages" + File.separator + "audio");
        File shadowLanguageDir = new File(builderContext.stagedShadowDir, "languages"+File.separator + packageInfo.getLanguageCode());

        File sourceCommunitiesDir = new File(builderContext.sourceTbLoadersDir, "communities");
        File stagedCommunitiesDir = new File(builderContext.stagedDeploymentDir, "communities");

        for (File f : new File[]{stagedAudioDir, stagedLanguageDir, stagedLanguageDir, sourceCommunitiesDir, stagedCommunitiesDir}) {
            if (!f.exists() && !f.mkdirs()) {
                throw(new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", f)));
            }
        }
        if (builderContext.deDuplicateAudio) {
            for (File f : new File[]{shadowAudioFilesDir, shadowLanguageDir}) {
                if (!f.exists() && !f.mkdirs()) {
                    throw(new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", f)));
                }
            }
        }

        addPackageContentToImage(packageInfo, stagedImageDir);
        addPackagePromptsToImage(packageInfo, stagedImageDir);
        addPackageSystemFilesToImage(packageInfo, stagedImageDir);
        if (packageInfo.hasTutorial()) {
            addTutorialToImage(packageInfo, stagedImageDir);
        }

        // Custom greetings
        exportGreetings(sourceCommunitiesDir, stagedCommunitiesDir, packageInfo);

        builderContext.reportStatus(
                String.format("Done with adding image for %s and %s.%n", getPackageName(packageInfo), packageInfo.getLanguageCode()));
    }

    /**
     * Adds the content for the given package to the given image files. This creates and populates
     * the messages/lists/1 directory and the
     * @param packageInfo The package to be added to the image.
     * @param imageDir The location of the iamge files, where the content is written.
     */
    private void addPackageContentToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir)
        throws
            IOException,
            BaseAudioConverter.ConversionException,
            AudioItemRepository.UnsupportedFormatException,
            TBBuilder.TBBuilderException {
        File messagesDir = new File(imageDir, "messages");
        File listsDir = new File(messagesDir, "lists" + File.separator + "1");
        if (!listsDir.exists() && !listsDir.mkdirs()) {
            throw(new TBBuilder.TBBuilderException(String.format("Unable to create directory: %s%n", listsDir)));
        }
        File audioDir = new File(messagesDir, "audio");
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());

        File shadowAudioFilesDir = new File(builderContext.stagedShadowDir, "messages" + File.separator + "audio");

        File activeLists = new File(listsDir, "_activeLists.txt");
        boolean haveUfContent = false;
        try (PrintWriter activeListsWriter = new PrintWriter(activeLists)) {
            // Export the playlist content
            for (DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo : packageInfo.getPlaylists()) {
                String promptCat = playlistInfo.getCategoryId();
                boolean isUfContent = promptCat.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                haveUfContent |= isUfContent;
                // Add the category id to the master list (_activeLists)
                activeListsWriter.println(isUfContent?"":"!" + promptCat);

                File listFile = new File(listsDir, promptCat + ".txt");
                try (PrintWriter listWriter = new PrintWriter(listFile)) {
                    addPlaylistContentToImage(playlistInfo, listWriter, audioDir, shadowAudioFilesDir);
                }
            }

            // If uf is not public, there was no UF in _activeLists.txt, and no (empty) uf.txt file. Create it now.
            if (!haveUfContent || packageInfo.isUfHidden()) {
                File listFile = new File(listsDir, Constants.CATEGORY_UNCATEGORIZED_FEEDBACK + ".txt");
                listFile.createNewFile();
                if (packageInfo.isUfPublic()) {
                    activeListsWriter.println(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                }
            }
            // If the deployment should include the tutorial, add it here.
            if (packageInfo.hasTutorial()) {
                addTutorialToImage(packageInfo, imageDir);
                activeListsWriter.println('!'+ Constants.CATEGORY_TUTORIAL);

            }
        }
        // Export the intro, if there is one.
        if (packageInfo.hasIntro()) {
            File exportFile = new File(languageDir,"intro.a18");
            repository.exportAudioFileWithFormat(packageInfo.getIntro(), exportFile, getAudioFormat());
        }
    }

    /**
     * Adds the content for a playlist to the given image's files.
     * @param playlistInfo The playlist to be added to the image.
     * @param listWriter Write audio item ids here.
     * @param audioDir Audio files (or shadow markers) go here.
     * @param shadowDir If shodowing, the real files go here.
     * @throws IOException if a file can't be read or written
     */
    private void addPlaylistContentToImage(DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo, PrintWriter listWriter,
        File audioDir, File shadowDir) throws
                          IOException {

        for (String audioItemId : playlistInfo.getAudioItemIds()) {
            // Export the audio item.
            AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB()
                .getMetadataStore().getAudioItem(audioItemId);
            builderContext.reportStatus(String.format("    Exporting audioitem %s to %s%n", audioItemId, audioDir));
            String filename = repository.getAudioFilename(audioItem, getAudioFormat());

            File exportFile = determineShadowFile(audioDir, filename, shadowDir);
            if (!exportFile.exists()) {
                try {
                    repository.exportAudioFileWithFormat(audioItem, exportFile, getAudioFormat());
                } catch (Exception ex) {
                    builderContext.logException(ex);
                }
            }

            // Add the audio item id to the list file.
            listWriter.println(audioItemId);
        }
    }


    private void addPackagePromptsToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir) throws IOException {
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());
        File shadowLanguageDir = new File(builderContext.stagedShadowDir, "languages"+File.separator + packageInfo.getLanguageCode());

        // Copy the system prompt files from TB_Options.
        for (String prompt : TBBuilder.getRequiredSystemMessages(packageInfo.hasTutorial())) {
            String promptFilename = prompt + '.' + getAudioFormat().getFileExtension();

            File exportFile = determineShadowFile(languageDir, promptFilename, shadowLanguageDir);
            if (!exportFile.getParentFile().exists()) exportFile.getParentFile().mkdirs();
            if (!exportFile.exists()) {
                try {
                    repository.exportSystemPromptFileWithFormat(prompt, exportFile, packageInfo.getLanguageCode(),
                        getAudioFormat());
                } catch(Exception ex) {
                    // Keep going after failing to export a prompt.
                    builderContext.logException(ex);
                }
            }
        }
        // Copy the boilerplate files, $0-1.txt (tutorial list), 0.a18 (cha-ching), 7.a18 (silence)
        addBoilerplateFile(determineShadowFile(languageDir, TUTORIAL_LIST, shadowLanguageDir));
        addBoilerplateFile(determineShadowFile(languageDir, BELL_SOUND_V1, shadowLanguageDir));
        addBoilerplateFile(determineShadowFile(languageDir, SILENCE_V1, shadowLanguageDir));

        // Copy the prompt files from TB_Options.
        File shadowCatDir = new File(shadowLanguageDir, "cat");
        File catDir = new File(languageDir, "cat");

        Set<String> categories = packageInfo.getPlaylists()
            .stream()
            .map(DeploymentInfo.PackageInfo.PlaylistInfo::getCategoryId)
            .collect(Collectors.toCollection(LinkedHashSet::new)); // preserve encounter order
        // The propts for feedback are needed, at least to tell users where their feedback went.
        categories.add(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
        // If the deployment should include the tutorial, also include the tutorial prompts.
        if (packageInfo.hasTutorial()) {
            categories.add(Constants.CATEGORY_TUTORIAL);
        }
        for (String prompt : categories) {
            String promptFilename = prompt + '.' + getAudioFormat().getFileExtension();
            String prompt2Filename = 'i'+prompt;
            File exportFile = determineShadowFile(catDir, promptFilename, prompt2Filename, shadowCatDir);
            if (!exportFile.getParentFile().exists()) exportFile.getParentFile().mkdirs();
            // If the short-prompt audio file is not in the shadow directory, copy *both* there now.
            if (!exportFile.exists()) {
                try {
                    // This function handles the difference between languages/en/cat/2-0.a18 and "Health.a18".
                    repository.exportCategoryPromptPairWithFormat(getPackageName(packageInfo),
                        prompt,
                        exportFile,
                        packageInfo.getLanguageCode(),
                        getAudioFormat());
                } catch (BaseAudioConverter.ConversionException | AudioItemRepository.UnsupportedFormatException ex) {
                    // Keep going after failing to export a prompt.
                    builderContext.logException(ex);
                }
            }
        }
    }

    private void addPackageSystemFilesToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir) throws
                                                                                                     IOException {
        File systemDir = new File(imageDir, "system");
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());
        systemDir.mkdirs();

        // The package marker file.
        File packageMarkerFile = new File(systemDir, getPackageName(packageInfo) + ".pkg");
        packageMarkerFile.createNewFile();

        // The config.txt file. It could have just as easily been in basic/system/config.txt.
        File sourceConfigFile = new File(builderContext.sourceTbOptionsDir, "config_files"+File.separator+"config.txt");
        FileUtils.copyFileToDirectory(sourceConfigFile, systemDir);

        // .grp marker file
        for (String group : packageInfo.getGroups()) {
            String filename = group + ".grp";
            File file = new File(systemDir, filename);
            file.createNewFile();
        }

        // profiles.txt
        String profileString = getPackageName(packageInfo).toUpperCase() + "," + packageInfo.getLanguageCode() + ","
            + TBBuilder.firstMessageListName + ",menu\n";
        File profileFile = new File(systemDir, "profiles.txt");
        try (FileWriter fw = new FileWriter(profileFile);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(profileString);
        }

        // Appropriate control file in languages directory, based on "intro" and uf hidden.
        String sourceControlFilename = String.format("system_menus"+File.separator+"control-%s_intro%s.txt",
            packageInfo.hasIntro()?"with":"no",
            packageInfo.isUfHidden()?"_no_fb":"");
        File sourceControlFile = new File(builderContext.sourceTbOptionsDir, sourceControlFilename);
        FileUtils.copyFile(sourceControlFile, new File(languageDir, "control.txt"));
    }

    /**
     * Copy the files for the tutorial. Currently just the $0-1.txt file, but this could also include the
     * audio files themselves.
     * @param packageInfo The package that needs a tutorial.
     * @param imageDir Where the files are to be put.
     * @throws IOException if the copy fails.
     */
    private void addTutorialToImage(DeploymentInfo.PackageInfo packageInfo, File imageDir) throws IOException {
        File sourceLanguageDir = new File(builderContext.sourceTbOptionsDir, "languages"+File.separator + packageInfo.getLanguageCode());
        File sourceTutorialTxt = new File(sourceLanguageDir, Constants.CATEGORY_TUTORIAL + ".txt");
        File languageDir = new File(imageDir, "languages" + File.separator + packageInfo.getLanguageCode());
        File tutorialTxt = new File(languageDir, sourceTutorialTxt.getName());
        IOUtils.copy(sourceTutorialTxt, tutorialTxt);

    }

    /**
     * Creates a File object, either in the real directory, or in the shadow directory, depending on the value of
     * builderContext.deDuplicateAudio.
     * @param realDirectory Where the file ultimately needs to wind up; may get a zero-byte marker inside the 
     *                      deployment, so that we can keep only a single copy of the file.
     * @param filename The name of the file that will be created.
     * @param shadowDirectory A "shadow" directory, shared across all packages. One file here may ultimately
     *                        be copied to many packages.
     * @return The actual file to be written.
     * @throws IOException if there is a problem creating the zero-byte shadow file.
     */
    private File determineShadowFile(File realDirectory, String filename, File shadowDirectory) throws IOException {
        return determineShadowFile(realDirectory, filename, null, shadowDirectory);
    }

    /**
     * Almost exactly like determineShadowFile(File, String, File).
     * Creates a File object, either in the real directory, or in the shadow directory, depending on the value of
     * builderContext.deDuplicateAudio.
     * @param realDirectory Where the file ultimately needs to wind up; may get a zero-byte marker inside the 
     *                      deployment, so that we can keep only a single copy of the file.
     * @param filename The name of the file that will be created.
     * @param filename2 The name of another file for which a shadow file will be created. Note that this assumes
     *                  that the caller knows how to create the second file in the same directory as the first file.
     *                  This is used for the pairs of playlist prompts.
     * @param shadowDirectory A "shadow" directory, shared across all packages. One file here may ultimately
     *                        be copied to many packages.
     * @return The actual file to be written.
     * @throws IOException if there is a problem creating the zero-byte shadow file.
     */
    private File determineShadowFile(File realDirectory, String filename, String filename2, File shadowDirectory) throws IOException {
        File exportFile;
        if (builderContext.deDuplicateAudio) {
            // Leave a 0-byte marker file to indicate an audio file that should be here.
            File markerFile = new File(realDirectory, filename);
            markerFile.createNewFile();
            markerFile = new File(realDirectory, filename2);
            markerFile.createNewFile();
            exportFile = new File(shadowDirectory, filename);
        } else {
            // Export file to actual location.
            exportFile = new File(realDirectory, filename);
        }
        return exportFile;
    }

    /**
     * Export the greetings for the recipients of the package. May include recipients from other variants.
     *
     * @param sourceCommunitiesDir The communities directory in the TB-Loaders directory.
     * @param stagedCommunitiesDir The communities directory in the output staging directory.
     * @param packageInfo The package info.
     * @throws IOException If a greeting file can't be read or written.
     * @throws BaseAudioConverter.ConversionException if a greeting file can't be converted.
     */
    private void exportGreetings(File sourceCommunitiesDir,
        File stagedCommunitiesDir,
        DeploymentInfo.PackageInfo  packageInfo) throws
                                                                                    IOException,
                                                                                    BaseAudioConverter.ConversionException {
        // If we know the deployment, we can be more specific, possibly save some space.
        RecipientList recipients = builderContext.deploymentNo > 0
           ? builderContext.programSpec.getRecipientsForDeploymentAndLanguage(deploymentInfo.getDeploymentNumber(),
            packageInfo.getLanguageCode())
           : builderContext.programSpec.getRecipients();
        for (Recipient recipient : recipients) {
            // TODO: Get recipient specific audio format, in case there are ever programs with mixed v1/v2 TBs.
            exportGreetingForRecipient(recipient, sourceCommunitiesDir, stagedCommunitiesDir, getAudioFormat());
        }
    }

    /**
     * Exports the greeting file for one recipient.
     * @param recipient The recipient for which to export the greeting.
     * @param sourceCommunitiesDir Where greetings come from ( {program}/TB-Loaders/communities )
     * @param stagedCommunitiesDir Where greetings go to.
     * @param audioFormat Audio format for the greeting.
     * @throws IOException if a greeting can't be read or written.
     * @throws BaseAudioConverter.ConversionException If a greeting can't be converted.
     */
    private void exportGreetingForRecipient(Recipient recipient,
            File sourceCommunitiesDir,
            File stagedCommunitiesDir,
            AudioItemRepository.AudioFormat audioFormat) throws
                                                         IOException,
                                                         BaseAudioConverter.ConversionException {
        String communityDirName = builderContext.programSpec.getRecipientsMap().getOrDefault(recipient.recipientid, recipient.recipientid);
        File sourceCommunityDir = new File(sourceCommunitiesDir, communityDirName);
        File targetCommunityDir = new File(stagedCommunitiesDir, communityDirName);
        File sourceLanguageDir = new File(sourceCommunityDir, "languages" + File.separator + recipient.languagecode);
        File targetLanguageDir = new File(targetCommunityDir, "languages" + File.separator + recipient.languagecode);
        File targetSystemDir = new File(targetCommunityDir, "system");
        // Copy the greeting in the recipient's language, if there is one. Create the vestigal ".grp" file as well.
        if (sourceLanguageDir.exists() && sourceLanguageDir.isDirectory() && Objects.requireNonNull(sourceLanguageDir.listFiles()).length > 0) {
            targetLanguageDir.mkdirs();
            targetSystemDir.mkdirs();
            File targetFile = new File(targetLanguageDir, CUSTOM_GREETING_V1);
            repository.exportGreetingWithFormat(sourceLanguageDir, targetFile, audioFormat);
            File groupFile = new File(targetSystemDir, recipient.languagecode + ".grp");
            groupFile.createNewFile();
        }
    }
    
}
