package org.literacybridge.acm.tbbuilder;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.tools.DBExporter;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.LogHelper;
import org.literacybridge.core.fs.ZipUnzip;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TBBuilder {
    private static final String[] CSV_COLUMNS_CONTENT_IN_PACKAGE = { "project",
        "contentpackage", "contentid", "categoryid", "order" };
    private static final String[] CSV_COLUMNS_CATEGORIES_IN_PACKAGE = { "project",
        "contentpackage", "categoryid", "order" };
    private static final String[] CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT = {
        "project", "deployment", "contentpackage", "packagename", "startDate",
        "endDate", "languageCode", "groups", "distribution" };
    private static final String CONTENT_IN_PACKAGES_CSV_FILE_NAME = "contentinpackages.csv";
    private static final String CATEGORIES_IN_PACKAGES_CSV_FILE_NAME = "categoriesinpackages.csv";
    private static final String PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME = "packagesindeployment.csv";

    private final static String [] REQUIRED_SYSTEM_MESSAGES_UF = {
     "0", "1", "2", "3", "4", "5", "6", "9", "10", "11",
        "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "28", "29",
        "33", "37", "38", "41", "53", "54", "61", "62", "63", "65", "80"
    };
    private final static String [] REQUIRED_SYSTEM_MESSAGES_NO_UF = {
        "0", "1", "2", "4", "5", "6", "7", "9", "10", "11",
        "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "28", "29",
        "33", "37", "38", "41", "53", "54", "61", "62", "63", "65", "80"
    };
    private final static String MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE = "r1220.img";

    public static String firstMessageListName = "1";
    public static String IntroMessageID = "0-5";
    private static String IntroMessageListFilename = IntroMessageID + ".txt";
    private static final String FEEDBACK_FROM_USERS = "9-0";
    public static final String ACM_PREFIX = "ACM-";
    private final static int MAX_DEPLOYMENTS = 5;

    private File sourceTbLoadersDir;
    private File sourceTbOptionsDir;

    private File targetDeploymentDir;
    private CSVWriter contentInPackageCSVWriter;
    private CSVWriter categoriesInPackageCSVWriter;
    private CSVWriter packagesInDeploymentCSVWriter;
    private File targetStagingDir;
    private String deploymentNumber;
    private List<String> fatalMessages = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();
    private List<String> warningMessages = new ArrayList<>();
    private Set<String> errorCommunities = new HashSet<>();
    private Set<String> errorLanguages = new HashSet<>();
    public String project;

    public static void main(String[] args) throws Exception {
        File lbDir = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);
        File logDir = new File(lbDir, "logs");
        new LogHelper().inDirectory(logDir).withName("TBBuilder.log").absolute().initialize();

        System.out.println(String.format("TB-Builder version %s", Constants.ACM_VERSION));
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        if (args[0].equalsIgnoreCase("CREATE")) {
            if (args.length < 5 || (args.length > 5 && ((args.length % 3) != 0))) {
                printUsage();
                System.exit(1);
            }

            TBBuilder tbb = new TBBuilder(args[1]);
            tbb.doCreate(args);
        } else if (args[0].equalsIgnoreCase("PUBLISH")) {
            if (args.length < 3) {
                printUsage();
                System.exit(1);
            }

            TBBuilder tbb = new TBBuilder(args[1]);
            int deploymentCount = args.length - 2;
            if (deploymentCount > MAX_DEPLOYMENTS)
                deploymentCount = MAX_DEPLOYMENTS;
            String[] deployments = new String[deploymentCount];
            System.arraycopy(args, 2, deployments, 0, deploymentCount);
            tbb.publish(deployments);
        } else {
            printUsage();
            System.exit(1);
        }
    }

    /**
     * Construct the TBBuilder. Opens the ACM that's being operated upon.
     *
     * @param sharedACM the ACM name.
     * @throws Exception if the database can't be initialized.
     */
    TBBuilder(String sharedACM) throws Exception {
        sharedACM = ACMConfiguration.cannonicalAcmDirectoryName(sharedACM);
        CommandLineParams params = new CommandLineParams();
        params.disableUI = true;
        params.sandbox = true;
        params.sharedACM = sharedACM;
        ACMConfiguration.initialize(params);
        ACMConfiguration.getInstance().setCurrentDB(params.sharedACM);
        // Like ~/Dropbox/ACM-UWR/TB-Loaders
        sourceTbLoadersDir = ACMConfiguration.getInstance().getTbLoaderDirFor(sharedACM);
        sourceTbOptionsDir = new File(sourceTbLoadersDir, "TB_Options");
        project = sharedACM.substring(ACM_PREFIX.length());
        // ~/LiteracyBridge/TB-Loaders
        File localTbLoadersDir = new File(
            ACMConfiguration.getInstance().getApplicationHomeDirectory(),
            Constants.TBLoadersHomeDir);
        // Like ~/LiteracyBridge/TB-Loaders/UWR
        targetStagingDir = new File(localTbLoadersDir, project);
    }

    /**
     * Gathers the files that will make a deployment, into the directory
     * ~/LiteracyBridge/TB-Loaders/PROJ/.
     * Existing content in the /content/DEPLOYMENT and /metadata/DEPLOYMENT
     * subdirectories of that directory will be deleted.
     *
     * @param args Command line args to the program.
     *             The first arg is "CREATE" (or we wouldn't be here)
     *             The second is the ACM name _with_ ACM- prefix
     *             Next is the deployment name. By convention project-year-name-sequence
     *             Then there is either:
     *             package-name language
     *             Or one or more instances of:
     *             (package-name language group) ...
     * @throws Exception if one of the packages encounters an IO error.
     */
    private void doCreate(String[] args) throws Exception {
        validateDeployment(args);

        createDeployment(args[2]);

        if (args.length == 5) {
            // one package with only default group
            addImage(args[3], args[4], TBLoaderConstants.DEFAULT_GROUP_LABEL);
        } else {
            // one or more packages with specified group
            int argIx = 3;
            while (argIx < args.length) {
                // First package is also for default group
                if (argIx == 3) {
                    addImage(args[argIx], args[argIx + 1], TBLoaderConstants.DEFAULT_GROUP_LABEL,
                        args[argIx + 2]);
                } else {
                    addImage(args[argIx], args[argIx + 1], args[argIx + 2]);
                }
                argIx += 3;
            }
        }

        contentInPackageCSVWriter.close();
        categoriesInPackageCSVWriter.close();
        packagesInDeploymentCSVWriter.close();

    }

    /**
     * Creates the structure for a Deployment, into which packages can be added.
     *
     * @param deployment name to be created.
     * @throws Exception if there is an IO error.
     */
    public void createDeployment(String deployment) throws Exception {
        deploymentNumber = deployment;
        targetDeploymentDir = new File(targetStagingDir, "content/" + deploymentNumber);
        File targetMetadataDir = new File(targetStagingDir, "metadata/" + deploymentNumber);

        // use LB Home Dir to create folder, then zip to Dropbox and delete the
        // folder
        IOUtils.deleteRecursive(targetDeploymentDir);
        targetDeploymentDir.mkdirs();
        IOUtils.deleteRecursive(targetMetadataDir);
        targetMetadataDir.mkdirs();

        contentInPackageCSVWriter = new CSVWriter(
            new FileWriter(
                new File(targetMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME)),
            ',');
        categoriesInPackageCSVWriter = new CSVWriter(
            new FileWriter(
                new File(targetMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)),
            ',');
        packagesInDeploymentCSVWriter = new CSVWriter(
            new FileWriter(
                new File(targetMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)),
            ',');

        // write column headers
        contentInPackageCSVWriter.writeNext(CSV_COLUMNS_CONTENT_IN_PACKAGE);
        categoriesInPackageCSVWriter.writeNext(CSV_COLUMNS_CATEGORIES_IN_PACKAGE);
        packagesInDeploymentCSVWriter.writeNext(CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File sourceFirmware = latestFirmwareImage();
        File targetBasicDir = new File(targetDeploymentDir, "basic");
        FileUtils.copyFileToDirectory(sourceFirmware, targetBasicDir);

        File sourceCommunitiesDir = new File(sourceTbLoadersDir, "communities");
        File targetCommunitiesDir = new File(targetDeploymentDir, "communities");
        FileUtils.copyDirectory(sourceCommunitiesDir, targetCommunitiesDir);

        deleteRevFiles(targetStagingDir);
        String revision;
        revision = TBLoaderConstants.UNPUBLISHED_REV + "_"
            + TBLoaderUtils.getDateTime().substring(8, 17);
        File newRev = new File(targetStagingDir, revision + ".rev");
        newRev.createNewFile();

        System.out.printf("%nDone with deployment of basic/community content.%n");
    }

    private File latestFirmwareImage() {
        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File sourceFirmware = null;
        File[] firmwareOptions = new File(sourceTbOptionsDir, "firmware")
            .listFiles();
        for (File f : firmwareOptions) {
            if (sourceFirmware == null) {
                sourceFirmware = f;
            } else if (sourceFirmware.getName()
                .compareToIgnoreCase(f.getName()) < 0) {
                sourceFirmware = f;
            }
        }
        return sourceFirmware;
    }

    /**
     * Adds a Content Package to a Deployment. Copies the files to the staging directory.
     *
     * @param packageName  being added to the Deployment.
     * @param languageCode of the content in the Package.
     * @param groups       that should receive the Package.
     * @throws Exception if there is an error creating or reading a file.
     */
    private void addImage(String packageName, String languageCode, String... groups)
        throws Exception {
        Set<String> exportedCategories = null;
        boolean hasIntro = false;
        int groupCount = groups.length;
        System.out.printf("%n%nExporting package %s%n", packageName);

        File sourcePackageDir = new File(sourceTbLoadersDir, "packages/" + packageName);
        File sourceMessagesDir = new File(sourcePackageDir, "messages");
        File sourceListsDir = new File(sourceMessagesDir,
            "lists/" + TBBuilder.firstMessageListName);
        File targetImagesDir = new File(targetDeploymentDir, "images");
        File targetImageDir = new File(targetImagesDir, packageName);

        IOUtils.deleteRecursive(targetImageDir);
        targetImageDir.mkdirs();

        if (!sourceListsDir.exists() || !sourceListsDir.isDirectory()) {
            System.err.printf("Directory not found: %s%n", sourceListsDir);
            System.exit(1);
        } else if (sourceListsDir.listFiles().length == 0) {
            System.err.printf("No lists found in %s%n", sourceListsDir);
            System.exit(1);
        }

        validatePackageForLanguage(packageName, languageCode);

        File targetMessagesDir = new File(targetImageDir, "messages");
        FileUtils.copyDirectory(sourceMessagesDir, targetMessagesDir);

        File targetAudioDir = new File(targetMessagesDir, "audio");
        File targetListsDir = new File(targetMessagesDir,
            "lists/" + TBBuilder.firstMessageListName);
        File targetLanguagesDir = new File(targetImageDir, "languages");
        File targetLanguageDir = new File(targetLanguagesDir, languageCode);
        File targetWelcomeMessageDir = targetLanguageDir;

        if (!targetAudioDir.exists() && !targetAudioDir.mkdirs()) {
            System.err.printf("Unable to create directory: %s%n", targetAudioDir);
            System.exit(1);
        }

        if (!targetLanguageDir.exists() && !targetLanguageDir.mkdirs()) {
            System.err.printf("Unable to create directory: %s%n", targetLanguageDir);
            System.exit(1);
        }

        if (!targetWelcomeMessageDir.exists() && !targetWelcomeMessageDir.mkdirs()) {
            System.err.printf("Unable to create directory: %s%n", targetWelcomeMessageDir);
            System.exit(1);
        }

        File[] lists = targetListsDir.listFiles();
        for (File list : lists) {
            if (list.getName().equals(TBBuilder.IntroMessageListFilename)) {
                exportList(packageName, list, targetWelcomeMessageDir, "intro.a18", false);
                list.delete();
                hasIntro = true;
            } else if (list.getName().equalsIgnoreCase("_activeLists.txt")) {
                exportedCategories = exportCategoriesInPackage(packageName, list);
            } else {
                exportList(packageName, list, targetAudioDir, true);
            }
        }

        File sourceBasic = new File(sourceTbOptionsDir, "basic");
        FileUtils.copyDirectory(sourceBasic, targetImageDir);

        File sourceConfigFile = new File(sourceTbOptionsDir, "config_files/config.txt");
        File targetSystemDir = new File(targetImageDir, "system");
        FileUtils.copyFileToDirectory(sourceConfigFile, targetSystemDir);

        File sourceLanguage = new File(sourceTbOptionsDir, "languages/" + languageCode);
        FileUtils.copyDirectory(sourceLanguage, targetLanguageDir);

        // If there is no category "9-0" in the _activeLists.txt file, then the user feedback
        // should not be playable. In that case, use the "_nofb" versions of control.txt.
        // Those have a "UFH", User Feedback Hidden, in the control file, which prevents the
        // Talking Book from *adding* a "9-0" to the _activeLists.txt file when user feedback
        // is recorded. If the 9-0 is already there, then the users can already hear other
        // users' feedback.
        boolean hasNoUf = !exportedCategories.contains(FEEDBACK_FROM_USERS);
        String sourceControlFilename = String.format("system_menus/control-%s_intro%s.txt",
            hasIntro?"with":"no",
            hasNoUf?"_no_fb":"");
        File sourceControlFile = new File(sourceTbOptionsDir, sourceControlFilename);
        FileUtils.copyFile(sourceControlFile, new File(targetLanguageDir, "control.txt"));

        // create profile.txt
        String profileString = packageName.toUpperCase() + "," + languageCode + ","
            + TBBuilder.firstMessageListName + ",menu\n";
        File profileFile = new File(targetSystemDir, "profiles.txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(profileFile));
        out.write(profileString);
        out.close();

        for (String group : groups) {
            File f = new File(targetSystemDir, group + TBLoaderConstants.GROUP_FILE_EXTENSION);
            f.createNewFile();
        }

        File f = new File(targetSystemDir, packageName + ".pkg");
        f.createNewFile();

        exportPackagesInDeployment(packageName, languageCode, groups);
        System.out.println(
            String.format("Done with adding image for %s and %s.", packageName, languageCode));
    }

    /**
     * Validates that the packages and communities pass certain sanity tests. (See individual
     * verifications for details.)
     *
     * @param args to the program, containing package names, languages, and groups.
     * @throws IOException if a file can't be read.
     */
    private void validateDeployment(String[] args) throws IOException {
        // Validate that the deployment is listed in the deployments.csv file.
        File deploymentsList = new File(sourceTbLoadersDir, "deployments.csv");
        if (deploymentsList.exists()) {
            boolean found = false;
            FileReader fileReader = new FileReader(deploymentsList);
            CSVReader csvReader = new CSVReader(fileReader);

            List<String[]> lines = csvReader.readAll();
            int count = 0;
            int deploymentIx = -1;
            for (String[] line : lines) {
                if (count++ == 0) {
                    for (int ix=0; ix<line.length; ix++) {
                        if (line[ix].equalsIgnoreCase("deployment")) {
                            deploymentIx = ix;
                            break;
                        }
                    }
                } else {
                    if (line[deploymentIx].equalsIgnoreCase(args[2])) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                fatalMessages.add(String.format("'%s' is not a valid deployment for ACM '%s'.", args[2], args[1]));
            }
        }

        // Get all of the languages and groups in the Deployment. As we iterate over the
        // packages and their langugages, validate the package/language combinations.
        Set<String> groups = new HashSet<>();
        Set<String> languages = new HashSet<>();

        validatePackageForLanguage(args[3], args[4]);

        languages.add(args[4].toLowerCase());
        groups.add(TBLoaderConstants.DEFAULT_GROUP_LABEL);
        if (args.length > 5)
            groups.add(args[5].toLowerCase());
        for (int argIx = 6; argIx < args.length; argIx += 3) {
            languages.add(args[argIx + 1].toLowerCase());
            groups.add(args[argIx + 2].toLowerCase());

            validatePackageForLanguage(args[argIx], args[argIx + 1]);
        }

        validateCommunities(new File(sourceTbLoadersDir, "communities"), languages, groups);

        // If there are errors or warnings, print them and let user decide whether to continue.
        if (fatalMessages.size() > 0 || errorMessages.size() > 0 || warningMessages.size() > 0) {
            if (fatalMessages.size() > 0) {
                System.err.printf("%n%n********************************************************************************%n");
                System.err.printf("%d Fatal Error(s) found in Deployment:%n", fatalMessages.size());
                for (String msg : fatalMessages)
                    System.err.println(msg);
            }
            if (errorMessages.size() > 0) {
                System.err.printf("%n%n================================================================================%n");
                System.err.printf("%d Error(s) found in Deployment:%n", errorMessages.size());
                for (String msg : errorMessages)
                    System.err.println(msg);
                if (errorCommunities.size() > 0) {
                    System.err.printf("%nThe following communities may not work properly with this Deployment:%n");
                    for (String community : errorCommunities) {
                        System.err.printf("'%s' ", community);
                    }
                    System.err.printf("%n");
                }
                if (errorLanguages.size() > 0) {
                    System.err.printf("%nThe following languages may not work properly with this Deployment:%n");
                    for (String community : errorLanguages) {
                        System.err.printf("'%s' ", community);
                    }
                    System.err.printf("%n");
                }
            }
            if (warningMessages.size() > 0) {
                System.err.printf("%n%n--------------------------------------------------------------------------------%n");
                System.err.printf("%d Warning(s) found in Deployment:%n", warningMessages.size());
                for (String msg : warningMessages)
                    System.err.println(msg);
            }

            if (fatalMessages.size() > 0) {
                System.err.printf("%n%nCannot continue, aborting.%n");
                System.exit(1);
            }
            System.err.printf("%n%nDo you want to continue (y/N)? ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String response = br.readLine().trim();
            if (response.length() == 0 || response.toLowerCase().charAt(0) != 'y') {
                System.err.println("Exiting.");
                System.exit(1);
            }
            System.err.printf("%nContinuing with %s.%n%n", errorMessages.size()>0? "errors" : "warnings");

        }
    }

    /**
     * Checks the exported package for the given language. Checks that the system prompt
     * recordings exist in TB-Loaders/TB_Options/languages/{language}/cat, and that the
     * playlist's list file exists.
     * <p>
     * If any file is missing, prints an error message and exits.
     *
     * @param packageName  The name of the package, used to construct the directory
     *                     (TB-Loaders/packages/{package}/messages/lists/1/)
     *                     containing the _activeLists.txt and individual playlist .txt
     *                     list files.
     * @param languageCode The language for which the package is being exported.
     * @throws IOException if any files can't be read.
     */
    private void validatePackageForLanguage(String packageName, String languageCode)
        throws IOException {
        // Get the directory containing the _activeLists.txt file plus the playlist files (like "2-0.txt")
        String listsPath =
            "packages/" + packageName + "/messages/lists/" + TBBuilder.firstMessageListName;
        File sourceListsDir = new File(sourceTbLoadersDir, listsPath);

        // Get the directory with the system prompt recordings for the language.
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(sourceTbLoadersDir, languagesPath);
        File languageDir = IOUtils.FileIgnoreCase(languagesDir, languageCode);
        File promptsDir = new File(languageDir, "cat");
        String firmwarePath = "TB_Options" + File.separator + "firmware";
        File firmwareDir = new File(sourceTbLoadersDir, firmwarePath);

        // Read the source _activeLists.txt file.
        File activeList = new File(sourceListsDir, "_activeLists.txt");
        if (!activeList.exists()) {
            fatalMessages.add(
                String.format("File '%s' not found for Package '%s'.", activeList.getName(),
                    packageName));
        } else {
            //read file into stream, try-with-resources
            try (BufferedReader br = new BufferedReader(new FileReader(activeList))) {
                String line;
                boolean foundUserFeedback = false;
                while ((line = br.readLine()) != null) {
                    // '!' means subject is locked.
                    if (line.charAt(0) == '!')
                        line = line.substring(1);
                    line = line.trim();
                    if (line.length() < 1)
                        continue;

                    if (line.equals(FEEDBACK_FROM_USERS)) {
                        foundUserFeedback = true;
                    }

                    // We have the category, ensure the system prompt exists.
                    File p1 = new File(promptsDir, line + ".a18");
                    File p2 = new File(promptsDir, "i" + line + ".a18");
                    if (!p1.exists()) {
                        errorMessages.add(
                            String.format("Missing category prompt for %s in language %s.", line,
                                languageCode));
                        errorLanguages.add(languageCode);
                    }
                    if (!p2.exists()) {
                        errorMessages.add(
                            String.format("Missing long category prompt for %s in language %s.", line,
                                languageCode));
                        errorLanguages.add(languageCode);
                    }

                    // Be sure there is a list for the category.
                    if (!line.equals("9-0") && !line.equals("$0-1")) {
                        File pList = new File(sourceListsDir, line + ".txt");
                        if (!pList.exists()) {
                            errorMessages.add(
                                String.format("Missing playlist file '%s.txt', for Package '%s', language '%s'.",
                                    line, packageName, languageCode));
                            errorLanguages.add(languageCode);
                        }
                    }
                }
                String[] required_messages = foundUserFeedback ? REQUIRED_SYSTEM_MESSAGES_UF : REQUIRED_SYSTEM_MESSAGES_NO_UF;
                for (String prompt : required_messages) {
                    File p1 = new File(languageDir, prompt + ".a18");
                    if (!p1.exists()) {
                        errorMessages.add(
                            String.format("Missing system message for %s in language %s.", prompt,
                                languageCode));
                        errorLanguages.add(languageCode);
                    }
                }
                // A firmware update may be required to support hidden user feedback. Check the
                // version currently in the project.
                if (!foundUserFeedback) {
                    String image = latestFirmwareImage().getName().toLowerCase();
                    if (image.compareTo(MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE) < 0) {
                        fatalMessages.add(String.format("Minimum firmware image for hidden user feedback is %s, but found %s.", MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE, image));
                    }
                }

                // If User Feedback will be hidden, warn the user of that fact, in case it is unexpected.
                if (!foundUserFeedback) {
                    warningMessages.add(String.format("User Feedback WILL BE HIDDEN for Package '%s' language '%s'.",
                        packageName, languageCode));
                }
            } catch (Exception ex) {
                errorMessages.add(
                    String.format("Exception reading _activeLists.txt for Package '%s' language '%s': %s",
                        packageName, languageCode, ex.getMessage()));
                errorLanguages.add(languageCode);
            }
        }
    }

    /**
     * Validates the Deployment for the project's communities:
     * For every directory in {project}/TB-Loaders/communities
     * -- look for a languages directory
     * --- look for individual language subdirectories ('en', 'dga', etc.)
     * --- ensure that at least one language is in the Deployment languages
     * ---- find any greetings (10.a18) files in the language directory
     * warn in no greetings
     * -- look for a system directory
     * --- look for the individual group files (dga.grp, default.grp, etc.)
     * --- ensure that at least one group is in the Deployment groups
     *
     * @param communitiesDir 'communities' directory, contains one directory per community
     * @param languages      in the Deployment, lowercase
     * @param groups         in the Deployment, lowercase
     */
    private void validateCommunities(
        File communitiesDir, Set<String> languages, Set<String> groups) {
        File[] communities = communitiesDir.listFiles();
        if (communities == null || communities.length == 0) {
            errorMessages.add("Missing or empty directory: " + communitiesDir.getAbsolutePath());
            return;
        }
        for (File c : communities) {
            if (c.exists() && c.isDirectory()) {
                boolean foundLanguage = false;
                boolean oneLanguage = false;
                boolean foundGreeting = false;
                boolean foundGroup = false;
                // Examine the community directory. We *must* find languages/{lang} where lang is
                // in {languages}, and we *want* to find languages/{lang}/10.a18
                // But, if the custom greeting is missing, we still have the default 10.a18.
                File languagesDir = new File(c, "languages");
                if (!languagesDir.exists() || !languagesDir.isDirectory()) {
                    errorMessages.add(String.format("Missing or empty 'languages' directory for community '%s'.", c.getName()));
                    errorCommunities.add(c.getName());
                } else {
                    // Look for individual language directories, 'en', 'dga', ...
                    File[] langs = languagesDir.listFiles(path -> path.isDirectory());
                    oneLanguage = langs != null && langs.length == 1;
                    for (File lang : langs) {
                        // Look for a greeting in the language.
                        if (lang.exists()) {
                            String languageName = lang.getName().toLowerCase();
                            if (languages.contains(languageName)) {
                                foundLanguage = true;
                                foundGreeting |= IOUtils.FileIgnoreCase(lang, "10.a18").exists();
                            }
                        }
                    }
                }
                // We *must* find system / {group}.grp
                File systemDir = new File(c, "system");
                if (!systemDir.exists() || !systemDir.isDirectory()) {
                    errorMessages.add(String.format("Missing or empty 'system' directory for community '%s'.", c.getName()));
                    errorCommunities.add(c.getName());
                } else {
                    // Look for .grp files.
                    File[] grps = systemDir.listFiles((dir, name) -> name.toLowerCase()
                        .endsWith(TBLoaderConstants.GROUP_FILE_EXTENSION));
                    for (File grp : grps) {
                        String groupName = StringUtils.substring(grp.getName().toLowerCase(), 0,
                            -4);
                        foundGroup |= groups.contains(groupName);
                    }
                }
                if (!foundLanguage) {
                    errorMessages.add(
                        String.format("Community '%s' does not have any language in the Deployment.",
                            c.getName()));
                    errorCommunities.add(c.getName());
                } else {
                    if (!foundGreeting) {
                        warningMessages.add(
                            String.format("No custom greeting is for community '%s'.", c.getName()));
                    }
                    if (!oneLanguage) {
                        warningMessages.add(
                            String.format("Community '%s' has multiple languages.", c.getName()));
                    }
                }
                if (!foundGroup) {
                    errorMessages.add(
                        String.format("Community '%s' is not in any group in the Deployment.",
                            c.getName()));
                    errorCommunities.add(c.getName());
                }
            }
        }
    }

    private static char getLatestDistributionRevision(
        File publishTbLoadersDir,
        final String distribution) throws Exception {
        char revision = 'a';
        File[] files = publishTbLoadersDir.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".rev") && name.toLowerCase().startsWith(distribution.toLowerCase()));
        if (files.length > 1)
            throw new Exception("Too many *rev files.  There can only be one.");
        else if (files.length == 1) {
            // Assuming distribution-X.rev, pick the next higher than 'X'
            char foundRevision = files[0].getName().charAt(distribution.length() + 1);
            if (foundRevision >= revision) {
                revision = ++foundRevision;
                // If there's already a directory (or file) of the new name, keep looking.
                File probe = new File(publishTbLoadersDir, distribution + '-' + revision);
                while (probe.exists() && Character.isLetter(revision)) {
                    revision++;
                    probe = new File(publishTbLoadersDir, distribution + '-' + revision);
                }
                // If no un-used name found, keep the original one.
                if (!Character.isLetter(revision)) {
                    if (!Character.isLetter(foundRevision)) {
                        throw new Exception(
                            "Too many revisions. Can't allocate a new file name suffix.");
                    }
                    revision = foundRevision;
                }
            }
        }
        // Delete *.rev, then create our distribution-revision.rev marker file.
        files = publishTbLoadersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".rev"));
        for (File f : files) {
            f.delete();
        }
        File newRev = new File(publishTbLoadersDir, distribution + "-" + revision + ".rev");
        newRev.createNewFile();
        return revision;
    }

    /**
     * Zips up a Deployment, and places it in a Dropbox/{ACM-NAME}/TB-Loaders/published/{Deployment}-{counter}
     * directory. Creates a marker file named {Deployment}-{counter}.rev
     *
     * @param deployments List of deployments. Effectively, always exactly one.
     * @throws Exception if a file can't be read.
     */
    private void publish(final String[] deployments) throws Exception {
        // e.g. 'ACM-UWR/TB-Loaders/published/'
        final File publishBaseDir = new File(sourceTbLoadersDir, "published");
        publishBaseDir.mkdirs();
        String distribution = deployments[0]; // assumes first deployment is most
        // recent and should be name of
        // distribution
        char revision = getLatestDistributionRevision(publishBaseDir, distribution
                                                     );
        final String publishDistributionName = distribution + "-" + revision; // e.g.
        // '2015-6-c'

        // e.g. 'ACM-UWR/TB-Loaders/published/2015-6-c'
        final File publishDistributionDir = new File(publishBaseDir,
            publishDistributionName);
        publishDistributionDir.mkdirs();

        String zipSuffix = distribution + "-" + revision + ".zip";
        File localContent = new File(targetStagingDir, "content");
        ZipUnzip.zip(localContent,
            new File(publishDistributionDir, "content-" + zipSuffix), true,
            deployments);

        // merge csv files
        File localMetadata = new File(targetStagingDir, "metadata");
        List<String> deploymentsList = Arrays.asList(deployments);
        File[] deploymentDirs = localMetadata.listFiles(f -> f.isDirectory()
            && deploymentsList.contains(f.getName()));
        final List<File> inputContentCSVFiles = new LinkedList<File>();
        final List<File> inputCategoriesCSVFiles = new LinkedList<File>();
        final List<File> inputPackagesCSVFiles = new LinkedList<File>();
        for (File deploymentDir : deploymentDirs) {
            deploymentDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.getName().endsWith(CONTENT_IN_PACKAGES_CSV_FILE_NAME)) {
                        inputContentCSVFiles.add(f);
                        return true;
                    } else if (f.getName().endsWith(CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)) {
                        inputCategoriesCSVFiles.add(f);
                        return true;
                    } else if (f.getName().endsWith(PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)) {
                        inputPackagesCSVFiles.add(f);
                        return true;
                    }
                    return false;
                }
            });
        }
        File metadataDir = new File(publishDistributionDir, "metadata");
        if (!metadataDir.exists())
            metadataDir.mkdir();
        File mergedCSVFile = new File(metadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME);
        mergeCSVFiles(inputContentCSVFiles, mergedCSVFile, CSV_COLUMNS_CONTENT_IN_PACKAGE);

        mergedCSVFile = new File(metadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME);
        mergeCSVFiles(inputCategoriesCSVFiles, mergedCSVFile, CSV_COLUMNS_CATEGORIES_IN_PACKAGE);

        mergedCSVFile = new File(metadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME);
        mergeCSVFiles(inputPackagesCSVFiles, mergedCSVFile, CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        new DBExporter(ACM_PREFIX + project, metadataDir).export();

        deleteRevFiles(targetStagingDir);
    }

    /**
     * Deletes all the *.rev files from the given directory.
     *
     * @param dir from which to remove .rev files.
     */
    private static void deleteRevFiles(File dir) {
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".rev"));
        for (File revisionFile : files) {
            revisionFile.delete();
        }
    }

    private void exportList(
        String contentPackage, File list,
        File targetDirectory, boolean writeToCSV) throws Exception {
        exportList(contentPackage, list, targetDirectory, null, writeToCSV);
    }

    private void exportPackagesInDeployment(
        String contentPackage,
        String languageCode, String[] groups) throws IOException {
        String groupsconcat = StringUtils.join(groups, ',');
        String[] csvColumns = new String[9];
        csvColumns[0] = project.toUpperCase();
        csvColumns[1] = deploymentNumber.toUpperCase();
        csvColumns[2] = contentPackage.toUpperCase();
        csvColumns[3] = contentPackage.toUpperCase();
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int date = cal.get(Calendar.DAY_OF_MONTH);
        csvColumns[4] = String.valueOf(month) + "/" + String.valueOf(date) + "/"
            + String.valueOf(year); // approx start date
        csvColumns[5] = null; // end date unknown at this point
        csvColumns[6] = languageCode;
        csvColumns[7] = groupsconcat;
        csvColumns[8] = null; // distribution name not known until publishing
        // NOTE that we don't ever include the distribution name in the metadata.
        // It's grabbed by the shell scripts from the folder name,
        // and then a SQL UPDATE adds it in after uploading the CSV.
        packagesInDeploymentCSVWriter.writeNext(csvColumns);
    }

    private Set<String> exportCategoriesInPackage(
        String contentPackage,
        File activeLists) throws IOException {
        Set<String> categoriesInPackage = new HashSet<>();
        String[] csvColumns = new String[4];
        csvColumns[0] = project.toUpperCase();
        csvColumns[1] = contentPackage.toUpperCase();

        // AudioItemRepository repository =
        // ACMConfiguration.getCurrentDB().getRepository();

        int order = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(activeLists))) {
            while (reader.ready()) {
                String categoryID = reader.readLine().trim();
                if (categoryID.startsWith("!")) {
                    categoryID = categoryID.substring(1);
                }
                if (categoryID.startsWith("$")) {
                    categoryID = categoryID.substring(1);
                }
                csvColumns[2] = categoryID;
                csvColumns[3] = Integer.toString(order);
                categoriesInPackageCSVWriter.writeNext(csvColumns);
                order++;
                categoriesInPackage.add(categoryID);
            }
        }
        return categoriesInPackage;
    }

    private void exportList(
        String contentPackage, File list,
        File targetDirectory, String filename, boolean writeToCSV)
        throws Exception {
        System.out.println("  Exporting list " + list);
        String[] csvColumns = new String[5];
        csvColumns[0] = project.toUpperCase();
        csvColumns[1] = contentPackage.toUpperCase();
        csvColumns[3] = list.getName().substring(0, list.getName().length() - 4); // strip
        // .txt

        AudioItemRepository repository = ACMConfiguration.getInstance()
            .getCurrentDB().getRepository();

        int order = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(list))) {
            while (reader.ready()) {
                String uuid = reader.readLine();
                AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB()
                    .getMetadataStore().getAudioItem(uuid);
                System.out.println(String.format("    Exporting audioitem %s to %s", uuid, targetDirectory));
                if (filename == null) {
                    repository.exportA18WithMetadata(audioItem, targetDirectory);
                } else {
                    repository.exportA18WithMetadataToFile(audioItem,
                        new File(targetDirectory, filename));
                }

                if (writeToCSV) {
                    csvColumns[2] = uuid;
                    csvColumns[4] = Integer.toString(order);
                    contentInPackageCSVWriter.writeNext(csvColumns);
                }

                order++;
            }
        }
    }

    private static void mergeCSVFiles(
        Iterable<File> inputFiles, File output,
        String[] header) throws IOException {

        try (CSVWriter writer = new CSVWriter(new FileWriter(output), ',')) {
            writer.writeNext(header);

            for (File input : inputFiles) {
                CSVReader reader = new CSVReader(new FileReader(input), ',');
                // skip header
                reader.readNext();
                writer.writeAll(reader.readAll());
                reader.close();
            }
        }
    }

    private static void printUsage() {
        String NL = System.lineSeparator();
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM");
        String Y_M = sdfDate.format(new Date());

        String helpStr =
            // @formatter:off
            // This may be printing on a Windows system, in a default console. Limit the width
            // to 80 columns. Ugly on a wider screen, but much better for narrow ones.
            // ----+----1----+----2----+----3----+----4----+----5----+----6----+----7----+----8
            "TB-Builder.bat runs " + NL +
                "    java -cp acm.jar:lib/* org.literacybridge.acm.tbbuilder.TBBuilder" + NL + NL +
                "Prepares a new Deployment (Content Update) to be loaded onto Talking Books by" + NL +
                "the TB-Loader. There are two steps, the CREATE step and the PUBLISH step." + NL + NL +
                "    TB-Builder.bat CREATE ACM-NAME DEPLOYMENT package1 language1 group1 " + NL +
                "                                                package2 language2 group2 ..." + NL + NL +
                "    TB-Builder.bat PUBLISH ACM-NAME DEPLOYMENT1 DEPLOYMENT2 ..." + NL + NL +
                "Usually, the \"language\" and \"group\" are the same, and, usually, there is" + NL +
                "only one deployment. If you need to use groups or multiple deployments, please" + NL +
                "contact Literacy Bridge Seattle for detailed instructions." + NL + NL +
                "For example, assume your ACM is named ACM-DEMO, and that you have two Packages," + NL +
                "DEMO-" + Y_M + "-EPO in the Esperanto language, and DEMO-" + Y_M + "-TLH in the Klingon" + NL +
                "language, and that you wish to create a Deployment named DEMO-" + Y_M + "." + NL +
                "The steps to follow would be like this:" + NL + NL +
                "1) Use a CREATE step to put the two Packages together into a Deployment." + NL +
                "     TB-Builder.bat CREATE ACM-DEMO DEMO-" + Y_M + " DEMO-" + Y_M + "-EPO EPO EPO " + NL +
                "        DEMO-" + Y_M + "-TLH TLH TLH" + NL +
                "   (You can now, optionally, but only on the same machine, use TB-Loader to put" + NL +
                "   this deployment onto a Talking Book, for testing purposes.)" + NL + NL +
                "2) Use a PUBLISH step to make the Deployment available to everyone." + NL +
                "     TB-Builder.bat PUBLISH ACM-DEMO DEMO-" + Y_M + NL + NL;
            // @formatter:on
        System.out.print(helpStr);
    }

}
