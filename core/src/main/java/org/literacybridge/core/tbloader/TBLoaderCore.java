package org.literacybridge.core.tbloader;

import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.fs.ZipUnzip;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;
import static org.literacybridge.core.fs.TbFile.Flags.append;
import static org.literacybridge.core.fs.TbFile.Flags.contentRecursive;
import static org.literacybridge.core.fs.TbFile.Flags.nil;
import static org.literacybridge.core.fs.TbFile.Flags.recursive;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.checkDisk;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearFeedbackCategories;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearStats;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearSystem;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearUserRecordings;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.copyStatsAndFiles;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.finishing;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.gatherDeviceFiles;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.gatherUserRecordings;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.listDeviceFiles;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.listDeviceFiles2;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.reformatting;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.relabelling;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.starting;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateCommunity;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateContent;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateSystem;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR;


/**
 * Implementation of the core of the TB-Loader. This code
 * - copies files FROM the talking book, with statistics, signin recordings, and files (for
 * diagnostic purposes). These files are copied to "tbcd{tbid}/collected-data"/{project}/...
 * with this directory structure:
 * TEST
 * ├── OperationalData
 * │   └── 000C
 * │       └── tbData
 * │           └── tbData-v03-2016y12m20d-000C.csv
 * ├── TalkingBookData
 * │   └── TEST-2016-1
 * │       └── 000C
 * │           └── demo-Seattle
 * │               └── B-000C0300
 * │                   └── 2016y12m20d08h48m37s-000C.zip
 * └── UserRecordings
 * └── TEST-2016-1
 * └── 000C
 * └── demo-Seattle
 * ├── B-000C0300_9-0_071631C0.a18
 * ├── B-000C0300_9-0_4E1F2A05.a18
 * ├── B-000C0300_9-0_8125F26A.a18
 * └── B-000C0300_9-0_C9061DA6.a18
 * <p>
 * The .zip file is a copy of log files, statistics files, and files from the Talking Book's system
 * directory, structured like this:
 * <p>
 * 2016y12m20d08h48m37s-000C.zip
 * └── 2016y12m20d08h48m37s-000C
 *    ├── 10.a18
 *    ├── dir.txt
 *    ├── log
 *    │   └── log.txt
 *    ├── log-archive
 *    │   ├── log_B-000C0300_0000_0001.txt
 * . . . more log files
 *    │   └── log_B-000C0300_0005_0001.txt
 *    ├── statistics
 *    │   ├── ACM-TEST-2016-1-EN^B-000C0300_071631C0.stat
 * . . . more stat files
 *    │   └── log_B-000C0300_0005_0001.txt
 *    │   ├── ACM-TEST-2016-1-EN^a-265AC9DE_8BF48F08.stat
 *    │   ├── B-000C0300.csv
 *    │   ├── B-000C0300_016.vol
 *    │   ├── SN.csv
 *    │   └── flashData.bin
 *    └── system
 *        ├── ACM-TEST-2016-1-en.pkg
 *        ├── B-000C0300.srn
 * . . . more files from system directory (but NOT *.img files)
 *        ├── r1215.rev
 *        ├── sig.grp
 *        └── sysdata.txt
 * <p>
 * <p>
 * - copies files TO the Talking Book. The files are copied from a deployment directory, with
 * this a structure like:
 * content
 * └── TEST-2016-1
 * ├── basic
 * │   └── r1215.img
 * ├── communities
 * │   ├── QONOS
 * . . . more communities
 * │   └── demo-Seattle
 * │       ├── languages
 * . . . files for each community
 * └── images
 * └── ACM-TEST-2016-1-en
 * ├── Inbox
 * ├── languages
 * │   └── en
 * │       ├── $0-1.txt
 * . . . system messages
 * │       ├── 9.a18
 * │       ├── cat
 * │       │   ├── $0-1.a18
 * . category prompts
 * │       │   └── i9-0.a18
 * │       ├── control.txt
 * │       └── intro.a18
 * . . . more languages as required
 * ├── log
 * ├── log-archive
 * ├── messages
 * │   ├── audio
 * │   │   ├── ECH_0236_63ED86F0.a18
 * . . . audio content
 * │   │   └── b-30373732_EB084B98.a18
 * │   └── lists
 * │       └── 1
 * │           ├── 1-0.txt
 * │           ├── 1-2.txt
 * │           ├── 2-0.txt
 * │           ├── 9-0.txt
 * │           └── _activeLists.txt
 * ├── statistics
 * └── system
 * ├── ACM-TEST-2016-1-en.pkg
 * ├── config.txt
 * ├── default.grp
 * └── profiles.txt
 * <p>
 * Note that Inbox, log, log-archive, and statistics are empty directories, just to create the targets.
 * <p>
 * - creates new files on the Talking book.
 */


/**
 * Helper class to make building a TBLoaderCore easier.
 */
public class TBLoaderCore {
    private static final Logger LOG = Logger.getLogger(TBLoaderCore.class.getName());

    public static class Builder {
        private TBLoaderConfig tbLoaderConfig;
        private TBDeviceInfo tbDeviceInfo;
        private DeploymentInfo oldDeploymentInfo;
        private DeploymentInfo newDeploymentInfo;
        private TbFile deploymentDirectory;
        private String location;
        private ProgressListener progressListenerListener;
        private boolean statsOnly = false;
        private boolean refreshFirmware = false;

        public Builder() {}

        public TBLoaderCore build() {
            List<String> missing = new ArrayList<>();

            if (tbLoaderConfig == null) missing.add("tbLoaderConfig");
            if (tbDeviceInfo == null) missing.add("tbDeviceInfo");
            if (oldDeploymentInfo == null) missing.add("oldDeploymentInfo");
            if (newDeploymentInfo == null) missing.add("newDeploymentInfo");
            if (deploymentDirectory == null) missing.add("deploymentDirectory");
            if (location == null) missing.add("location");
            if (progressListenerListener == null) missing.add("progressListenerListener");
            if (!missing.isEmpty()) {
                throw new IllegalStateException("TBLoaderConfig not initialized with " + missing.toString());
            }
            return new TBLoaderCore(tbLoaderConfig,
                    tbDeviceInfo,
                    oldDeploymentInfo,
                    newDeploymentInfo,
                    location,
                    statsOnly,
                    refreshFirmware,
                    deploymentDirectory,
                    progressListenerListener);
        }

