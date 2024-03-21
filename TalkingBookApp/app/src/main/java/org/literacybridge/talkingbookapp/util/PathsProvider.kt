package org.literacybridge.talkingbookapp.util

import android.util.Log
import org.literacybridge.talkingbookapp.App
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists


/**
 * The Deployments are stored locallin in a directory structure like this:
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/basic/{image}.img
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/communities/{... community names ...}/...
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/images/{... image names ...}/...
 */
object PathsProvider {
    private const val TAG = "TBL!:" + "PathsProvider"
    private var sTbLoaderAppContext: App = App()

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
    fun getLocalDeploymentDirectory(project: String): File? {
        val projectDir = getProjectDirectory(project)
        val contentDir = File(projectDir, "content")
        var deployments: Array<File?>? = null
        if (contentDir.exists() && contentDir.isDirectory) {
            deployments = contentDir.listFiles()
            if (deployments != null && deployments.size == 1) {
                return deployments[0]
            }
        }
        return null
    }

    fun getProgramSpecDir(project: String): File {
        val deploymentDir = getLocalDeploymentDirectory(project)
        return File(deploymentDir, "programspec")
    }

    val localTempDirectory: File?
        /**
         * Gets a File object that represents a temporary directory into which files from
         * the Talking Book can be copied (or zipped).
         *
         * @return the temporary directory's File.
         */
        get() = App.context.externalCacheDir
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
        if (src.toFile().isDirectory) {
            for (file in src.toFile().listFiles()!!) {
                moveDirectory(file.toPath(), dest.resolve(src.relativize(file.toPath())))
            }
        }
        return try {
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING)
            src.deleteIfExists()
            true
        } catch (e: IOException) {
            false
        }
    }
}
