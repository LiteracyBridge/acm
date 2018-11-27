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
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.literacybridge.core.tbloader.TBLoaderConstants.RECIPIENTID_PROPERTY;

public class TBBuilder {
    private static final String[] CSV_COLUMNS_CONTENT_IN_PACKAGE = { "project",
        "contentpackage", "contentid", "categoryid", "order" };
    private static final String[] CSV_COLUMNS_CATEGORIES_IN_PACKAGE = { "project",
        "contentpackage", "categoryid", "order" };
    private static final String[] CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT = {
        "project", "deployment", "contentpackage", "packagename", "startDate",
        "endDate", "languageCode", "grouplangs", "distribution" };
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

    private File sourceProgramspecDir;
    
    private File sourceTbLoadersDir;
    private File sourceTbOptionsDir;

    private File stagedDeploymentDir;
    private CSVWriter contentInPackageCSVWriter;
    private CSVWriter categoriesInPackageCSVWriter;
    private CSVWriter packagesInDeploymentCSVWriter;
    private File stagingDir;
    private String deployment;
    private List<String> fatalMessages = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();
    private List<String> warningMessages = new ArrayList<>();
    private Set<String> errorCommunities = new HashSet<>();
    private Set<String> errorLanguages = new HashSet<>();
    public String project;

    /**
     * Class to hold the name, language, and groups of a single package.
     */
    private static class PackageInfo {
        final String name;
        final String language;
        final String[] groups;

