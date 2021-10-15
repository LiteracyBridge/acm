package org.literacybridge.core.tbloader;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.fs.ZipUnzip;
import org.literacybridge.core.tbdevice.TbDeviceInfo;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.literacybridge.core.fs.TbFile.Flags.append;
import static org.literacybridge.core.fs.TbFile.Flags.contentRecursive;
import static org.literacybridge.core.fs.TbFile.Flags.nil;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.checkDisk;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.copyStatsAndFiles;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.delay;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.finishing;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.listDeviceFiles;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.listDeviceFiles2;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.starting;
import static org.literacybridge.core.tbloader.TBLoaderConstants.*;
import static org.literacybridge.core.tbloader.TBLoaderUtils.getBytesString;

//@formatter:off
/*
  Implementation of the core of the TB-Loader. This code
  - copies files FROM the talking book, with statistics, signin recordings, and files (for
  diagnostic purposes). These files are copied to "tbcd{tbid}/collected-data"/{project}/...
  with this directory structure:
  TEST
  ├── OperationalData
  │   └── 000C
  │       └── tbData
  │           └── tbData-v03-2016y12m20d-000C.csv
  ├── TalkingBookData
  │   └── TEST-2016-1
  │       └── 000C
  │           └── demo-Seattle
  │               └── B-000C0300
  │                   └── 2016y12m20d08h48m37s-000C.zip
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
     │   └── log.txt
     ├── log-archive
     │   ├── log_B-000C0300_0000_0001.txt
  . . . more log files
     │   └── log_B-000C0300_0005_0001.txt
     ├── statistics
     │   ├── ACM-TEST-2016-1-EN^B-000C0300_071631C0.stat
  . . . more stat files
     │   └── log_B-000C0300_0005_0001.txt
     │   ├── ACM-TEST-2016-1-EN^a-265AC9DE_8BF48F08.stat
     │   ├── B-000C0300.csv
     │   ├── B-000C0300_016.vol
     │   ├── SN.csv
     │   └── flashData.bin
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
     │   └── r1215.img
     ├── communities
     │   ├── QONOS
     . . . more communities
     │   └── demo-Seattle
     │       ├── languages
     . . . files for each community
     └── images
        └── ACM-TEST-2016-1-en
           ├── Inbox
           ├── languages
           │   └── en
           │       ├── $0-1.txt
           . . . system messages
           │       ├── 9.a18
           │       ├── cat
           │       │   ├── $0-1.a18
           . category prompts
           │       │   └── i9-0.a18
           │       ├── control.txt
           │       └── intro.a18
           . . . more languages as required
           ├── log
           ├── log-archive
           ├── messages
           │   ├── audio
           │   │   ├── ECH_0236_63ED86F0.a18
           . . . audio content
           │   │   └── b-30373732_EB084B98.a18
           │   └── lists
           │       └── 1
           │           ├── 1-0.txt
           │           ├── 1-2.txt
           │           ├── 2-0.txt
           │           ├── 9-0.txt
           │           └── _activeLists.txt
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
//@formatter:on

/**
 * Helper class to make building a TBLoaderCore easier.
 */
public abstract class TBLoaderCore {
    private static final Logger LOG = Logger.getLogger(TBLoaderCore.class.getName());
    protected Result.FORMAT_OP mFormatOp;

    @SuppressWarnings("unused")
    public enum Action {
        DEPLOY,
        STATSONLY,
        FINALIZE
    }

    // The Talking Book is hard coded to expect the MS-DOS line ending.
    protected static final String MSDOS_LINE_ENDING = new String(new byte[] { 0x0d, 0x0a });

