package org.literacybridge.core.fs;

import java.io.File;

/**
 * Created by bill on 12/14/16.
 */

public interface TbLoaderPathProvider {
    /**
     * Gets a File object that represents the root of a Talking Book.
     * @return the Talking Book's root directory's File.
     */
    File getTbDevice();

    /**
     * Gets a File object that represents a directory containing downloaded Deployments.
     * @return the directory's File.
     */
    File getLocalContentRepository();

    /**
     * Gets a File object that represents a temporary directory into which files from
     * the Talking Book can be copied (or zipped).
     * @return the temporary directory's File.
     */
    File getLocalTempDirectory();

    /**
     * Gets a File object that represents a directory into which files intended to be uploaded
     * to a LB server should be placed. Think of this as a staging directory to the cloud.
     * @return the staging directory's File.
     */
    File getUploadDirectory();
}
