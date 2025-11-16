package org.literacybridge.acm.tbbuilder

import com.opencsv.CSVReader
import com.opencsv.CSVWriterBuilder
import kotlinx.serialization.json.*
import org.apache.commons.io.FileUtils
import org.json.simple.JSONObject
import org.literacybridge.acm.Constants
import org.literacybridge.acm.cloud.Authenticator
import org.literacybridge.acm.tbbuilder.TBBuilder.BuilderContext
import org.literacybridge.acm.tools.DBExporter
import org.literacybridge.core.fs.ZipUnzip
import org.literacybridge.core.spec.ProgramSpec
import org.literacybridge.core.tbloader.TBLoaderConstants
import java.io.*
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

internal class Publish(
    @field:Suppress("unused") private val tbBuilder: TBBuilder,
    private val builderContext: BuilderContext
) {
    /**
     * Zips up a Deployment, and places it in a {Home}/{ACM-NAME}/TB-Loaders/published/{Deployment}-{counter}
     * directory. Creates a marker file named {Deployment}-{counter}.rev
     *
     * @param deploymentList List of deployments. Effectively, always exactly one.
     * @throws Exception if a file can't be read.
     */
    @Throws(Exception::class)
    fun publishDeployment(deploymentList: List<String>) {
        // Make a local copy so we can munge it.
        val deployments = ArrayList(deploymentList).stream()
            .map { obj: String -> obj.uppercase(Locale.getDefault()) }
            .collect(Collectors.toList())
        assert(deployments[0] == builderContext.deploymentName)

        // e.g. 'ACM-UWR/TB-Loaders/published/'
        val publishBaseDir = File(builderContext.sourceTbLoadersDir, "published")
        publishBaseDir.mkdirs()

        builderContext.revision = getNextDeploymentRevision(publishBaseDir, builderContext.deploymentName)
        val publishDistributionName = builderContext.deploymentName + "-" + builderContext.revision // e.g.
        // Remove any .rev file that we had left to mark the deployment as unpublished.
        Utils.deleteRevFiles(builderContext.stagedDeploymentDir)

        // Add a revision marker to the deployment_info.properties file.
        addRevisionMarkerToDeploymentInfo()

        // e.g. 'ACM-UWR/TB-Loaders/published/2015-6-c'
        val publishDistributionDir = File(publishBaseDir, publishDistributionName)
        publishDistributionDir.mkdirs()

        // Copy the program spec to a directory outside of the .zip file.
        if (builderContext.stagedProgramspecDir.exists() && builderContext.stagedProgramspecDir.isDirectory) {
            val publishedProgramSpecDir = File(publishDistributionDir, Constants.ProgramSpecDir)
            FileUtils.copyDirectory(builderContext.stagedProgramspecDir, publishedProgramSpecDir)
        }

        val zipSuffix = builderContext.deploymentName + "-" + builderContext.revision + ".zip"
        val localContent = File(builderContext.stagingDir, "content")
        // Adds the given deployments to the .zip file. The assistant only supports one deployment
        // per .zip file.
        ZipUnzip.zip(
            localContent,
            File(publishDistributionDir, "content-$zipSuffix"), true,
            deployments.toTypedArray<String>()
        )

        // merge csv files
        val stagedMetadata = File(builderContext.stagingDir, "metadata")
        //List<String> deploymentsList = Arrays.asList(deployments);
        val metadataDirs = stagedMetadata.listFiles { f: File ->
            f.isDirectory
                    && deployments.contains(f.name)
        }
        val inputContentCSVFiles: MutableList<File> = LinkedList()
        val inputCategoriesCSVFiles: MutableList<File> = LinkedList()
        val inputPackagesCSVFiles: MutableList<File> = LinkedList()
        for (metadataDir in Objects.requireNonNull<Array<File>?>(metadataDirs)) {
            // Return ignored; output captured in the callback.
            metadataDir.listFiles { f: File ->
                if (f.name.endsWith(TBBuilder.CONTENT_IN_PACKAGES_CSV_FILE_NAME)) {
                    inputContentCSVFiles.add(f)
                    return@listFiles true
                } else if (f.name.endsWith(TBBuilder.CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)) {
                    inputCategoriesCSVFiles.add(f)
                    return@listFiles true
                } else if (f.name.endsWith(TBBuilder.PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)) {
                    inputPackagesCSVFiles.add(f)
                    return@listFiles true
                }
                false
            }
        }
        val publishedMetadataDir = File(publishDistributionDir, "metadata")
        if (!publishedMetadataDir.exists()) {
            publishedMetadataDir.mkdir()
        }
        var mergedCSVFile = File(publishedMetadataDir, TBBuilder.CONTENT_IN_PACKAGES_CSV_FILE_NAME)
        mergeCSVFiles(inputContentCSVFiles, mergedCSVFile, TBBuilder.CSV_COLUMNS_CONTENT_IN_PACKAGE)

        mergedCSVFile = File(publishedMetadataDir, TBBuilder.CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)
        mergeCSVFiles(inputCategoriesCSVFiles, mergedCSVFile, TBBuilder.CSV_COLUMNS_CATEGORIES_IN_PACKAGE)

        mergedCSVFile = File(publishedMetadataDir, TBBuilder.PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)
        mergeCSVFiles(inputPackagesCSVFiles, mergedCSVFile, TBBuilder.CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT)

        DBExporter(TBBuilder.ACM_PREFIX + builderContext.project, publishedMetadataDir).export()

        // Note that what we've just published is the latest on this computer.
        Utils.deleteRevFiles(builderContext.stagingDir)
        val newRev = File(
            builderContext.stagingDir,
            builderContext.deploymentName + "-" + builderContext.revision + ".rev"
        )

        // TODO: upload csv metadata
        newRev.createNewFile()
    }

    fun addRevisionMarkerToDeploymentInfo() {
        val deploymentProperties: Properties
        val propsFile = File(builderContext.stagedProgramspecDir, ProgramSpec.DEPLOYMENT_INFO_PROPERTIES_NAME)
        var saved = false

        try {
            BufferedInputStream(FileInputStream(propsFile)).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    deploymentProperties = Properties()
                    deploymentProperties.load(bis)

                    // We have the deployment, add the revision marker.
                    deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_REVISON, builderContext.revision)
                    deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_NAME, builderContext.deploymentName)
                    BufferedOutputStream(FileOutputStream(propsFile)).use { out ->
                        deploymentProperties.store(out, null)
                        saved = true
                    }
                }
            }
        } catch (e: IOException) {
            // Ignore and continue with empty deployment properties.
        }
        if (!saved) {
            LOG.log(Level.SEVERE, "Unable to save deployment revision to " + propsFile.absolutePath)
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(Publish::class.java.name)

        /**
         * Given a TB-Loader "published" directory and a Deployment name, find the next revision
         * for the Deployment, and create a .rev file with that revision. Return the revision.
         *
         * @param publishTbLoadersDir The directory in which the deployments are published.
         * @param deployment          The Deployment (name) for which we want the next revision suffix.
         * @return the revision suffix as a String. Like "a", "b"... "aa"... "aaaaba", etc
         * @throws Exception if the new .rev file can't be created.
         */
        @JvmStatic
        @Throws(Exception::class)
        fun getNextDeploymentRevision(publishTbLoadersDir: File, deployment: String): String {
            var revision = "a" // If we don't find anything higher, start with 'a'.

            var highestRevision = ""
            // Find all the revisions of the given deployment.
            val fileNames = publishTbLoadersDir.list { dir: File?, name: String ->
                name.lowercase(Locale.getDefault()).startsWith(deployment.lowercase(Locale.getDefault()))
            }
            if (fileNames != null && fileNames.size > 0) {
                for (fileName in fileNames) {
                    // Extract just the revision string.
                    var fileRevision = ""
                    val deplMatcher = TBLoaderConstants.DEPLOYMENT_REVISION_PATTERN.matcher(fileName)
                    if (deplMatcher.matches()) {
                        fileRevision = deplMatcher.group(2).lowercase(Locale.getDefault())
                    }
                    // A longer name is always greater. When the lengths are the same, then we need
                    // to compare the strings.
                    if (fileRevision.length == highestRevision.length) {
                        if (fileRevision.compareTo(highestRevision) > 0) {
                            highestRevision = fileRevision
                        }
                    } else if (fileRevision.length > highestRevision.length) {
                        highestRevision = fileRevision
                    }
                }
                revision = incrementRevision(highestRevision)
            }

            // Delete *.rev, then create our deployment-revision.rev marker file.
            Utils.deleteRevFiles(publishTbLoadersDir)

            val newRev = File(publishTbLoadersDir, "$deployment-$revision.rev")
            newRev.createNewFile()
            return revision
        }

        /**
         * Given a revision string, like "a", or "zz", create the next higher value, like "b" or "aaa".
         *
         * @param revision to be incremented
         * @return the incremented value
         */
        @JvmStatic
        fun incrementRevision(revision: String): String {
            require(revision.matches("^[a-z]+$".toRegex())) { "Revision string must match \"^[a-z]+$\"." }
            val chars = revision.toCharArray()
            // Looking for a digit we can add to.
            var looking = true
            var ix = chars.size - 1
            while (ix >= 0 && looking) {
                if (++chars[ix] <= 'z') {
                    looking = false
                } else {
                    chars[ix] = 'a'
                }
                ix--
            }
            var result = String(chars)
            if (looking) {
                // still looking, add another "digit".
                result = "a$result"
            }
            return result
        }

        private fun mergeCSVFiles(
            inputFiles: Iterable<File>, output: File,
            header: Array<String>
        ) {
            try {
                CSVWriterBuilder(FileWriter(output)).build().use { writer ->
                    writer.writeNext(header)
                    for (input in inputFiles) {
                        val reader = CSVReader(FileReader(input))
                        // skip header
                        reader.readNext()
                        writer.writeAll(reader.readAll())
                        reader.close()
                    }
                }
            } catch (ex: Exception) {
                // ignore
            }
        }
    }
}