    @SuppressWarnings("unused")
    public static class Builder {
        private TBLoaderConfig mTbLoaderConfig;
        private TbDeviceInfo mTbDeviceInfo;
        private TbDeviceInfo.DEVICE_VERSION mSpecifiedVersion = null;
        private DeploymentInfo mOldDeploymentInfo;
        private DeploymentInfo mNewDeploymentInfo;
        private TbFile mDeploymentDirectory;
        private String mLocation = "Other";
        private String mCoordinates;
        private ProgressListener mProgressListener;
        private boolean mStatsOnly = false;
        protected boolean mRefreshFirmware = false;
        protected int mPostUpdateDelayMillis = 0;
        protected final Set<String> mAcceptableFirmware = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

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
                throw new IllegalStateException("TBLoaderCore.Builder not initialized with " + missing);
            }
            TbDeviceInfo.DEVICE_VERSION deviceVersion = (mSpecifiedVersion != null) ? mSpecifiedVersion : mTbDeviceInfo.getDeviceVersion();
            return deviceVersion == TbDeviceInfo.DEVICE_VERSION.TBv1
                   ? new TBLoaderCoreV1(this)
                   : new TBLoaderCoreV2(this);
        }

        public Builder withTbLoaderConfig(TBLoaderConfig tbLoaderConfig) {
            this.mTbLoaderConfig = tbLoaderConfig;
            return this;
        }

        public Builder withTbDeviceInfo(TbDeviceInfo tbDeviceInfo) {
            this.mTbDeviceInfo = tbDeviceInfo;
            return this;
        }

        public Builder withTbDeviceVersion(TbDeviceInfo.DEVICE_VERSION deviceVersion) {
            this.mSpecifiedVersion = deviceVersion;
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

        public Builder withPostUpdateDelay(int postUpdateDelayMillis) {
            this.mPostUpdateDelayMillis = postUpdateDelayMillis;
            return this;
        }

        /**
         * A list of the firmware versions that are acceptable to this deployment.
         * @param acceptables versions, as a comma separated list of strings.
         * @return the builder
         */
        public Builder withAcceptableFirmware(String acceptables) {
            if (StringUtils.isNotBlank(acceptables)) {
                this.mAcceptableFirmware.addAll(Arrays.stream(acceptables.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList()));
            }
            return this;
        }

    }

    protected final Builder mBuilder;

    protected final TBLoaderConfig mTbLoaderConfig;
    protected final TbDeviceInfo mTbDeviceInfo;

    protected final DeploymentInfo mOldDeploymentInfo;
    protected final DeploymentInfo mNewDeploymentInfo;
    // A description of the location, like "Community", "WA Office", "Other".
    protected final String mLocation;
    // latitude longitude
    protected final String mCoordinates;
    protected final boolean mStatsOnly;
    protected final TbFile mDeploymentDirectory;
    protected final ProgressListener mProgressListener;

    protected boolean mClearedFlashStatistics;

    // For tracking the progress of individual update steps.
    private ProgressListener.Steps mCurrentStep;
    private long mStepStartTime;
    protected int mStepFileCount;
    protected long mStepBytesCount;
    private OperationLog.Operation mStepsLog;
    protected TbFile.CopyProgress mCopyListener;

    private final TbFlashData mTtbFlashData;

    private final RelativePath mTalkingBookDataDirectoryPath;
    protected final RelativePath mTalkingBookDataZipPath;

    protected final TbFile mLogDirectory;
    private final TbFile mCollectedDataDirectory;
    protected final String mUpdateTimestampISO;
    private final String mUpdateTimestamp;
    protected final String mCollectionTempName;

    protected final UUID mDeploymentUUID;
    protected final UUID mStatsCollectedUUID;

    protected boolean mTbHasDiskCorruption = false;
    protected TbFile mTalkingBookRoot;
    protected TbFile mTempDirectory;
    protected TbFile mTalkingBookDataRoot;
    private long mUpdateStartTime;

    TBLoaderCore(Builder builder) {
        this.mBuilder = builder;
        this.mTbDeviceInfo = builder.mTbDeviceInfo;
        this.mTbLoaderConfig = builder.mTbLoaderConfig;
        this.mTtbFlashData = builder.mTbDeviceInfo.getFlashData();
        this.mDeploymentUUID = UUID.randomUUID();
        this.mStatsCollectedUUID = UUID.randomUUID();

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
        if (!mStatsOnly) {
            // There is only a firmware if we're doing an update.
            // The provided firmware is always acceptable, so ensure it is in the list.
            builder.mAcceptableFirmware.add(builder.mNewDeploymentInfo.getFirmwareRevision());
        }
        mDeploymentDirectory = builder.mDeploymentDirectory;
        mProgressListener = builder.mProgressListener;

        // "tbData"
        mLogDirectory = mCollectedDataDirectory         // like /Users/alice/Dropbox/tbcd000c
            .open(mOldDeploymentInfo.getProjectName())  // {tbloaderConfig.project}
            .open(OPERATIONAL_DATA)                     // "OperationalData"
            .open(mTbLoaderConfig.getTbLoaderId())      // {tbloaderConfig.tbLoaderId}
            .open("tbData");

        // This is the path name for the "TalkingBookData" from this TB. In particular, this is the path
        // name for the directory that will contain the collected data, and then the .zip file of that data.
        // like TalkingBookData/{Deployment name}/{tbloader id}/{community name}/{tb serial no}
        RelativePath talkingBookDataParentPath = new RelativePath(
                TBLoaderConstants.TALKING_BOOK_DATA,           // "TalkingBookData"
                mOldDeploymentInfo.getDeploymentName(),   // like "DEMO-2016-1"
                builder.mTbLoaderConfig.getTbLoaderId(),   // like "000c"
                mOldDeploymentInfo.getCommunity(),        // like "demo-seattle"
                builder.mTbDeviceInfo.getSerialNumber());  // like "B-000C1234"
        // like TalkingBookData/{Deployment name}/{tbloader id}/{community name}/{tb serial no}/{timestamp}-{tbloader id}
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

        // This is a format for a date format used a lot in TB statistics.
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy'y'MM'm'dd'd'", Locale.US);
        String strDate = sdfDate.format(new Date());
        String csvFilename = String.format("tbData-%s-%s-%s.csv",
                                        VERSION_TBDATA,
                                        strDate,
                                        mTbLoaderConfig.getTbLoaderId());

        // like /Users/alice/Dropbox/tbcd000c/{PROJECT}/OperationalData/{TBCDID}/tbdata-v03-{YYYYyMMmDDd}-{TBCDID}.csv
        TbFile csvFile = mLogDirectory.open(csvFilename);

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
            bw.write(durationSeconds + ",");
            bw.write(mTbDeviceInfo.getSerialNumber().toUpperCase() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getDeploymentName().toUpperCase() + ",");
            bw.write(mStatsOnly?",":String.join(",", mNewDeploymentInfo.getPackageNames()).toUpperCase() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getFirmwareRevision() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getCommunity().toUpperCase() + ",");
            bw.write(mStatsOnly?",":mNewDeploymentInfo.getUpdateTimestamp() + ",");
            bw.write(mOldDeploymentInfo.getSerialNumber().toUpperCase() + ",");
            bw.write(mOldDeploymentInfo.getDeploymentName().toUpperCase() + ",");
            bw.write(String.join(",", String.join(",", mOldDeploymentInfo.getPackageNames())).toUpperCase() + ",");
            bw.write(mOldDeploymentInfo.getFirmwareRevision() + ",");
            bw.write(mOldDeploymentInfo.getCommunity().toUpperCase() + ",");
            bw.write(mOldDeploymentInfo.getUpdateTimestamp() + ",");
            String lastSynchDir = mOldDeploymentInfo.getUpdateDirectory();
            bw.write((lastSynchDir != null ? lastSynchDir.toUpperCase() : "") + ",");
            bw.write(mTbDeviceInfo.getLabel() + ",");
            bw.write(mTbDeviceInfo.isCorrupted() + ",");

            // With one exception, the stats are ordered the same as the csv values. Not necessary,
            // of course, but can help matching them up.
            // The exception is that, here, the action is first, not fifth.
            operationInfo
                .put("action", action)
                .put("tbcdid", mTbLoaderConfig.getTbLoaderId())
                .put("username", mTbLoaderConfig.getUserEmail())
                .put("useremail", mTbLoaderConfig.getUserEmail())
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
                    .put("out_package", String.join(",", mNewDeploymentInfo.getPackageNames()).toUpperCase())
                    .put("out_firmware", mNewDeploymentInfo.getFirmwareRevision())
                    .put("out_community", mNewDeploymentInfo.getCommunity().toUpperCase())
                    .put("out_rotation", mNewDeploymentInfo.getUpdateTimestamp())
                    .put("out_project", mNewDeploymentInfo.getProjectName())
                    .put("out_testing", mNewDeploymentInfo.isTestDeployment());

                // Everything there is to know about a deployment to a Talking Book should be here.
                deploymentLog
                    .put("action", action)
                    .put("tbcdid", mTbLoaderConfig.getTbLoaderId())
                    .put("username", mTbLoaderConfig.getUserEmail())
                    .put("useremail", mTbLoaderConfig.getUserEmail())
                    .put("sn", mTbDeviceInfo.getSerialNumber().toUpperCase())
                    .put("newsn", mNewDeploymentInfo.isNewSerialNumber())
                    .put("project", mNewDeploymentInfo.getProjectName().toUpperCase())
                    .put("deployment", mNewDeploymentInfo.getDeploymentName().toUpperCase())
                    .put("package", String.join(",", mNewDeploymentInfo.getPackageNames()).toUpperCase())
                    .put("community", mNewDeploymentInfo.getCommunity().toUpperCase())
                    .put("firmware", mNewDeploymentInfo.getFirmwareRevision())
                    .put("location", mLocation.toUpperCase())
                    .put("timestamp", mUpdateTimestampISO)
                    .put("duration", Integer.toString(durationSeconds))
                    .put("testing", mNewDeploymentInfo.isTestDeployment());
                if (mNewDeploymentInfo.getDeploymentNumber() > 0) {
                    deploymentLog.put("deploymentnumber", mNewDeploymentInfo.getDeploymentNumber());
                }
                if (mNewDeploymentInfo.getRecipientid() != null) {
                    opLog.put("out_recipientid", mNewDeploymentInfo.getRecipientid());
                    deploymentLog.put("recipientid", mNewDeploymentInfo.getRecipientid());
                }
                if (mCoordinates != null && mCoordinates.length() > 0) {
                    deploymentLog.put("coordinates", mCoordinates);
                }

            }

            statsInfo
                .put("in_sn", mOldDeploymentInfo.getSerialNumber().toUpperCase())
                .put("in_deployment", mOldDeploymentInfo.getDeploymentName().toUpperCase())
                .put("in_package", String.join(",", mOldDeploymentInfo.getPackageNames()).toUpperCase())
                .put("in_firmware", mOldDeploymentInfo.getFirmwareRevision())
                .put("in_community", mOldDeploymentInfo.getCommunity().toUpperCase())
                .put("in_project", mOldDeploymentInfo.getProjectName())
                .put("in_update_timestamp", mOldDeploymentInfo.getUpdateTimestamp())
                .put("in_synchdir", (lastSynchDir != null ? lastSynchDir.toUpperCase() : ""))
                .put("in_disk_label", mTbDeviceInfo.getLabel())
                .put("disk_corrupted", mTbDeviceInfo.isCorrupted());
            if (mOldDeploymentInfo.getRecipientid() != null) {
                statsInfo.put("in_recipientid", mOldDeploymentInfo.getRecipientid());
                opLog.put("in_recipientid", mOldDeploymentInfo.getRecipientid());
            }

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

                final String[] N = {"0", "1", "2", "3", "4"};
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

            String inDeploymentUUID = mTbDeviceInfo.getDeploymentUUID();
            if (inDeploymentUUID != null) {
                statsLog.put("deployment_uuid", inDeploymentUUID);
                opLog.put("in_deployment_uuid", inDeploymentUUID);
                deploymentLog.put("prev_deployment_uuid", inDeploymentUUID);
            }
            if (!mStatsOnly) {
                opLog.put("out_deployment_uuid", mDeploymentUUID);
                deploymentLog.put("deployment_uuid", mDeploymentUUID);
            }
            statsLog.put("stats_uuid", mStatsCollectedUUID);
            opLog.put("stats_uuid", mStatsCollectedUUID);

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
            writeLogDataToFile(opLog, mLogDirectory.open("tbData"+logSuffix));
            writeLogDataToFile(statsLog, mLogDirectory.open("statsData"+logSuffix));
            if (!mStatsOnly) {
                writeLogDataToFile(deploymentLog, mLogDirectory.open("deployments" + logSuffix));
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
    public static class Result {
        public enum FORMAT_OP {noFormat, succeeded, failed}

        public final boolean gotStatistics;
        public final boolean corrupted;
        public final FORMAT_OP reformatOp;
        public final boolean verified;
        public final String duration;

        private Result() {
            this.gotStatistics = false;
            this.corrupted = false;
            this.reformatOp = FORMAT_OP.noFormat;
            this.verified = false;
            this.duration = "";
        }

        protected Result(long startTime,
            boolean gotStatistics,
            boolean corrupted,
            FORMAT_OP reformatOp,
            boolean verified) {
            this.gotStatistics = gotStatistics;
            this.corrupted = corrupted;
            this.reformatOp = reformatOp;
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
        mFormatOp = Result.FORMAT_OP.noFormat;
        LOG.log(Level.FINE, "TBL!: performOperation");

        mStepsLog = OperationLog.startOperation(mStatsOnly ? "CorTalkingBookCollectStatistics" : "CoreTalkingBookUpdate");
        mProgressListener.step(starting);

        mCopyListener = (fromFile, toFile) -> {
            mStepFileCount++;
            mProgressListener.detail(toFile.getName());
        };

        mUpdateStartTime = System.nanoTime();
        mClearedFlashStatistics = false;

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
        LOG.log(Level.FINE, "TBL!: copy stats To:" + projectCollectedData.toString());

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

            gotStatistics = true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "TBL!: Unable to get stats:", e);
            mProgressListener.log("Unable to gather statistics");
            mProgressListener.log(getStackTrace(e));
            // gotStatistics remains false; we'll log the failure later.
        }

        boolean verified = false;
        try {
            if (!mStatsOnly && !mTbHasDiskCorruption) {
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

                delayForAndroid();
            }

        } catch (Throwable e) {
            LOG.log(Level.WARNING, "TBL!: Unable to update Talking Book:", e);
            mProgressListener.log("Unable to update Talking Book");
            mProgressListener.log(getStackTrace(e));
        }

        // Must not write to the Talking Book after this point.
        try {
            String action = getActionString(gotStatistics, verified);

            zipAndCopyFiles(projectCollectedData, action);

            writeTbLog(action);

            if (!mTbHasDiskCorruption) {
                disconnectDevice();
            }

        } catch (Throwable e) {
            LOG.log(Level.WARNING, "TBL!: Unable to zip Talking Book statistics:", e);
            mProgressListener.log("Unable to zip Talking Book statistics");
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
                gotStatistics, mTbHasDiskCorruption, mFormatOp,
                verified);
        String completionMessage = String.format("TB-Loader updated in %s", result.duration);
        mProgressListener.detail("");
        mProgressListener.log(completionMessage);
        LOG.log(Level.INFO, completionMessage);
        mStepsLog.finish();
        return result;
    }

    protected abstract void gatherDeviceFiles() throws IOException;
    protected abstract void gatherUserRecordings(TbFile projectCollectedData) throws IOException;
    protected abstract void clearStatistics() throws IOException;
    protected abstract void clearUserRecordings();
    protected abstract void clearFeedbackCategories();
    protected abstract Result reformatRelabel() throws IOException;
    protected abstract void clearSystemFiles();
    protected abstract void updateSystemFiles() throws IOException;
    protected abstract void updateContent() throws IOException;
    protected abstract void updateCommunity() throws IOException;
    protected abstract boolean verifyTalkingBook();
    protected abstract void forceFirmwareRefresh();

    /**
     * A helper class to filter content. Tracks "shadowed" files for later copying with the real thing,
     * and ignores "lists" and profiles.txt, which must be merged later.
     */
    protected static class ContentCopyFilter implements TbFile.CopyFilter {
        // The directories that may contain shadowed files.
        private final List<String> shadowedDirs;
        // The location of real files (to replace the shadows).
        private final TbFile shadowFilesDir;
        // Accumulate the pending copies here.
        private final Map<String, TbFile> shadowedFiles;
        // For converting to paths relative to the image.
        private final int imagePathLength;
        // Paths that we need to intercept to support multiple images per device.
        private final Set<String> pathsToProcessLater;

        ContentCopyFilter(TbFile imagePath,
            List<String> shadowedDirs,
            TbFile shadowFilesDir,
            Map<String, TbFile> shadowedFiles, Set<String> pathsToProcessLater) {
            this.shadowedDirs = shadowedDirs;
            this.shadowFilesDir = shadowFilesDir;
            this.shadowedFiles = shadowedFiles;
            this.imagePathLength = imagePath.getAbsolutePath().length();
            this.pathsToProcessLater = pathsToProcessLater;
        }

        @Override
        public boolean accept(TbFile file) {
            // If this is a zero-byte file...
            if (file.exists() && file.length() == 0) {
                // See if there is a corresponding shadow file.
                String parentDirName = file.getParent().getAbsolutePath();
                for (String shadowedDirName : shadowedDirs) {
                    if (parentDirName.startsWith(shadowedDirName)) {
                        // We found a matching shadow directory, see if the shadow contains this file.
                        String relativeParent = parentDirName.substring(imagePathLength);
                        TbFile shadowSrcFile = shadowFilesDir.open(relativeParent).open(file.getName());
                        if (shadowSrcFile.exists()) {
                            // There is a real file for this shadow. Remember it to copy later.
                            String relativeTarget = String.join(File.separator, relativeParent) + File.separator + file.getName();
                            // Only remember the file once.
                            if (!shadowedFiles.containsKey(relativeTarget)) {
                                shadowedFiles.put(relativeTarget, file);
                            }
                            return false;
                        }
                        // There wasn't a cached file; legitimately a 0-byte file.
                        return true;
                    }
                }
            } else //noinspection RedundantIfStatement
                if (file.exists() && pathsToProcessLater.contains(file.getAbsolutePath())) {
                return false;
            }
            return true;
        }
    }


    /**
     * Tries to determine if the Talking Book storage is good. On Windows, runs chkdsk. On other OS,
     * just pokes around the file system.
     *
     * @return True if the Talking Book storage looks good.
     * @throws IOException if chkdsk throws an IOException
     */
    protected boolean isTalkingBookStorageGood() throws IOException {
        boolean goodCard;
        // This is, sadly, broken. The old one (dos only) simply tried to do a "dir" commmand on the drive.
        // This one tries to look for a bunch of directories.
        // TODO: figure out something that sorta works.
        //goodCard = checkConnection(true);
        goodCard = true;
        //noinspection ConstantConditions
        if (!goodCard) {
            return false;
        }
        if (OSChecker.WINDOWS) {
            startStep(checkDisk);
            mTbHasDiskCorruption = !mTbLoaderConfig.getCommandLineUtils().checkDisk(mTbDeviceInfo.getRootFile()
                .getAbsolutePath());
            if (mTbHasDiskCorruption) {
                mTbDeviceInfo.setCorrupted();
                mProgressListener.log("Storage corrupted, attempting repair.");
                String tbPath = mTbDeviceInfo.getRootFile().getAbsolutePath();
                String logFileName = mLogDirectory.open("chkdsk-reformat.txt").getAbsolutePath();
                mTbLoaderConfig.getCommandLineUtils().checkDiskAndFix(tbPath, logFileName);
                finishStep("Attempted repair of Talking Book storage");
            } else {
                finishStep("storage good");
            }
        } else {
            mProgressListener.log("chkdsk not supported on this OS");
        }
        return true;
    }

    protected String getDeploymentPropertiesForUserFeedback() throws IOException {
        // If there is a deployment.properties file on the device, we'll copy it as UF_FILENAME.properties for
        // every UF file.
        Properties feedbackProperties = new Properties();
        feedbackProperties.putAll(mTbDeviceInfo.loadDeploymentProperties());
        // Add the collection time properties. Prepend with "collection." to prevent collisions.
        collectionProperties(null).getProperties()
            .forEach((key, value) -> feedbackProperties.put("collection."+key.toString(), value.toString()));
        ByteArrayOutputStream bos = null;
        if (feedbackProperties.size() > 0) {
            bos = new ByteArrayOutputStream();
            feedbackProperties.store(bos, "User Feedback");
        }
        return bos!=null ? bos.toString() : null;
    }

    protected void createDeploymentPropertiesFile() throws IOException {
        TbFile system = mTalkingBookRoot.open("system");
        String currentFirmware = mOldDeploymentInfo.getFirmwareRevision();
        boolean needFirmware = !(mBuilder.mAcceptableFirmware.contains(currentFirmware)) ||
                currentFirmware.equals(UNKNOWN) || mBuilder.mRefreshFirmware;
        // 'properties' format file, with useful information for statistics gathering (next time around).
        PropsWriter props = new PropsWriter();
        props
                .append(TBLoaderConstants.TALKING_BOOK_ID_PROPERTY, mNewDeploymentInfo.getSerialNumber())
                .append(TBLoaderConstants.PROJECT_PROPERTY, mNewDeploymentInfo.getProjectName())
                .append(TBLoaderConstants.DEPLOYMENT_PROPERTY, mNewDeploymentInfo.getDeploymentName())
                .append(TBLoaderConstants.PACKAGE_PROPERTY, String.join(",", mNewDeploymentInfo.getPackageNames()))
                .append(TBLoaderConstants.COMMUNITY_PROPERTY, mNewDeploymentInfo.getCommunity())
                .append(TBLoaderConstants.TIMESTAMP_PROPERTY, mUpdateTimestampISO)
                .append(
                        TBLoaderConstants.TEST_DEPLOYMENT_PROPERTY, mNewDeploymentInfo.isTestDeployment())
                .append(TBLoaderConstants.USERNAME_PROPERTY, mTbLoaderConfig.getUserName())
                .append(TBLoaderConstants.USEREMAIL_PROPERTY, mTbLoaderConfig.getUserEmail())
                .append(TBLoaderConstants.TBCDID_PROPERTY, mTbLoaderConfig.getTbLoaderId())
                .append(TBLoaderConstants.NEW_SERIAL_NUMBER_PROPERTY, mNewDeploymentInfo.isNewSerialNumber())
                .append(TBLoaderConstants.LOCATION_PROPERTY, mLocation)
                .append(TBLoaderConstants.DEPLOYMENT_UUID_PROPERTY, mDeploymentUUID);
        if (mNewDeploymentInfo.getDeploymentNumber() > 0) {
            props.append(TBLoaderConstants.DEPLOYMENT_NUMBER_PROPERTY, mNewDeploymentInfo.getDeploymentNumber());
        }
        if (needFirmware) {
            props.append(TBLoaderConstants.FIRMWARE_PROPERTY, mNewDeploymentInfo.getFirmwareRevision());
        } else {
            props.append(TBLoaderConstants.LATEST_FIRMWARE_PROPERTY, mNewDeploymentInfo.getFirmwareRevision())
                    .append(TBLoaderConstants.FIRMWARE_PROPERTY, mOldDeploymentInfo.getFirmwareRevision());
        }
        if (mCoordinates != null && mCoordinates.length() > 0) {
            props.append(TBLoaderConstants.COORDINATES_PROPERTY, mCoordinates);
        }
        if (mNewDeploymentInfo.getRecipientid() != null) {
            props.append(TBLoaderConstants.RECIPIENTID_PROPERTY, mNewDeploymentInfo.getRecipientid());
        }
        eraseAndOverwriteFile(system.open(TBLoaderConstants.DEPLOYMENT_PROPERTIES_NAME), props.toString());
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
        void add(TBLoaderCore.DirectoryCount childCount) {
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
    private TBLoaderCore.DirectoryCount listDirectory(String rootPath, TbFile directory, StringBuilder buffer) {
        TBLoaderCore.DirectoryCount myCount = new TBLoaderCore.DirectoryCount();
        TbFile[] children = directory.listFiles();
        Arrays.sort(children, (o1, o2) -> {
            String n1 = o1.getName();
            String n2 = o2.getName();
            return n1.compareToIgnoreCase(n2);
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
                try {
                    mProgressListener.detail(child.getAbsolutePath().substring(rootPath.length()) + "/*");
                    myCount.add(listDirectory(rootPath, child, buffer));
                } catch (Exception ex) {
                    mProgressListener.detail(child.toString());
                    Map<String,String> exceptionInfo = new HashMap<>();
                    exceptionInfo.put("message", ex.getMessage());
                    exceptionInfo.put("rootPath", rootPath);
                    exceptionInfo.put("child", child.toString());
                    OperationLog.logEvent("listDirectory Exception", exceptionInfo);
                }
            }
        }

        return myCount;
    }

    private String getDeviceFileList() {
        StringBuilder builder = new StringBuilder();
        TBLoaderCore.DirectoryCount counts = listDirectory(mTalkingBookRoot.getAbsolutePath(), mTalkingBookRoot, builder);

        // Add the summary.
        builder.append(String.format(Locale.US, "%5cTotal Files Listed:\n%1$10c%6d File(s)%,15d bytes\n",
            ' ',
            counts.files,
            counts.size));
        builder.append(String.format(Locale.US, "%10c%6d Dir(s)", ' ', counts.dirs));
        mProgressListener.log(String.format(Locale.US, "%d files, %d dirs, %,d bytes", counts.files, counts.dirs, counts.size));
        long free = mTalkingBookRoot.getFreeSpace();
        if (free > 0) {
            builder.append(String.format(Locale.US, " %,15d bytes free", free));
            mProgressListener.log(true, String.format(Locale.US, "%,d bytes free", free));
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
     * Creates a file like a properties file.
     */
    protected /*static*/ class PropsWriter {
        private final Properties properties = new Properties();
        private final StringBuilder props = new StringBuilder();
        public String toString() { return props.toString(); }
        public Properties getProperties() {
            return properties;
        }
        public PropsWriter append(String name, Object value) {
            try {
                properties.setProperty(name, value.toString());
                props.append(name).append('=').append(value).append(MSDOS_LINE_ENDING);
            } catch (Exception ex) {
                mProgressListener.log("Exception adding property "+name);
            }
            return this;
        }
    }

    protected PropsWriter collectionProperties(PropsWriter props) {
        if (props == null)
            props = new PropsWriter();
        props
            .append(TBLoaderConstants.ACTION_PROPERTY, mStatsOnly?"stats":"update")
            .append(TBLoaderConstants.CLEARED_FLASH_PROPERTY, mClearedFlashStatistics)
            .append(TBLoaderConstants.TIMESTAMP_PROPERTY, mUpdateTimestampISO)
            .append(TBLoaderConstants.USERNAME_PROPERTY, mTbLoaderConfig.getUserName())
            .append(TBLoaderConstants.USEREMAIL_PROPERTY, mTbLoaderConfig.getUserEmail())
            .append(TBLoaderConstants.TBCDID_PROPERTY, mTbLoaderConfig.getTbLoaderId())
            .append(TBLoaderConstants.LOCATION_PROPERTY, mLocation)
            .append(TBLoaderConstants.STATS_COLLECTED_UUID_PROPERTY, mStatsCollectedUUID);
        if (mCoordinates != null && mCoordinates.length() > 0) {
            props.append(TBLoaderConstants.COORDINATES_PROPERTY, mCoordinates);
        }
        return props;
    }

    /**
     * Zips the statistics, logs, and other files, and copies them to the collected data directory.
     *
     * @param projectCollectedData The files will be zipped into a file in this directory.
     * @param action to be logged, for statistics.
     * @throws IOException If the .zip can't be created.
     */
    private void zipAndCopyFiles(TbFile projectCollectedData, String action) throws IOException {
        startStep(copyStatsAndFiles);

        // Create statsCollected.properties in the mTalkingBookDataRoot directory.
        // Include a newly allocated UUID, which will be written to tbData.log,
        // action, tbcdid, username, update_date_time, location, coordinates (if we have them)
        // Objective is to leave the mTalkingBookDataRoot with everythign known about the stats
        // collection, so that it can be processed without needing tbData.

        // 'properties' format file, with useful information for statistics processing.
        PropsWriter props = new PropsWriter();
        props.append(TBLoaderConstants.TB_LOG_ACTION_PROPERTY, action);
        collectionProperties(props);
        eraseAndOverwriteFile(mTalkingBookDataRoot.open(TBLoaderConstants.STATS_COLLECTED_PROPERTIES_NAME), props.toString());

        // Same name and location as the tempDirectory, but a file with a .zip extension.
        TbFile tempZip = mTalkingBookDataRoot.getParent().open(mCollectionTempName + ".zip");

        File sourceFilesDir = new File(mTalkingBookDataRoot.getAbsolutePath());
        File tempZipFile = new File(tempZip.getAbsolutePath());
        ZipUnzip.zip(sourceFilesDir, tempZipFile, true);

        // Where the .zip is supposed to go.
        TbFile outputZip = projectCollectedData.open(mTalkingBookDataZipPath);

        // Make the directory to hold the .zip, if necessary, then put it there.
        outputZip.getParent().mkdirs();
        mStepBytesCount += TbFile.copy(tempZip, outputZip);

        // Clean out everything we put in the temp directory. Any other cruft that was there, as well.
        mTempDirectory.delete(contentRecursive);
        finishStep();
    }


    /**
     * Determines the string to log for "action" in the tb log file.
     * @param gotStatistics Whether statistics were successfully retrieved.
     * @param verified Whether the TB was successfully verified after deployment.
     * @return the string best describing the action.
     */
    private String getActionString(boolean gotStatistics, boolean verified) {
        String action;
        if (mStatsOnly) {
            if (gotStatistics) {
                action = "stats-only";
            } else {
                action = "stats-error";
            }
        } else {
            if (verified) {
                if (mBuilder.mRefreshFirmware) {
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
        return action;
    }

    /**
     * Writes the TB data log file.
     *
     * @param action The string describing the action.
     */
    private void writeTbLog(String action) throws IOException {
        mProgressListener.log("Logging TB data");
        logTBData(action, getDurationInSeconds(mUpdateStartTime));
    }

    /**
     * Disconnects the Talking Book from the system.
     *
     * @throws IOException if there is an error disconnecting the drive. Note that on some OS
     * it may be completely impossible to disconnect the drive, and no exception is thrown.
     */
    private void disconnectDevice() throws IOException {
        if (OSChecker.WINDOWS) {
            mProgressListener.log("Disconnecting TB");
            mTbLoaderConfig.getCommandLineUtils().disconnectDrive(mTbDeviceInfo.getRootFile().getAbsolutePath());
        }
    }

    private void delayForAndroid() {
        if (mBuilder.mPostUpdateDelayMillis > 0) {
            long elapsed = 0;
            int n = 0;
            Random r = new Random();
            startStep(delay);
            while (elapsed < mBuilder.mPostUpdateDelayMillis) {
                mProgressListener.detail(String.format(Locale.US, "Finalizing part %d", ++n));
                int interval = 500 + r.nextInt(1000);
                elapsed += interval;
                try {
                    //noinspection BusyWait
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    // Ignore and continue
                }
            }
            finishStep();
        }
    }


    /**
     * Clears counters to start a new step.
     *
     * @param step The particular step being started.
     */
    protected void startStep(ProgressListener.Steps step) {
        mCurrentStep = step;
        mStepFileCount = 0;
        mStepBytesCount = 0;
        mStepStartTime = System.currentTimeMillis();
        mProgressListener.step(step);
    }

    /**
     * Helper to summarize counters and time at the end of a step.
     */
    protected void finishStep(String... resultStrings) {
        mStepsLog.split(mCurrentStep.toString()+".time");
        StringBuilder builder = new StringBuilder(mCurrentStep.description());
        for (String rs : resultStrings) {
            builder.append(", ").append(rs);
        }

        builder.append(": ");
        if (mCurrentStep.hasFiles) {
            mStepsLog.put(mCurrentStep + ".files", mStepFileCount);
            builder.append(String.format(Locale.US, "%d file(s), ", mStepFileCount));
            if (mStepBytesCount>0) {
                builder.append(String.format(Locale.US, "%s, ", getBytesString(mStepBytesCount)));
            }
        }
        builder.append(getStepTime());

        mProgressListener.log(builder.toString());
    }
    /**
     * Returns a nicely formatted string of a step's elapsed time.
     *
     * @return the nicely formatted string.
     */
    protected String getStepTime() {
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

    protected void eraseAndOverwriteFile(TbFile file, String content) throws IOException {
        mProgressListener.detail(file.getName());
        mStepFileCount++;
        byte[] contentBytes = content.getBytes();
        mStepBytesCount += contentBytes.length;
        if (file.getParent().exists()) {
            file.getParent().mkdirs();
        }
        InputStream is = new ByteArrayInputStream(contentBytes);
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
            elapsedTime = durationMinutes + " minutes " + durationSeconds + " seconds";
        } else
            elapsedTime = durationSeconds + " seconds";
        return elapsedTime;
    }

}
