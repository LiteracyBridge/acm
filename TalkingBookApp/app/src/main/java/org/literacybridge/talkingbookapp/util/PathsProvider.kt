package org.literacybridge.talkingbookapp.util

import android.util.Log
import org.literacybridge.core.tbloader.TBLoaderConstants
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.database.ProgramContentEntity
import org.literacybridge.talkingbookapp.util.Constants.Companion.LOG_TAG
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption


/**
 * The Deployments are stored locallin in a directory structure like this:
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/basic/{image}.img
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/communities/{... community names ...}/...
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/images/{... image names ...}/...
 */
object PathsProvider {
    private const val TAG = "TBL!:" + "PathsProvider"
//    private var sTbLoaderAppContext: App = App()

//    fun init(applicationContext: App) {
//        sTbLoaderAppContext = applicationContext
//    }

    val localContentDirectory: File?
        /**
         * Gets a File object that represents a directory containing all projects with their downloaded
         * Deployments.
         *
         * @return the directory's File.
         */
        get() = App.context.getExternalFilesDir("")

    /**
     * Gets the {externalFilesDirectory}/{project} directory for a specific project.
     *
     * @param project The name of the project.
     * @return the project's directory's File.
     */
    fun getProjectDirectory(project: String): File {
        val dir = File(App.context.getExternalFilesDir(project), "")

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    val locationsCacheDirectory: File?
        /**
         * Gets a File object that represents a directory containing cached location information.
         *
         * @return the directory's File.
         */
        get() = App.context.getExternalFilesDir("locations")

    /**
     * Gets the {externalFilesDirectory}/localrepository/{project}/content/{Deployment name} directory
     * for a specific project. There should be only one {Deployment name}, and this returns null if
     * there is not exactly one.
     *
     * @param project The desired project.
     * @return The Deployment directory for the project.
     */
    fun getLocalDeploymentDirectory(project: ProgramContentEntity): File {
        return File(project.localPath)
    }

    fun getProgramSpecDir(project: ProgramContentEntity): File {
        return File(project.localPath, "programspec")
    }

    fun getStatsDirectory(timestamp: String): File {
        return File(
            localTempDirectory,
            "${TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME}${File.separator}${timestamp}"
        )
    }

    val localTempDirectory: File
        /**
         * Gets a File object that represents a temporary directory into which files from
         * the Talking Book can be copied (or zipped).
         *
         * @return the temporary directory's File.
         */
        get() = File(App.context.getExternalFilesDir("temp"), "")

    val uploadDirectory: File?
        /**
         * Gets a File object that represents a directory into which files intended to be uploaded
         * to a LB server should be placed. Think of this as a staging directory to the cloud.
         *
         * @return the staging directory's File.
         */
        get() = App.context.getExternalFilesDir("upload")

    val logDirectory: File?
        /**
         * Gets a File object that represents a directory into which log files should be placed. The
         * expectation is that they will eventually be moved to the "upload" directory.
         *
         * @return the staging directory's File.
         */
        get() = App.context.getExternalFilesDir("log")
    val srnDirectory: File?
        /**
         * Gets a File object that represents a directory into which srn files should be placed. The
         * expectation is that they will eventually be moved to the "upload" directory.
         *
         * @return the staging directory's File.
         */
        get() = App.context.getExternalFilesDir("srn")

    fun moveDirectory(src: Path, dest: Path): Boolean {
        return try {
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            Log.d("$LOG_TAG funMoveDirectory:", e.stackTraceToString())
            false
        }
    }
}
