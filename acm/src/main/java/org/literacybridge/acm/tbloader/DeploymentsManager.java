package org.literacybridge.acm.tbloader;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.cloud.ProjectsHelper;
import org.literacybridge.acm.cloud.ProjectsHelper.DeploymentInfo;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.fs.ZipUnzip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_REVERSE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_REVISION_PATTERN;
import static org.literacybridge.core.tbloader.TBLoaderConstants.UNPUBLISHED_DEPLOYMENT_PATTERN;
import static org.literacybridge.core.tbloader.TBLoaderConstants.UNPUBLISHED_REV;

class DeploymentsManager {
    private static final Logger LOG = Logger.getLogger(DeploymentsManager.class.getName());
    private final String project;
    private final File localProjectDir;

    private LocalDeployment localDeployment;
    private AvailableDeployments availableDeployments;

    private State state = State.Unset;

    // TODO: Remove when Dropbox completely de-implemented.
    private boolean useDropboxDeployments = false;
    DeploymentsManager(String project) {
        this(project, false);
    }

    // TODO: Remove 'useDropbox' hwen Dropbox completely de-implemented
    DeploymentsManager(String project, boolean useDropbox) {
        this.project = project;
        this.useDropboxDeployments = useDropbox;
        // ~/LiteracyBridge/TB-Loaders/{project}
        localProjectDir = ACMConfiguration.getInstance().getLocalTbLoaderDirFor(project);
    }

    /**
     * Gets the state of the local deployment, if any. One of the State enum values.
     * @return the State
     */
    synchronized State getState() {
        if (state == State.Unset) {
            AvailableDeployments ad = getAvailableDeployments();
            LocalDeployment ld = getLocalDeployment();

            if (ad.isMissingLatest()) {
                state = State.Missing_Latest;
            } else if (ld.errorMessage != null) {
                state = State.Bad_Local;
            } else if (ld.localRevision == null) {
                state = State.No_Deployment;
            } else if (ld.isUnpublished) {
                state = State.OK_Unpublished;
            } else if (ad.isOffline()) {
                state = State.OK_Cached;
            } else if (ld.localRevision.equalsIgnoreCase(ad.getCurrentRevId())) {
                state = State.OK_Latest;
            } else {
                state = State.Not_Latest;
            }
        }
        return state;
    }

    /**
     * Retrieves information about the local deployment, if any.
     * @return a LocalDeployment object. If that object's errorMessage field is non-null,
     * there is some problem with the local deployment. Otherwise, localRev contains the
     * current local revision, if any (if null, there is no local revision). If the field
     * isUnpublished is true, the local deployment is unpublished.
     */
    synchronized LocalDeployment getLocalDeployment() {
        if (localDeployment == null) {
            localDeployment = findLocalDeployment();
        }
        return localDeployment;
    }

    /**
     * Returns a list of the programs with locally cached deployments.
     * @return the list.
     */
    public static List<String> getLocalPrograms() {
        File localProgramsDir = ACMConfiguration.getInstance().getLocalTbLoadersDir();

        List<String> result = new ArrayList<>();
        File[] programDirs = localProgramsDir.listFiles(File::isDirectory);
        if (programDirs != null) {
            for (File programDir : programDirs) {
                File[] revFiles = programDir.listFiles(f -> f.isFile() && f.getName()
                    .toLowerCase()
                    .endsWith(".rev"));
                if (revFiles != null && revFiles.length > 0) {
                    result.add(programDir.getName());
                }
            }
        }
        return result;
    }

