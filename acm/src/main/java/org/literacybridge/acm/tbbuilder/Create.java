package org.literacybridge.acm.tbbuilder;

import com.opencsv.CSVWriterBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static org.literacybridge.acm.Constants.CUSTOM_GREETING;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CATEGORIES_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CONTENT_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CATEGORIES_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CONTENT_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT;
import static org.literacybridge.acm.tbbuilder.TBBuilder.PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME;

@SuppressWarnings({"ResultOfMethodCallIgnored", "JavadocReference"})
class Create {

    private final TBBuilder tbBuilder;
    private final TBBuilder.BuilderContext builderContext;
    private final AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();

    Create(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext) {
        this.tbBuilder = tbBuilder;
        this.builderContext = builderContext;
    }


    void createDeploymentWithPackages(List<TBBuilder.PackageInfo> packages) throws Exception {
        createDeployment();

        for (TBBuilder.PackageInfo pi : packages) {
            addImage(pi);
        }

        builderContext.contentInPackageCSVWriter.close();
        builderContext.categoriesInPackageCSVWriter.close();
        builderContext.packagesInDeploymentCSVWriter.close();

    }

    /**
     * Creates the structure for a Deployment, into which packages can be added.
     *
     * @throws Exception if there is an IO error.
     */
    private void createDeployment() throws Exception {
        File stagedMetadataDir = new File(builderContext.stagingDir, "metadata" + File.separator + builderContext.deploymentName);
        DateFormat ISO8601time = new SimpleDateFormat("HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
        ISO8601time.setTimeZone(TBLoaderConstants.UTC);
        String timeStr = ISO8601time.format(new Date());
        String revFileName = String.format(TBLoaderConstants.UNPUBLISHED_REVISION_FORMAT, timeStr, builderContext.deploymentName);
        // use LB Home Dir to create folder, then zip to Dropbox and delete the
        // folder
        IOUtils.deleteRecursive(builderContext.stagedDeploymentDir);
        builderContext.stagedDeploymentDir.mkdirs();
        IOUtils.deleteRecursive(stagedMetadataDir);
        stagedMetadataDir.mkdirs();
        IOUtils.deleteRecursive(builderContext.stagedProgramspecDir);
        builderContext.stagedProgramspecDir.mkdirs();

        builderContext.contentInPackageCSVWriter = new CSVWriterBuilder(
                new FileWriter(new File(stagedMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME))).build();
        builderContext.categoriesInPackageCSVWriter = new CSVWriterBuilder(
                new FileWriter(new File(stagedMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME))).build();
        builderContext.packagesInDeploymentCSVWriter = new CSVWriterBuilder(
                new FileWriter(new File(stagedMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME))).build();

        // write column headers
        builderContext.contentInPackageCSVWriter.writeNext(CSV_COLUMNS_CONTENT_IN_PACKAGE);
        builderContext.categoriesInPackageCSVWriter.writeNext(CSV_COLUMNS_CATEGORIES_IN_PACKAGE);
        builderContext.packagesInDeploymentCSVWriter.writeNext(CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File sourceFirmware = tbBuilder.utils.latestFirmwareImage();
        File stagedBasicDir = new File(builderContext.stagedDeploymentDir, "basic");
        FileUtils.copyFileToDirectory(sourceFirmware, stagedBasicDir);

        if (builderContext.sourceProgramspecDir != null) {
            FileUtils.copyDirectory(builderContext.sourceProgramspecDir, builderContext.stagedProgramspecDir);
        }

        Utils.deleteRevFiles(builderContext.stagingDir);
        // Leave a marker to indicate that there exists an unpublished deployment here.
        File newRev = new File(builderContext.stagingDir, revFileName);
        newRev.createNewFile();
        // Put a marker inside the unpublished content, so that we will be able to tell which of
        // possibly several is the unpublished one.
        Utils.deleteRevFiles(builderContext.stagedDeploymentDir);
        newRev = new File(builderContext.stagedDeploymentDir, revFileName);
        newRev.createNewFile();

        builderContext.reportStatus("%nDone with deployment of basic/community content.%n");
    }

    /**
     * Adds a Content Package to a Deployment. Copies the files to the staging directory.
     *
     * @param pi Information about the package: name, language, groups.
     * @throws Exception if there is an error creating or reading a file.
     */
    private void addImage(TBBuilder.PackageInfo pi) throws Exception {
        Set<String> exportedCategories = null;
        boolean hasIntro = false;
        builderContext.reportStatus("%n%nExporting package %s%n", pi.name);

        File sourcePackageDir = new File(builderContext.sourceTbLoadersDir, "packages"+File.separator + pi.name);
        File sourceMessagesDir = new File(sourcePackageDir, "messages");
        File sourceListsDir = new File(sourceMessagesDir,
                "lists/" + TBBuilder.firstMessageListName);
        File stagedImagesDir = new File(builderContext.stagedDeploymentDir, "images");
        File stagedImageDir = new File(stagedImagesDir, pi.name);

        IOUtils.deleteRecursive(stagedImageDir);
        stagedImageDir.mkdirs();

        if (!sourceListsDir.exists() || !sourceListsDir.isDirectory()) {
            throw(new TBBuilder.TBBuilderException(String.format("Directory not found: %s%n", sourceListsDir)));
        } else //noinspection ConstantConditions
            if (sourceListsDir.listFiles().length == 0) {
            throw(new TBBuilder.TBBuilderException(String.format("No lists found in %s%n", sourceListsDir)));
        }

        File sourceCommunitiesDir = new File(builderContext.sourceTbLoadersDir, "communities");
        File stagedCommunitiesDir = new File(builderContext.stagedDeploymentDir, "communities");

        File stagedMessagesDir = new File(stagedImageDir, "messages");
        FileUtils.copyDirectory(sourceMessagesDir, stagedMessagesDir);

        File stagedAudioDir = new File(stagedMessagesDir, "audio");
        File stagedListsDir = new File(stagedMessagesDir,
                "lists/" + TBBuilder.firstMessageListName);
        File stagedLanguagesDir = new File(stagedImageDir, "languages");
        File stagedLanguageDir = new File(stagedLanguagesDir, pi.language);
        File shadowFilesDir = new File(builderContext.stagedDeploymentDir, "shadowFiles");
        File shadowAudioFilesDir = new File(shadowFilesDir, "messages" + File.separator + "audio");
        File shadowLanguageDir = new File(shadowFilesDir, "languages"+File.separator + pi.language);

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

        File[] listFiles = stagedListsDir.listFiles();
        //noinspection ConstantConditions
        for (File listFile : listFiles) {
            // We found a "0-5.txt" file (note that there's no entry in the _activeLists.txt file)
            // Export the ids listed in the file as languages/intro.txt (last one wins), and
            // delete the file. Remember that the file existed; we'll use that to select a
            // control.txt file that plays the intro.a18 at startup.
            if (listFile.getName().equalsIgnoreCase("_activeLists.txt")) {
                exportedCategories = exportCategoriesInPackage(pi.name, listFile);
            } else if (listFile.getName().equals(TBBuilder.IntroMessageListFilename)) {
                exportIntroMessage(listFile, stagedLanguageDir, pi.audioFormat);
                listFile.delete();
                hasIntro = true;
            } else {
                exportContentForPlaylist(pi.name, listFile, shadowAudioFilesDir, stagedAudioDir, pi.audioFormat);
            }
        }

        if (exportedCategories == null) {
            throw new IllegalStateException("Missing _activeLists.txt file");
        }

        // Empty directory structure
        File sourceBasic = new File(builderContext.sourceTbOptionsDir, "basic");
        FileUtils.copyDirectory(sourceBasic, stagedImageDir);

        // The config.txt file. It could have just as easily been in basic/system/config.txt.
        File sourceConfigFile = new File(builderContext.sourceTbOptionsDir, "config_files"+File.separator+"config.txt");
        File stagedSystemDir = new File(stagedImageDir, "system");
        FileUtils.copyFileToDirectory(sourceConfigFile, stagedSystemDir);

        // Custom greetings
        exportGreetings(sourceCommunitiesDir, stagedCommunitiesDir, pi);

        // System and category prompts
        File sourceLanguageDir = new File(builderContext.sourceTbOptionsDir, "languages"+File.separator + pi.language);
        exportSystemPrompts(shadowLanguageDir, stagedLanguageDir, pi.language, pi.audioFormat, exportedCategories.contains(Constants.CATEGORY_TUTORIAL));
        // The prompt "9-0" is always needed to announce where user feedback is recorded. "i9-0" is only needed
        // if user feedback is public.
        Set<String> neededPrompts = new HashSet<>(exportedCategories);
        neededPrompts.add(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
        exportPlaylistPrompts(pi, neededPrompts, shadowLanguageDir, stagedLanguageDir);

        // If the deployment has the tutorial, copy necessary files.
        if (exportedCategories.contains(Constants.CATEGORY_TUTORIAL)) {
            exportTutorial(sourceLanguageDir, stagedLanguageDir);
        }
        // If there is no category "9-0" in the _activeLists.txt file, then the user feedback
        // should not be playable. In that case, use the "_nofb" versions of control.txt.
        // Those have a "UFH", User Feedback Hidden, in the control file, which prevents the
        // Talking Book from *adding* a "9-0" to the _activeLists.txt file when user feedback
        // is recorded. If the 9-0 is already there, then the users can already hear other
        // users' feedback.
        boolean hasNoUf = !exportedCategories.contains(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
        String sourceControlFilename = String.format("system_menus"+File.separator+"control-%s_intro%s.txt",
                hasIntro?"with":"no",
                hasNoUf?"_no_fb":"");
        File sourceControlFile = new File(builderContext.sourceTbOptionsDir, sourceControlFilename);
        FileUtils.copyFile(sourceControlFile, new File(stagedLanguageDir, "control.txt"));

        // create profiles.txt
        String profileString = pi.name.toUpperCase() + "," + pi.language + ","
                + TBBuilder.firstMessageListName + ",menu\n";
        File profileFile = new File(stagedSystemDir, "profiles.txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(profileFile));
        out.write(profileString);
        out.close();

        for (String group : pi.groups) {
            File f = new File(stagedSystemDir, group + TBLoaderConstants.GROUP_FILE_EXTENSION);
            f.createNewFile();
        }

        File f = new File(stagedSystemDir, pi.name + ".pkg");
        f.createNewFile();

        exportPackagesInDeployment(pi.name, pi.language, pi.groups);
        builderContext.reportStatus(
                String.format("Done with adding image for %s and %s.%n", pi.name, pi.language));
    }

    /**
     * For some package, create the line in the packagesindeployment.csv file. See {@link CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT}
     * for the columns.
     * @param contentPackage The name of the package. This is built from the program name, the deployment, the
     *                       language, and variant (if any). The only real requirement is that it be unique.
     * @param languageCode Language of the package.
     * @param groups "Groups" of the package. We only use the language as a group. "Variants" are denoted in
     *               the package name.
     */
    private void exportPackagesInDeployment(
            String contentPackage,
            String languageCode, String[] groups)
    {
        String groupsconcat = StringUtils.join(groups, ',');
        String[] csvColumns = new String[9];
        csvColumns[0] = builderContext.project.toUpperCase();
        csvColumns[1] = builderContext.deploymentName.toUpperCase();
        csvColumns[2] = contentPackage.toUpperCase();
        csvColumns[3] = contentPackage.toUpperCase();
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int date = cal.get(Calendar.DAY_OF_MONTH);
        csvColumns[4] = month + "/" + date + "/"
                + year; // approx start date
        csvColumns[5] = null; // end date unknown at this point
        csvColumns[6] = languageCode;
        csvColumns[7] = groupsconcat;
        csvColumns[8] = null; // distribution name not known until publishing
        // NOTE that we don't ever include the distribution name in the metadata.
        // It's grabbed by the shell scripts from the folder name,
        // and then a SQL UPDATE adds it in after uploading the CSV.
        builderContext.packagesInDeploymentCSVWriter.writeNext(csvColumns);
    }

    /**
     * Given the _activeLists.txt file for a package, extract the list of category names from the file,
     * and return that as a set of strings. Write each category name to the "categoriesinPackage" csv. See
     * {@link CSV_COLUMNS_CATEGORIES_IN_PACKAGE} for columns.
     * @param contentPackage Name of the package. For the .csv file.
     * @param activeLists File with a list of playlist categories (like "2-0.txt" or "LB-2_uzz71upxwm_zf.txt"
     * @return a set of strings of the categories.
     * @throws IOException if a file can't be read or written.
     */
    private Set<String> exportCategoriesInPackage(
            String contentPackage,
            File activeLists) throws IOException {
        Set<String> categoriesInPackage = new LinkedHashSet<>();
        String[] csvColumns = new String[4];
        csvColumns[0] = builderContext.project.toUpperCase();
        csvColumns[1] = contentPackage.toUpperCase();

        int order = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(activeLists))) {
            while (reader.ready()) {
                String categoryID = reader.readLine().trim();
                if (categoryID.startsWith("!")) {
                    categoryID = categoryID.substring(1);
                }
                categoriesInPackage.add(categoryID);
                if (categoryID.startsWith("$")) {
                    categoryID = categoryID.substring(1);
                }
                csvColumns[2] = categoryID;
                csvColumns[3] = Integer.toString(order);
                builderContext.categoriesInPackageCSVWriter.writeNext(csvColumns);
                order++;
            }
        }
        return categoriesInPackage;
    }

    /**
     * Export the greetings for the recipients of the package. May include recipients from other variants.
     *
     * @param sourceCommunitiesDir The communities directory in the TB-Loaders directory.
     * @param stagedCommunitiesDir The communities directory in the output staging directory.
     * @param pi The package info.
     * @throws IOException If a greeting file can't be read or written.
     * @throws BaseAudioConverter.ConversionException if a greeting file can't be converted.
     */
    private void exportGreetings(File sourceCommunitiesDir,
            File stagedCommunitiesDir,
            TBBuilder.PackageInfo pi) throws
                                                                                    IOException,
                                                                                    BaseAudioConverter.ConversionException {
        // If we know the deployment, we can be more specific, possibly save some space.
        RecipientList recipients = builderContext.deploymentNo > 0
           ? builderContext.programSpec.getRecipientsForDeploymentAndLanguage(builderContext.deploymentNo, pi.language)
           : builderContext.programSpec.getRecipients();
        for (Recipient recipient : recipients) {
            // TODO: Get recipient specific audio format, in case there are ever programs with mixed v1/v2 TBs.
            exportGreetingForRecipient(recipient, sourceCommunitiesDir, stagedCommunitiesDir, pi.audioFormat);
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
            File targetFile = new File(targetLanguageDir, CUSTOM_GREETING);
            repository.exportGreetingWithFormat(sourceLanguageDir, targetFile, audioFormat);
            File groupFile = new File(targetSystemDir, recipient.languagecode + ".grp");
            groupFile.createNewFile();
        }
    }

    /**
     * Copy the files for the tutorial. Currently just the $0-1.txt file, but this could also include the
     * audio files themselves.
     * @param sourceLanguageDir Source of files.
     * @param stagedLanguageDir Destination of files.
     * @throws IOException If a file can't be copied.
     */
    private void exportTutorial(File sourceLanguageDir, File stagedLanguageDir) throws IOException {
        File sourceTutorialTxt = new File(sourceLanguageDir, Constants.CATEGORY_TUTORIAL + ".txt");
        File stagedTutorialTxt = new File(stagedLanguageDir, sourceTutorialTxt.getName());
        IOUtils.copy(sourceTutorialTxt, stagedTutorialTxt);
    }

    /**
     * Export the system files. Copy the actual files to the "shadow" directory, so we keep only one copy
     * of each file, regardless how many images it appears in. Place a zero-byte marker file where the real
     * files should go. The TB-Loader will fix it up.
     * @param shadowLanguageDir Where the real files go.
     * @param stagedLanguageDir Where the zero-byte marker files go.
     * @param language Language for which prompts are needed.
     * @param audioFormat Audio format for which prompts are needed.
     * @throws IOException If a file can't be copied, found, etc.
     */
    private void exportSystemPrompts(File shadowLanguageDir,
            File stagedLanguageDir,
            String language,
            AudioItemRepository.AudioFormat audioFormat, boolean hasTutorial) throws
                                                         IOException {
        for (String prompt : TBBuilder.getRequiredSystemMessages(hasTutorial)) {
            String promptFilename = prompt + '.' + audioFormat.getFileExtension();

            File exportFile;
            if (builderContext.deDuplicateAudio) {
                File markerFile = new File(stagedLanguageDir, promptFilename);
                markerFile.createNewFile();
                exportFile = new File(shadowLanguageDir, promptFilename);
            } else {
                exportFile = new File(stagedLanguageDir, promptFilename);
            }
            if (!exportFile.exists()) {
                try {
                    repository.exportSystemPromptFileWithFormat(prompt, exportFile, language, audioFormat);
                } catch(Exception ex) {
                    // Keep going after failing to export a prompt.
                    builderContext.logException(ex);
                }
            }
        }
    }

    /**
     *
     * @param pi Info about the package, including language and audio format.
     * @param playlistCategories A collection containing the categories. Prompts for these are exported.
     * @param shadowLanguageDir Where the real files go.
     * @param stagedLanguageDir Where the zero-byte marker files go.
     */
    private void exportPlaylistPrompts(TBBuilder.PackageInfo pi,
            Collection<String> playlistCategories,
            File shadowLanguageDir,
            File stagedLanguageDir) {
        File shadowCatDir = new File(shadowLanguageDir, "cat");
        File stagedCatDir = new File(stagedLanguageDir, "cat");
        // We've just created the parents successfully. There is no valid reason for these to fail.
        if (builderContext.deDuplicateAudio) {
            shadowCatDir.mkdirs();
        }
        stagedCatDir.mkdirs();

        for (String prompt : playlistCategories) {
            String promptFilename = prompt + '.' + pi.audioFormat.getFileExtension();
            File exportFile;
            try {
                if (builderContext.deDuplicateAudio) {
                    // Create both marker files
                    File markerFile = new File(stagedCatDir, promptFilename);
                    markerFile.createNewFile();
                    markerFile = new File(stagedCatDir, 'i' + promptFilename);
                    markerFile.createNewFile();
                    // The name of the short prompt, in the shadow directory.
                    exportFile = new File(shadowCatDir, promptFilename);
                }
                else {
                    // The name of the short prompt, in the non-shadowed target directory.
                    exportFile = new File(stagedCatDir, promptFilename);
                }
                // If the short-prompt audio file is not in the shadow directory, copy *both* there now.
                if (!exportFile.exists()) {
                    repository.exportCategoryPromptPairWithFormat(pi.name,
                            prompt,
                            exportFile,
                            pi.language,
                            pi.audioFormat);
                }
            } catch(Exception ex) {
                // Keep going after failing to export a prompt.
                builderContext.logException(ex);
            }
        }
    }

    /**
     * Given a file with a list of audio item ids, extract those audio items to the given directory. Optionally
     * add each audio item to the "contentInPackage" csv (see {@link CSV_COLUMNS_CONTENT_IN_PACKAGE} for columns.)
     * @param contentPackage Name of the package, only used for the .csv file.
     * @param list File with list of item ids.
     * @param shadowDirectory The directory to receive the actual file. A copy of the file may already be there.
     * @param targetDirectory The ultimate destination directory of the file. A 0-byte marker file is written
     *                        there at this time, filled in with the actual content at deployment time.
     * @param audioFormat The needed AudioFormat
     * @throws Exception if a file can't be read or written.
     */
    private void exportContentForPlaylist(
            String contentPackage, File list,
            File shadowDirectory,
            File targetDirectory,
            AudioItemRepository.AudioFormat audioFormat)
            throws Exception {
        builderContext.reportStatus("  Exporting list %n" + list);
        String[] csvColumns = new String[5];
        csvColumns[0] = builderContext.project.toUpperCase();
        csvColumns[1] = contentPackage.toUpperCase();
        csvColumns[3] = FilenameUtils.removeExtension(list.getName());

        int order = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(list))) {
            while (reader.ready()) {
                String audioItemId = reader.readLine();
                AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB()
                        .getMetadataStore().getAudioItem(audioItemId);
                builderContext.reportStatus(String.format("    Exporting audioitem %s to %s%n", audioItemId, targetDirectory));
                String filename = repository.getAudioFilename(audioItem, audioFormat);

                File exportFile;
                if (builderContext.deDuplicateAudio) {
                    // Leave a 0-byte marker file to indicate an audio file that should be here.
                    File markerFile = new File(targetDirectory, filename);
                    markerFile.createNewFile();
                    exportFile = new File(shadowDirectory, filename);
                } else {
                    // Export file to actual location.
                    exportFile = new File(targetDirectory, filename);
                }
                if (!exportFile.exists()) {
                    repository.exportAudioFileWithFormat(audioItem,
                            exportFile,
                            audioFormat);
                }

                csvColumns[2] = audioItemId;
                csvColumns[4] = Integer.toString(order);
                builderContext.contentInPackageCSVWriter.writeNext(csvColumns);

                order++;
            }
        }
    }

    /**
     * Given a file with a list of audio item ids, extract those audio items to the given directory. Optionally
     * add each audio item to the "contentInPackage" csv.
     * @param list File with list of item ids.
     * @param targetDirectory Directory into which to extract the audio items.
     * @param audioFormat The needed AudioFormat
     * @throws Exception if a file can't be read or written.
     */
    private void exportIntroMessage(
            File list,
            File targetDirectory,
            AudioItemRepository.AudioFormat audioFormat)
            throws Exception {
        builderContext.reportStatus("  Exporting list %n" + list);

        try (BufferedReader reader = new BufferedReader(new FileReader(list))) {
            while (reader.ready()) {
                String audioItemId = reader.readLine();
                AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getAudioItem(audioItemId);
                builderContext.reportStatus(String.format("    Exporting audioitem %s to %s%n", audioItemId, targetDirectory));
                File exportFile = new File(targetDirectory,"intro.a18");

                repository.exportAudioFileWithFormat(audioItem, exportFile, audioFormat);
            }
        }
    }
}
