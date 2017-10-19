package org.literacybridge.core.tbloader;

import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.OperationLog;
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
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.core.fs.TbFile.Flags.append;
import static org.literacybridge.core.fs.TbFile.Flags.contentRecursive;
import static org.literacybridge.core.fs.TbFile.Flags.nil;
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
import static org.literacybridge.core.tbloader.TBLoaderConstants.ISO8601;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_AUDIO_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_LISTS_PATH;


/*
  Implementation of the core of the TB-Loader. This code
  - copies files FROM the talking book, with statistics, signin recordings, and files (for
  diagnostic purposes). These files are copied to "tbcd{tbid}/collected-data"/{project}/...
  with this directory structure:
  TEST
  ├── OperationalData
  │   └── 000C
  │       └── tbData
  │           └── tbData-v03-2016y12m20d-000C.csv
  ├── TalkingBookData
  │   └── TEST-2016-1
  │       └── 000C
  │           └── demo-Seattle
  │               └── B-000C0300
  │                   └── 2016y12m20d08h48m37s-000C.zip
  └── UserRecordings
  └── TEST-2016-1
  └── 000C
  └── demo-Seattle
  ├── B-000C0300_9-0_071631C0.a18
  ├── B-000C0300_9-0_4E1F2A05.a18
  ├── B-000C0300_9-0_8125F26A.a18
  └── B-000C0300_9-0_C9061DA6.a18
  <p>
  The .zip file is a copy of log files, statistics files, and files from the Talking Book's system
  directory, structured like this:
  <p>
  2016y12m20d08h48m37s-000C.zip
  └── 2016y12m20d08h48m37s-000C
     ├── 10.a18
     ├── dir.txt
     ├── log
     │   └── log.txt
     ├── log-archive
     │   ├── log_B-000C0300_0000_0001.txt
  . . . more log files
     │   └── log_B-000C0300_0005_0001.txt
     ├── statistics
     │   ├── ACM-TEST-2016-1-EN^B-000C0300_071631C0.stat
  . . . more stat files
     │   └── log_B-000C0300_0005_0001.txt
     │   ├── ACM-TEST-2016-1-EN^a-265AC9DE_8BF48F08.stat
     │   ├── B-000C0300.csv
     │   ├── B-000C0300_016.vol
     │   ├── SN.csv
     │   └── flashData.bin
     └── system
         ├── ACM-TEST-2016-1-en.pkg
         ├── B-000C0300.srn
  . . . more files from system directory (but NOT *.img files)
         ├── r1215.rev
         ├── sig.grp
         └── sysdata.txt
  <p>
  <p>
  - copies files TO the Talking Book. The files are copied from a deployment directory, with
  this a structure like:
  content
  └── TEST-2016-1
  ├── basic
  │   └── r1215.img
  ├── communities
  │   ├── QONOS
  . . . more communities
  │   └── demo-Seattle
  │       ├── languages
  . . . files for each community
  └── images
  └── ACM-TEST-2016-1-en
  ├── Inbox
  ├── languages
  │   └── en
  │       ├── $0-1.txt
  . . . system messages
  │       ├── 9.a18
  │       ├── cat
  │       │   ├── $0-1.a18
  . category prompts
  │       │   └── i9-0.a18
  │       ├── control.txt
  │       └── intro.a18
  . . . more languages as required
  ├── log
  ├── log-archive
  ├── messages
  │   ├── audio
  │   │   ├── ECH_0236_63ED86F0.a18
  . . . audio content
  │   │   └── b-30373732_EB084B98.a18
  │   └── lists
  │       └── 1
  │           ├── 1-0.txt
  │           ├── 1-2.txt
  │           ├── 2-0.txt
  │           ├── 9-0.txt
  │           └── _activeLists.txt
  ├── statistics
  └── system
  ├── ACM-TEST-2016-1-en.pkg
  ├── config.txt
  ├── default.grp
  └── profiles.txt
  <p>
  Note that Inbox, log, log-archive, and statistics are empty directories, just to create the targets.
  <p>
  - creates new files on the Talking book.
 */


/**
 * Helper class to make building a TBLoaderCore easier.
 */
public class TBLoaderCore {
    private static final Logger LOG = Logger.getLogger(TBLoaderCore.class.getName());

    public static enum Action {
        DEPLOY,
        STATSONLY,
        FINALIZE
    }

    // The Talking Book is hard coded to expect the MS-DOS line ending.
    static final String MSDOS_LINE_ENDING = new String(new byte[] { 0x0d, 0x0a });

    public static class Builder {
        private TBLoaderConfig mTbLoaderConfig;
        private TBDeviceInfo mTbDeviceInfo;
        private DeploymentInfo mOldDeploymentInfo;
        private DeploymentInfo mNewDeploymentInfo;
        private TbFile mDeploymentDirectory;
        private String mLocation;
        private String mCoordinates;
        private ProgressListener mProgressListener;
        private boolean mStatsOnly = false;
        private boolean mRefreshFirmware = false;

        public Builder() {}

        public TBLoaderCore build() {
            List<String> missing = new ArrayList<>();

            if (mTbLoaderConfig == null) missing.add("tbLoaderConfig");
            if (mTbDeviceInfo == null) missing.add("tbDeviceInfo");
            if (mOldDeploymentInfo == null) missing.add("oldDeploymentInfo");
            if (mLocation == null) missing.add("location");
            if (mProgressListener == null) missing.add("progressListener");
            if (!mStatsOnly) {
                // Just getting stats doesn't use any new deployment.
                if (mNewDeploymentInfo == null) missing.add("newDeploymentInfo");
                if (mDeploymentDirectory == null) missing.add("deploymentDirectory");
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException("TBLoaderCore.Builder not initialized with " + missing.toString());
            }
            return new TBLoaderCore(this);
        }

        public Builder withTbLoaderConfig(TBLoaderConfig tbLoaderConfig) {
            this.mTbLoaderConfig = tbLoaderConfig;
            return this;
        }

        public Builder withTbDeviceInfo(TBDeviceInfo tbDeviceInfo) {
            this.mTbDeviceInfo = tbDeviceInfo;
            return this;
        }

        public Builder withNewDeploymentInfo(DeploymentInfo newDeploymentInfo) {
            this.mNewDeploymentInfo = newDeploymentInfo;
            return this;
        }

        public Builder withOldDeploymentInfo(DeploymentInfo oldDeploymentInfo) {
            this.mOldDeploymentInfo = oldDeploymentInfo;
            return this;
        }

        public Builder withDeploymentDirectory(TbFile deploymentDirectory) {
            this.mDeploymentDirectory = deploymentDirectory;
            return this;
        }