    private LocalDeployment findLocalDeployment() {
        // Get *.rev files. Expect at most one.
        File[] revFiles = localProjectDir.listFiles(f ->
            f.isFile() && f.getName().toLowerCase().endsWith(".rev"));
        // Get content/* directories. Expect at most one.
        File[] contentDirs = new File(localProjectDir, "content").listFiles(File::isDirectory);
        if (revFiles==null || revFiles.length==0 || contentDirs==null || contentDirs.length==0) {
            // No local content; need to get from Dropbox.
            return new LocalDeployment();
        }
        if (revFiles.length==1) { // ) && (contentDirs != null && contentDirs.length==1)) {
            // There is exactly one .rev file, and at least one content file. See if
            // there is a match.
            Map<String,File> localFilesMap = Arrays.stream(contentDirs)
                .collect(Collectors.toMap(File::getName, Function.identity()));
            String localRevMarker = revFiles[0].getName();
            String localRev = FilenameUtils.removeExtension(localRevMarker);

            if (localRevMarker.startsWith(UNPUBLISHED_REV)) {
                // If it is a new-style unpublished marker, we know the deployment, and
                // can verify it matches some content we have. But if an old-style marker,
                // we can only accept it if there is exactly one Deployment in content.
                Matcher unpublishedMatcher = UNPUBLISHED_DEPLOYMENT_PATTERN.matcher(localRevMarker);
                if (unpublishedMatcher.matches()) {
                    String unpublishedDeployment = unpublishedMatcher.group(2);
                    if (localFilesMap.containsKey(unpublishedDeployment)) {
                        return new LocalDeployment(localRev, localFilesMap.get(unpublishedDeployment));
                    } else {
                        return new LocalDeployment();
                    }
                } else {
                    // Old style. Is there a single content directory?
                    if (contentDirs.length==1) {
                        // Note that this is just an assumption, though highly probable.
                        return new LocalDeployment(localRev, contentDirs[0]);
                    } else {
                        return new LocalDeployment("Ambiguous unpublished deployment.");
                    }
                }
            } else {
                Matcher publishedMatcher = DEPLOYMENT_REVISION_PATTERN.matcher(localRevMarker);
                if (publishedMatcher.matches()) {
                    String publishedDeployment = publishedMatcher.group(1);
                    if (localFilesMap.containsKey(publishedDeployment)) {
                        return new LocalDeployment(localRev, localFilesMap.get(publishedDeployment));
                    } else {
                        // No local content for the local .rev file.
                        return new LocalDeployment("Local content doesn't match local revision.");
                    }
                } else {
                    // Not a good marker file. Same as no marker.
                    return new LocalDeployment();
                }
            }

        }
        // There are too many .rev files locally.
        return new LocalDeployment("Extraneous local .rev files detected.");
    }

    /**
     * Returns a list of all available deployments, in a map of deployment name to deployment directory.
     * Enumerating the map returns the names in newest to oldest order.
     */
    AvailableDeployments getAvailableDeployments() {
        if (availableDeployments == null) {
            availableDeployments = findAvailableDeployments();
        }
        return availableDeployments;
    }

    /**
     * Examines S3 to find the deployments there. For every
     * deployment found, keeps track of the highest suffix (-a, -b, etc), and keeps track
     * of any .rev files found. (There should be only one.)
     * @return an AvailableDeployments object, with a map of deployment to highest revision, and
     * the latest published deployment.
     */
    private AvailableDeployments findAvailableDeployments() {
        if (useDropboxDeployments) {
            return new AvailableDropboxDeployments();
        }
        
        if (!Authenticator.getInstance().isAuthenticated()) {
            return new NoAvailableOfflineDeployments();
        }

//        return new AvailableDropboxDeployments(orderedDeployments, latestPublishedRevMarker);
        AvailableCloudDeployments depls = new AvailableCloudDeployments();
        depls.findDeployments();
        return depls;

    }

    /**
     * Clears old content and *.rev files from the localProjectDir.
     */
    synchronized void clearLocalDeployments() {
        File localContentDir = new File(localProjectDir, "content");
        File localMetadataDir = new File(localProjectDir, "metadata");
        IOUtils.deleteRecursive(localContentDir);
        IOUtils.deleteRecursive(localMetadataDir);
        deleteRevFiles(localProjectDir);
        localDeployment = null;
    }

    /**
     * Retrieves the Deployment from the given directory.
     * @param desiredDeployment Name of desired Deployment
     * @throws IOException if the Deployment can't be unzipped.
     */
    void getDeployment(String desiredDeployment, BiConsumer<Long, Long> progressHandler) throws IOException {
        clearLocalDeployments();
        availableDeployments.fetchDeployment(desiredDeployment, localProjectDir, progressHandler);
    }

    /**
     * Removes any *.rev files from the given directory. NOT recursive.
     * @param directory to be cleaned.
     */
    private static void deleteRevFiles(File directory) {
        File[] files = directory.listFiles((dir1, name) -> name.toLowerCase().endsWith(".rev"));
        assert files != null;
        //noinspection ResultOfMethodCallIgnored
        Arrays.stream(files).forEach(File::delete);
    }