        PackageInfo(String name, String language, String... groups) {
            this.name = name;
            this.language = language.toLowerCase();
            this.groups = new String[groups.length];
            for (int ix=0; ix<groups.length; ix++) {
                this.groups[ix] = groups[ix].toLowerCase();
            }
        }
    }


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
            List<String> argsList = Arrays.asList(args);
            List<String> deploymentList = argsList.subList(2, argsList.size());
            tbb.publish(deploymentList);
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
        sourceProgramspecDir = ACMConfiguration.getInstance().getProgramSpecDirFor(sharedACM);
        project = ACMConfiguration.cannonicalProjectName(sharedACM);
        // ~/LiteracyBridge/TB-Loaders
        File localTbLoadersDir = new File(
            ACMConfiguration.getInstance().getApplicationHomeDirectory(),
            Constants.TBLoadersHomeDir);
        // Like ~/LiteracyBridge/TB-Loaders/UWR
        stagingDir = new File(localTbLoadersDir, project);
    }

    /**
     * From the main(String[] args) parameter, build a list of packages, their language, and groups.
     * @param args from main(), as described with doCreate()
     * @return List of PackageInfo objects as specified in the args.
     */
    private List<PackageInfo> getePackageInfoForCreate(String[] args) {
        List<PackageInfo> packages = new ArrayList<>();
        if (args.length == 5) {
            // one package with only default group
            packages.add(new PackageInfo(args[3], args[4], TBLoaderConstants.DEFAULT_GROUP_LABEL));
        } else {
            // one or more packages with specified group
            int argIx = 3;
            while (argIx < args.length) {
                // First package is also the default, for communities with no other group defined.
                if (argIx == 3) {
                    packages.add(new PackageInfo(args[argIx], args[argIx + 1], TBLoaderConstants.DEFAULT_GROUP_LABEL,
                        args[argIx + 2]));
                } else {
                    packages.add(new PackageInfo(args[argIx], args[argIx + 1], args[argIx + 2]));
                }
                argIx += 3;
            }
        }
        return packages;
    }

    /**
     * Gathers the files that will make a deployment, into the directory
     * ~/LiteracyBridge/TB-Loaders/PROJ/.
     * Existing content in the /content/DEPLOYMENT and /metadata/DEPLOYMENT
     * subdirectories of that directory will be deleted.
     *
     * @param args from main(), as follows:
     *             args[0] operation, CREATE (not PUBLISH)
     *             args[1] the ACM upon which to operate
     *             args[2] the Deployment name
     *             args[3] the first package name
     *             args[4] the first package language
     *             args[5] (optional) the first package group (DEFAULT is always assigned to first pkg)
     *             args[6..8] (optional) second package name, language, group
     *             args[9..11] (optional) third package name, language, group
     *             . . . optional triples of (package name, language, group)
     * @throws Exception if one of the packages encounters an IO error.
     */
    private void doCreate(String[] args) throws Exception {
        List<PackageInfo> packages = getePackageInfoForCreate(args);
        String acmName = args[1];
        String deployment = args[2];
        validateDeployment(acmName, deployment, packages);

        createDeployment(args[2]);

        for (PackageInfo pi : packages) {
            addImage(pi);
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
    private void createDeployment(String deployment) throws Exception {
        this.deployment = deployment.toUpperCase();
        stagedDeploymentDir = new File(stagingDir, "content" + File.separator + this.deployment);
        File stagedMetadataDir = new File(stagingDir, "metadata" + File.separator + this.deployment);
        File stagedProgramspecDir = new File(stagedDeploymentDir, Constants.ProgramSpecDir);

        // use LB Home Dir to create folder, then zip to Dropbox and delete the
        // folder
        IOUtils.deleteRecursive(stagedDeploymentDir);
        stagedDeploymentDir.mkdirs();
        IOUtils.deleteRecursive(stagedMetadataDir);
        stagedMetadataDir.mkdirs();
        IOUtils.deleteRecursive(stagedProgramspecDir);
        stagedProgramspecDir.mkdirs();

        contentInPackageCSVWriter = new CSVWriter(
            new FileWriter(
                new File(stagedMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME)),
            ',');
        categoriesInPackageCSVWriter = new CSVWriter(
            new FileWriter(
                new File(stagedMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)),
            ',');
        packagesInDeploymentCSVWriter = new CSVWriter(
            new FileWriter(
                new File(stagedMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)),
            ',');

        // write column headers
        contentInPackageCSVWriter.writeNext(CSV_COLUMNS_CONTENT_IN_PACKAGE);
        categoriesInPackageCSVWriter.writeNext(CSV_COLUMNS_CATEGORIES_IN_PACKAGE);
        packagesInDeploymentCSVWriter.writeNext(CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File sourceFirmware = latestFirmwareImage();
        File stagedBasicDir = new File(stagedDeploymentDir, "basic");
        FileUtils.copyFileToDirectory(sourceFirmware, stagedBasicDir);

        File sourceCommunitiesDir = new File(sourceTbLoadersDir, "communities");
        File stagedCommunitiesDir = new File(stagedDeploymentDir, "communities");
        FileUtils.copyDirectory(sourceCommunitiesDir, stagedCommunitiesDir);

        FileUtils.copyDirectory(sourceProgramspecDir, stagedProgramspecDir);
        
        deleteRevFiles(stagingDir);
        String revision;
        revision = TBLoaderConstants.UNPUBLISHED_REV + "_"
            + TBLoaderUtils.getDateTime().substring(8, 17);
        File newRev = new File(stagingDir, revision + ".rev");
        newRev.createNewFile();

        System.out.printf("%nDone with deployment of basic/community content.%n");
    }

    private File latestFirmwareImage() {
        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File latestFirmware = null;
        File[] firmwareVersions = new File(sourceTbOptionsDir, "firmware")
            .listFiles();
        for (File f : firmwareVersions) {
            if (latestFirmware == null) {
                latestFirmware = f;
            } else if (latestFirmware.getName().compareToIgnoreCase(f.getName()) < 0) {
                latestFirmware = f;
            }
        }
        return latestFirmware;
    }

    /**
     * Adds a Content Package to a Deployment. Copies the files to the staging directory.
     *
     * @param pi Information about the package: name, language, groups.
     * @throws Exception if there is an error creating or reading a file.
     */
    private void addImage(PackageInfo pi) throws Exception {
        Set<String> exportedCategories = null;
        boolean hasIntro = false;
        int groupCount = pi.groups.length;
        System.out.printf("%n%nExporting package %s%n", pi.name);

        File sourcePackageDir = new File(sourceTbLoadersDir, "packages/" + pi.name);
        File sourceMessagesDir = new File(sourcePackageDir, "messages");
        File sourceListsDir = new File(sourceMessagesDir,
            "lists/" + TBBuilder.firstMessageListName);
        File stagedImagesDir = new File(stagedDeploymentDir, "images");
        File stagedImageDir = new File(stagedImagesDir, pi.name);

        IOUtils.deleteRecursive(stagedImageDir);
        stagedImageDir.mkdirs();

        if (!sourceListsDir.exists() || !sourceListsDir.isDirectory()) {
            System.err.printf("Directory not found: %s%n", sourceListsDir);
            System.exit(1);
        } else if (sourceListsDir.listFiles().length == 0) {
            System.err.printf("No lists found in %s%n", sourceListsDir);
            System.exit(1);
        }

        validatePackageForLanguage(pi);

        File stagedMessagesDir = new File(stagedImageDir, "messages");
        FileUtils.copyDirectory(sourceMessagesDir, stagedMessagesDir);

        File stagedAudioDir = new File(stagedMessagesDir, "audio");
        File stagedListsDir = new File(stagedMessagesDir,
            "lists/" + TBBuilder.firstMessageListName);
        File stagedLanguagesDir = new File(stagedImageDir, "languages");
        File stagedLanguageDir = new File(stagedLanguagesDir, pi.language);
        File stagedWelcomeMessageDir = stagedLanguageDir;

        if (!stagedAudioDir.exists() && !stagedAudioDir.mkdirs()) {
            System.err.printf("Unable to create directory: %s%n", stagedAudioDir);
            System.exit(1);
        }

        if (!stagedLanguageDir.exists() && !stagedLanguageDir.mkdirs()) {
            System.err.printf("Unable to create directory: %s%n", stagedLanguageDir);
            System.exit(1);
        }

        if (!stagedWelcomeMessageDir.exists() && !stagedWelcomeMessageDir.mkdirs()) {
            System.err.printf("Unable to create directory: %s%n", stagedWelcomeMessageDir);
            System.exit(1);
        }

        File[] lists = stagedListsDir.listFiles();
        for (File list : lists) {
            if (list.getName().equals(TBBuilder.IntroMessageListFilename)) {
                exportList(pi.name, list, stagedWelcomeMessageDir, "intro.a18", false);
                list.delete();
                hasIntro = true;
            } else if (list.getName().equalsIgnoreCase("_activeLists.txt")) {
                exportedCategories = exportCategoriesInPackage(pi.name, list);
            } else {
                exportList(pi.name, list, stagedAudioDir, true);
            }
        }

        File sourceBasic = new File(sourceTbOptionsDir, "basic");
        FileUtils.copyDirectory(sourceBasic, stagedImageDir);

        File sourceConfigFile = new File(sourceTbOptionsDir, "config_files/config.txt");
        File stagedSystemDir = new File(stagedImageDir, "system");
        FileUtils.copyFileToDirectory(sourceConfigFile, stagedSystemDir);

        File sourceLanguage = new File(sourceTbOptionsDir, "languages/" + pi.language);
        FileUtils.copyDirectory(sourceLanguage, stagedLanguageDir);

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
        FileUtils.copyFile(sourceControlFile, new File(stagedLanguageDir, "control.txt"));

        // create profile.txt
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
        System.out.println(
            String.format("Done with adding image for %s and %s.", pi.name, pi.language));
    }

    /**
     * Validates that the packages and communities pass certain sanity tests. (See individual
     * verifications for details.)
     *
     * @throws IOException if a file can't be read.
     */
    private void validateDeployment(String acmName, String deployment, List<PackageInfo> packages) throws IOException {
        boolean strictNaming = ACMConfiguration.getInstance().getCurrentDB().strictDeploymentNaming();
        if (strictNaming) {
            // Validate that the deployment is listed in the deployments.csv file.
            File deploymentsList = new File(sourceProgramspecDir, "deployments.csv");
            if (deploymentsList.exists()) {
                boolean found = false;
                FileReader fileReader = new FileReader(deploymentsList);
                CSVReader csvReader = new CSVReader(fileReader);

                List<String[]> lines = csvReader.readAll();
                String nextDeploymentName = null;
                String prevDeploymentName = null;
                int count = 0;
                int deploymentIx = -1;
                int startDateIx = -1;
                for (String[] line : lines) {
                    if (count++ == 0) {
                        for (int ix = 0; ix < line.length; ix++) {
                            if (line[ix].equalsIgnoreCase("deployment")) {
                                deploymentIx = ix;
                            } else if (line[ix].equalsIgnoreCase("startdate")) {
                                startDateIx = ix;
                            }
                        }
                    } else {
                        // Look for the given deployment in the deployments.csv. Also look for a
                        // likely deployment name, either the next deployment after today, or the
                        // last deployment in the list. This won't be the right one when re-building
                        // the current deployment, but the prompt should give a good hint as to what
                        // the name should be.
                        if (line[deploymentIx].equalsIgnoreCase(deployment)) {
                            found = true;
                            break;
                        } else if (StringUtils.isEmpty(nextDeploymentName)){
                            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
                            try {
                                Date startDate = df1.parse(line[startDateIx]);
                                if (startDate.after(new Date())) {
                                    nextDeploymentName = line[deploymentIx];
                                } else {
                                    prevDeploymentName = line[deploymentIx];
                                }
                            } catch (ParseException e) {
                                // Not a valid iso8601 string. Ignore it.
                            }
                        }
                    }
                }
                if (!found) {
                    String invalidMessage = String.format( "'%s' is not a valid deployment for ACM '%s'.",
                        deployment, ACMConfiguration.cannonicalProjectName(acmName));
                    if (StringUtils.isNotEmpty(nextDeploymentName) || StringUtils.isNotEmpty(prevDeploymentName)) {
                        String name = StringUtils.defaultIfEmpty(nextDeploymentName, prevDeploymentName);
                        invalidMessage += " (Did you mean '" + name + "'?)";
                    }
                    fatalMessages.add(invalidMessage);
                }
            }
        }

        // Get all of the languages and groups in the Deployment. As we iterate over the
        // packages and their langugages, validate the package/language combinations.
        Set<String> groups = new HashSet<>();
        Set<String> languages = new HashSet<>();

        for (PackageInfo pi : packages) {
            validatePackageForLanguage(pi);
            languages.add(pi.language);
            Collections.addAll(groups, pi.groups);
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
     * @param pi Information about the package: name, language, groups
     *           (TB-Loaders/packages/{name}/messages/lists/1/)
     *           containing the _activeLists.txt and individual playlist .txt
     *           list files.
     * @throws IOException if any files can't be read.
     */
    private void validatePackageForLanguage(PackageInfo pi)
        throws IOException {
        // Get the directory containing the _activeLists.txt file plus the playlist files (like "2-0.txt")
        String listsPath =
            "packages/" + pi.name + "/messages/lists/" + TBBuilder.firstMessageListName;
        File sourceListsDir = new File(sourceTbLoadersDir, listsPath);

        // Get the directory with the system prompt recordings for the language.
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(sourceTbLoadersDir, languagesPath);
        File languageDir = IOUtils.FileIgnoreCase(languagesDir, pi.language);
        File promptsDir = new File(languageDir, "cat");
        String firmwarePath = "TB_Options" + File.separator + "firmware";

        // Read the source _activeLists.txt file.
        File activeList = new File(sourceListsDir, "_activeLists.txt");
        if (!activeList.exists()) {
            fatalMessages.add(
                String.format("File '%s' not found for Package '%s'.", activeList.getName(),
                    pi.name));
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
                                pi.language));
                        errorLanguages.add(pi.language);
                    }
                    if (!p2.exists()) {
                        errorMessages.add(
                            String.format("Missing long category prompt for %s in language %s.", line,
                                pi.language));
                        errorLanguages.add(pi.language);
                    }

                    // Be sure there is a list for the category.
                    if (!line.equals("9-0") && !line.equals("$0-1")) {
                        File pList = new File(sourceListsDir, line + ".txt");
                        if (!pList.exists()) {
                            errorMessages.add(
                                String.format("Missing playlist file '%s.txt', for Package '%s', language '%s'.",
                                    line, pi.name, pi.language));
                            errorLanguages.add(pi.language);
                        }
                    }
                }
                String[] required_messages = foundUserFeedback ? REQUIRED_SYSTEM_MESSAGES_UF : REQUIRED_SYSTEM_MESSAGES_NO_UF;
                for (String prompt : required_messages) {
                    File p1 = new File(languageDir, prompt + ".a18");
                    if (!p1.exists()) {
                        errorMessages.add(
                            String.format("Missing system message for %s in language %s.", prompt,
                                pi.language));
                        errorLanguages.add(pi.language);
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
                        pi.name, pi.language));
                }
            } catch (Exception ex) {
                errorMessages.add(
                    String.format("Exception reading _activeLists.txt for Package '%s' language '%s': %s",
                        pi.name, pi.language, ex.getMessage()));
                errorLanguages.add(pi.language);
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
        Map<String, List<String>> foundRecipientIds = new HashMap<>();
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
                // Validate recipientid, if present, is unique.
                String recipientid = TBLoaderUtils.getRecipientProperty(c, RECIPIENTID_PROPERTY);
                if (recipientid != null) {
                    List<String> recips = foundRecipientIds.get(recipientid);
                    if (recips == null) {
                        recips = new ArrayList<>();
                        foundRecipientIds.put(recipientid, recips);
                    }
                    recips.add(c.getName());
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
        for (Map.Entry<String, List<String>> e : foundRecipientIds.entrySet()) {
            if (e.getValue().size() > 1) {
                String dirs = e.getValue()
                    .stream()
                    .collect(Collectors.joining(", "));
                String msg = String.format("Recipientid %s found in multiple communities: %s",
                    e.getKey(), dirs);
                fatalMessages.add(msg);
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
     * @param deploymentList List of deployments. Effectively, always exactly one.
     * @throws Exception if a file can't be read.
     */
    private void publish(List<String> deploymentList) throws Exception {
        // Make a local copy so we can munge it.
        List<String> deployments = new ArrayList<>(deploymentList).stream().map(String::toUpperCase).collect(Collectors.toList());

        // e.g. 'ACM-UWR/TB-Loaders/published/'
        final File publishBaseDir = new File(sourceTbLoadersDir, "published");
        publishBaseDir.mkdirs();
        String distribution = deployments.get(0); // assumes first deployment is most
        this.deployment = distribution;
        // recent and should be name of
        // distribution
        char revision = getLatestDistributionRevision(publishBaseDir, distribution);
        final String publishDistributionName = distribution + "-" + revision; // e.g.
        // '2015-6-c'
        stagedDeploymentDir = new File(stagingDir, "content" + File.separator + this.deployment);

        // e.g. 'ACM-UWR/TB-Loaders/published/2015-6-c'
        final File publishDistributionDir = new File(publishBaseDir, publishDistributionName);
        publishDistributionDir.mkdirs();

        // Copy the program spec to a directory outside of the .zip file.
        File stagedProgramspecDir = new File(stagedDeploymentDir, Constants.ProgramSpecDir);
        if (stagedProgramspecDir.exists() && stagedProgramspecDir.isDirectory()) {
            File publishedProgramSpecDir = new File(publishDistributionDir, Constants.ProgramSpecDir);
            FileUtils.copyDirectory(stagedProgramspecDir, publishedProgramSpecDir);
        }

        String zipSuffix = distribution + "-" + revision + ".zip";
        File localContent = new File(stagingDir, "content");
        ZipUnzip.zip(localContent,
            new File(publishDistributionDir, "content-" + zipSuffix), true,
            deployments.toArray(new String[deployments.size()]));

        // merge csv files
        File stagedMetadata = new File(stagingDir, "metadata");
        //List<String> deploymentsList = Arrays.asList(deployments);
        File[] metadataDirs = stagedMetadata.listFiles(f -> f.isDirectory()
            && deployments.contains(f.getName()));
        final List<File> inputContentCSVFiles = new LinkedList<File>();
        final List<File> inputCategoriesCSVFiles = new LinkedList<File>();
        final List<File> inputPackagesCSVFiles = new LinkedList<File>();
        for (File metadataDir : metadataDirs) {
            metadataDir.listFiles(new FileFilter() {
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
        File publishedMetadataDir = new File(publishDistributionDir, "metadata");
        if (!publishedMetadataDir.exists())
            publishedMetadataDir.mkdir();
        File mergedCSVFile = new File(publishedMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME);
        mergeCSVFiles(inputContentCSVFiles, mergedCSVFile, CSV_COLUMNS_CONTENT_IN_PACKAGE);

        mergedCSVFile = new File(publishedMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME);
        mergeCSVFiles(inputCategoriesCSVFiles, mergedCSVFile, CSV_COLUMNS_CATEGORIES_IN_PACKAGE);

        mergedCSVFile = new File(publishedMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME);
        mergeCSVFiles(inputPackagesCSVFiles, mergedCSVFile, CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        new DBExporter(ACM_PREFIX + project, publishedMetadataDir).export();

        deleteRevFiles(stagingDir);
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
        File exportDirectory, boolean writeToCSV) throws Exception {
        exportList(contentPackage, list, exportDirectory, null, writeToCSV);
    }

    private void exportPackagesInDeployment(
        String contentPackage,
        String languageCode, String[] groups)
    {
        String groupsconcat = StringUtils.join(groups, ',');
        String[] csvColumns = new String[9];
        csvColumns[0] = project.toUpperCase();
        csvColumns[1] = deployment.toUpperCase();
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
        File exportDirectory, String filename, boolean writeToCSV)
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
                System.out.println(String.format("    Exporting audioitem %s to %s", uuid, exportDirectory));
                if (filename == null) {
                    repository.exportA18WithMetadata(audioItem, exportDirectory);
                } else {
                    repository.exportA18WithMetadataToFile(audioItem,
                        new File(exportDirectory, filename));
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
