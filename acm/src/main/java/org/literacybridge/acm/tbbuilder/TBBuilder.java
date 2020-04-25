package org.literacybridge.acm.tbbuilder;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_REVISION_PATTERN;
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

    public final static String [] REQUIRED_SYSTEM_MESSAGES_UF = {
        "0", "1", "2", "3", "4", "5", "6", "9", "10", "11",
        "16", "17", "19", "20", "21", "22", "23", "24", "25", "26", "28", "29",
        "33", "37", "38", "41", "53", "54", "61", "62", "63", "65", "80"
    };
    public final static String [] REQUIRED_SYSTEM_MESSAGES_NO_UF = {
        "0", "1", "2", "3", "4", "5", "6", "9", "10", "11",
        "16", "17", "19", "20", "21", "22", "23", "24", "25", "26", "28", "29",
        "33", "37", "38", "41", "53", "54", "61", "62", "63", "65", "80"
    };
    public final static String MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE = "r1220.img";

    public static String firstMessageListName = "1";
    private static String IntroMessageListFilename = Constants.CATEGORY_INTRO_MESSAGE + ".txt";
    public static final String ACM_PREFIX = "ACM-";
    private final static int MAX_DEPLOYMENTS = 5;

    private static final String WORK_IN_PROGRESS = "work_in_progress.properties";

    // Begin instance data

    private File sourceProgramspecDir;

    private File sourceTbLoadersDir;
    private File sourceTbOptionsDir;

    private File stagedDeploymentDir;
    private File stagedProgramspecDir;
    private CSVWriter contentInPackageCSVWriter;
    private CSVWriter categoriesInPackageCSVWriter;
    private CSVWriter packagesInDeploymentCSVWriter;
    private File stagingDir;
    private String deploymentName;
    private List<String> fatalMessages = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();
    private List<String> warningMessages = new ArrayList<>();
    private Set<String> errorCommunities = new HashSet<>();
    private Set<String> errorLanguages = new HashSet<>();
    private String project;

    private Consumer<String> statusWriter;
    private String revision;

    private void reportStatus(String format, Object... args) {
        statusWriter.accept(String.format(format, args));
    }

    /**
     * Expose to API callers the "revision" that was calculated.
     * @return the version of the Deployment, 'a', 'b', 'aa', etc.
     */
    public String getRevision() {
        return revision;
    }

    public File getStagedProgramspecDir() {
        return stagedProgramspecDir;
    }

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

    public static class TBBuilderException extends Exception {
        TBBuilderException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) throws Exception {
        File lbDir = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);
        File logDir = new File(lbDir, "logs");
        new LogHelper().inDirectory(logDir).withName("TBBuilder.log").absolute().initialize();

        System.out.println(String.format("TB-Builder version %s", Constants.ACM_VERSION));
        if (args.length < 3) {
            printUsage();
        }
        String command = args[0].toUpperCase();
        String project = args[1];
        String deploymentName = args[2].toUpperCase();

        TBBuilder tbb = new TBBuilder(project, deploymentName, System.out::print);

        try {
            if (command.equals("CREATE")) {
                // arguments NOT including the deployment name.
                List<String> argsList = Arrays.asList(args).subList(3, args.length);
                tbb.validateAndCreate(argsList);
            } else if (command.equals("PUBLISH")) {
                // arguments INCLUDING the deployment name.
                int deploymentCount = Math.min(args.length - 2, MAX_DEPLOYMENTS);
                List<String> deploymentList = Arrays.asList(args).subList(2, deploymentCount+2);
                tbb.publishDeployment(deploymentList);
            } else {
                System.out.printf("Unknown operation '%s'. Operations are CREATE and PUBLISH\n",
                    args[0]);
                printUsage();
            }
        } catch (Exception e) {
            System.err.print(e.toString());
        }
    }

    /**
     * Construct the TBBuilder. Opens the ACM that's being operated upon.
     *
     * @param sharedACM the ACM name.
     * @throws Exception if the database can't be initialized.
     */
    public TBBuilder(String sharedACM, String deploymentName, Consumer<String> statusWriter) throws Exception {
        sharedACM = ACMConfiguration.cannonicalAcmDirectoryName(sharedACM);
        if (!ACMConfiguration.isInitialized()) {
            CommandLineParams params = new CommandLineParams();
            params.disableUI = true;
            params.sandbox = true;
            params.sharedACM = sharedACM;
            ACMConfiguration.initialize(params);
            ACMConfiguration.getInstance().setCurrentDB(params.sharedACM);
        } else if (!ACMConfiguration.getInstance().getCurrentDB().getSharedACMname().equals(sharedACM)) {
            throw new IllegalArgumentException("Passed ACM Name must equal already-opened ACM name.");
        }

        this.statusWriter = statusWriter;
        this.project = ACMConfiguration.cannonicalProjectName(sharedACM);
        this.deploymentName = deploymentName;
        // Like ~/Dropbox/ACM-UWR/TB-Loaders
        sourceTbLoadersDir = ACMConfiguration.getInstance().getTbLoaderDirFor(sharedACM);
        sourceTbOptionsDir = new File(sourceTbLoadersDir, "TB_Options");
        sourceProgramspecDir = ACMConfiguration.getInstance().getProgramSpecDirFor(sharedACM);
        // ~/LiteracyBridge/TB-Loaders
        File localTbLoadersDir = new File(
            ACMConfiguration.getInstance().getApplicationHomeDirectory(),
            Constants.TBLoadersHomeDir);
        // Like ~/LiteracyBridge/TB-Loaders/UWR
        stagingDir = new File(localTbLoadersDir, project);
        stagedDeploymentDir = new File(stagingDir, "content" + File.separator + this.deploymentName);
        stagedProgramspecDir = new File(stagedDeploymentDir, Constants.ProgramSpecDir);
    }

    /**
     * From the main(String[] args) parameter, build a list of packages, their language, and groups.
     *
     * @param args from main(), after the command, the project/acm, and the deployment name.
     * @return List of PackageInfo objects as specified in the args.
     */
    private List<PackageInfo> getPackageInfoForCreate(List<String> args) {
        if (args.size() < 2 || (args.size() > 2 && ((args.size() % 3) != 0))) {
            System.out.print("Unexpected number of arguments for CREATE.\n");
            printUsage();
        }
        
        List<PackageInfo> packages = new ArrayList<>();
        if (args.size() == 2) {
            // one package with only default group
            packages.add(new PackageInfo(args.get(0), args.get(1), TBLoaderConstants.DEFAULT_GROUP_LABEL));
        } else {
            // one or more packages with specified group
            int argIx = 0;
            while (argIx < args.size()) {
                // First package is also the default, for communities with no other group defined.
                if (argIx == 0) {
                    packages.add(new PackageInfo(args.get(argIx), args.get(argIx + 1), TBLoaderConstants.DEFAULT_GROUP_LABEL,
                        args.get(argIx + 2)));
                } else {
                    packages.add(new PackageInfo(args.get(argIx), args.get(argIx + 1), args.get(argIx + 2)));
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
     * @param args args[0] the first package name
     *             args[1] the first package language
     *             args[2] (optional) the first package group (DEFAULT is always assigned to first pkg)
     *             args[3..5] (optional) second package name, language, group
     *             args[6..8] (optional) third package name, language, group
     *             . . . optional triples of (package name, language, group)
     * @throws Exception if one of the packages encounters an IO error.
     */
    private void validateAndCreate(List<String> args) throws Exception {
        List<PackageInfo> packages = getPackageInfoForCreate(args);
        validateDeployment(packages);
        createDeploymentWithPackages(packages);
    }

    /**
     * Creates a deployment from the packages described by args.
     * @param args args[0] the first package name
     *             args[1] the first package language
     *             args[2] (optional) the first package group (DEFAULT is always assigned to first pkg)
     *             args[3..5] (optional) second package name, language, group
     *             args[6..8] (optional) third package name, language, group
     *             . . . optional triples of (package name, language, group)
     * @throws Exception if the deployment can't be created.
     */
    public void createDeployment(List<String> args) throws Exception {
        List<PackageInfo> packages = getPackageInfoForCreate(args);
        createDeploymentWithPackages(packages);
    }

    private void createDeploymentWithPackages(List<PackageInfo> packages) throws Exception {
        createDeployment();

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
     * @throws Exception if there is an IO error.
     */
    private void createDeployment() throws Exception {
        File stagedMetadataDir = new File(stagingDir, "metadata" + File.separator + this.deploymentName);
        DateFormat ISO8601time = new SimpleDateFormat("HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
        ISO8601time.setTimeZone(TBLoaderConstants.UTC);
        String timeStr = ISO8601time.format(new Date());
        String revFileName = String.format(TBLoaderConstants.UNPUBLISHED_REVISION_FORMAT, timeStr, deploymentName);
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

        if (sourceProgramspecDir != null) {
            FileUtils.copyDirectory(sourceProgramspecDir, stagedProgramspecDir);
        }
        
        deleteRevFiles(stagingDir);
        // Leave a marker to indicate that there exists an unpublished deployment here.
        File newRev = new File(stagingDir, revFileName);
        newRev.createNewFile();
        // Put a marker inside the unpublished content, so that we will be able to tell which of
        // possibly several is the unpublished one.
        deleteRevFiles(stagedDeploymentDir);
        newRev = new File(stagedDeploymentDir, revFileName);
        newRev.createNewFile();

        reportStatus("%nDone with deployment of basic/community content.%n");
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
        reportStatus("%n%nExporting package %s%n", pi.name);

        File sourcePackageDir = new File(sourceTbLoadersDir, "packages"+File.separator + pi.name);
        File sourceMessagesDir = new File(sourcePackageDir, "messages");
        File sourceExtraPromptsDir = new File(sourcePackageDir, "prompts"+File.separator+pi.language);
        File sourceListsDir = new File(sourceMessagesDir,
            "lists/" + TBBuilder.firstMessageListName);
        File stagedImagesDir = new File(stagedDeploymentDir, "images");
        File stagedImageDir = new File(stagedImagesDir, pi.name);

        IOUtils.deleteRecursive(stagedImageDir);
        stagedImageDir.mkdirs();

        if (!sourceListsDir.exists() || !sourceListsDir.isDirectory()) {
            throw(new TBBuilderException(String.format("Directory not found: %s%n", sourceListsDir)));
        } else if (sourceListsDir.listFiles().length == 0) {
            throw(new TBBuilderException(String.format("No lists found in %s%n", sourceListsDir)));
        }

        File stagedMessagesDir = new File(stagedImageDir, "messages");
        FileUtils.copyDirectory(sourceMessagesDir, stagedMessagesDir);

        File stagedAudioDir = new File(stagedMessagesDir, "audio");
        File stagedListsDir = new File(stagedMessagesDir,
            "lists/" + TBBuilder.firstMessageListName);
        File stagedLanguagesDir = new File(stagedImageDir, "languages");
        File stagedLanguageDir = new File(stagedLanguagesDir, pi.language);
        File stagedWelcomeMessageDir = stagedLanguageDir;

        if (!stagedAudioDir.exists() && !stagedAudioDir.mkdirs()) {
            throw(new TBBuilderException(String.format("Unable to create directory: %s%n", stagedAudioDir)));
        }

        if (!stagedLanguageDir.exists() && !stagedLanguageDir.mkdirs()) {
            throw(new TBBuilderException(String.format("Unable to create directory: %s%n", stagedLanguageDir)));
        }

        if (!stagedWelcomeMessageDir.exists() && !stagedWelcomeMessageDir.mkdirs()) {
            throw(new TBBuilderException(String.format("Unable to create directory: %s%n", stagedWelcomeMessageDir)));
        }

        File[] listFiles = stagedListsDir.listFiles();
        for (File listFile : listFiles) {
            // We found a "0-5.txt" file (note that there's no entry in the _activeLists.txt file)
            // Export the ids listed in the file as languages/intro.txt (last one wins), and
            // delete the file. Remember that the file existed; we'll use that to select a
            // control.txt file that plays the intro.a18 at startup.
            if (listFile.getName().equals(TBBuilder.IntroMessageListFilename)) {
                exportContentForList(pi.name, listFile, stagedWelcomeMessageDir, "intro.a18", false);
                listFile.delete();
                hasIntro = true;
            } else if (listFile.getName().equalsIgnoreCase("_activeLists.txt")) {
                exportedCategories = exportCategoriesInPackage(pi.name, listFile);
            } else {
                exportContentForList(pi.name, listFile, stagedAudioDir, true);
            }
        }

        File sourceBasic = new File(sourceTbOptionsDir, "basic");
        FileUtils.copyDirectory(sourceBasic, stagedImageDir);

        File sourceConfigFile = new File(sourceTbOptionsDir, "config_files/config.txt");
        File stagedSystemDir = new File(stagedImageDir, "system");
        FileUtils.copyFileToDirectory(sourceConfigFile, stagedSystemDir);

        File sourceLanguage = new File(sourceTbOptionsDir, "languages/" + pi.language);
        FileUtils.copyDirectory(sourceLanguage, stagedLanguageDir);

        // If the package provides some additional audio prompts, copy them to the staging area.
        if (sourceExtraPromptsDir.exists() && sourceExtraPromptsDir.isDirectory()) {
            FileUtils.copyDirectory(sourceExtraPromptsDir, stagedLanguageDir);
        }

        // If there is no category "9-0" in the _activeLists.txt file, then the user feedback
        // should not be playable. In that case, use the "_nofb" versions of control.txt.
        // Those have a "UFH", User Feedback Hidden, in the control file, which prevents the
        // Talking Book from *adding* a "9-0" to the _activeLists.txt file when user feedback
        // is recorded. If the 9-0 is already there, then the users can already hear other
        // users' feedback.
        boolean hasNoUf = !exportedCategories.contains(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
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
        reportStatus(
            String.format("Done with adding image for %s and %s.%n", pi.name, pi.language));
    }

    /**
     * Validates that the packages and communities pass certain sanity tests. (See individual
     * verifications for details.)
     *
     * @throws IOException if a file can't be read.
     */
    private void validateDeployment(List<PackageInfo> packages) throws IOException {
        boolean strictNaming = ACMConfiguration.getInstance().getCurrentDB().isStrictDeploymentNaming();
        if (strictNaming) {
            // Validate that the deployment is listed in the deployments.csv file.
            File deploymentsList = new File(sourceProgramspecDir, "deployments.csv");
            if (deploymentsList.exists()) {
                boolean found = false;
                FileReader fileReader = new FileReader(deploymentsList);
                CSVReader csvReader = new CSVReader(fileReader);


                String nextDeploymentName = null;
                String prevDeploymentName = null;
                int deploymentIx = -1;
                int deploymentNumberIx = -1;
                int startDateIx = -1;
                String[] line = csvReader.readNext();
                for (int ix = 0; ix < line.length; ix++) {
                    if (line[ix].equalsIgnoreCase("deployment")) {
                        deploymentIx = ix;
                    } else if (line[ix].equalsIgnoreCase("deploymentnumber")) {
                        deploymentNumberIx = ix;
                    } else if (line[ix].equalsIgnoreCase("startdate")) {
                        startDateIx = ix;
                    }
                }

                while ((line = csvReader.readNext()) != null) {
                    // Look for the given deployment in the deployments.csv. Also look for a
                    // likely deployment name, either the next deployment after today, or the
                    // last deployment in the list. This won't be the right one when re-building
                    // the current deployment, but the prompt should give a good hint as to what
                    // the name should be.


                    if (deploymentIx < 0) {
                        if (deploymentNumberIx >= 0 && isAcceptableNameFor(deploymentName,
                            project,
                            line[deploymentNumberIx])) {
                            found = true;
                            break;
                        }
                    } else {
                        if (line[deploymentIx].equalsIgnoreCase(deploymentName)) {
                            found = true;
                            break;
                        } else if (StringUtils.isEmpty(nextDeploymentName)) {
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
                        deploymentName, ACMConfiguration.cannonicalProjectName(project));
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

    static private Pattern deplPattern = Pattern.compile("(.*)-(\\d+)-(\\d+)");
    private boolean isAcceptableNameFor(String deployment, String acmName, String deploymentNumberStr) {
        int deploymentNum = Integer.parseInt(deploymentNumberStr);
        Calendar rightNow = Calendar.getInstance();
        int year = rightNow.get(Calendar.YEAR);
        int shortYear = year % 100;
        Matcher matcher = deplPattern.matcher(deployment);
        if (matcher.matches() && matcher.groupCount()==3) {
            // Our deployment?
            if (Integer.parseInt(matcher.group(3)) != deploymentNum) return false;
            // Our ACM?
            if (!ACMConfiguration.cannonicalProjectName(acmName).equalsIgnoreCase(matcher.group(1))) return false;
            // This year? Last year? Next year?
            int deplYear = Integer.parseInt(matcher.group(2));
            return Math.abs(deplYear - year) <= 1 || Math.abs(deplYear - shortYear) <= 1;
        }
        return false;
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
     */
    private void validatePackageForLanguage(PackageInfo pi) {
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

                    if (line.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK)) {
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

                    // Be sure there is a .txt list file for the category. We don't check for
                    // uncategorized feedback because it will be created on demand, and we
                    // don't check for the TB long description because it is provided in
                    // the languages directory.
                    if (!line.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK) && !line.equals(Constants.CATEGORY_TUTORIAL)) {
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
                    File[] langs = languagesDir.listFiles(File::isDirectory);
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
                String dirs = String.join(", ", e.getValue());
                String msg = String.format("Recipientid %s found in multiple communities: %s",
                    e.getKey(), dirs);
                fatalMessages.add(msg);
            }
        }
    }

    /**
     * Given a TB-Loader "published" directory and a Deployment name, find the next revision
     * for the Deployment, and create a .rev file with that revision. Return the revision.
     * @param publishTbLoadersDir The directory in which the deployments are published.
     * @param deployment The Deployment (name) for which we want the next revision suffix.
     * @return the revision suffix as a String. Like "a", "b"... "aa"... "aaaaba", etc
     * @throws Exception if the new .rev file can't be created.
     */
    static String getNextDeploymentRevision(File publishTbLoadersDir, final String deployment) throws Exception {
        String revision = "a"; // If we don't find anything higher, start with 'a'.

        String highestRevision = "";
        // Find all the revisions of the given deployment.
        String[] fileNames = publishTbLoadersDir.list((dir, name) ->
            name.toLowerCase().startsWith(deployment.toLowerCase()));
        if (fileNames != null && fileNames.length > 0) {
            for (String fileName : fileNames) {
                // Extract just the revision string.
                String fileRevision = "";
                Matcher deplMatcher = DEPLOYMENT_REVISION_PATTERN.matcher(fileName);
                if (deplMatcher.matches()) {
                    fileRevision = deplMatcher.group(2).toLowerCase();
                }
                // A longer name is always greater. When the lengths are the same, then we need
                // to compare the strings.
                if (fileRevision.length() == highestRevision.length()) {
                    if (fileRevision.compareTo(highestRevision) > 0) {
                        highestRevision = fileRevision;
                    }
                } else if (fileRevision.length() > highestRevision.length()) {
                    highestRevision = fileRevision;
                }
            }
            revision = incrementRevision(highestRevision);
        }

        // Delete *.rev, then create our deployment-revision.rev marker file.
        deleteRevFiles(publishTbLoadersDir);

        File newRev = new File(publishTbLoadersDir, deployment + "-" + revision + ".rev");
        newRev.createNewFile();
        return revision;
    }

    /**
     * Given a revision string, like "a", or "zz", create the next higher value, like "b" or "aaa".
     * @param revision to be incremented
     * @return the incremented value
     */
    static String incrementRevision(String revision) {
        if (revision==null || !revision.matches("^[a-z]+$")) {
            throw new IllegalArgumentException("Revision string must match \"^[a-z]+$\".");
        }
        char[] chars = revision.toCharArray();
        // Looking for a digit we can add to.
        boolean looking = true;
        for (int ix=chars.length-1; ix>=0 && looking; ix--) {
            if (++chars[ix] <= 'z') {
                looking = false;
            } else {
                chars[ix] = 'a';
            }
        }
        String result = new String(chars);
        if (looking) {
            // still looking, add another "digit".
            result = "a"+result;
        }
        return result;
    }

    /**
     * Zips up a Deployment, and places it in a Dropbox/{ACM-NAME}/TB-Loaders/published/{Deployment}-{counter}
     * directory. Creates a marker file named {Deployment}-{counter}.rev
     *
     * @param deploymentList List of deployments. Effectively, always exactly one.
     * @throws Exception if a file can't be read.
     */
    public void publishDeployment(List<String> deploymentList) throws Exception {
        // Make a local copy so we can munge it.
        List<String> deployments = new ArrayList<>(deploymentList).stream().map(String::toUpperCase).collect(Collectors.toList());
        assert deployments.get(0).equals(deploymentName);

        // e.g. 'ACM-UWR/TB-Loaders/published/'
        final File publishBaseDir = new File(sourceTbLoadersDir, "published");
        publishBaseDir.mkdirs();

        revision = getNextDeploymentRevision(publishBaseDir, deploymentName);
        final String publishDistributionName = deploymentName + "-" + revision; // e.g.
        // Remove any .rev file that we had left to mark the deployment as unpublished.
        deleteRevFiles(stagedDeploymentDir);

        // e.g. 'ACM-UWR/TB-Loaders/published/2015-6-c'
        final File publishDistributionDir = new File(publishBaseDir, publishDistributionName);
        publishDistributionDir.mkdirs();

        // Copy the program spec to a directory outside of the .zip file.
        if (stagedProgramspecDir.exists() && stagedProgramspecDir.isDirectory()) {
            File publishedProgramSpecDir = new File(publishDistributionDir, Constants.ProgramSpecDir);
            FileUtils.copyDirectory(stagedProgramspecDir, publishedProgramSpecDir);
        }

        String zipSuffix = deploymentName + "-" + revision + ".zip";
        File localContent = new File(stagingDir, "content");
        ZipUnzip.zip(localContent,
            new File(publishDistributionDir, "content-" + zipSuffix), true,
            deployments.toArray(new String[0]));

        // merge csv files
        File stagedMetadata = new File(stagingDir, "metadata");
        //List<String> deploymentsList = Arrays.asList(deployments);
        File[] metadataDirs = stagedMetadata.listFiles(f -> f.isDirectory()
            && deployments.contains(f.getName()));
        final List<File> inputContentCSVFiles = new LinkedList<>();
        final List<File> inputCategoriesCSVFiles = new LinkedList<>();
        final List<File> inputPackagesCSVFiles = new LinkedList<>();
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

        // Note that what we've just published is the latest on this computer.
        deleteRevFiles(stagingDir);
        File newRev = new File(stagingDir, deploymentName + "-" + revision + ".rev");
        newRev.createNewFile();
    }

    /**
     * Deletes all the *.rev files from the given directory.
     *
     * @param dir from which to remove .rev files.
     */
    private static void deleteRevFiles(File dir) {
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".rev"));
        if (files != null) {
            for (File revisionFile : files) {
                revisionFile.delete();
            }
        }
    }

    private void exportPackagesInDeployment(
        String contentPackage,
        String languageCode, String[] groups)
    {
        String groupsconcat = StringUtils.join(groups, ',');
        String[] csvColumns = new String[9];
        csvColumns[0] = project.toUpperCase();
        csvColumns[1] = deploymentName.toUpperCase();
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
        packagesInDeploymentCSVWriter.writeNext(csvColumns);
    }

    private Set<String> exportCategoriesInPackage(
        String contentPackage,
        File activeLists) throws IOException {
        Set<String> categoriesInPackage = new LinkedHashSet<>();
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

    private void exportContentForList(
        String contentPackage, File list,
        File targetDirectory, boolean writeToCSV) throws Exception {
        exportContentForList(contentPackage, list, targetDirectory, null, writeToCSV);
    }

    private void exportContentForList(
        String contentPackage, File list,
        File targetDirectory, String filename, boolean writeToCSV)
        throws Exception {
        reportStatus("  Exporting list %n" + list);
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
                reportStatus(String.format("    Exporting audioitem %s to %s%n", uuid, targetDirectory));
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
        SimpleDateFormat sdfDate = new SimpleDateFormat("yy-MM");
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
                "    TB-Builder.bat CREATE ACM-NAME deployment-name package1 language1 group1 " + NL +
                "                                                package2 language2 group2 ..." + NL + NL +
                "    TB-Builder.bat PUBLISH ACM-NAME deployment-name" + NL + NL +
                "Usually, the \"language\" and \"group\" are the same. If you need to use groups " + NL +
                "or manage multiple deployments at the same time, please contact Amplio Seattle" + NL +
                "for detailed instructions." + NL + NL +
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
        System.exit(1);
    }

}