        public Builder withTbLoaderConfig(TBLoaderConfig tbLoaderConfig) {
            this.tbLoaderConfig = tbLoaderConfig;
            return this;
        }

        public Builder withTbDeviceInfo(TBDeviceInfo tbDeviceInfo) {
            this.tbDeviceInfo = tbDeviceInfo;
            return this;
        }

        public Builder withNewDeploymentInfo(DeploymentInfo newDeploymentInfo) {
            this.newDeploymentInfo = newDeploymentInfo;
            return this;
        }

        public Builder withOldDeploymentInfo(DeploymentInfo oldDeploymentInfo) {
            this.oldDeploymentInfo = oldDeploymentInfo;
            return this;
        }

        public Builder withDeploymentDirectory(TbFile deploymentDirectory) {
            this.deploymentDirectory = deploymentDirectory;
            return this;
        }

        public Builder withLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder withProgressListener(ProgressListener progressListenerListener) {
            this.progressListenerListener = progressListenerListener;
            return this;
        }

        public Builder withStatsOnly(boolean statsOnly) {
            this.statsOnly = statsOnly;
            return this;
        }

        public Builder withRefreshFirmware(boolean refreshFirmware) {
            this.refreshFirmware = refreshFirmware;
            return this;
        }


    }


    private final TBLoaderConfig tbLoaderConfig;
    private final TBDeviceInfo tbDeviceInfo;

    private DeploymentInfo mOldDeploymentInfo;
    private DeploymentInfo mNewDeploymentInfo;
    private String mLocation;
    private boolean mGrabStatsOnly;
    private boolean mForceFirmware;
    private TbFile mDeploymentDirectory;
    private ProgressListener mProgressListenerListener;

    // For tracking the progress of individual update steps.
    private long mStepStartTime;
    private int mStepFileCount;
    private TbFile.CopyProgress mCopyListener;

    private TbFlashData tbFlashData;

    private RelativePath mTalkingBookDataDirectoryPath;
    private RelativePath mTalkingBookDataZipPath;

    private final TbFile collectedDataDirectory;
    private final String updateTimestamp;
    private final String collectionTempName;

    private boolean tbHasDiskCorruption = false;
    private TbFile talkingBookRoot;
    private TbFile tempDirectory;
    private TbFile talkingBookDataRoot;
    private long mUpdateStartTime;

    @Deprecated
    public TBLoaderCore(TBLoaderConfig tbLoaderConfig, TBDeviceInfo tbDeviceInfo) {
        this.tbDeviceInfo = tbDeviceInfo;
        this.tbLoaderConfig = tbLoaderConfig;
        this.tbFlashData = tbDeviceInfo.getFlashData();

        // Like "tbcd1234/collected-data"
        collectedDataDirectory = tbLoaderConfig.getCollectedDataDirectory();

        // Roughly when an update starts.
        updateTimestamp = TBLoaderUtils.getDateTime();
        // Like 2016y12m25d01h23m45s-000c. Also known as the "synch" directory.
        collectionTempName = updateTimestamp + "-" + tbLoaderConfig.getTbLoaderId();
    }

    private TBLoaderCore(TBLoaderConfig tbLoaderConfig,
            TBDeviceInfo tbDeviceInfo,
            DeploymentInfo oldDeploymentInfo,
            DeploymentInfo newDeploymentInfo,
            String location,
            boolean grabStatsOnly,
            boolean forceFirmware,
            TbFile deploymentDirectory,
            ProgressListener progressListenerListener) {
        this.tbDeviceInfo = tbDeviceInfo;
        this.tbLoaderConfig = tbLoaderConfig;
        this.tbFlashData = tbDeviceInfo.getFlashData();

        // Like "tbcd1234/collected-data"
        collectedDataDirectory = tbLoaderConfig.getCollectedDataDirectory();

        // Roughly when an update starts.
        updateTimestamp = TBLoaderUtils.getDateTime();
        // Like 2016y12m25d01h23m45s-000c. Also known as the "synch" directory.
        collectionTempName = updateTimestamp + "-" + tbLoaderConfig.getTbLoaderId();

        mOldDeploymentInfo = oldDeploymentInfo;
        mNewDeploymentInfo = newDeploymentInfo;
        mLocation = location;
        mGrabStatsOnly = grabStatsOnly;
        mForceFirmware = forceFirmware;
        mDeploymentDirectory = deploymentDirectory;
        mProgressListenerListener = progressListenerListener;

        // This is the path name for the "TalkingBookData" from this TB. In particular, this is the path
        // name for the directory that will contain the collected data, and then the .zip file of that data.
        // like TalkingBookData/{content update name}/{tbloader id}/{community name}/{tb serial no}
        RelativePath talkingBookDataParentPath = new RelativePath(
                TBLoaderConstants.TB_DATA_PATH,           // "TalkingBookData"
                mOldDeploymentInfo.getDeploymentName(),   // like "DEMO-2016-1"
                tbLoaderConfig.getTbLoaderId(),           // like "000c"
                mOldDeploymentInfo.getCommunity(),        // like "demo-seattle"
                getTbDeviceInfo().getSerialNumber());     // like "B-000C1234"
        // like TalkingBookData/{content update name}/{tbloader id}/{community name}/{tb serial no}/{timestamp}-{tbloader id}
        // like "2016y12m25d01h23m45s-000c"
        mTalkingBookDataDirectoryPath = new RelativePath(
                talkingBookDataParentPath,
                collectionTempName);
        // like "2016y12m25d01h23m45s-000c"
        mTalkingBookDataZipPath = new RelativePath(
                talkingBookDataParentPath,
                collectionTempName + ".zip");


    }


    public TBLoaderConfig getTbLoaderConfig() {
        return tbLoaderConfig;
    }

    public TBDeviceInfo getTbDeviceInfo() {
        return tbDeviceInfo;
    }