    enum State {
        Unset,
        Bad_Local,      // Needs to be fixed. Easiest is delete all, re-copy.
        Missing_Latest, // Something is wrong with the Dropbox state.
        No_Deployment,  // No deployment, simply copy latest.
        OK_Unpublished, // Unpublished deployment.
        OK_Latest,      // Local is latest, seems OK.
        Not_Latest,     // Local is not latest.
        OK_Cached       // We have a local that looks fine, but are offline.
    }


    /**
     * Class to describe the local Deployment, if any.
     */
    static class LocalDeployment {
        String localDeployment;     // "TEST-19-1"
        String localRevision;       // "'"a" or "UNPUBLISHED"
        boolean isUnpublished;      // True if unpublished
        File localContent;          // Local directory with deployment. "TEST-19-1"
        String errorMessage;        // If there is no local deployment, this contains explanation.

        private LocalDeployment() {
            this(null, null);
        }

        /**
         * Constructor for the case when there is no error.
         * @param localRevMarkerName Local rev file name, without extension (but with -a, -b, etc).
         * @param localContent Local directory containing Deployment content. Named as the
         *                     Deployment name, without any -a, -b, ...
         *
         * NOTE: BOTH of localRec and localContent can be null, if there is no local Deployment.
         */
        private LocalDeployment(String localRevMarkerName, File localContent) {
            this.localContent = localContent;
            if (localRevMarkerName != null) {
                this.isUnpublished = localRevMarkerName.toUpperCase().startsWith(UNPUBLISHED_REV);
                if (isUnpublished) {
                    Matcher unpubMatcher = UNPUBLISHED_DEPLOYMENT_PATTERN.matcher(localRevMarkerName);
                    if (unpubMatcher.matches()) {
                        this.localDeployment = unpubMatcher.group(2);
                    }
                    this.localRevision = UNPUBLISHED_REV;
                } else {
                    Matcher pubMatcher = DEPLOYMENT_REVISION_PATTERN.matcher(localRevMarkerName);
                    if (pubMatcher.matches()) {
                        this.localDeployment = pubMatcher.group(1);
                        this.localRevision = pubMatcher.group(2);
                    }
                }
            }
        }