        /**
         * Note that this is like "Community", "WA Office", or "Other". NOT lat/long,
         * which is "coordinates".
         * @param location where the update is being performed.
         * @return this object, for chaining.
         */
        public Builder withLocation(String location) {
            this.mLocation = location;
            return this;
        }

        public Builder withCoordinates(String coordinates) {
            this.mCoordinates = coordinates;
            return this;
        }

        public Builder withProgressListener(ProgressListener progressListener) {
            this.mProgressListener = progressListener;
            return this;
        }

        public Builder withStatsOnly() {
            return withStatsOnly(true);
        }
        public Builder withStatsOnly(boolean statsOnly) {
            this.mStatsOnly = statsOnly;
            return this;
        }

        public Builder withRefreshFirmware(boolean refreshFirmware) {
            this.mRefreshFirmware = refreshFirmware;
            return this;
        }

    }

    //private final Builder mConfig;

    private final TBLoaderConfig mTbLoaderConfig;
    private final TBDeviceInfo mTbDeviceInfo;

    private DeploymentInfo mOldDeploymentInfo;
    private DeploymentInfo mNewDeploymentInfo;
    // A description of the location, like "Community", "WA Office", "Other".
    private String mLocation;
    // latitude longitude
    private String mCoordinates;
    private boolean mStatsOnly;
    private boolean mForceFirmware;
    private TbFile mDeploymentDirectory;
    private ProgressListener mProgressListener;

    // For tracking the progress of individual update steps.
    private ProgressListener.Steps mCurrentStep;
    private long mStepStartTime;
    private int mStepFileCount;
    private OperationLog.Operation mStepsLog;
    private TbFile.CopyProgress mCopyListener;

    private TbFlashData mTtbFlashData;

    private RelativePath mTalkingBookDataDirectoryPath;
    private RelativePath mTalkingBookDataZipPath;

    private final TbFile mCollectedDataDirectory;
    private final String mUpdateTimestampISO;
    private final String mUpdateTimestamp;
    private final String mCollectionTempName;

    private boolean mTbHasDiskCorruption = false;
    private TbFile mTalkingBookRoot;
    private TbFile mTempDirectory;
    private TbFile mTalkingBookDataRoot;
    private long mUpdateStartTime;

    private TBLoaderCore(Builder builder) {
        this.mTbDeviceInfo = builder.mTbDeviceInfo;
        this.mTbLoaderConfig = builder.mTbLoaderConfig;
        this.mTtbFlashData = builder.mTbDeviceInfo.getFlashData();

        // Like "tbcd1234/collected-data"
        mCollectedDataDirectory = builder.mTbLoaderConfig.getCollectedDataDirectory();

        // Roughly when an update starts.
        Date now = new Date();
        mUpdateTimestampISO = ISO8601.format(now);          // 20170928T223152.123Z
        mUpdateTimestamp = TBLoaderUtils.getDateTime(now);  // 2017Y09M28D22H31M52S
        // Like 2016y12m25d01h23m45s-000c. Also known as the "synch" directory.
        mCollectionTempName = mUpdateTimestamp + "-" + builder.mTbLoaderConfig.getTbLoaderId();

        mOldDeploymentInfo = builder.mOldDeploymentInfo;
        mNewDeploymentInfo = builder.mNewDeploymentInfo;
        mLocation = builder.mLocation;
        mCoordinates = builder.mCoordinates;
        mStatsOnly = builder.mStatsOnly;
        mForceFirmware = builder.mRefreshFirmware;
        mDeploymentDirectory = builder.mDeploymentDirectory;
        mProgressListener = builder.mProgressListener;

        // This is the path name for the "TalkingBookData" from this TB. In particular, this is the path
        // name for the directory that will contain the collected data, and then the .zip file of that data.
        // like TalkingBookData/{content update name}/{tbloader id}/{community name}/{tb serial no}
        RelativePath talkingBookDataParentPath = new RelativePath(
                TBLoaderConstants.TB_DATA_PATH,           // "TalkingBookData"
                mOldDeploymentInfo.getDeploymentName(),   // like "DEMO-2016-1"
                builder.mTbLoaderConfig.getTbLoaderId(),   // like "000c"
                mOldDeploymentInfo.getCommunity(),        // like "demo-seattle"
                builder.mTbDeviceInfo.getSerialNumber());  // like "B-000C1234"
        // like TalkingBookData/{content update name}/{tbloader id}/{community name}/{tb serial no}/{timestamp}-{tbloader id}
        // like "2016y12m25d01h23m45s-000c"
        mTalkingBookDataDirectoryPath = new RelativePath(
                talkingBookDataParentPath, mCollectionTempName);
        // like "2016y12m25d01h23m45s-000c"
        mTalkingBookDataZipPath = new RelativePath(
                talkingBookDataParentPath,
                mCollectionTempName + ".zip");


    }

    /**
     * Helper to append the contents of an OperationLog.Operation to a file.
     * @param logData The Operation to be appended.
     * @param logFile The file to which the Operation is to be appended.
     * @throws IOException If there is an error writing to the file.
     */
    private void writeLogDataToFile(OperationLog.Operation logData, TbFile logFile) throws IOException {
        // Copy the k=v .log file next to the .csv file.
        InputStream logContent = new ByteArrayInputStream(logData.formatLog().getBytes());
        boolean isNewLogFile = !logFile.exists();
        TbFile.Flags logFlag = isNewLogFile ? nil : append;
        logFile.createNew(logContent, logFlag);
        logContent.close();
    }