    private void logTBData(String action,
            DeploymentInfo oldDeployment,
            DeploymentInfo newDeployment,
            String location,
            int durationSeconds) {

        final String VERSION_TBDATA = "v03";
        BufferedWriter bw;

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy'y'MM'm'dd'd'");
        String strDate = sdfDate.format(new Date());
        String filename = String.format("tbData-%s-%s-%s.csv",
                VERSION_TBDATA,
                strDate,
                tbLoaderConfig.getTbLoaderId());

        TbFile logFile = collectedDataDirectory         // like /Users/alice/Dropbox/tbcd000c
                .open(tbLoaderConfig.getProject())      // {tbloaderConfig.project}
                .open("OperationalData")                // "OperationalData"
                .open(tbLoaderConfig.getTbLoaderId())   // {tbloaderConfig.tbLoaderId}
                .open("tbData")                         // "tbData"
                .open(filename);                        // like "tbData-v03-2016y12m25d-000c"


        try {
            logFile.getParent().mkdirs();
            boolean isNewFile = !logFile.exists();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(baos));
            if (bw != null) {
                if (isNewFile) {
                    bw.write(
                            "PROJECT,UPDATE_DATE_TIME,OUT_SYNCH_DIR,LOCATION,ACTION,DURATION_SEC,");
                    bw.write(
                            "OUT-SN,OUT-DEPLOYMENT,OUT-IMAGE,OUT-FW-REV,OUT-COMMUNITY,OUT-ROTATION-DATE,");
                    bw.write(
                            "IN-SN,IN-DEPLOYMENT,IN-IMAGE,IN-FW-REV,IN-COMMUNITY,IN-LAST-UPDATED,IN-SYNCH-DIR,IN-DISK-LABEL,CHKDSK CORRUPTION?,");
                    bw.write("FLASH-SN,FLASH-REFLASHES,");
                    bw.write(
                            "FLASH-DEPLOYMENT,FLASH-IMAGE,FLASH-COMMUNITY,FLASH-LAST-UPDATED,FLASH-CUM-DAYS,FLASH-CORRUPTION-DAY,FLASH-VOLT,FLASH-POWERUPS,FLASH-PERIODS,FLASH-ROTATIONS,");
                    bw.write(
                            "FLASH-MSGS,FLASH-MINUTES,FLASH-STARTS,FLASH-PARTIAL,FLASH-HALF,FLASH-MOST,FLASH-ALL,FLASH-APPLIED,FLASH-USELESS");
                    for (int i = 0; i < 5; i++) {
                        bw.write(
                                ",FLASH-ROTATION,FLASH-MINUTES-R" + i + ",FLASH-PERIOD-R" + i
                                        + ",FLASH-HRS-POST-UPDATE-R" + i + ",FLASH-VOLT-R" + i);
                    }
                    bw.write("\n");
                }
                bw.write(tbLoaderConfig.getProject().toUpperCase() + ",");
                bw.write(updateTimestamp.toUpperCase() + ",");
                bw.write(updateTimestamp.toUpperCase() + "-" + tbLoaderConfig.getTbLoaderId()
                        .toUpperCase() + ",");
                bw.write(location.toUpperCase() + ",");
                bw.write(action + ",");
                bw.write(Integer.toString(durationSeconds) + ",");
                bw.write(tbDeviceInfo.getSerialNumber().toUpperCase() + ",");
                bw.write(newDeployment.getDeploymentName().toUpperCase() + ",");
                bw.write(newDeployment.getPackageName().toUpperCase() + ",");
                bw.write(newDeployment.getFirmwareRevision() + ",");
                bw.write(newDeployment.getCommunity().toUpperCase() + ",");
                bw.write(newDeployment.getUpdateTimestamp() + ",");
                bw.write(oldDeployment.getSerialNumber().toUpperCase() + ",");
                bw.write(oldDeployment.getDeploymentName().toUpperCase() + ",");
                bw.write(oldDeployment.getPackageName().toUpperCase() + ",");
                bw.write(oldDeployment.getFirmwareRevision() + ",");
                bw.write(oldDeployment.getCommunity().toUpperCase() + ",");
                bw.write(oldDeployment.getUpdateTimestamp() + ",");
                String lastSynchDir = oldDeployment.getUpdateDirectory();
                bw.write((lastSynchDir != null ? lastSynchDir.toUpperCase() : "") + ",");
                bw.write(tbDeviceInfo.getLabel() + ",");
                bw.write(tbDeviceInfo.isCorrupted() + ",");

                if (tbFlashData != null) {
                    bw.write(tbFlashData.getSerialNumber().toUpperCase() + ",");
                    bw.write(tbFlashData.getCountReflashes() + ",");
                    bw.write(tbFlashData.getDeploymentNumber().toUpperCase() + ",");
                    bw.write(tbFlashData.getImageName().toUpperCase() + ",");
                    bw.write(tbFlashData.getLocation().toUpperCase() + ",");
                    bw.write(tbFlashData.getUpdateYear() + "/" + tbFlashData.getUpdateMonth() + "/"
                            + tbFlashData.getUpdateDate() + ",");
                    bw.write(tbFlashData.getCumulativeDays() + ",");
                    bw.write(tbFlashData.getCorruptionDay() + ",");
                    bw.write(tbFlashData.getLastInitVoltage() + ",");
                    bw.write(tbFlashData.getPowerups() + ",");
                    bw.write(tbFlashData.getPeriods() + ",");
                    bw.write(tbFlashData.getProfileTotalRotations() + ",");
                    bw.write(tbFlashData.getTotalMessages() + ",");
                    int totalSecondsPlayed = 0, countStarted = 0, countQuarter = 0, countHalf = 0, countThreequarters = 0, countCompleted = 0, countApplied = 0, countUseless = 0;
                    for (int m = 0; m < tbFlashData.getTotalMessages(); m++) {
                        for (int r = 0; r < (tbFlashData.getProfileTotalRotations() < 5 ?
                                tbFlashData.getProfileTotalRotations() :
                                5); r++) {
                            totalSecondsPlayed += tbFlashData.getStats()[m][r].getTotalSecondsPlayed();
                            countStarted += tbFlashData.getStats()[m][r].getCountStarted();
                            countQuarter += tbFlashData.getStats()[m][r].getCountQuarter();
                            countHalf += tbFlashData.getStats()[m][r].getCountHalf();
                            countThreequarters += tbFlashData.getStats()[m][r].getCountThreequarters();
                            countCompleted += tbFlashData.getStats()[m][r].getCountCompleted();
                            countApplied += tbFlashData.getStats()[m][r].getCountApplied();
                            countUseless += tbFlashData.getStats()[m][r].getCountUseless();
                        }
                    }
                    bw.write(totalSecondsPlayed / 60 + ",");
                    bw.write(countStarted + ",");
                    bw.write(countQuarter + ",");
                    bw.write(countHalf + ",");
                    bw.write(countThreequarters + ",");
                    bw.write(countCompleted + ",");
                    bw.write(countApplied + ",");
                    bw.write(String.valueOf(countUseless));
                    for (int r = 0; r < (tbFlashData.getProfileTotalRotations() < 5 ?
                            tbFlashData.getProfileTotalRotations() :
                            5); r++) {
                        bw.write(
                                "," + r + "," + tbFlashData.totalPlayedSecondsPerRotation(r) / 60
                                        + "," + tbFlashData.getRotations()[r].getStartingPeriod() + ",");
                        bw.write(tbFlashData.getRotations()[r].getHoursAfterLastUpdate() + ","
                                + tbFlashData.getRotations()[r].getInitVoltage());
                    }
                }
                bw.write("\n");
                bw.flush();
                bw.close();

                InputStream content = new ByteArrayInputStream(baos.toByteArray());
                TbFile.Flags flag = isNewFile ? nil : append;

                logFile.createNew(content, flag);
                content.close();
                baos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class Result {
        public final boolean success;
        public final boolean corrupted;
        public final boolean reformatFailed;
        public final boolean verified;
        public final String duration;

        private Result() {
            this.success = false;
            this.corrupted = false;
            this.reformatFailed = false;
            this.verified = false;
            this.duration = "";
        }

        private Result(long startTime,
                boolean success,
                boolean corrupted,
                boolean reformatFailed,
                boolean verified) {
            this.success = success;
            this.corrupted = corrupted;
            this.reformatFailed = reformatFailed;
            this.verified = verified;
            this.duration = getDuration(startTime); // Nice printed format
        }
    }

    /**
     * Perform the update. See above for definitions of the directory structures.
     *
     * @return a Result object
     */
    public Result update() {
        if (mOldDeploymentInfo == null) {
            mOldDeploymentInfo = new DeploymentInfo();
        }
        System.out.println("oldDeploymentInfo:\n" + mOldDeploymentInfo.toString());
        System.out.println("newDeploymentInfo:\n" + mNewDeploymentInfo.toString());
        System.out.println("location: " + mLocation);

        mProgressListenerListener.step(starting);

        mCopyListener = new TbFile.CopyProgress() {
            @Override
            public void copying(String filename) {
                mStepFileCount++;
                mProgressListenerListener.detail(filename);
            }
        };

        mUpdateStartTime = System.nanoTime();

        // Get the roots for the Talking Book, the Temp directory, and the Staging Directory.
        talkingBookRoot = tbDeviceInfo.getRootFile();
        tempDirectory = tbLoaderConfig.getTempDirectory();
        talkingBookDataRoot = tempDirectory.open(mTalkingBookDataDirectoryPath);
        talkingBookDataRoot.mkdirs();
        // mkdir "${syncpath}"
        // at the end, this gets zipped up into the copyTo (Dropbox dir)
        mProgressListenerListener.detail("Creating syncdirPath: " + mTalkingBookDataDirectoryPath);

        // Like tbcd1234/collected-data/XYZ
        TbFile projectCollectedData = collectedDataDirectory.open(mOldDeploymentInfo.getProjectName());
        LOG.log(Level.INFO, "copy stats To:" + projectCollectedData.toString());

        boolean gotStatistics = false;
        try {
            // Like {dropbox path} / tbcd1234/collected-data/XYZ
            projectCollectedData.mkdirs();
            if (!isTalkingBookStorageGood()) {
                return new Result();
            }

            listDeviceFiles();

            gatherDeviceFiles();

            gatherUserRecordings(projectCollectedData);

            clearStatistics();

            clearUserRecordings();

            clearFeedbackCategories();

            zipAndCopyFiles(projectCollectedData);

            gotStatistics = true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to get stats:", e);
            mProgressListenerListener.log("Unable to gather statistics");
            mProgressListenerListener.log(getStackTrace(e));
        }

        boolean verified = false;
        try {
            if (!mGrabStatsOnly) {
                Result x = reformatRelabel();
                if (x != null) return x;

                clearSystemFiles();

                updateSystemFiles();

                updateContent();

                updateCommunity();

                mProgressListenerListener.step(finishing);

                verified = verifyTalkingBook();

                forceFirmwareRefresh();

                listDeviceFilesPostUpdate();

            }

            writeTbLog(gotStatistics, verified);

            disconnectDevice();

        } catch (Throwable e) {
            LOG.log(Level.WARNING, "Unable to update Talking Book:", e);
            mProgressListenerListener.log("Unable to update Talking Book");
            mProgressListenerListener.log(getStackTrace(e));
        }

        if (gotStatistics && verified) {
            mProgressListenerListener.log("Update complete.");
        } else {
            mProgressListenerListener.log("Update failed.");
        }

        Result result = new Result(mUpdateStartTime,
                gotStatistics,
                tbHasDiskCorruption,
                false,
                verified);
        String completionMessage = String.format("TB-Loader updated in %s", result.duration);
        mProgressListenerListener.detail("");
        mProgressListenerListener.log(completionMessage);
        LOG.log(Level.INFO, completionMessage);
        return result;
    }

    /**
     * Tries to determine if the Talking Book storage is good. On Windows, runs chkdsk. On other OS,
     * just pokes around the file system.
     *
     * @return True if the Talking Book storage looks good.
     * @throws IOException
     */
    private boolean isTalkingBookStorageGood() throws IOException {
        boolean goodCard;
        // This is, sadly, broken. The old one (dos only) simply tried to do a "dir" commmand on the drive.
        // This one tries to look for a bunch of directories.
        // TODO: figure out something that sorta works.
        //goodCard = checkConnection(true);
        goodCard = true;
        if (!goodCard) {
            return false;
        }
        if (OSChecker.WINDOWS) {
            newStep(checkDisk);
            tbHasDiskCorruption = !CommandLineUtils.checkDisk(tbDeviceInfo.getRootFile()
                    .getAbsolutePath());
            if (tbHasDiskCorruption) {
                tbDeviceInfo.setCorrupted(true);
                mProgressListenerListener.log("Storage corrupted, attempting repair.");
                CommandLineUtils.checkDisk(tbDeviceInfo.getRootFile().getAbsolutePath(),
                        new RelativePath(mTalkingBookDataDirectoryPath,
                                "chkdsk-reformat.txt").asString());
                mProgressListenerListener.log(String.format(
                        "Attempted repair of Talking Book storage, %s",
                        getStepTime()));
            } else {
                mProgressListenerListener.log("Storage Good");
            }
        } else {
            mProgressListenerListener.log("Skipping chkdsk; not supported on this OS.");
        }
        return true;
    }

    /**
     * Helper class to keep track of counts of files, directories, and file sizes.
     */
    private static class DirectoryCount {
        long size;
        int files;
        int dirs;

        /**
         * Add another DirectoryCount to this one.
         *
         * @param childCount A DirectoryCount object from a child directory.
         */
        void add(DirectoryCount childCount) {
            this.size += childCount.size;
            this.files += childCount.files;
            this.dirs += childCount.dirs;
        }

        /**
         * Count one file or directory.
         *
         * @param file The file or directory to be counted.
         */
        void add(TbFile file) {
            if (file.isDirectory()) {
                this.dirs += 1;
            } else {
                this.size += file.length();
                this.files += 1;
            }
        }
    }

    /**
     * Formats one directory entry in much the same way as a DOS dir command.
     *
     * @param entry  The directory entry.
     * @param buffer String builder into which to place the entry.
     */
    private void listDirectoryEntry(TbFile entry, StringBuilder buffer) {
        Date date = new Date(entry.lastModified());
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy  hh:mm aa");
        String formattedDate = sdf.format(date);
        String dirOrSize;
        if (entry.isDirectory()) {
            dirOrSize = "    <DIR>          ";
        } else {
            dirOrSize = String.format("   %,15d ", entry.length());
        }
        buffer.append(formattedDate).append(dirOrSize).append(entry.getName()).append("\n");
    }

    /**
     * Lists the given directory. If there are child directories, lists them, too.
     *
     * @param directory The directory to list.
     * @param buffer    Build the listing here.
     * @return A DirectoryCount with counts for this directory and any children.
     */
    private DirectoryCount listDirectory(TbFile directory, StringBuilder buffer) {
        DirectoryCount myCount = new DirectoryCount();
        TbFile[] children = directory.listFiles();
        Arrays.sort(children, new Comparator<TbFile>() {
            @Override
            public int compare(TbFile o1, TbFile o2) {
                String n1 = o1.getName();
                String n2 = o2.getName();
                return n1.compareToIgnoreCase(n2);
            }
        });

        // Header for a directory listing.
        buffer.append(" Directory of ").append(directory.getAbsolutePath()).append("\n\n");
        // List entries of the directory.
        for (TbFile child : children) {
            myCount.add(child);
            listDirectoryEntry(child, buffer);
        }
        // Print the summary line
        buffer.append(String.format("%11c%5d File(s)%,15d bytes\n\n",
                ' ',
                myCount.files,
                myCount.size));

        // List the child directories.
        for (TbFile child : children) {
            if (child.isDirectory()) {
                myCount.add(listDirectory(child, buffer));
            }
        }

        return myCount;
    }

    public String getDeviceFileList() {
        StringBuilder builder = new StringBuilder();
        DirectoryCount counts = listDirectory(talkingBookRoot, builder);

        // Add the summary.
        builder.append(String.format("%5cTotal Files Listed:\n%1$10c%6d File(s)%,15d bytes\n",
                ' ',
                counts.files,
                counts.size));
        builder.append(String.format("%10c%6d Dir(s)", ' ', counts.dirs));
        long free = talkingBookRoot.getFreeSpace();
        if (free > 0) {
            builder.append(String.format(" %,15d bytes free", free));
        }
        builder.append("\n");
        return builder.toString();
    }

    /**
     * Lists all the files on the Talking Book, and writes the listing to a file named sysdata.txt.
     *
     * @throws IOException
     */
    private void listDeviceFiles() throws IOException {
        newStep(listDeviceFiles);

        // rem Capturing Full Directory
        // dir ${device_drive} /s > "${syncpath}\dir.txt"
        String fileList = getDeviceFileList();
        eraseAndOverwriteFile(talkingBookDataRoot.open(TBLoaderConstants.DIRS_TXT), fileList);
        mProgressListenerListener.log(String.format("Captured device file list, %s",
                getStepTime()));
    }

    /**
     * Lists all the files on the Talking Book, after the update. Note that this is written
     * to the temp directory, and not zipped and uploaded.
     *
     * The main purpose, sadly, is to get the Android implementation of a USB host to write out
     * the contents of the files.
     *
     * @throws IOException
     */
    private void listDeviceFilesPostUpdate() throws IOException {
        newStep(listDeviceFiles2);

        // rem Capturing Full Directory
        // dir ${device_drive} /s > "${syncpath}\dir.txt"
        String fileList = getDeviceFileList();
        eraseAndOverwriteFile(tempDirectory.open(TBLoaderConstants.DIRS_POST_TXT), fileList);
        mProgressListenerListener.log(String.format("Captured device file list, %s",
                getStepTime()));
    }

    /**
     * Copies a subset of files from the Talking Book. For logs, and for troubleshooting purposes.
     *
     * @throws IOException
     */
    private void gatherDeviceFiles() throws IOException {// And Copy from tbRoot to tempRoot, with filter.
        newStep(gatherDeviceFiles);
        // rem Collecting Usage Data
        // xcopy ${device_drive} "${syncpath}" /E /Y /EXCLUDE:software\scripts\exclude.txt /C
        //    /e : Copies all subdirectories, even if they are empty. Use /e with the /s and /t command-line options.
        //    /y : Suppresses prompting to confirm that you want to overwrite an existing destination file.
        //    /c : Ignores errors.
        // Filter to exclude certain named files or directories, anything starting with ".", or ending with ".img" or ".old".
        TbFile.CopyFilter copyFilesFilter = new TbFile.CopyFilter() {
            Set excludedNames = new HashSet<>(Arrays.asList("languages",
                    "messages",
                    "ostats",
                    "inbox",
                    "archive",
                    "android",
                    "config.bin",
                    "lost.dir",
                    "system volume information"));

            @Override
            public boolean accept(TbFile file) {
                String name = file.getName().toLowerCase();
                if (excludedNames.contains(name)) {
                    return false;
                }
                if (name.charAt(0) == '.') {
                    return false;
                }
                if (name.endsWith(".img") ||
                        name.endsWith(".old")) {
                    return false;
                }
                return true;
            }
        };

        TbFile.copyDir(talkingBookRoot, talkingBookDataRoot, copyFilesFilter, mCopyListener);
        mProgressListenerListener.log(String.format("Copied %d device files, %s",
                mStepFileCount,
                getStepTime()));
    }

    /**
     * Copy user recordings from the Talking Book to the collected data directory.
     *
     * @param projectCollectedData
     * @throws IOException
     */
    private void gatherUserRecordings(TbFile projectCollectedData) throws IOException {
        newStep(gatherUserRecordings);
        // rem Collecting User Recordings
        // mkdir "${recording_path}"
        // xcopy "${device_drive}\messages\audio\*_9_*.a18" "${recording_path}" /C
        // xcopy "${device_drive}\messages\audio\*_9-0_*.a18" "${recording_path}" /C

        // Build the user recordings source path.
        TbFile recordingsSrc = talkingBookRoot.open(TBLoaderConstants.TB_AUDIO_PATH);  // "messages/audio"
        // Build the user recordings destination path.
        TbFile recordingsDst = projectCollectedData           // like .../"tbcd1234/collected-data/UWR"
                .open(TBLoaderConstants.USER_RECORDINGS_PATH) // "UserRecordings"
                .open(mOldDeploymentInfo.getDeploymentName())  // like "UNICEF-2016-14"
                .open(tbLoaderConfig.getTbLoaderId())         // like "000C"
                .open(mOldDeploymentInfo.getCommunity());      // like "VING VING"

        TbFile.CopyFilter copyRecordingsFilter = new TbFile.CopyFilter() {
            @Override
            public boolean accept(TbFile file) {
                String name = file.getName();
                // match *_9_*.a18 or *_9-0_*.a18
                return name.matches("(?i).*(?:_9_|_9-0_).*\\.a18");
            }
        };
        if (recordingsSrc.exists()) {
            TbFile.copyDir(recordingsSrc, recordingsDst, copyRecordingsFilter, mCopyListener);
        }

        mProgressListenerListener.log(String.format("Copied %d user recordings, %s",
                mStepFileCount,
                getStepTime()));
    }

    /**
     * Erases files from the log, log-archive, and statistics directories on the Talking Book.
     */
    private void clearStatistics() {
        newStep(clearStats);

        // rem Deleting Usage Statistics
        // del ${device_drive}\log\*.* /Q
        // del ${device_drive}\log-archive\*.* /S /Q
        // del ${device_drive}\statistics\*.* /S /Q
        // del ${device_drive}\statistics\stats\*.* /S /Q
        //      /s : Deletes specified files from the current directory and all subdirectories. Displays the names of the files as they are being deleted.
        //      /q : Specifies quiet mode. You are not prompted for delete confirmation.
        talkingBookRoot.open("log").delete(contentRecursive);
        talkingBookRoot.open("log-archive").delete(contentRecursive);
        talkingBookRoot.open("statistics").delete(contentRecursive);

        mProgressListenerListener.log(String.format("Cleared statistics, %s", getStepTime()));
    }

    /**
     * Erases user recordings from the Talking Book.
     */
    private void clearUserRecordings() {
        newStep(clearUserRecordings);

        // rem Deleting User Recordings
        // del ${device_drive}\messages\audio\*_9_*.a18 /Q
        // del ${device_drive}\messages\audio\*_9-0_*.a18 /Q
        TbFile audioDirectory = talkingBookRoot.open(TBLoaderConstants.TB_AUDIO_PATH);
        String[] names = audioDirectory.list(new TbFile.FilenameFilter() {
            @Override
            public boolean accept(TbFile parent, String name) {
                // match *_9_*.a18 or *_9-0_*.a18
                return name.matches("(?i).*(?:_9_|_9-0_).*\\.a18");
            }
        });
        if (names != null) {
            for (String name : names) {
                audioDirectory.open(name).delete();
            }
        }

        mProgressListenerListener.log(String.format("Cleared user recordings, %s", getStepTime()));
    }

    /**
     * Deletes the ".txt" files that would have pointed to user recordings.
     */
    private void clearFeedbackCategories() {
        String[] names;
        mProgressListenerListener.step(clearFeedbackCategories);


        // rem Deleting User Feedback Category
        // for /f %f in ('dir ${device_drive}\messages\lists /b /AD') do del ${device_drive}\messages\lists\%f\9*.txt /Q
        TbFile listsDirectory = talkingBookRoot.open(TBLoaderConstants.TB_LISTS_PATH);
        names = listsDirectory.list();
        if (names != null) {
            for (String name : names) {
                TbFile listDirectory = listsDirectory.open(name);
                String[] toDelete = listDirectory.list(new TbFile.FilenameFilter() {
                    @Override
                    public boolean accept(TbFile parent, String name) {
                        return name.startsWith("9") && name.toLowerCase().endsWith(".txt");
                    }
                });
                if (toDelete != null) {
                    for (String d : toDelete) {
                        listDirectory.open(d).delete();
                    }
                }
            }
        }

        mProgressListenerListener.log(String.format("Cleared feedback categories, %s",
                getStepTime()));
    }

    /**
     * Zips the statistics, logs, and other files, and copies them to the collected data directory.
     *
     * @param projectCollectedData
     * @throws IOException
     */
    private void zipAndCopyFiles(TbFile projectCollectedData) throws IOException {
        newStep(copyStatsAndFiles);

        // Same name and location as the tempDirectory, but a file with a .zip extension.
        TbFile tempZip = talkingBookDataRoot.getParent().open(collectionTempName + ".zip");

        File sourceFilesDir = new File(talkingBookDataRoot.getAbsolutePath());
        File tempZipFile = new File(tempZip.getAbsolutePath());
        ZipUnzip.zip(sourceFilesDir, tempZipFile, true);

        // Maybe
        TbFile outputZip = projectCollectedData
                .open(mTalkingBookDataZipPath);

        outputZip.getParent().mkdirs();
        TbFile.copy(tempZip, outputZip);

        // Clean out everything we put in the temp directory. Any other cruft that was there, as well.
        tempDirectory.delete(contentRecursive);
        mProgressListenerListener.log(String.format("Copied statistics and files, %s",
                getStepTime()));
    }

    /**
     * Attempts to reformat the Talking Book if that's needed.
     * Attempts to relabel the Talking Book, if the label doesn't match the serial number.
     *
     * @return A Result if the TB needs reformatting, but can't be (ie, still needs reformatting.)
     * Returns null if format succeeded or not needed.
     * @throws IOException
     */
    private Result reformatRelabel() throws IOException {
        boolean goodCard;
        if (tbHasDiskCorruption) {
            mProgressListenerListener.step(reformatting);
            if (!OSChecker.WINDOWS) {
                // distinction without a difference... has corruption, reformat didn't fail because
                // no reformat was attempted.
                return new Result(0, false, true, false, false);
            }
            goodCard = CommandLineUtils.formatDisk(tbDeviceInfo.getRootFile().getAbsolutePath(),
                    tbDeviceInfo.getSerialNumber().toUpperCase());
            if (!goodCard) {
                mProgressListenerListener.log("Reformat failed");
                return new Result(0, false, true, true, false);
            } else {
                mProgressListenerListener.log(String.format("Reformatted card, %s", getStepTime()));
            }
        } else {
            if (!mNewDeploymentInfo.getSerialNumber()
                    .equalsIgnoreCase(
                            tbDeviceInfo.getLabelWithoutDriveLetter())) {
                if (!OSChecker.WINDOWS) {
                    mProgressListenerListener.log("Skipping relabeling; not supported on this OS.");
                } else {
                    mProgressListenerListener.step(relabelling);
                    CommandLineUtils.relabel(tbDeviceInfo.getRootFile().getAbsolutePath(),
                            mNewDeploymentInfo.getSerialNumber());
                }
            }
        }
        return null;
    }

    /**
     * Clears old files from / /system/, and /languages/.
     * <p>
     * Derived from the "update.txt" file.
     *
     * @throws IOException
     */
    private void clearSystemFiles() throws IOException {
        newStep(clearSystem);

        talkingBookRoot.open("archive").deleteDirectory();
        talkingBookRoot.open("LOST.DIR").deleteDirectory();

        // Delete files from /
        String[] names = talkingBookRoot.list(new TbFile.FilenameFilter() {
            @Override
            public boolean accept(TbFile parent, String name) {
                name = name.toLowerCase();
                return name.endsWith(".img") ||
                        name.endsWith(".rtc");
            }
        });
        if (names != null) {
            for (String name : names) {
                mProgressListenerListener.detail(name);
                talkingBookRoot.open(name).delete();
            }
        }

        // Delete files from /system
        TbFile system = talkingBookRoot.open("system");
        names = system.list(new TbFile.FilenameFilter() {
            @Override
            public boolean accept(TbFile parent, String name) {
                name = name.toLowerCase();
                return name.endsWith(".srn") ||
                        name.endsWith(".loc") ||
                        name.endsWith(".prj") ||
                        name.endsWith(".dep") ||
                        name.endsWith(".pkg");
            }
        });
        if (names != null) {
            for (String name : names) {
                mProgressListenerListener.detail(name);
                system.open(name).delete();
            }
        }

        // These two files will be rebuilt upon first boot of the Talking Book.
        mProgressListenerListener.detail("config.bin");
        TbFile toDelete = system.open("config.bin");
        if (toDelete.exists()) toDelete.delete();

        mProgressListenerListener.detail("control.bin");
        toDelete = talkingBookRoot.open(TBLoaderConstants.TB_LANGUAGES_PATH).open("control.bin");
        if (toDelete.exists()) toDelete.delete();


    }

    /**
     * Creates new system files.
     * <p>
     * Derived from the "update.txt" file.
     */
    private void updateSystemFiles() throws IOException {
        newStep(updateSystem);

        TbFile system = talkingBookRoot.open("system");

        // software\robocopy content\${new_deployment}\basic ${device_drive} /E /NP /XD .* /XA:H /XF *.srn *.rev
        //      /E    : Copy subdirectories, including Empty ones.
        //      /NP   : No ProgressListener - don't display percentage copied.
        //      /XD   : eXclude Directories matching given names/paths.
        //      /XA:H : eXclude files with any of the given Attributes set.
        //      /XF   : eXclude Files matching given names/paths/wildcards.
        TbFile.CopyFilter basicFilter = new TbFile.CopyFilter() {
            @Override
            public boolean accept(TbFile file) {
                String name = file.getName();
                return !name.endsWith(".srn") && !name.endsWith(".rev");
            }
        };
        TbFile.copyDir(mDeploymentDirectory.open(TBLoaderConstants.CONTENT_BASIC_SUBDIR),
                talkingBookRoot, basicFilter,
                mCopyListener);

        Calendar cal = Calendar.getInstance();
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        String dateInMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        String year = String.valueOf(cal.get(Calendar.YEAR));

        // The Talking Book is hard coded to expect the MS-DOS line ending.
        String MSDOS_LINE_ENDING = new String(new byte[] { 0x0d, 0x0a });

        String projectName = mNewDeploymentInfo.getProjectName().toUpperCase();
        String communityName = mNewDeploymentInfo.getCommunity().toUpperCase();
        String srn = mNewDeploymentInfo.getSerialNumber().toUpperCase();
        String deploymentName = mNewDeploymentInfo.getDeploymentName().toUpperCase();
        String packageName = mNewDeploymentInfo.getPackageName().toUpperCase(); // aka 'image'
        String sysDataTxt = String.format("SRN:%s%s", srn, MSDOS_LINE_ENDING) +
                String.format("IMAGE:%s%s", packageName, MSDOS_LINE_ENDING) +
                String.format("UPDATE:%s%s", deploymentName, MSDOS_LINE_ENDING) +
                String.format("LOCATION:%s%s", communityName, MSDOS_LINE_ENDING) +
                String.format("YEAR:%s%s", year, MSDOS_LINE_ENDING) +
                String.format("MONTH:%s%s", month, MSDOS_LINE_ENDING) +
                String.format("DATE:%s%s", dateInMonth, MSDOS_LINE_ENDING) +
                String.format("PROJECT:%s%s", projectName, MSDOS_LINE_ENDING);
        eraseAndOverwriteFile(talkingBookRoot.open(TBLoaderConstants.SYS_DATA_TXT), sysDataTxt);
        eraseAndOverwriteFile(talkingBookRoot.open("inspect"), ".");
        eraseAndOverwriteFile(talkingBookRoot.open("0h1m0s.rtc"), ".");

        talkingBookRoot.open("log").mkdir();
        talkingBookRoot.open("log-archive").mkdir();
        talkingBookRoot.open("Inbox").mkdir();
        talkingBookRoot.open("statistics").mkdir();

        system.mkdir();
        eraseAndOverwriteFile(system.open(srn + ".srn"), ".");
        eraseAndOverwriteFile(system.open(deploymentName + ".dep"), ".");
        eraseAndOverwriteFile(system.open(communityName + ".loc"), communityName);
        eraseAndOverwriteFile(system.open("last_updated.txt"), collectionTempName);
        eraseAndOverwriteFile(system.open(projectName + ".prj"), projectName);
        eraseAndOverwriteFile(system.open("notest.pcb"), ".");

        mProgressListenerListener.log(String.format("Updated TB files, %s", getStepTime()));
    }

    /**
     * Updates content files on the Talking Book.
     * <p>
     * Derived from the "community.txt" file.
     *
     * @throws IOException
     */
    private void updateContent() throws IOException {
        newStep(updateContent);

        TbFile imagePath = mDeploymentDirectory.open(IMAGES_SUBDIR)
                .open(mNewDeploymentInfo.getPackageName());
        if (imagePath.exists()) {
            TbFile.copyDir(imagePath, talkingBookRoot, null, mCopyListener);
        }

        mProgressListenerListener.log(String.format("Updated %d files of TB content, %s",
                mStepFileCount,
                getStepTime()));
    }

    /**
     * Updates community specific content files on the Talking Book.
     * <p>
     * Derived from the "community.txt" file.
     *
     * @throws IOException
     */
    private void updateCommunity() throws IOException {
        newStep(updateCommunity);

        TbFile communityPath = mDeploymentDirectory.open(TBLoaderConstants.COMMUNITIES_SUBDIR).open(
                mNewDeploymentInfo.getCommunity());
        if (communityPath.exists()) {
            TbFile.copyDir(communityPath, talkingBookRoot, null, mCopyListener);
        }

        mProgressListenerListener.log(String.format("Updated community content, %s",
                getStepTime()));
    }

    /**
     * Examine the file system on the Talking Book, to see if it seems to have the right files.
     *
     * @return True if the TB looks good.
     */
    private boolean verifyTalkingBook() {
        boolean verified;
        // rem REVIEW TARGET
        // type "${device_drive}\sysdata.txt"
        // dir ${device_drive} /s
        verified = talkingBookRoot.open(TBLoaderConstants.SYS_DATA_TXT).exists();
        return verified;
    }

    /**
     * By renaming the firmware to "system.img", the firmware will be reloaded, regardless of version match.
     */
    private void forceFirmwareRefresh() {// rename firmware at root to system.img to force TB to update itself
        if (mForceFirmware) {
            mProgressListenerListener.log("Forcing firmware refresh");
            TbFile firmware = talkingBookRoot.open(mNewDeploymentInfo.getFirmwareRevision() + ".img");
            firmware.renameTo("system.img");
        }
    }

    /**
     * Writes the TB data log file.
     *
     * @param verified
     * @return
     * @throws IOException
     */
    private void writeTbLog(boolean gotStatistics, boolean verified) throws IOException {
        String action = null;
        if (mGrabStatsOnly) {
            if (gotStatistics) {
                action = "stats-only";
            }
        } else {
            if (verified) {
                if (mForceFirmware) {
                    action = "update-fw";
                } else {
                    action = "update";
                }
            } else {
                action = "update-failed verification";
            }
        }

        if (action != null) {
            mProgressListenerListener.log("Logging TB data");
            logTBData(action, mOldDeploymentInfo,
                    mNewDeploymentInfo, mLocation, getDurationInSeconds(mUpdateStartTime));
        }
    }

    /**
     * Disconnects the Talking Book from the system.
     *
     * @throws IOException
     */
    private void disconnectDevice() throws IOException {
        if (OSChecker.WINDOWS) {
            mProgressListenerListener.log("Disconnecting TB");
            CommandLineUtils.disconnectDrive(tbDeviceInfo.getRootFile().getAbsolutePath());
        }
    }


    /**
     * Clears counters to start a new step.
     *
     * @param step
     */
    private void newStep(ProgressListener.Steps step) {
        mStepFileCount = 0;
        mStepStartTime = System.currentTimeMillis();
        mProgressListenerListener.step(step);
    }

    /**
     * Returns a nicely formatted string of a step's elapsed time.
     *
     * @return
     */
    private String getStepTime() {
        long millis = System.currentTimeMillis() - mStepStartTime;
        if (millis < 1000) {
            // Less than one second
            return String.format("%d ms", millis);
        } else if (millis < 60000) {
            // Less than one minute
            String time = String.format("%f", millis / 1000.0);
            return time.substring(0, 4) + " s";
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%d:%02d", minutes, seconds);
        }
    }


    private static String getStackTrace(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    private void eraseAndOverwriteFile(TbFile file, String content) throws IOException {
        mProgressListenerListener.detail(file.getName());
        mStepFileCount++;
        if (file.getParent().exists()) {
            file.getParent().mkdirs();
        }
        InputStream is = new ByteArrayInputStream(content.getBytes());
        file.createNew(is);
    }


    private static int getDurationInSeconds(long startTime) {
        long durationNanoseconds = System.nanoTime() - startTime;
        return (int) (durationNanoseconds / 1e9);
    }

    private static String getDuration(long startTime) {
        String elapsedTime;
        int durationMinutes;
        int durationSeconds = getDurationInSeconds(startTime);
        if (durationSeconds > 60) {
            durationMinutes = durationSeconds / 60;
            durationSeconds -= durationMinutes * 60;
            elapsedTime = Integer.toString(durationMinutes) + " minutes " + Integer.toString(
                    durationSeconds) + " seconds";
        } else
            elapsedTime = Integer.toString(durationSeconds) + " seconds";
        return elapsedTime;
    }

}