        /**
         * Constructor for the case when there is an error.
         * @param errorMessage describing the error.
         */
        private LocalDeployment(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public interface AvailableDeployments {
        String getRevIdForDeployment(String desiredDeployment);
        boolean isOffline();
        boolean isMissingLatest();
        String getCurrentRevId();
        String getCurrentDeployment();

        void fetchDeployment(String desiredDeployment,
            File localProjectDir,
            BiConsumer<Long, Long> progressHandler) throws IOException;

        /**
         * @return Map of Deployment name to Deployment Details.
         */
        Map<String, String> getDeploymentDescriptions();
    }

    /**
     * Class to describe the available & latest Deployments, if any.
     */
    class AvailableDropboxDeployments implements AvailableDeployments {
        Map<String, File> deployments;
        String currentDeployment;   // like TEST-19-2
        String currentRevId;            // Like "c" from "TEST-19-2-c"
        boolean isMissingLatest;

        private AvailableDropboxDeployments() {
            // Map deployment name (w/o the -x suffix) to File for the directory.
            Map<String, File> orderedDeployments = new LinkedHashMap<>();
            String latestPublishedRevMarker = null;
            boolean multiPublishedRev = false;
            String ACMName = ACMConfiguration.cannonicalAcmDirectoryName(project);
            File publishedDir = new File(ACMConfiguration.getInstance().getTbLoaderDirFor(ACMName),
                "published");
            File[] publishedFiles = publishedDir.listFiles();
            if (publishedFiles != null) {
                Map<String, File> directoriesByDeployment = new HashMap<>();
                // Get the deployments, and their latest versions (based on the -x suffix).
                for (File pf: publishedFiles) {
                    // Only consider non-hidden files and directories.
                    if (!pf.isHidden() && !pf.getName().startsWith(".")) {
                        // Expecting to see at most one .rev file.
                        if (pf.isFile() && pf.getName().endsWith(".rev")) {
                            // Found the marker for the most recent TB-Builder PUBLISH. It SHOULD match
                            // the name of the file ~/LiteracyBridge/TB-Loaders/{project}/*.rev
                            multiPublishedRev = latestPublishedRevMarker != null;
                            latestPublishedRevMarker = FilenameUtils.removeExtension(pf.getName());

                        } else if (pf.isDirectory()) {
                            Matcher deplMatcher = DEPLOYMENT_REVISION_PATTERN.matcher(pf.getName());
                            if (deplMatcher.matches()) {
                                // Found a published directory. Ensure it contains the expected zip file.
                                File zip = new File(pf, "content-" + pf.getName() + ".zip");
                                if (zip.exists() && zip.isFile()) {
                                    // Based on the -x suffix, keep the latest published instance of this deployment.
                                    String deployment = deplMatcher.group(1);
                                    if (directoriesByDeployment.containsKey(deployment)) {
                                        // Compare file names. If new name is greater, store file.
                                        int cmp = compareByRevision(pf.getName(), directoriesByDeployment.get(deployment).getName());
                                        if (cmp > 0) {
                                            directoriesByDeployment.put(deployment, pf);
                                        }
                                    } else {
                                        directoriesByDeployment.put(deployment, pf);
                                    }
                                }
                            }
                        }
                    }
                }
                // If there is more than one, we don't know which is real.
                if (multiPublishedRev) {
                    latestPublishedRevMarker = null;
                }
                // Sort newest to oldest.
                orderedDeployments = directoriesByDeployment.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(LASTMODIFIED_REVERSE))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
            }

            this.deployments = orderedDeployments;
            if (latestPublishedRevMarker !=null) {
                Matcher deplMatcher = DEPLOYMENT_REVISION_PATTERN.matcher(latestPublishedRevMarker);
                if (deplMatcher.matches()) {
                    this.currentDeployment = deplMatcher.group(1); // like "TEST-19-2-c"
                    this.currentRevId = deplMatcher.group(2); // like "c"
                }
            }
            isMissingLatest = latestPublishedRevMarker == null || !deployments.containsKey(currentDeployment);

        }

        public String getRevIdForDeployment(String desiredDeployment) {
            // The directory for the deployment includes the RevId as a suffix.
            File deploymentDir = deployments.get(desiredDeployment);
            Matcher deplMatcher = DEPLOYMENT_REVISION_PATTERN.matcher(deploymentDir.getName());
            if (deplMatcher.matches()) {
                // Group 1 is the deployment name, group 2 is the revid.
                return deplMatcher.group(2);
            }
            return null;
        }

        @Override
        public boolean isOffline() {
            return false;
        }

        @Override
        public boolean isMissingLatest() {
            return isMissingLatest;
        }

        /**
         * Gets the revision marker for the latest deployment.
         * @return the revision marker for the latest deployment, like "c".
         */
        @Override
        public String getCurrentRevId() {
            return currentRevId;
        }

        @Override
        public String getCurrentDeployment() {
            return currentDeployment;
        }

        /**
         * Unzips a deployment from Dropbox to ~/LiteracyBridge. The zip is in
         * ~/Dropbox/ACM-{proj}/TB-Loaders/published/{deployment-rev}/content-{deployment-rev}.zip
         * It's contents are in a tree like content/{deployment}/...
         * The "-rev" part is a hyphen and a version letter, starting with 'a'.
         * @param desiredDeployment The desired deployment, like TEST-19-1
         * @param localProjectDir Where to put the deployment files.
         * @throws IOException if the zip can't be read, or the deployment can't be written.
         */
        @Override
        public void fetchDeployment(String desiredDeployment, File localProjectDir, BiConsumer<Long, Long> progressHandler)
            throws IOException
        {
            File srcDirectory = deployments.get(desiredDeployment);

            //    7z x -y -o"%userprofile%\LiteracyBridge\TB-Loaders\%project%" "..\ACM-%project%\TB-Loaders\published\%latestUpdate%\content-%latestUpdate%.zip"
            String zipFileName = String.format("content-%s.zip", srcDirectory.getName());
            File zipFile = new File(srcDirectory, zipFileName);
            ZipUnzip.unzip(zipFile, localProjectDir);

            // Leave a marker to indicate what is here.
            String revFileName = srcDirectory.getName() + ".rev";
            File revFile = new File(localProjectDir, revFileName);
            if (!revFile.createNewFile()) {
                LOG.warning(String.format("Could not create file '%s'", revFile.getAbsolutePath()));
            }
        }

        @Override
        public Map<String, String> getDeploymentDescriptions() {
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, File> e : deployments.entrySet()) {
                result.put(e.getKey(), e.getValue().getName());
            }
            return result;
        }

