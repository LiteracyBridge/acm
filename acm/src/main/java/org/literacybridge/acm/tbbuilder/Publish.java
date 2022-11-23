package org.literacybridge.acm.tbbuilder;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.tools.DBExporter;
import org.literacybridge.core.fs.ZipUnzip;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.literacybridge.acm.tbbuilder.TBBuilder.ACM_PREFIX;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CATEGORIES_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CONTENT_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CATEGORIES_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CONTENT_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT;
import static org.literacybridge.acm.tbbuilder.TBBuilder.PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_REVISION_PATTERN;

class Publish {
    private static final Logger LOG = Logger.getLogger(Publish.class.getName());

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final TBBuilder tbBuilder;
    private final TBBuilder.BuilderContext builderContext;

    Publish(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext) {
        this.tbBuilder = tbBuilder;
        this.builderContext = builderContext;
    }

    /**
     * Zips up a Deployment, and places it in a {Home}/{ACM-NAME}/TB-Loaders/published/{Deployment}-{counter}
     * directory. Creates a marker file named {Deployment}-{counter}.rev
     *
     * @param deploymentList List of deployments. Effectively, always exactly one.
     * @throws Exception if a file can't be read.
     */
    public void publishDeployment(List<String> deploymentList) throws Exception {
        // Make a local copy so we can munge it.
        List<String> deployments = new ArrayList<>(deploymentList).stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        assert deployments.get(0).equals(builderContext.deploymentName);

        // e.g. 'ACM-UWR/TB-Loaders/published/'
        final File publishBaseDir = new File(builderContext.sourceTbLoadersDir, "published");
        //noinspection ResultOfMethodCallIgnored
        publishBaseDir.mkdirs();

        builderContext.revision = getNextDeploymentRevision(publishBaseDir, builderContext.deploymentName);
        final String publishDistributionName = builderContext.deploymentName + "-" + builderContext.revision; // e.g.
        // Remove any .rev file that we had left to mark the deployment as unpublished.
        Utils.deleteRevFiles(builderContext.stagedDeploymentDir);

        // Add a revision marker to the deployment_info.properties file.
        addRevisionMarkerToDeploymentInfo();

        // e.g. 'ACM-UWR/TB-Loaders/published/2015-6-c'
        final File publishDistributionDir = new File(publishBaseDir, publishDistributionName);
        //noinspection ResultOfMethodCallIgnored
        publishDistributionDir.mkdirs();

        // Copy the program spec to a directory outside of the .zip file.
        if (builderContext.stagedProgramspecDir.exists() && builderContext.stagedProgramspecDir.isDirectory()) {
            File publishedProgramSpecDir = new File(publishDistributionDir, Constants.ProgramSpecDir);
            FileUtils.copyDirectory(builderContext.stagedProgramspecDir, publishedProgramSpecDir);
        }

        String zipSuffix = builderContext.deploymentName + "-" + builderContext.revision + ".zip";
        File localContent = new File(builderContext.stagingDir, "content");
        // Adds the given deployments to the .zip file. The assistant only supports one deployment
        // per .zip file.
        ZipUnzip.zip(localContent,
                new File(publishDistributionDir, "content-" + zipSuffix), true,
                deployments.toArray(new String[0]));

        // merge csv files
        File stagedMetadata = new File(builderContext.stagingDir, "metadata");
        //List<String> deploymentsList = Arrays.asList(deployments);
        File[] metadataDirs = stagedMetadata.listFiles(f -> f.isDirectory()
                && deployments.contains(f.getName()));
        final List<File> inputContentCSVFiles = new LinkedList<>();
        final List<File> inputCategoriesCSVFiles = new LinkedList<>();
        final List<File> inputPackagesCSVFiles = new LinkedList<>();
        for (File metadataDir : Objects.requireNonNull(metadataDirs)) {
            // Return ignored; output captured in the callback.
            //noinspection ResultOfMethodCallIgnored
            metadataDir.listFiles(f -> {
                if (f.getName().endsWith(CONTENT_IN_PACKAGES_CSV_FILE_NAME)) {
                    inputContentCSVFiles.add(f);
                    return true;
                }
                else if (f.getName().endsWith(CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)) {
                    inputCategoriesCSVFiles.add(f);
                    return true;
                }
                else if (f.getName().endsWith(PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)) {
                    inputPackagesCSVFiles.add(f);
                    return true;
                }
                return false;
            });
        }
        File publishedMetadataDir = new File(publishDistributionDir, "metadata");
        if (!publishedMetadataDir.exists()) { //noinspection ResultOfMethodCallIgnored
            publishedMetadataDir.mkdir(); }
        File mergedCSVFile = new File(publishedMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME);
        mergeCSVFiles(inputContentCSVFiles, mergedCSVFile, CSV_COLUMNS_CONTENT_IN_PACKAGE);

        mergedCSVFile = new File(publishedMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME);
        mergeCSVFiles(inputCategoriesCSVFiles, mergedCSVFile, CSV_COLUMNS_CATEGORIES_IN_PACKAGE);

        mergedCSVFile = new File(publishedMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME);
        mergeCSVFiles(inputPackagesCSVFiles, mergedCSVFile, CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

        new DBExporter(ACM_PREFIX + builderContext.project, publishedMetadataDir).export();

        // Note that what we've just published is the latest on this computer.
        Utils.deleteRevFiles(builderContext.stagingDir);
        File newRev = new File(builderContext.stagingDir,
                builderContext.deploymentName + "-" + builderContext.revision + ".rev");
        //noinspection ResultOfMethodCallIgnored
        newRev.createNewFile();
    }

    void addRevisionMarkerToDeploymentInfo() {
        Properties deploymentProperties;
        File propsFile = new File(builderContext.stagedProgramspecDir, ProgramSpec.DEPLOYMENT_INFO_PROPERTIES_NAME);
        boolean saved = false;

        try (InputStream fis = new BufferedInputStream(new FileInputStream(propsFile))) {
            try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                deploymentProperties = new Properties();
                deploymentProperties.load(bis);

                // We have the deployment, add the revision marker.
                deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_REVISON, builderContext.revision);
                deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_NAME, builderContext.deploymentName);

                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(propsFile))) {
                    deploymentProperties.store(out, null);
                    saved = true;
                }
            }
        } catch (IOException e) {
            // Ignore and continue with empty deployment properties.
        }
        if (!saved) {
            LOG.log(Level.SEVERE, "Unable to save deployment revision to " + propsFile.getAbsolutePath());
        }
    }

    /**
     * Given a TB-Loader "published" directory and a Deployment name, find the next revision
     * for the Deployment, and create a .rev file with that revision. Return the revision.
     *
     * @param publishTbLoadersDir The directory in which the deployments are published.
     * @param deployment          The Deployment (name) for which we want the next revision suffix.
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
                }
                else if (fileRevision.length() > highestRevision.length()) {
                    highestRevision = fileRevision;
                }
            }
            revision = incrementRevision(highestRevision);
        }

        // Delete *.rev, then create our deployment-revision.rev marker file.
        Utils.deleteRevFiles(publishTbLoadersDir);

        File newRev = new File(publishTbLoadersDir, deployment + "-" + revision + ".rev");
        //noinspection ResultOfMethodCallIgnored
        newRev.createNewFile();
        return revision;
    }

    /**
     * Given a revision string, like "a", or "zz", create the next higher value, like "b" or "aaa".
     *
     * @param revision to be incremented
     * @return the incremented value
     */
    static String incrementRevision(String revision) {
        if (revision == null || !revision.matches("^[a-z]+$")) {
            throw new IllegalArgumentException("Revision string must match \"^[a-z]+$\".");
        }
        char[] chars = revision.toCharArray();
        // Looking for a digit we can add to.
        boolean looking = true;
        for (int ix = chars.length - 1; ix >= 0 && looking; ix--) {
            if (++chars[ix] <= 'z') {
                looking = false;
            }
            else {
                chars[ix] = 'a';
            }
        }
        String result = new String(chars);
        if (looking) {
            // still looking, add another "digit".
            result = "a" + result;
        }
        return result;
    }

    private static void mergeCSVFiles(
            Iterable<File> inputFiles, File output,
            String[] header) {

        try (ICSVWriter writer = new CSVWriterBuilder(new FileWriter(output)).build()) {
             writer.writeNext(header);

            for (File input : inputFiles) {
                CSVReader reader = new CSVReader(new FileReader(input));
                // skip header
                reader.readNext();
                writer.writeAll(reader.readAll());
                reader.close();
            }
        } catch (Exception ex) {
            // ignore
        }
    }


}
