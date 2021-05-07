package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    File structure.
        ~/Dropbox (Amplio)
            ACM-${programid             # various config files for the program
                TB-Loaders              # bat files to run TB-Loader
                    TB_Options
                        activeLists
                        basic
                        config_files
                        firmware
                        languages
                        system_menus
                    communities
                    metadata            # Partial metadata snapshot (This is incomplete)
                    packages            # "Packages" are exported here. These could be cleaned up immediately.
                    published           # The published deployments go here
                content                 # Deeply nested directory
                programspec             # Currently active program spec is cached here

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
    
    private final File programHomeDir;

    private final String programName;
    private final String acmDbDirName;
    private final boolean usesDropbox;

    PathsProvider(String acmDbDirName, boolean usesDropbox) {
        programName = ACMConfiguration.cannonicalProjectName(acmDbDirName);
        acmDbDirName = usesDropbox ? ACMConfiguration.cannonicalAcmDirectoryName(acmDbDirName) : acmDbDirName;
        this.acmDbDirName = acmDbDirName;
        this.usesDropbox = usesDropbox;

        if (usesDropbox) {
            programHomeDir = new File(AmplioHome.getDropboxDir(), acmDbDirName);
        } else {
            programHomeDir = new File(AmplioHome.getHomeDbsRootDir(), acmDbDirName);
        }
        //noinspection ResultOfMethodCallIgnored
        programHomeDir.mkdirs();
    }

    /**************************************************************************
     * Directories and files of the ACM Database. These are either in dropbox
     * or are synced with S3.
     * These files are shared and synchronized among all users.
     *
     * Wherever you see only ~/Amplio, it might also be ~/LiteracyBridge, and
     * only ~/Amplio/acm-dbs might also be ~/Dropbox
     **************************************************************************/

    /**
     *
     * @return ~/Dropbox/ACM-${programId} or ~/Amplio/acm-dbs/${programId}
     */
    public File getProgramHomeDir() {
        return programHomeDir;
    }

    /**
     * Is this database held in Dropbox?
     * @return true if so.
     */
    public boolean isDropboxDb() {
        return usesDropbox;
    }

    /**
     * @return ${programId} (if in Amplio) or ACM-${programId} (if in Dropbox)
     */
    public String getProgramDirName() {
        return programHomeDir.getName();
    }

    /**
     * Get the program configuration file, config.properties.
     * @return ${programDbDir}/config.properties
     */
    public File getProgramConfigFile() {
        return new File(programHomeDir, Constants.CONFIG_PROPERTIES);
    }

    /**
     * The directory with audio content.
     * @return ~/Dropbox/ACM-${programId}/content or ~/Amplio/acm-dbs/${programId}/content
     */
    public File getProgramContentDir() {
        return new File(programHomeDir, Constants.RepositoryHomeDir);
    }

    /**
     * The directory with TB-Loaders.
     * @return ~/Dropbox/ACM-${programId}/TB-Loaders or ~/Amplio/acm-dbs/${programId}
     */
    public File getProgramTbLoadersDir() {
        return new File(programHomeDir, Constants.TBLoadersHomeDir);
    }

    /**
     * The directory with the Program specification
     * @return ~/Amplio/acm-dbs/${programId}/programspec
     */
    public File getProgramSpecDir() {
        return new File(programHomeDir, Constants.ProgramSpecDir);
    }

    /**
     * The (optional) file with a list of deployments for which user feedback is processed.
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
    public String getProgramName() {
        return programName;
    }

    /**************************************************************************
     * Local directories and files of the ACM and TB-Loader applications.
     * These files are local to the local computer only. If they're lost they
     * can be recreated.
     **************************************************************************/

    /**
     * The local directory for building a new TB-Loader (Deployment), or expanding a TB-Loader.
     *
     * @return ~/Amplio/TB-Loaders/${programId}
     */
    File getLocalTbLoaderDir() {
        return new File(AmplioHome.getLocalTbLoadersDir(), programName);
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName} (may include ACM-, for Dropbox programs)
     */
    File getLocalAcmTempDir() {
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
        return new File(getLocalAcmTempDir(), Constants.DBHomeDir);
    }

    /**
     * @return ~/Amplio/ACM/cache/${acmDbDirName}
     */
    File getLocalAcmCacheDir() {
        return new File(AmplioHome.getCachesDir(), acmDbDirName);
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName}/content (acmDbDirName may include ACM-, for Dropbox programs)
     */
    File getLocalAcmSandboxDir() {
        return new File(getLocalAcmTempDir(), Constants.RepositoryHomeDir);
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName}.lock (acmDbDirName may include ACM-. for Dropbox programs0
     */
    public File getLocalLockFile() {
        return new File(AmplioHome.getTempsDir(), acmDbDirName + ".lock");
    }

    /**
     * @return ~/Amplio/ACM/temp/${acmDbDirName}-checkedout.properties (acmDbDirName may include ACM-. for Dropbox programs0
     */
    public File getLocalCheckoutFile() {
        return new File(AmplioHome.getTempsDir(), acmDbDirName + Constants.CHECKOUT_PROPERTIES_SUFFIX);
    }

}
