package org.literacybridge.acm.tbloader;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.fs.ZipUnzip;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_REVERSE;

class DeploymentsManager {
    private final String project;
    private final File localProjectDir;

    private LocalDeployment localDeployment;
    private AvailableDeployments availableDeployments;

    private State state = State.Unset;

    DeploymentsManager(String project) {
        this.project = project;
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

            if (ad.isMissingLatest) {
                state = State.Missing_Latest;
            } else if (ld.errorMessage != null) {
                state = State.Bad_Local;
            } else if (ld.localDeploymentRev == null) {
                state = State.No_Deployment;
            } else if (ld.isUnpublished) {
                state = State.OK_Unpublished;
            } else if (ld.localDeploymentRev.equalsIgnoreCase(ad.latestPublishedRev)) {
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

    private LocalDeployment findLocalDeployment() {
        // Get *.rev files. Expect at most one.
        File[] revFiles = localProjectDir.listFiles(f ->
            f.isFile() && f.getName().toLowerCase().endsWith(".rev"));
        // Get content/* directories. Expect at most one.
        File[] contentDirs = new File(localProjectDir, "content").listFiles(File::isDirectory);
        if (revFiles.length==0 && (contentDirs==null || contentDirs.length==0)) {
            // No local content; need to get from Dropbox.
            return new LocalDeployment(null, null);
        }
        if (revFiles.length==1 && (contentDirs != null && contentDirs.length==1)) {
            // There are the right numbers of files; see if their names match.
            String localRev = FilenameUtils.removeExtension(revFiles[0].getName());
            String localContent = contentDirs[0].getName();
            if (localRev.toUpperCase().startsWith(localContent.toUpperCase()) ||
                    localRev.toUpperCase().startsWith(TBLoaderConstants.UNPUBLISHED_REV)) {
                // Yes, we have exactly the latest published deployment copied locally.
                return new LocalDeployment(localRev, contentDirs[0]);
            } else {
                // No, the .rev doesn't match the content.
                return new LocalDeployment("Local content doesn't match local revision.");
            }
        }
        // There are too many files locally.
        return new LocalDeployment("Extraneous local files detected.");
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

    private AvailableDeployments findAvailableDeployments() {
        // Map deployment name (w/o the -x suffix) to File for the directory.
        Map<String, File> orderedDeployments = new LinkedHashMap<>();
        String latestPublishedRev = null;
        boolean multiPublishedRev = false;
        String ACMName = ACMConfiguration.cannonicalAcmDirectoryName(project);
        File publishedDir = new File(ACMConfiguration.getInstance().getTbLoaderDirFor(ACMName),
            "published");
        File[] publishedFiles = publishedDir.listFiles();
        if (publishedFiles != null) {
            Map<String, File> deployments = new HashMap<>();
            // Get the deployments, and their latest versions (based on the -x suffix).
            for (File pf: publishedFiles) {
                // Only consider non-hidden files and directories.
                if (!pf.isHidden() && !pf.getName().startsWith(".")) {
                    // Expecting to see at most one .rev file.
                    if (pf.isFile() && pf.getName().endsWith(".rev")) {
                        // Found the marker for the most recent TB-Builder PUBLISH. It SHOULD match
                        // the name of the file ~/LiteracyBridge/TB-Loaders/{project}/*.rev
                        multiPublishedRev = latestPublishedRev != null;
                        latestPublishedRev = FilenameUtils.removeExtension(pf.getName());
                    } else if (pf.isDirectory() && pf.getName().matches(".*-[a-zA-Z]$")){
                        // Found a published directory. Ensure it contains the expected zip file.
                        File zip = new File(pf, "content-"+pf.getName()+".zip");
                        if (zip.exists() && zip.isFile()) {
                            // Based on the -x suffix, keep the latest one.
                            String depl = pf.getName();
                            depl = depl.substring(0, depl.length() - 2);
                            if (deployments.containsKey(depl)) {
                                // Compare file names. If new name is greater, store file.
                                int cmp = pf.getName().compareTo(deployments.get(depl).getName());
                                if (cmp > 0) {
                                    deployments.put(depl, pf);
                                }
                            } else {
                                deployments.put(depl, pf);
                            }
                        }
                    }
                }
            }
            // If there is more than one, we don't know which is real.
            if (multiPublishedRev) {
                latestPublishedRev = null;
            }
            // Sort newest to oldest.
            List<String> deploymentNames = new ArrayList<>(deployments.keySet());
            deploymentNames.sort((a,b)-> LASTMODIFIED_REVERSE.compare(deployments.get(a), deployments.get(b)));
            // Build a new map, preserving order.
            for (String n: deploymentNames) {
                orderedDeployments.put(n, deployments.get(n));
            }
        }
        return new AvailableDeployments(orderedDeployments, latestPublishedRev);
    }

    /**
     * Unzips a deployment from Dropbox to ~/LiteracyBridge. The zip is in
     * ~/Dropbox/ACM-{proj}/TB-Loaders/published/{deployment-rev}/content-{deployment-rev}.zip
     * It's contents are in a tree like content/{deployment}/...
     * The "-rev" part is a hyphen and a version letter, starting with 'a'.
     * @param directory The directory containing the zip file, named as above.
     * @throws IOException if the zip can't be read, or the deployment can't be written.
     */
    private void fetchDeployment(File directory) throws IOException {
        //    7z x -y -o"%userprofile%\LiteracyBridge\TB-Loaders\%project%" "..\ACM-%project%\TB-Loaders\published\%latestUpdate%\content-%latestUpdate%.zip"
        String zipFileName = String.format("content-%s.zip", directory.getName());
        File zipFile = new File(directory, zipFileName);
        ZipUnzip.unzip(zipFile, localProjectDir);

        // Leave a marker to indicate what is here.
        String revFileName = directory.getName() + ".rev";
        File revFile = new File(localProjectDir, revFileName);
        revFile.createNewFile();
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
     * @param desired Name of desired Deployment
     * @throws IOException if the Deployment can't be unzipped.
     */
    void getDeployment(String desired) throws IOException {
        File directory = availableDeployments.deployments.get(desired);
        clearLocalDeployments();
        fetchDeployment(directory);
    }

    /**
     * Removes any *.rev files from the given directory. NOT recursive.
     * @param directory to be cleaned.
     */
    private static void deleteRevFiles(File directory) {
        File[] files = directory.listFiles((dir1, name) -> name.toLowerCase().endsWith(".rev"));
        Arrays.stream(files).forEach(File::delete);
    }

    enum State {
        Unset,
        Bad_Local,      // Needs to be fixed. Easiest is delete all, re-copy.
        Missing_Latest, // Something is wrong with the Dropbox state.
        No_Deployment,  // No deployment, simply copy latest.
        OK_Unpublished, // Unpublished deployment.
        OK_Latest,      // Local is latest, seems OK.
        Not_Latest      // Local is not latest.
    }


    /**
     * Class to describe the local Deployment, if any.
     */
    static class LocalDeployment {
        String localDeployment;
        String localDeploymentRev;
        boolean isUnpublished;
        File localContent;
        String errorMessage;

        /**
         * Constructor for the case when there is no error.
         * @param localDeploymentRev Local rev file name, without extension (but with -a, -b, etc).
         * @param localContent Local directory containing Deployment content. Named as the
         *                     Deployment name, without any -a, -b, ...
         *
         * NOTE: BOTH of localRec and localContent can be null, if there is no local Deployment.
         */
        private LocalDeployment(String localDeploymentRev, File localContent) {
            this.localDeploymentRev = localDeploymentRev;
            this.localContent = localContent;
            this.isUnpublished = localDeploymentRev
                !=null && localDeploymentRev.toUpperCase().startsWith(TBLoaderConstants.UNPUBLISHED_REV);
            if (localDeploymentRev !=null && !isUnpublished) {
                this.localDeployment = localDeploymentRev.substring(0, localDeploymentRev.length()-2);
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

    /**
     * Class to describe the available & latest Deployments, if any.
     */
    static class AvailableDeployments {
        Map<String, File> deployments;
        String latestPublished;
        String latestPublishedRev;
        boolean isMissingLatest;

        private AvailableDeployments(Map<String, File> deployments, String latestPublishedRev) {
            this.deployments = deployments;
            this.latestPublishedRev = latestPublishedRev;
            if (latestPublishedRev!=null) {
                this.latestPublished = latestPublishedRev.substring(0,
                    latestPublishedRev.length() - 2);
            }
            isMissingLatest = latestPublishedRev==null || !deployments.containsKey(latestPublished);
        }

        String getRevForDeployment(String needed) {
            File revDir = deployments.get(needed);
            return revDir.getName();
        }
    }

}
