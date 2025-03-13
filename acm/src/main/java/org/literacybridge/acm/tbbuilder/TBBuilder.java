package org.literacybridge.acm.tbbuilder;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.config.PathsProvider;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.acm.gui.assistants.util.AcmContent;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.DeplomentPlatform;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TBBuilder {
    static final String[] CSV_COLUMNS_CONTENT_IN_PACKAGE = {"project",
            "contentpackage", "contentid", "categoryid", "order"};
    static final String[] CSV_COLUMNS_CATEGORIES_IN_PACKAGE = {"project",
            "contentpackage", "categoryid", "order"};
    static final String[] CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT = {
            "project", "deployment", "contentpackage", "packagename", "startDate",
            "endDate", "languageCode", "grouplangs", "distribution"};
    static final String CONTENT_IN_PACKAGES_CSV_FILE_NAME = "contentinpackages.csv";
    static final String CATEGORIES_IN_PACKAGES_CSV_FILE_NAME = "categoriesinpackages.csv";
    static final String PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME = "packagesindeployment.csv";

    // Note that "0" and "7" are required, but are provided as resources in the TB-Loader.
    public final static List<String> REQUIRED_SYSTEM_MESSAGES = Arrays.asList(
            "1", "2", "3", "4", "5", "6", "9", "10", "11",
            "16", "17", "19", "20", "21", "22", "23", "24", "25", "26", "28", "29",
            "33", "37", "38", "41", "53", "54", "61", "62", "63", "65", "80"
    );
    public final static List<String> TUTORIAL_SYSTEM_MESSAGES = Arrays.asList(
            "16", "17", "19", "20", "21", "22", "23", "24", "25", "26", "28", "54"
    );

    public static final int MAX_PACKAGES_TBV1 = 2;  // limitation of the firmware.
    public static final int MAX_PACKAGES_TBV2 = 9;  // arbitrarily chosen.

    public final static String MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE = "r1220.img";

    public static String firstMessageListName = "1";
    static final String IntroMessageListFilename = Constants.CATEGORY_INTRO_MESSAGE + ".txt";
    public static final String ACM_PREFIX = "ACM-";

    public static List<String> getRequiredSystemMessages(boolean includeTbTutorial) {
        if (includeTbTutorial) return REQUIRED_SYSTEM_MESSAGES;
        return REQUIRED_SYSTEM_MESSAGES.stream()
                .filter(m -> !TUTORIAL_SYSTEM_MESSAGES.contains(m))
                .collect(Collectors.toList());
    }

    // Begin instance data

    static class BuilderContext {
        static DateFormat ISO8601time = new SimpleDateFormat("HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset

        static {
            ISO8601time.setTimeZone(TBLoaderConstants.UTC);
        }

        final DBConfiguration dbConfig;

        final String project;         // like "TEST"
        final String deploymentName;  // like "TEST-20-2"
        String revision;              // like "a", or "b", ...
        final String buildTimestamp;  // for unpublished revisions.

        final public int deploymentNo;
        final File sourceProgramspecDir;
        final ProgramSpec programSpec;

        final File sourceHomeDir;
        final File sourceTbLoadersDir;
        final File sourceTbOptionsDir;

        final File stagingDir;            // ~/LiteracyBridge/TB-Loaders/TEST
        final File stagedDeploymentDir;   // {stagingDir}/content/TEST-20-2
        final File stagedShadowDir;       // {stagedDeploymentDir}/shadowFiles
        final File stagedProgramspecDir;  // {stagedDeploymentDir}/programspec

        final boolean deDuplicateAudio;

        final public Consumer<Exception> exceptionLogger;

        void logException(Exception ex) {
            exceptionLogger.accept(ex);
        }

        final Consumer<String> statusWriter;

        void reportStatus(String format, Object... args) {
            statusWriter.accept(String.format(format, args));
        }

        BuilderContext(DBConfiguration dbConfig, int deploymentNo, String deploymentName, Consumer<String> statusWriter, Consumer<Exception> exceptionLogger) {
            this("", ISO8601time.format(new Date()), dbConfig, deploymentNo, deploymentName, statusWriter, exceptionLogger);
        }

        BuilderContext(String prefix, BuilderContext other) {
            this(prefix, other.buildTimestamp, ACMConfiguration.getInstance().getDbConfiguration(other.project), other.deploymentNo, other.deploymentName, other.statusWriter, other.exceptionLogger);
        }

        private BuilderContext(String prefix, String timeString, DBConfiguration dbConfig, int deploymentNo, String deploymentName, Consumer<String> statusWriter, Consumer<Exception> exceptionLogger) {
            this.dbConfig = dbConfig;
            this.buildTimestamp = timeString;

            this.deploymentNo = deploymentNo;
            this.deploymentName = deploymentName;

            this.statusWriter = statusWriter;
            this.exceptionLogger = exceptionLogger;
            this.project = dbConfig.getProgramId();
            this.deDuplicateAudio = dbConfig.isDeDuplicateAudio();

            PathsProvider pathsProvider = dbConfig.getPathProvider();
            this.sourceHomeDir = pathsProvider.getProgramHomeDir();
            // Like ~/Amplio/acm-dbs/UWR/TB-Loaders
            this.sourceTbLoadersDir = pathsProvider.getProgramTbLoadersDir();
            this.sourceTbOptionsDir = new File(this.sourceTbLoadersDir, "TB_Options");

            // ~/Amplio/TB-Loaders
            File localTbLoadersDir = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(), Constants.TBLoadersHomeDir);
            // Like ~/Amplio/TB-Loaders/TEST
            this.stagingDir = new File(localTbLoadersDir, prefix + this.project);
            // Like ~/Amplio/TB-Loaders/TEST/content/TEST-20-2
            this.stagedDeploymentDir = new File(this.stagingDir, "content" + File.separator + this.deploymentName);
            this.stagedShadowDir = new File(this.stagedDeploymentDir, "shadowFiles");

            // Like ~/Amplio/TB-Loaders/TEST/content/TEST-20-2/programspec
            this.stagedProgramspecDir = new File(this.stagedDeploymentDir, Constants.ProgramSpecDir);
            // Open the program specification from when the deployment was created.
            this.sourceProgramspecDir = pathsProvider.getProgramSpecDir();
            this.programSpec = new ProgramSpec(this.sourceProgramspecDir);
        }

    }

    private final BuilderContext builderContext;
    final Utils utils;
    final DeplomentPlatform platform;

    /**
     * Expose to API callers the "revision" that was calculated.
     *
     * @return the version of the Deployment, 'a', 'b', 'aa', etc.
     */
    public String getRevision() {
        return builderContext.revision;
    }

    public List<String> getAcceptableFirmwareVersions(boolean isUserFeedbackPublic) {
        List<String> versions = utils.allFirmwareImageVersions();
        String minVersion = FilenameUtils.getBaseName(MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE);
        if (!isUserFeedbackPublic) {
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
            for (int ix = 0; ix < groups.length; ix++) {
                this.groups[ix] = groups[ix].toLowerCase();
            }
        }
    }

    public static class TBBuilderException extends Exception {
        TBBuilderException(String message) {
            super(message);
        }
    }

    public void publishDeployment(List<String> deploymentList) throws Exception {
        new Publish(this, builderContext).publishDeployment(deploymentList);
    }

    /**
     * Construct the TB Builder.
     *
     * @param dbConfig        for the database for which the deployment will be created.
     * @param deploymentNo    The deployment number.
     * @param deploymentName  The deployment name. Currently fixed based on programid, deployment #, and deployment year.
     * @param statusWriter    write progress messages here.
     * @param exceptionLogger write exceptions here.
     * @throws Exception if an exception happens tha twe can't recover from.
     */
    public TBBuilder(
            DBConfiguration dbConfig,
            int deploymentNo,
            String deploymentName,
            DeplomentPlatform platform,
            Consumer<String> statusWriter,
            Consumer<Exception> exceptionLogger
    ) throws Exception {
        builderContext = new BuilderContext(dbConfig, deploymentNo, deploymentName, statusWriter, exceptionLogger);
        utils = new Utils(this, builderContext);
        this.platform = platform;
    }

    /**
     * From the main(String[] args) parameter, build a list of packages, their language, and groups.
     *
     * @param args from main(), after the command, the project/acm, and the deployment name.
     * @return List of PackageInfo objects as specified in the args.
     */
    private List<PackageInfo> getPackageInfoForCreate(List<String> args) {
        if (args.size() < 2 || (args.size() > 2 && ((args.size() % 3) != 0))) {
            throw new IllegalArgumentException("Unexpected number of arguments for CREATE.");
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
//        new Validator(this, builderContext).validateDeployment(packages);
        new Create(this, builderContext).createDeploymentWithPackages(packages);
    }

    /**
     * Creates a deployment from the packages described by args.
     *
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

    // TODO: move platform here
    public void createDeployment(DeploymentInfo deploymentInfo, AcmContent.AcmRootNode playlistRootNode) throws Exception {
        if (platform == DeplomentPlatform.TalkingBook) {
            // Was "v1-"
            BuilderContext bc1 = new BuilderContext("", builderContext);
            CreateForV1 cfv1 = new CreateForV1(this, bc1, deploymentInfo);
            cfv1.go();

            // Was "v2-"
            DBConfiguration dbConfig = ACMConfiguration.getInstance().getDbConfiguration(builderContext.project);
            if (dbConfig.hasTbV2Devices()) {
                BuilderContext bc2 = new BuilderContext("", builderContext);
                CreateForV2 cfv2 = new CreateForV2(this, bc2, deploymentInfo);
                cfv2.go();
            }
        } else {
            BuilderContext bc2 = new BuilderContext("", builderContext);
            CreateForCompanionApp cfv2 = new CreateForCompanionApp(this, bc2, deploymentInfo, playlistRootNode);
            cfv2.go();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.printf("TB-Builder version %s%n", Constants.ACM_VERSION);
        System.out.println("\n================================================================================\n\n" +
                "The command line TB-Builder has been retired.\n\n" +
                "To create a Deployment, use the Deployment Assistant in the ACM.\n\n" +
                "Contact Amplio (support@amplio.org) with questions or for assistance.\n\n" +
                "================================================================================\n\n");
        System.exit(1);
    }
}
