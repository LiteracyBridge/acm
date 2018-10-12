package org.literacybridge.androidtbloader.util;

import android.os.Environment;
import android.util.Log;

import org.literacybridge.androidtbloader.TBLoaderAppContext;

import java.io.File;

/**
 * The Deployments are stored locallin in a directory structure like this:
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/basic/{image}.img
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/communities/{... community names ...}/...
 * {externalFilesDirectory}/localrepository/{project}/content/{Deployment name}/images/{... image names ...}/...
 */

public class PathsProvider {
    private static final String TAG = "TBL!:" + "PathsProvider";

    private static TBLoaderAppContext sTbLoaderAppContext;
    public static void init(TBLoaderAppContext applicationContext) {
        sTbLoaderAppContext = applicationContext;
    }

    /**
     * Gets a File object that represents a directory containing all projects with their downloaded
     * Deployments.
     * @return the directory's File.
     */
    public static File getLocalContentDirectory() {
        return sTbLoaderAppContext.getExternalFilesDir("localrepository");
    }

    /**
     * Gets the {externalFilesDirectory}/localrepository/{project} directory for a specific project.
     * @param project The name of the project.
     * @return the project's directory's File.
     */
    public static File getLocalContentProjectDirectory(String project) {
        return new File(sTbLoaderAppContext.getExternalFilesDir("localrepository"), project);
    }

    /**
     * Gets a File object that represents a directory containing cached location information.
     * @return the directory's File.
     */
    public static File getLocationsCacheDirectory() {
        return sTbLoaderAppContext.getExternalFilesDir("locations");
    }

    /**
     * Gets the {externalFilesDirectory}/localrepository/{project}/content/{Deployment name} directory
     * for a specific project. There should be only one {Deployment name}, and this returns null if
     * there is not exactly one.
     * @param project The desired project.
     * @return The Deployment directory for the project.
     */
    public static File getLocalDeploymentDirectory(String project) {
        File projectDir = getLocalContentProjectDirectory(project);
        File contentDir = new File(projectDir, "content");
        File [] deployments = null;
        if (contentDir.exists() && contentDir.isDirectory()) {
            deployments = contentDir.listFiles();
            if (deployments != null && deployments.length == 1) {
                return deployments[0];
            }
        }
        return null;
    }

    public static File getProgramSpecDir(String project) {
        File deploymentDir = getLocalDeploymentDirectory(project);
        return new File(deploymentDir, "programspec");
    }

    /**
     * Gets a File object that represents a temporary directory into which files from
     * the Talking Book can be copied (or zipped).
     * @return the temporary directory's File.
     */
    public static File getLocalTempDirectory() {
        return sTbLoaderAppContext.getExternalCacheDir();
    }

    /**
     * Gets a File object that represents a directory into which files intended to be uploaded
     * to a LB server should be placed. Think of this as a staging directory to the cloud.
     * @return the staging directory's File.
     */
    public static File getUploadDirectory() {
        return sTbLoaderAppContext.getExternalFilesDir("upload");
    }

    /**
     * Gets a File object that represents a directory into which log files should be placed. The
     * expectation is that they will eventually be moved to the "upload" directory.
     * @return the staging directory's File.
     */
    public static File getLogDirectory() {
        return sTbLoaderAppContext.getExternalFilesDir("log");
    }

    /**
     * Gets a File object that represents a directory into which srn files should be placed. The
     * expectation is that they will eventually be moved to the "upload" directory.
     * @return the staging directory's File.
     */
    public static File getSrnDirectory() {
        return sTbLoaderAppContext.getExternalFilesDir("srn");
    }

}