        /**
         * Standard compare function
         * @param a One value to compare.
         * @param b The other value.
         * @return 0 if the strings are equal, a value less than zero if a is lexicographically less
         *  than b, and greater than 0 if a is greater than b.
         */
        private int compareByRevision(String a, String b) {
            // A longer string must have a longer revision component. Since there are no "leading
            // zeros", a longer revision string is a larger value. And since the strings are the
            // same except for their revision parts, if the lengths are the same, we can simply
            // compare the strings.
            int result = a.length() - b.length();
            if (result == 0) {
                result = a.compareTo(b);
            }
            return result;
        }
    }

    /**
     * There aren't any available Deployments when we're offline. Of course.
     */
    static class NoAvailableOfflineDeployments implements AvailableDeployments {

        @Override
        public String getRevIdForDeployment(String desiredDeployment) {
            return null;
        }

        @Override
        public boolean isOffline() {
            return true;
        }

        @Override
        public boolean isMissingLatest() {
            return false;
        }

        @Override
        public String getCurrentRevId() {
            return null;
        }

        @Override
        public String getCurrentDeployment() {
            return null;
        }

        @Override
        public void fetchDeployment(String desiredDeployment,
            File localProjectDir,
            BiConsumer<Long, Long> progressHandler)
        {

        }

        @Override
        public Map<String, String> getDeploymentDescriptions() {
            return new HashMap<String,String>();
        }
    }

    /**
     * Describes the deployments available in the cloud.
     */
    class AvailableCloudDeployments implements AvailableDeployments {
        Map<String, DeploymentInfo> deploymentsInfo;
        DeploymentInfo currentDeploymentInfo;

        void findDeployments() {
            Authenticator authInstance = Authenticator.getInstance();
            ProjectsHelper projectsHelper = authInstance.getProjectsHelper();
            deploymentsInfo = projectsHelper.getDeploymentInfo(project);
            currentDeploymentInfo = deploymentsInfo.values().stream().filter(DeploymentInfo::isCurrent).findAny().orElse(null);
        }

        @Override
        public String getRevIdForDeployment(String desiredDeployment) {
            DeploymentInfo di = deploymentsInfo.get(desiredDeployment);
            return di == null ? null : di.getRevId();
        }

        @Override
        public boolean isOffline() {
            return false;
        }

        @Override
        public boolean isMissingLatest() {
            return deploymentsInfo.values().stream().noneMatch(DeploymentInfo::isCurrent);
        }

        @Override
        public String getCurrentRevId() {
            return currentDeploymentInfo == null ? null : currentDeploymentInfo.getRevId();
        }

        @Override
        public String getCurrentDeployment() {
            return currentDeploymentInfo == null ? null : currentDeploymentInfo.getDeploymentName();
        }

        @Override
        public void fetchDeployment(String desiredDeployment, File localProjectDir, BiConsumer<Long, Long> progressHandler)
            throws IOException
        {
            DeploymentInfo di = deploymentsInfo.get(desiredDeployment);
            File tempDir = Files.createTempDirectory("tbloader-tmp").toFile();
            File tempFile = new File(tempDir, di.getFileName());

            Authenticator authInstance = Authenticator.getInstance();
            ProjectsHelper projectsHelper = authInstance.getProjectsHelper();
            projectsHelper.downloadDeployment(di, tempFile, progressHandler/*(p,t)->{System.out.printf("%d/%d\n",p,t);}*/);

            //    7z x -y -o"%userprofile%\LiteracyBridge\TB-Loaders\%project%" "..\ACM-%project%\TB-Loaders\published\%latestUpdate%\content-%latestUpdate%.zip"
            ZipUnzip.unzip(tempFile, localProjectDir);

            // Leave a marker to indicate what is here.
            String revFileName = di.getVersionMarker() + ".rev";
            File revFile = new File(localProjectDir, revFileName);
            if (!revFile.createNewFile()) {
                LOG.warning(String.format("Could not create file '%s'", revFile.getAbsolutePath()));
            }

        }

        @Override
        public Map<String, String> getDeploymentDescriptions() {
            Map<String,String> result = new LinkedHashMap<>();
            for (Map.Entry<String,DeploymentInfo> e : deploymentsInfo.entrySet()) {
                result.put(e.getKey(), e.getValue().getVersionMarker());
            }
            return result;
        }

    }

    }