    /**
     * Writes the log files that the statistics processing will use to analyze deployments and usage.
     * @param action The TB-Loader action, "update", "update-fw", "stats-only", etc.
     * @param durationSeconds How long it took to collect any stats and perform any Deployment.
     * @throws IOException if there is an error writing any of the log files.
     */
    private void logTBData(
        String action,
        int durationSeconds) throws IOException {

        OperationLog.Operation opLog = OperationLog.startOperation("LogTbData");
        OperationLog.Operation statsLog = OperationLog.startOperation("statsdata");
        OperationLog.Operation deploymentLog = OperationLog.startOperation("deployment");
        OperationLog.Info operationInfo = new OperationLog.Info();
        OperationLog.Info statsInfo = new OperationLog.Info();

        
        final String VERSION_TBDATA = "v03";
        BufferedWriter bw;

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy'y'MM'm'dd'd'", Locale.US);
        String strDate = sdfDate.format(new Date());
        String csvFilename = String.format("tbData-%s-%s-%s.csv",
                                        VERSION_TBDATA,
                                        strDate,
                                        mTbLoaderConfig.getTbLoaderId());

        TbFile logDir = mCollectedDataDirectory             // like /Users/alice/Dropbox/tbcd000c
                .open(mOldDeploymentInfo.getProjectName())       // {tbloaderConfig.project}
                .open("OperationalData")                    // "OperationalData"
                .open(mTbLoaderConfig.getTbLoaderId())      // {tbloaderConfig.tbLoaderId}
                .open("tbData");                            // "tbData"

        // like /Users/alice/Dropbox/tbcd000c/{PROJECT}/OperationalData/{TBCDID}/tbdata-v03-{YYYYyMMmDDd}-{TBCDID}.csv
        TbFile csvFile = logDir.open(csvFilename);

        try {
            csvFile.getParent().mkdirs();
            boolean isNewFile = !csvFile.exists();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(baos));
            if (isNewFile) {
                bw.write("PROJECT,UPDATE_DATE_TIME,OUT_SYNCH_DIR,LOCATION,ACTION,DURATION_SEC,");
                bw.write("OUT-SN,OUT-DEPLOYMENT,OUT-IMAGE,OUT-FW-REV,OUT-COMMUNITY,OUT-ROTATION-DATE,");
                bw.write("IN-SN,IN-DEPLOYMENT,IN-IMAGE,IN-FW-REV,IN-COMMUNITY,IN-LAST-UPDATED,IN-SYNCH-DIR,IN-DISK-LABEL,CHKDSK CORRUPTION?,");
                bw.write("FLASH-SN,FLASH-REFLASHES,");
                bw.write("FLASH-DEPLOYMENT,FLASH-IMAGE,FLASH-COMMUNITY,FLASH-LAST-UPDATED,FLASH-CUM-DAYS,FLASH-CORRUPTION-DAY,FLASH-VOLT,FLASH-POWERUPS,FLASH-PERIODS,FLASH-ROTATIONS,");
                bw.write("FLASH-MSGS,FLASH-MINUTES,FLASH-STARTS,FLASH-PARTIAL,FLASH-HALF,FLASH-MOST,FLASH-ALL,FLASH-APPLIED,FLASH-USELESS");
                for (int i = 0; i < 5; i++) {
                    bw.write(",FLASH-ROTATION,FLASH-MINUTES-R" + i + ",FLASH-PERIOD-R" + i
                                    + ",FLASH-HRS-POST-UPDATE-R" + i + ",FLASH-VOLT-R" + i);
                }
                bw.write("\n");
            }
            // If the TB is moving between projects, the old project and new project are different.
            // This single value can then be correct for stats, or for deployment. Go for stats;
            // the deployment is tracked more easily in the deploymentLog.
            bw.write(mOldDeploymentInfo.getProjectName().toUpperCase() + ",");
            bw.write(mUpdateTimestamp.toUpperCase() + ",");
            bw.write(mUpdateTimestamp.toUpperCase() + "-" + mTbLoaderConfig.getTbLoaderId()
                    .toUpperCase() + ",");
            bw.write(mLocation.toUpperCase() + ",");
            bw.write(action + ",");
            bw.write(Integer.toString(durationSeconds) + ",");
            bw.write(mTbDeviceInfo.getSerialNumber().toUpperCase() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getDeploymentName().toUpperCase() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getPackageName().toUpperCase() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getFirmwareRevision() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getCommunity().toUpperCase() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getUpdateTimestamp() + ",");
            bw.write(mOldDeploymentInfo.getSerialNumber().toUpperCase() + ",");
            bw.write(mOldDeploymentInfo.getDeploymentName().toUpperCase() + ",");
            bw.write(mOldDeploymentInfo.getPackageName().toUpperCase() + ",");
            bw.write(mOldDeploymentInfo.getFirmwareRevision() + ",");
            bw.write(mOldDeploymentInfo.getCommunity().toUpperCase() + ",");
            bw.write(mOldDeploymentInfo.getUpdateTimestamp() + ",");
            String lastSynchDir = mOldDeploymentInfo.getUpdateDirectory();
            bw.write((lastSynchDir != null ? lastSynchDir.toUpperCase() : "") + ",");
            bw.write(mTbDeviceInfo.getLabel() + ",");
            bw.write(mTbDeviceInfo.isCorrupted() + ",");

            // With one exception, these are ordered the same as the csv values. Not necessary, of
            // course, but can help matching them up.
            // The exception is that, here, qthe action is first, not fifth.
            operationInfo
                .put("action", action)
                .put("tbcdid", mTbLoaderConfig.getTbLoaderId())
                .put("username", mTbLoaderConfig.getUserName())
                .put("project", mOldDeploymentInfo.getProjectName().toUpperCase())
                .put("update_date_time", mUpdateTimestamp.toUpperCase())
                .put("out_synch_dir", mUpdateTimestamp.toUpperCase() + "-" + mTbLoaderConfig.getTbLoaderId()
                        .toUpperCase())
                .put("location", mLocation.toUpperCase());
            if (mCoordinates != null && mCoordinates.length() > 0) {
                operationInfo.put("coordinates", mCoordinates);
            }
            operationInfo
                .put("duration_sec", Integer.toString(durationSeconds));
            opLog.put(operationInfo);
            statsLog.put(operationInfo);

            if (!mStatsOnly) {
                opLog
                    .put("out_sn", mTbDeviceInfo.getSerialNumber().toUpperCase())
                    .put("out_deployment", mNewDeploymentInfo.getDeploymentName().toUpperCase())
                    .put("out_package", mNewDeploymentInfo.getPackageName().toUpperCase())
                    .put("out_firmware", mNewDeploymentInfo.getFirmwareRevision())
                    .put("out_community", mNewDeploymentInfo.getCommunity().toUpperCase())
                    .put("out_rotation", mNewDeploymentInfo.getUpdateTimestamp())
                    .put("out_project", mNewDeploymentInfo.getProjectName())
                    .put("out_testing", mNewDeploymentInfo.isTestDeployment());

                // Everything there is to know about a deployment to a Talking Book should be here.
                deploymentLog
                    .put("action", action)
                    .put("tbcdid", mTbLoaderConfig.getTbLoaderId())
                    .put("username", mTbLoaderConfig.getUserName())
                    .put("sn", mTbDeviceInfo.getSerialNumber().toUpperCase())
                    .put("newsn", mNewDeploymentInfo.isNewSerialNumber())
                    .put("project", mNewDeploymentInfo.getProjectName().toUpperCase())
                    .put("deployment", mNewDeploymentInfo.getDeploymentName().toUpperCase())
                    .put("package", mNewDeploymentInfo.getPackageName().toUpperCase())
                    .put("community", mNewDeploymentInfo.getCommunity().toUpperCase())
                    .put("firmware", mNewDeploymentInfo.getFirmwareRevision())
                    .put("location", mLocation.toUpperCase())
                    .put("timestamp", mUpdateTimestampISO)
                    .put("duration", Integer.toString(durationSeconds))
                    .put("testing", mNewDeploymentInfo.isTestDeployment());
                if (mCoordinates != null && mCoordinates.length() > 0) {
                    deploymentLog.put("coordinates", mCoordinates);
                }

            }

            statsInfo
                .put("in_sn", mOldDeploymentInfo.getSerialNumber().toUpperCase())
                .put("in_deployment", mOldDeploymentInfo.getDeploymentName().toUpperCase())
                .put("in_package", mOldDeploymentInfo.getPackageName().toUpperCase())
                .put("in_firmware", mOldDeploymentInfo.getFirmwareRevision())
                .put("in_community", mOldDeploymentInfo.getCommunity().toUpperCase())
                .put("in_project", mOldDeploymentInfo.getProjectName())
                .put("in_update_timestamp", mOldDeploymentInfo.getUpdateTimestamp())
                .put("in_synchdir", (lastSynchDir != null ? lastSynchDir.toUpperCase() : ""))
                .put("in_disk_label", mTbDeviceInfo.getLabel())
                .put("disk_corrupted", mTbDeviceInfo.isCorrupted());

            if (mTtbFlashData != null) {
                bw.write(mTtbFlashData.getSerialNumber().toUpperCase() + ",");
                bw.write(mTtbFlashData.getCountReflashes() + ",");
                bw.write(mTtbFlashData.getDeploymentNumber().toUpperCase() + ",");
                bw.write(mTtbFlashData.getImageName().toUpperCase() + ",");
                bw.write(mTtbFlashData.getCommunity().toUpperCase() + ",");
                bw.write(mTtbFlashData.getUpdateYear() + "/" + mTtbFlashData.getUpdateMonth() + "/"
                        + mTtbFlashData.getUpdateDate() + ",");
                bw.write(mTtbFlashData.getCumulativeDays() + ",");
                bw.write(mTtbFlashData.getCorruptionDay() + ",");
                bw.write(mTtbFlashData.getLastInitVoltage() + ",");
                bw.write(mTtbFlashData.getPowerups() + ",");
                bw.write(mTtbFlashData.getPeriods() + ",");
                bw.write(mTtbFlashData.getProfileTotalRotations() + ",");
                bw.write(mTtbFlashData.getTotalMessages() + ",");
                
                int totalSecondsPlayed = 0, countStarted = 0, countQuarter = 0, countHalf = 0, countThreequarters = 0, countCompleted = 0, countApplied = 0, countUseless = 0;
                int numRotations = Math.max(5, mTtbFlashData.getProfileTotalRotations());
                for (int m = 0; m < mTtbFlashData.getTotalMessages(); m++) {
                    for (int r = 0; r < numRotations; r++) {
                        totalSecondsPlayed += mTtbFlashData.getStats()[m][r].getTotalSecondsPlayed();
                        countStarted += mTtbFlashData.getStats()[m][r].getCountStarted();
                        countQuarter += mTtbFlashData.getStats()[m][r].getCountQuarter();
                        countHalf += mTtbFlashData.getStats()[m][r].getCountHalf();
                        countThreequarters += mTtbFlashData.getStats()[m][r].getCountThreequarters();
                        countCompleted += mTtbFlashData.getStats()[m][r].getCountCompleted();
                        countApplied += mTtbFlashData.getStats()[m][r].getCountApplied();
                        countUseless += mTtbFlashData.getStats()[m][r].getCountUseless();
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
                for (int r = 0; r < numRotations; r++) {
                    bw.write("," + r + "," + mTtbFlashData.totalPlayedSecondsPerRotation(r) / 60
                                    + "," + mTtbFlashData.getRotations()[r].getStartingPeriod() + ",");
                    bw.write(mTtbFlashData.getRotations()[r].getHoursAfterLastUpdate() + ","
                            + mTtbFlashData.getRotations()[r].getInitVoltage());
                }

                statsInfo
                    .put("flash_sn", mTtbFlashData.getSerialNumber().toUpperCase())
                    .put("flash_reflashes", mTtbFlashData.getCountReflashes())
                    .put("flash_deployment", mTtbFlashData.getDeploymentNumber().toUpperCase())
                    .put("flash_package", mTtbFlashData.getImageName().toUpperCase())
                    .put("flash_community", mTtbFlashData.getCommunity().toUpperCase())
                    .put("flash_last_updated", mTtbFlashData.getUpdateYear() + "/" + mTtbFlashData.getUpdateMonth() + "/"
                        + mTtbFlashData.getUpdateDate())
                    .put("flash_cumulative_days", mTtbFlashData.getCumulativeDays())
                    .put("flash_corruption_day", mTtbFlashData.getCorruptionDay())
                    .put("flash_last_initial_v", mTtbFlashData.getLastInitVoltage())
                    .put("flash_powerups", mTtbFlashData.getPowerups())
                    .put("flash_periods", mTtbFlashData.getPeriods())
                    .put("flash_rotations", mTtbFlashData.getProfileTotalRotations())
                    .put("flash_num_messages", mTtbFlashData.getTotalMessages());

                statsInfo
                    .put("flash_total_seconds", totalSecondsPlayed)
                    .put("flash_started", countStarted)
                    .put("flash_one_quarter", countQuarter)
                    .put("flash_half", countHalf)
                    .put("flash_three_quarters", countThreequarters)
                    .put("flash_completed", countCompleted)
                    .put("flash_applied", countApplied)
                    .put("flash_useless", countUseless);

                final String N[] = {"0", "1", "2", "3", "4"};
                for (int r = 0; r < numRotations; r++) {
                    statsInfo
                        .put("flash_seconds_"+N[r], mTtbFlashData.totalPlayedSecondsPerRotation(r))
                        .put("flash_period_"+N[r], mTtbFlashData.getRotations()[r].getStartingPeriod())
                        .put("flash_hours_post_update_"+N[r], mTtbFlashData.getRotations()[r].getHoursAfterLastUpdate())
                        .put("flash_initial_v_"+N[r], mTtbFlashData.getRotations()[r].getInitVoltage());
                }

            }
            statsInfo.put("in_testing", mOldDeploymentInfo.isTestDeployment());

            opLog.put(statsInfo);
            statsLog.put(statsInfo);
            statsLog.put("statsonly", mStatsOnly);

            bw.write("\n");
            bw.flush();
            bw.close();

            InputStream content = new ByteArrayInputStream(baos.toByteArray());
            TbFile.Flags flag = isNewFile ? nil : append;
            opLog.put("append", !isNewFile);

            csvFile.createNew(content, flag);
            content.close();
            baos.close();

            // Copy the k=v .log files next to the .csv file.
            String logSuffix = String.format("-%s-%s.log",
                strDate,
                mTbLoaderConfig.getTbLoaderId());
            writeLogDataToFile(opLog, logDir.open("tbData"+logSuffix));
            writeLogDataToFile(statsLog, logDir.open("statsData"+logSuffix));
            if (!mStatsOnly) {
                writeLogDataToFile(deploymentLog, logDir.open("deployments" + logSuffix));
            }

        } catch (Exception e) {
            opLog.put("exception", e);
            e.printStackTrace();
            throw e;
        } finally {
            opLog.finish();
            statsLog.finish();
            if (!mStatsOnly) {
                deploymentLog.finish();
            }
        }
    }

    /**
     * For reporting a result back to callers, with explicit fields for various things that can
     * go wrong.
     */
    public class Result {
        public final boolean gotStatistics;
        public final boolean corrupted;
        public final boolean reformatFailed;
        public final boolean verified;
        public final String duration;

        private Result() {
            this.gotStatistics = false;
            this.corrupted = false;
            this.reformatFailed = false;
            this.verified = false;
            this.duration = "";
        }

        private Result(long startTime,
                boolean gotStatistics,
                boolean corrupted,
                boolean reformatFailed,
                boolean verified) {
            this.gotStatistics = gotStatistics;
            this.corrupted = corrupted;
            this.reformatFailed = reformatFailed;
            this.verified = verified;
            this.duration = getDuration(startTime); // Nice printed format
        }
    }

    /**
     * Gathers statics from the Talking Book.
     *
     * @return a Result object.
     */
    public Result collectStatistics() {
        if (!mStatsOnly) {
            throw new IllegalStateException("collectStatistics called without setting 'statsOnly'.");
        }
        return performOperation();
    }

    /**
     * Perform the update. See above for definitions of the directory structures.
     *
     * @return a Result object
     */
    public Result update() {
        if (mStatsOnly) {
            throw new IllegalStateException("update called with 'statsOnly' set.");
        }
        return performOperation();
    }

    /**
     * The setup and evaluation and statistics gathering are all in common between stats-only
     * and update.
     * @return a Result object that describes the result.
     */
    private Result performOperation() {
        LOG.log(Level.INFO, "TBL!: performOperation");

        mStepsLog = OperationLog.startOperation(mStatsOnly ? "CorTalkingBookCollectStatistics" : "CoreTalkingBookUpdate");
        mProgressListener.step(starting);

        mCopyListener = new TbFile.CopyProgress() {
            @Override
            public void copying(String filename) {
                mStepFileCount++;
                mProgressListener.detail(filename);
            }
        };

        mUpdateStartTime = System.nanoTime();

        // Get the roots for the Talking Book, the Temp directory, and the Staging Directory.
        mTalkingBookRoot = mTbDeviceInfo.getRootFile();
        mTempDirectory = mTbLoaderConfig.getTempDirectory();
        mTalkingBookDataRoot = mTempDirectory.open(mTalkingBookDataDirectoryPath);
        mTalkingBookDataRoot.mkdirs();
        // mkdir "${syncpath}"
        // at the end, this gets zipped up into the copyTo (Dropbox dir)
        mProgressListener.detail("Creating syncdirPath: " + mTalkingBookDataDirectoryPath);

        // Like tbcd1234/collected-data/XYZ
        TbFile projectCollectedData = mCollectedDataDirectory.open(mOldDeploymentInfo.getProjectName());
        LOG.log(Level.INFO, "TBL!: copy stats To:" + projectCollectedData.toString());

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
            LOG.log(Level.WARNING, "TBL!: Unable to get stats:", e);
            mProgressListener.log("Unable to gather statistics");
            mProgressListener.log(getStackTrace(e));
            // gotStatistics remains false; we'll log the failure later.
        }

        boolean verified = false;
        try {
            if (!mStatsOnly) {
                Result x = reformatRelabel();
                if (x != null) return x;

                clearSystemFiles();

                updateSystemFiles();

                updateContent();

                updateCommunity();

                mProgressListener.step(finishing);

                verified = verifyTalkingBook();

                forceFirmwareRefresh();

                listDeviceFilesPostUpdate();

            }

            writeTbLog(gotStatistics, verified);

            disconnectDevice();

        } catch (Throwable e) {
            LOG.log(Level.WARNING, "TBL!: Unable to update Talking Book:", e);
            mProgressListener.log("Unable to update Talking Book");
            mProgressListener.log(getStackTrace(e));
        }

        if (mStatsOnly) {
            mProgressListener.log("Get Stats " + (gotStatistics ? "successful." : "failed."));
        } else if (gotStatistics && verified) {
            mProgressListener.log("Update complete.");
        } else {
            mProgressListener.log("Update failed.");
        }

        Result result = new Result(mUpdateStartTime,
                gotStatistics, mTbHasDiskCorruption,
                false,
                verified);
        String completionMessage = String.format("TB-Loader updated in %s", result.duration);
        mProgressListener.detail("");
        mProgressListener.log(completionMessage);
        LOG.log(Level.INFO, completionMessage);
        mStepsLog.finish();
        return result;
    }

