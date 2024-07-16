package org.literacybridge.tbloaderandroid.util

import android.os.Environment
import org.literacybridge.tbloaderandroid.database.ProgramContentEntity
import org.literacybridge.core.tbloader.TBLoaderConstants
import java.io.File


/**
 * The Deployments are stored locally in in a directory structure like this:
 *
 * {externalStorageDirectory}/TalkingBookLoader/{project}/content/{Deployment name}/basic/{image}.img
 * {externalStorageDirectory}/TalkingBookLoader/{project}/content/{Deployment name}/communities/{... community names ...}/...
 * {externalStorageDirectory}/TalkingBookLoader/{project}/content/{Deployment name}/images/{... image names ...}/...
 */
object PathsProvider {
    private val contentDirectory: File
        /**
         * Gets a File object that represents a directory containing all projects with their downloaded
         * deployments on the device external storage.
         * Usually /storage/emulated/0/TalkingBookLoader
         *
         * @return the directory's File.
         */
        get() = Environment.getExternalStorageDirectory().resolve("TalkingBookLoader")

    /**
     * Gets the {externalStorageDirectory}/TalkingBookLoader/{project} directory for a specific project.
     *
     * @param project The name of the project.
     * @return the project's directory's File.
     */
    fun getProjectDirectory(project: String): File {
        if(!contentDirectory.exists()){
            contentDirectory.mkdirs()
        }

        val dir = File(contentDirectory, project)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    val locationsCacheDirectory: File
        /**
         * Gets a File object that represents a directory containing cached location information.
         *
         * @return the directory's File.
         */
        get() = contentDirectory.resolve("locations")

    /**
     * Gets the {externalFilesDirectory}/{project}/content/{Deployment name} directory
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
        get() = contentDirectory.resolve("temp")

    val uploadDirectory: File?
        /**
         * Gets a File object that represents a directory into which files intended to be uploaded
         * to a LB server should be placed. Think of this as a staging directory to the cloud.
         *
         * @return the staging directory's File.
         */
        get() = contentDirectory.resolve("upload")

    val logDirectory: File
        /**
         * Gets a File object that represents a directory into which log files should be placed. The
         * expectation is that they will eventually be moved to the "upload" directory.
         *
         * @return the staging directory's File.
         */
        get() = contentDirectory.resolve("log")

    val srnDirectory: File?
        /**
         * Gets a File object that represents a directory into which srn files should be placed. The
         * expectation is that they will eventually be moved to the "upload" directory.
         *
         * @return the staging directory's File.
         */
        get() = contentDirectory.resolve("srn")

//    fun moveDirectory(src: Path, dest: Path): Boolean {
//        return try {
//            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING)
//            true
//        } catch (e: IOException) {
//            Log.d("$LOG_TAG funMoveDirectory:", e.stackTraceToString())
//            false
//        }
//    }
}
