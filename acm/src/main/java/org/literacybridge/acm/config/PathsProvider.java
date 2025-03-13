package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    File structure of a program.
        ~/Amplio
            acm-dbs
                ${programid}
            TB-Loaders                  # Per program directory of current deployment data
            ACM
                software                # acm.jar, lib, FFMPEG, and a ton of junk
                cache                   # Intended for .wav files
                temp                    # Intended for things not to be written
            collectiondir               # Statistics and UF is collected here
            uploadqueue                 # When ready to be uploaded to S3, files are moved here.


 */
public class PathsProvider {
    private static final Logger LOG = Logger.getLogger(PathsProvider.class.getName());

    // ~/LiteracyBridge/ACM-${programid} or ~/Amplio/acm-dbs/${programid}
    private final File programHomeDir;

    private final String programId;
    private final String acmDbDirName;

    PathsProvider(String acmDbDirName) {
        programId = ACMConfiguration.cannonicalProjectName(acmDbDirName);
        acmDbDirName = acmDbDirName;
        assert (acmDbDirName != null);
        this.acmDbDirName = acmDbDirName;

        programHomeDir = new File(AmplioHome.getHomeDbsRootDir(), acmDbDirName);
        //noinspection ResultOfMethodCallIgnored
        programHomeDir.mkdirs();
    }

    /* ************************************************************************
     * Directories and files of the ACM Database. These are synced with S3.
     * These files are shared and synchronized among all users.
     * ************************************************************************/

    /**
     * Gets the home (or root) directory for the given program.
     *
     * @return ~/Amplio/acm-dbs/${programId}
     */
    public File getProgramHomeDir() {
        return programHomeDir;
    }

    /**
     * @return ${programId}
     */
    public String getProgramDirName() {
        return programHomeDir.getName();
    }

    /**
     * Get the program configuration file, config.properties.
     *
     * @return ${programDbDir}/config.properties
     */
    public File getProgramConfigFile() {
        return new File(programHomeDir, Constants.CONFIG_PROPERTIES);
    }

    /**
     * The directory with audio content.
     *
     * @return ~/Amplio/acm-dbs/${programId}/content
     */
    public File getProgramContentDir() {
        return new File(programHomeDir, Constants.RepositoryHomeDir);
    }

    /**
     * The directory with TB-Loaders.
     *
     * @return ~/Amplio/acm-dbs/${programId}/TB-Loaders
     */
    public File getProgramTbLoadersDir() {
        return new File(programHomeDir, Constants.TBLoadersHomeDir);
    }

    /**
     * The directory with the Program specification
     *
     * @return ~/Amplio/acm-dbs/${programId}/programspec
     */
    public File getProgramSpecDir() {
        return new File(programHomeDir, Constants.ProgramSpecDir);
    }

    /**
     * The (optional) file with a list of deployments for which user feedback is processed.
     *
     * @return ~/Amplio/acm-dbs/${programId}/userfeedback.includelist or null
     */
    public File getProgramUserFeedbackInclusionFile() {
        File file = new File(programHomeDir, Constants.USER_FEEDBACK_INCLUDELIST_FILENAME);
        if (file.exists() && file.isFile()) return file;
        return null;
    }

    /**
     * @return the program id.
     */
    public String getProgramId() {
        return programId;
    }

    /* ************************************************************************
     * Local directories and files of the ACM and TB-Loader applications.
     * These files are local to the local computer only. If they're lost they
     * can be recreated.
     **************************************************************************/

    /**
     * The local directory for building a new TB-Loader (Deployment), or expanding a TB-Loader.
     *
     * @return ~/Amplio/TB-Loaders/${programId}
     */
    public File getLocalTbLoaderDir() {
        return new File(AmplioHome.getLocalTbLoadersDir(), programId);
    }

    /**
     * The local directory for keeping track of Talking Books deployed and collected.
     *
     * @return ~/Amplio/TB-History/${programId}
     */
    public File getLocalTbLoaderHistoryDir() {
        return new File(AmplioHome.getTbLoaderHistoriesDir(), programId);
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName}
     */
    File getLocalProgramTempDir() {
        File dir = new File(AmplioHome.getTempsDir(), acmDbDirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                LOG.log(Level.SEVERE, String.format("Unable to create directory '%s'", acmDbDirName));
            }
        }
        return dir;
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName}/db
     */
    File getLocalTempDbDir() {
        return new File(getLocalProgramTempDir(), Constants.DBHomeDir);
    }

    /**
     * @return ~/Amplio/ACM/cache/${acmDbDirName}
     */
    File getLocalAcmCacheDir() {
        return new File(AmplioHome.getCachesDir(), acmDbDirName);
    }

    File getSandboxDir() {
        return new File(AmplioHome.getSandboxesDir(), programId);
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName}.lock
     */
    public File getLocalLockFile() {
        return new File(AmplioHome.getTempsDir(), acmDbDirName + ".lock");
    }

    /**
     * Returns/creates a lock file in the temp directory.
     *
     * @param name Name of the lock file.
     * @return ~/Amplio/ACM/temp/${name}.lock
     */
    static public File getLocalLockFile(String name, Boolean create) {
        try {
            File f = new File(AmplioHome.getTempsDir(), name + ".lock");
            if (create) {
                if (!f.exists()) f.createNewFile();
                return f;
            }
            return f;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName}-checkedout.properties
     */
    public File getLocalCheckoutFile() {
        return new File(AmplioHome.getTempsDir(), acmDbDirName + Constants.CHECKOUT_PROPERTIES_SUFFIX);
    }

}
