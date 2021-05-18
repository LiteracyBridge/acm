package org.literacybridge.acm.tbbuilder;

import com.opencsv.ICSVWriter;
import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.config.PathsProvider;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.utils.LogHelper;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TBBuilder {
    static final String[] CSV_COLUMNS_CONTENT_IN_PACKAGE = { "project",
        "contentpackage", "contentid", "categoryid", "order" };
    static final String[] CSV_COLUMNS_CATEGORIES_IN_PACKAGE = { "project",
        "contentpackage", "categoryid", "order" };
    static final String[] CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT = {
        "project", "deployment", "contentpackage", "packagename", "startDate",
        "endDate", "languageCode", "grouplangs", "distribution" };
    static final String CONTENT_IN_PACKAGES_CSV_FILE_NAME = "contentinpackages.csv";
    static final String CATEGORIES_IN_PACKAGES_CSV_FILE_NAME = "categoriesinpackages.csv";
    static final String PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME = "packagesindeployment.csv";

    public final static String [] REQUIRED_SYSTEM_MESSAGES = {
        "0", "1", "2", "3", "4", "5", "6", "9", "10", "11",
        "16", "17", "19", "20", "21", "22", "23", "24", "25", "26", "28", "29",
        "33", "37", "38", "41", "53", "54", "61", "62", "63", "65", "80"
    };
    public final static String MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE = "r1220.img";

    public static String firstMessageListName = "1";
    static final String IntroMessageListFilename = Constants.CATEGORY_INTRO_MESSAGE + ".txt";
    public static final String ACM_PREFIX = "ACM-";
    private final static int MAX_DEPLOYMENTS = 5;

    // Begin instance data

    static class BuilderContext {
        boolean isAssistantLaunched = true; // Unless command line tool.
        String project;         // like "TEST"
        String deploymentName;  // like "TEST-20-2"
        String revision;        // like "a", or "b", ...

        public int deploymentNo;
        File sourceProgramspecDir;
        ProgramSpec programSpec;

        File sourceTbLoadersDir;
        File sourceTbOptionsDir;

        File stagingDir;            // ~/LiteracyBridge/TB-Loaders/TEST
        File stagedDeploymentDir;   // {stagingDir}/content/TEST-20-2
        File stagedProgramspecDir;  // {stagedDeploymentDir}/programspec

        boolean deDuplicateAudio = false;

        List<String> fatalMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        List<String> warningMessages = new ArrayList<>();
        Set<String> errorCommunities = new HashSet<>();
        Set<String> errorLanguages = new HashSet<>();

        ICSVWriter contentInPackageCSVWriter;
        ICSVWriter categoriesInPackageCSVWriter;
        ICSVWriter packagesInDeploymentCSVWriter;

        public Consumer<Exception> exceptionLogger;
        void logException(Exception ex) { exceptionLogger.accept(ex); }
        Consumer<String> statusWriter;
        void reportStatus(String format, Object... args) {
            statusWriter.accept(String.format(format, args));
        }
    }

    private final BuilderContext builderContext = new BuilderContext();
    final Utils utils;

    /**
     * Expose to API callers the "revision" that was calculated.
     * @return the version of the Deployment, 'a', 'b', 'aa', etc.
     */
    public String getRevision() {
        return builderContext.revision;
    }
    public List<String> getAcceptableFirmwareVersions(boolean isUserFeedbackHidden) {
        List<String> versions = utils.allFirmwareImageVersions();
        String minVersion = FilenameUtils.getBaseName(MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE);
        if (isUserFeedbackHidden) {
            versions = versions.stream()
                .filter(verStr -> verStr.compareToIgnoreCase(minVersion) >= 0)
                .collect(Collectors.toList());
        }
        return versions;
    }
    public File getStagedProgramspecDir() {
        return builderContext.stagedProgramspecDir;
    }

    /**
     * Class to hold the name, language, and groups of a single package.
     */
    static class PackageInfo {
        final String name;
        final String language;
        final String[] groups;
        AudioItemRepository.AudioFormat audioFormat = AudioItemRepository.AudioFormat.A18;

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
        File lbDir = AmplioHome.getDirectory();
        File logDir = new File(lbDir, "logs");
        new LogHelper().inDirectory(logDir).withName("TBBuilder.log").absolute().initialize();

        System.out.printf("TB-Builder version %s%n", Constants.ACM_VERSION);
        System.out.println("\n================================================================================\n\n"+
                "The command line TB-Builder is deprecated and may not work correctly.\n\n"+
                "All users are strongly encouraged to use the Deployment Assistant in the ACM.\n\n"+
                "Contact Amplio (support@amplio.org) with questions or for assistance.\n\n"+
                "================================================================================\n\n");
        if (args.length < 3) {
            printUsage();
        }
        String command = args[0].toUpperCase();
        String project = args[1];
        String deploymentName = args[2].toUpperCase();

        TBBuilder tbb = new TBBuilder(project, deploymentName, System.out::print, Exception::printStackTrace);
        tbb.builderContext.isAssistantLaunched = false;

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

    public void publishDeployment(List<String> deploymentList) throws Exception {
        new Publish(this, builderContext).publishDeployment(deploymentList);
    }

    /**
     * Construct the TBBuilder. Opens the ACM that's being operated upon.
     *
     * @param sharedACM the ACM name.
     * @throws Exception if the database can't be initialized.
     */
    public TBBuilder(String sharedACM, String deploymentName, Consumer<String> statusWriter, Consumer<Exception> exceptionLogger) throws Exception {
//        sharedACM = ACMConfiguration.cannonicalAcmDirectoryName(sharedACM);
        if (!ACMConfiguration.isInitialized()) {
            CommandLineParams params = new CommandLineParams();
            params.disableUI = true;
            params.sandbox = true;
            params.sharedACM = sharedACM;
            ACMConfiguration.initialize(params);
            ACMConfiguration.getInstance().setCurrentDB(params.sharedACM);
        } else if (!ACMConfiguration.getInstance().getCurrentDB().getProgramHomeDirName().equals(sharedACM)) {
            throw new IllegalArgumentException("Passed ACM Name must equal already-opened ACM name.");
        }

        // Parse the deployment number from the name.
        Pattern deploymentNumberPattern = Pattern.compile("^.*-(\\d+)$");
        Matcher matcher = deploymentNumberPattern.matcher(deploymentName);
        if (matcher.matches()) {
            builderContext.deploymentNo = Integer.parseInt(matcher.group(1));
        }

        builderContext.statusWriter = statusWriter;
        builderContext.exceptionLogger = exceptionLogger;
        builderContext.project = ACMConfiguration.getInstance().getCurrentDB().getProgramName();
        builderContext.deploymentName = deploymentName;

        PathsProvider pathsProvider = ACMConfiguration.getInstance().getPathProvider(sharedACM);
        // Like ~/Dropbox/ACM-UWR/TB-Loaders
        builderContext.sourceTbLoadersDir = pathsProvider.getProgramTbLoadersDir();
        builderContext.sourceTbOptionsDir = new File(builderContext.sourceTbLoadersDir, "TB_Options");
        builderContext.sourceProgramspecDir = pathsProvider.getProgramSpecDir();
        builderContext.programSpec = new ProgramSpec(builderContext.sourceProgramspecDir);
        // ~/LiteracyBridge/TB-Loaders
        File localTbLoadersDir = new File(
            ACMConfiguration.getInstance().getApplicationHomeDirectory(),
            Constants.TBLoadersHomeDir);
        // Like ~/LiteracyBridge/TB-Loaders/UWR
        builderContext.stagingDir = new File(localTbLoadersDir, builderContext.project);
        // Like ~/LiteracyBridge/TB-Loaders/TEST/content/TEST-20-2
        builderContext.stagedDeploymentDir = new File(builderContext.stagingDir, "content" + File.separator + builderContext.deploymentName);
        // Like ~/LiteracyBridge/TB-Loaders/TEST/content/TEST-20-2/programspec
        builderContext.stagedProgramspecDir = new File(builderContext.stagedDeploymentDir, Constants.ProgramSpecDir);

        builderContext.deDuplicateAudio = ACMConfiguration.getInstance().getCurrentDB().isDeDuplicateAudio();

        utils = new Utils(this, builderContext);
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
        new Validator(this, builderContext).validateDeployment(packages);
        new Create(this, builderContext).createDeploymentWithPackages(packages);
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
        new Create(this, builderContext).createDeploymentWithPackages(packages);
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