    /**
     * Tries to determine if the Talking Book storage is good. On Windows, runs chkdsk. On other OS,
     * just pokes around the file system.
     *
     * @return True if the Talking Book storage looks good.
     * @throws IOException if chkdsk throws an IOException
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
            startStep(checkDisk);
            mTbHasDiskCorruption = !CommandLineUtils.checkDisk(mTbDeviceInfo.getRootFile()
                    .getAbsolutePath());
            if (mTbHasDiskCorruption) {
                mTbDeviceInfo.setCorrupted();
                mProgressListener.log("Storage corrupted, attempting repair.");
                CommandLineUtils.checkDisk(mTbDeviceInfo.getRootFile().getAbsolutePath(),
                                           new RelativePath(mTalkingBookDataDirectoryPath,
                                "chkdsk-reformat.txt").asString());
                finishStep("Attempted repair of Talking Book storage");
            } else {
                finishStep("storage good");
            }
        } else {
            mProgressListener.log("chkdsk not supported on this OS");
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
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy  hh:mm aa", Locale.US);
        String formattedDate = sdf.format(date);
        String dirOrSize;
        if (entry.isDirectory()) {
            dirOrSize = "    <DIR>          ";
        } else {
            dirOrSize = String.format(Locale.US, "   %,15d ", entry.length());
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
    private DirectoryCount listDirectory(String rootPath, TbFile directory, StringBuilder buffer) {
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
            mProgressListener.detail(child.getName());
        }
        // Print the summary line
        buffer.append(String.format(Locale.US, "%11c%5d File(s)%,15d bytes\n\n",
                ' ',
                myCount.files,
                myCount.size));

        // List the child directories.
        for (TbFile child : children) {
            if (child.isDirectory()) {
                mProgressListener.detail(child.getAbsolutePath().substring(rootPath.length())+"/*");
                myCount.add(listDirectory(rootPath, child, buffer));
            }
        }

        return myCount;
    }

    private String getDeviceFileList() {
        StringBuilder builder = new StringBuilder();
        DirectoryCount counts = listDirectory(mTalkingBookRoot.getAbsolutePath(), mTalkingBookRoot, builder);

        // Add the summary.
        builder.append(String.format(Locale.US, "%5cTotal Files Listed:\n%1$10c%6d File(s)%,15d bytes\n",
                ' ',
                counts.files,
                counts.size));
        builder.append(String.format(Locale.US, "%10c%6d Dir(s)", ' ', counts.dirs));
        mProgressListener.log(String.format("%d files, %d dirs, %,d bytes", counts.files, counts.dirs, counts.size));
        long free = mTalkingBookRoot.getFreeSpace();
        if (free > 0) {
            builder.append(String.format(Locale.US, " %,15d bytes free", free));
            mProgressListener.log(true, String.format("%,d bytes free", free));
        }
        builder.append("\n");
        return builder.toString();
    }

    /**
     * Lists all the files on the Talking Book, and writes the listing to a file named sysdata.txt.
     *
     * @throws IOException if we can't create the listing file.
     */
    private void listDeviceFiles() throws IOException {
        startStep(listDeviceFiles);

        // rem Capturing Full Directory
        // dir ${device_drive} /s > "${syncpath}\dir.txt"
        String fileList = getDeviceFileList();
        eraseAndOverwriteFile(mTalkingBookDataRoot.open(TBLoaderConstants.DIRS_TXT), fileList);
        finishStep();
    }

    /**
     * Lists all the files on the Talking Book, after the update. Note that this is written
     * to the temp directory, and not zipped and uploaded.
     *
     * The main purpose, sadly, is to get the Android implementation of a USB host to write out
     * the contents of the files.
     *
     * @throws IOException if we can't create the listing file.
     */
    private void listDeviceFilesPostUpdate() throws IOException {
        startStep(listDeviceFiles2);

        // rem Capturing Full Directory
        // dir ${device_drive} /s > "${syncpath}\dir.txt"
        String fileList = getDeviceFileList();
        eraseAndOverwriteFile(mTempDirectory.open(TBLoaderConstants.DIRS_POST_TXT), fileList);
        finishStep();
    }

    /**
     * Copies a subset of files from the Talking Book. For logs, and for troubleshooting purposes.
     *
     * @throws IOException if we can't copy one of the files.
     */
    private void gatherDeviceFiles() throws IOException {// And Copy from tbRoot to tempRoot, with filter.
        startStep(gatherDeviceFiles);
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
                // Black-listed name? Skip them.
                if (excludedNames.contains(name)) {
                    return false;
                }
                // Hidden file? Skip them.
                if (name.charAt(0) == '.') {
                    return false;
                }
                // Image or backup file? Skip them.
                if (name.endsWith(".img") || name.endsWith(".old")) {
                    return false;
                }
                return true;
            }
        };

        TbFile.copyDir(mTalkingBookRoot, mTalkingBookDataRoot, copyFilesFilter, mCopyListener);
        finishStep();
    }

    /**
     * Copy user recordings from the Talking Book to the collected data directory.
     *
     * @param projectCollectedData The recordings will be copied to a subdirectory "UserRecordings"
     *                             of this directory.
     * @throws IOException if a recording can't be copied.
     */
    private void gatherUserRecordings(TbFile projectCollectedData) throws IOException {
        startStep(gatherUserRecordings);
        // rem Collecting User Recordings
        // mkdir "${recording_path}"
        // xcopy "${device_drive}\messages\audio\*_9_*.a18" "${recording_path}" /C
        // xcopy "${device_drive}\messages\audio\*_9-0_*.a18" "${recording_path}" /C

        // Build the user recordings source path.
        TbFile recordingsSrc = mTalkingBookRoot.open(TBLoaderConstants.TB_AUDIO_PATH);  // "messages/audio"
        // Build the user recordings destination path.
        TbFile recordingsDst = projectCollectedData           // like .../"tbcd1234/collected-data/UWR"
                .open(TBLoaderConstants.USER_RECORDINGS_PATH) // "UserRecordings"
                .open(mOldDeploymentInfo.getDeploymentName())  // like "UNICEF-2016-14"
                .open(mTbLoaderConfig.getTbLoaderId())         // like "000C"
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

        finishStep();
    }

    /**
     * Erases files from the log, log-archive, and statistics directories on the Talking Book.
     */
    private void clearStatistics() {
        startStep(clearStats);

        // rem Deleting Usage Statistics
        // del ${device_drive}\log\*.* /Q
        // del ${device_drive}\log-archive\*.* /S /Q
        // del ${device_drive}\statistics\*.* /S /Q
        // del ${device_drive}\statistics\stats\*.* /S /Q
        //      /s : Deletes specified files from the current directory and all subdirectories. Displays the names of the files as they are being deleted.
        //      /q : Specifies quiet mode. You are not prompted for delete confirmation.
        mTalkingBookRoot.open("log").delete(contentRecursive);
        mTalkingBookRoot.open("log-archive").delete(contentRecursive);
        mTalkingBookRoot.open("statistics").delete(contentRecursive);

        finishStep();
    }

    /**
     * Erases user recordings from the Talking Book.
     */
    private void clearUserRecordings() {
        startStep(clearUserRecordings);

        // rem Deleting User Recordings
        // del ${device_drive}\messages\audio\*_9_*.a18 /Q
        // del ${device_drive}\messages\audio\*_9-0_*.a18 /Q
        TbFile audioDirectory = mTalkingBookRoot.open(TBLoaderConstants.TB_AUDIO_PATH);
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

        finishStep();
    }

    /**
     * Deletes the ".txt" files that would have pointed to user recordings.
     */
    private void clearFeedbackCategories() {
        String[] names;
        startStep(clearFeedbackCategories);

        // rem Deleting User Feedback Category
        // for /f %f in ('dir ${device_drive}\messages\lists /b /AD') do del ${device_drive}\messages\lists\%f\9*.txt /Q
        TbFile listsDirectory = mTalkingBookRoot.open(TB_LISTS_PATH);
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

        finishStep();
    }

    /**
     * Zips the statistics, logs, and other files, and copies them to the collected data directory.
     *
     * @param projectCollectedData The files will be zipped into a file in this directory.
     * @throws IOException If the .zip can't be created.
     */
    private void zipAndCopyFiles(TbFile projectCollectedData) throws IOException {
        startStep(copyStatsAndFiles);

        // Same name and location as the tempDirectory, but a file with a .zip extension.
        TbFile tempZip = mTalkingBookDataRoot.getParent().open(mCollectionTempName + ".zip");

        File sourceFilesDir = new File(mTalkingBookDataRoot.getAbsolutePath());
        File tempZipFile = new File(tempZip.getAbsolutePath());
        ZipUnzip.zip(sourceFilesDir, tempZipFile, true);

        // Maybe
        TbFile outputZip = projectCollectedData
                .open(mTalkingBookDataZipPath);

        outputZip.getParent().mkdirs();
        TbFile.copy(tempZip, outputZip);

        // Clean out everything we put in the temp directory. Any other cruft that was there, as well.
        mTempDirectory.delete(contentRecursive);
        finishStep();
    }

    /**
     * Attempts to reformat the Talking Book if that's needed.
     * Attempts to relabel the Talking Book, if the label doesn't match the serial number.
     *
     * @return A Result if the TB needs reformatting, but can't be (ie, still needs reformatting.)
     * Returns null if format succeeded or not needed.
     * @throws IOException if the format or relabel fails.
     */
    private Result reformatRelabel() throws IOException {
        boolean goodCard;
        if (mTbHasDiskCorruption) {
            mProgressListener.step(reformatting);
            if (!OSChecker.WINDOWS) {
                // distinction without a difference... has corruption, reformat didn't fail because
                // no reformat was attempted.
                return new Result(0, false, true, false, false);
            }
            goodCard = CommandLineUtils.formatDisk(mTbDeviceInfo.getRootFile().getAbsolutePath(),
                                                   mTbDeviceInfo.getSerialNumber().toUpperCase());
            if (!goodCard) {
                mProgressListener.log("Reformat failed");
                return new Result(0, false, true, true, false);
            } else {
                mProgressListener.log(String.format("Reformatted card, %s", getStepTime()));
            }
        } else {
            if (!mNewDeploymentInfo.getSerialNumber()
                    .equalsIgnoreCase(
                            mTbDeviceInfo.getLabelWithoutDriveLetter())) {
                if (!OSChecker.WINDOWS) {
                    mProgressListener.log("Skipping relabeling; not supported on this OS.");
                } else {
                    mProgressListener.step(relabelling);
                    CommandLineUtils.relabel(mTbDeviceInfo.getRootFile().getAbsolutePath(),
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
     * @throws IOException if a file can't be deleted.
     */
    private void clearSystemFiles() throws IOException {
        startStep(clearSystem);

        mStepFileCount += mTalkingBookRoot.open("archive").delete(TbFile.Flags.recursive);
        mStepFileCount += mTalkingBookRoot.open("LOST.DIR").delete(TbFile.Flags.recursive);
        mStepFileCount += mTalkingBookRoot.open(TB_LISTS_PATH).delete(TbFile.Flags.recursive);
        mStepFileCount += mTalkingBookRoot.open(TB_AUDIO_PATH).delete(TbFile.Flags.recursive);

        // Delete files from /
        String[] names = mTalkingBookRoot.list(new TbFile.FilenameFilter() {
            @Override
            public boolean accept(TbFile parent, String name) {
                name = name.toLowerCase();
                // Files with a particular extension.
                if (name.endsWith(".img") || name.endsWith(".rtc")) {
                    return true;
                }
                // Anything starting with ".", but not "." or ".." (!)
                if (name.startsWith(".")) {
                    if (name.length() > 2 ||
                        (name.length() == 2 && name.charAt(1) != '.')) {
                        return true;
                    }
                }
                // Directories that Android and Windows like to spew wherever possible. (macOS's files all
                // start with ".", so handled above.)
                return name.equals("android") || name.equals("music") || name.equals(
                    "system volume information");
            }
        });
        if (names != null) {
            for (String name : names) {
                mProgressListener.detail(name);
                mStepFileCount += mTalkingBookRoot.open(name).delete(TbFile.Flags.recursive);
            }
        }

        // Delete files from /system
        TbFile system = mTalkingBookRoot.open("system");
        names = system.list(new TbFile.FilenameFilter() {
            @Override
            public boolean accept(TbFile parent, String name) {
                name = name.toLowerCase();
                return name.endsWith(".dep") ||
                        name.endsWith(".grp") ||
                        name.endsWith(".loc") ||
                        name.endsWith(".pkg") ||
                        name.endsWith(".prj") ||
                        name.endsWith(".srn") ||
                        name.endsWith(".txt");
            }
        });
        if (names != null) {
            for (String name : names) {
                mProgressListener.detail(name);
                system.open(name).delete();
                mStepFileCount++;
            }
        }

        // These two files will be rebuilt upon first boot of the Talking Book.
        mProgressListener.detail("config.bin");
        TbFile toDelete = system.open("config.bin");
        if (toDelete.exists()) {
            toDelete.delete();
            mStepFileCount++;
        }

        mProgressListener.detail("control.bin");
        toDelete = mTalkingBookRoot.open(TBLoaderConstants.TB_LANGUAGES_PATH).open("control.bin");
        if (toDelete.exists()) {
            toDelete.delete();
            mStepFileCount++;
        }

        finishStep();
    }

    /**
     * Creates new system files.
     * <p>
     * Derived from the "update.txt" file.
     */
    private void updateSystemFiles() throws IOException {
        startStep(updateSystem);

        TbFile system = mTalkingBookRoot.open("system");

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
                       mTalkingBookRoot, basicFilter,
                       mCopyListener);

        Calendar cal = Calendar.getInstance();
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        String dateInMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        String year = String.valueOf(cal.get(Calendar.YEAR));

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
        eraseAndOverwriteFile(mTalkingBookRoot.open(TBLoaderConstants.SYS_DATA_TXT), sysDataTxt);
        eraseAndOverwriteFile(mTalkingBookRoot.open("inspect"), ".");
        eraseAndOverwriteFile(mTalkingBookRoot.open("0h1m0s.rtc"), ".");

        mTalkingBookRoot.open("log").mkdir();
        mTalkingBookRoot.open("log-archive").mkdir();
        mTalkingBookRoot.open("Inbox").mkdir();
        mTalkingBookRoot.open("statistics").mkdir();

        system.mkdir();
        eraseAndOverwriteFile(system.open(srn + ".srn"), ".");
        eraseAndOverwriteFile(system.open(deploymentName + ".dep"), ".");
        eraseAndOverwriteFile(system.open(communityName + ".loc"), communityName);
        eraseAndOverwriteFile(system.open("last_updated.txt"), mCollectionTempName);
        eraseAndOverwriteFile(system.open(projectName + ".prj"), projectName);
        eraseAndOverwriteFile(system.open("notest.pcb"), ".");

        // 'properties' format file, with useful information for statistics gathering (next time around).
        DeploymentProperties props = new DeploymentProperties();
        props
            .append(TBLoaderConstants.TALKING_BOOK_ID_PROPERTY, mNewDeploymentInfo.getSerialNumber())
            .append(TBLoaderConstants.PROJECT_PROPERTY, mNewDeploymentInfo.getProjectName())
            .append(TBLoaderConstants.DEPLOYMENT_PROPERTY, mNewDeploymentInfo.getDeploymentName())
            .append(TBLoaderConstants.PACKAGE_PROPERTY, mNewDeploymentInfo.getPackageName())
            .append(TBLoaderConstants.COMMUNITY_PROPERTY, mNewDeploymentInfo.getCommunity())
            .append(TBLoaderConstants.FIRMWARE_PROPERTY, mNewDeploymentInfo.getFirmwareRevision())
            .append(TBLoaderConstants.TIMESTAMP_PROPERTY, mUpdateTimestampISO)
            .append(
                TBLoaderConstants.TEST_DEPLOYMENT_PROPERTY, mNewDeploymentInfo.isTestDeployment())
            .append(TBLoaderConstants.USERNAME_PROPERTY, mTbLoaderConfig.getUserName())
            .append(TBLoaderConstants.TBCDID_PROPERTY, mTbLoaderConfig.getTbLoaderId())
            .append(TBLoaderConstants.NEW_SERIAL_NUMBER_PROPERTY, mNewDeploymentInfo.isNewSerialNumber())
            .append(TBLoaderConstants.LOCATION_PROPERTY, mLocation);
        if (mCoordinates != null && mCoordinates.length() > 0) {
            props.append(TBLoaderConstants.COORDINATES_PROPERTY, mCoordinates);
        }
        eraseAndOverwriteFile(system.open(TBLoaderConstants.DEPLOYMENT_PROPERTIES_NAME), props.toString());

        finishStep();
    }

    private static class DeploymentProperties {
        private StringBuilder props = new StringBuilder();
        public String toString() { return props.toString(); }
        public DeploymentProperties append(String name, Object value) {
            props.append(name).append('=').append(value.toString()).append(MSDOS_LINE_ENDING);
            return this;
        }
    }

    /**
     * Updates content files on the Talking Book.
     * <p>
     * Derived from the "community.txt" file.
     *
     * @throws IOException if a file can't be copied.
     */
    private void updateContent() throws IOException {
        startStep(updateContent);

        TbFile imagePath = mDeploymentDirectory.open(IMAGES_SUBDIR)
                .open(mNewDeploymentInfo.getPackageName());
        if (imagePath.exists()) {
            TbFile.copyDir(imagePath, mTalkingBookRoot, null, mCopyListener);
        }

        finishStep();
    }

    /**
     * Updates community specific content files on the Talking Book.
     * <p>
     * Derived from the "community.txt" file.
     *
     * @throws IOException if a file can't be copied.
     */
    private void updateCommunity() throws IOException {
        startStep(updateCommunity);

        TbFile communityPath = mDeploymentDirectory.open(TBLoaderConstants.COMMUNITIES_SUBDIR).open(
                mNewDeploymentInfo.getCommunity());
        if (communityPath.exists()) {
            TbFile.copyDir(communityPath, mTalkingBookRoot, null, mCopyListener);
        }

        finishStep();
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
        verified = mTalkingBookRoot.open(TBLoaderConstants.SYS_DATA_TXT).exists();
        return verified;
    }

    /**
     * By renaming the firmware to "system.img", the firmware will be reloaded, regardless of version match.
     */
    private void forceFirmwareRefresh() {// rename firmware at root to system.img to force TB to update itself
        if (mForceFirmware) {
            mProgressListener.log("Forcing firmware refresh");
            TbFile firmware = mTalkingBookRoot.open(mNewDeploymentInfo.getFirmwareRevision() + ".img");
            TbFile newFirmware = mTalkingBookRoot.open("system.img");
            firmware.renameTo(newFirmware.getAbsolutePath());
        }
    }

    /**
     * Writes the TB data log file.
     *
     * @param gotStatistics Did we successfully collect stats from the TB?
     * @param verified Did the Talking Book seem good after the update?
     */
    private void writeTbLog(boolean gotStatistics, boolean verified) throws IOException {
        String action;
        if (mStatsOnly) {
            if (gotStatistics) {
                action = "stats-only";
            } else {
                action = "stats-error";
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
            if (!gotStatistics) {
                action += "-stats-error";
            }
        }

        mProgressListener.log("Logging TB data");
        logTBData(action,
            getDurationInSeconds(mUpdateStartTime));
    }

    /**
     * Disconnects the Talking Book from the system.
     *
     * @throws IOException if there is an error disconnecting the drive. Note that on some OS
     * it may be completely impossible to disconnect the drive, and no exception is thrown.
     */
    private void disconnectDevice() throws IOException {
        if (OSChecker.WINDOWS) {
            String fn = mTbLoaderConfig.getWindowsUtilsDirectory().getAbsolutePath();
            CommandLineUtils.setUtilsDirectory(new File(fn));
            mProgressListener.log("Disconnecting TB");
            CommandLineUtils.disconnectDrive(mTbDeviceInfo.getRootFile().getAbsolutePath());
        }
    }


    /**
     * Clears counters to start a new step.
     *
     * @param step The particular step being started.
     */
    private void startStep(ProgressListener.Steps step) {
        mCurrentStep = step;
        mStepFileCount = 0;
        mStepStartTime = System.currentTimeMillis();
        mProgressListener.step(step);
    }

    /**
     * Helper to summarize counters and time at the end of a step.
     */
    private void finishStep(String... resultStrings) {
        mStepsLog.split(mCurrentStep.toString()+".time");
        StringBuilder builder = new StringBuilder(mCurrentStep.description());
        for (String rs : resultStrings) {
            builder.append(", ").append(rs);
        }

        builder.append(": ");
        if (mCurrentStep.hasFiles) {
            mStepsLog.put(mCurrentStep.toString() + ".files", mStepFileCount);
            builder.append(String.format(Locale.US, "%d file(s), ", mStepFileCount));
        }
        builder.append(getStepTime());

        mProgressListener.log(builder.toString());
    }
    /**
     * Returns a nicely formatted string of a step's elapsed time.
     *
     * @return the nicely formatted string.
     */
    private String getStepTime() {
        long millis = System.currentTimeMillis() - mStepStartTime;
        if (millis < 1000) {
            // Less than one second
            return String.format(Locale.US, "%d ms", millis);
        } else if (millis < 60000) {
            // Less than one minute
            String time = String.format(Locale.US, "%f", millis / 1000.0);
            return time.substring(0, 4) + " s";
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format(Locale.US, "%d:%02d", minutes, seconds);
        }
    }


    private static String getStackTrace(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    private void eraseAndOverwriteFile(TbFile file, String content) throws IOException {
        mProgressListener.detail(file.getName());
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
