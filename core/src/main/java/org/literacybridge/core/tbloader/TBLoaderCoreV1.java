package org.literacybridge.core.tbloader;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TbFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.literacybridge.core.fs.TbFile.Flags.contentRecursive;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearFeedbackCategories;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearStats;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearSystem;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearUserRecordings;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.gatherDeviceFiles;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.gatherUserRecordings;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.reformatting;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.relabelling;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateCommunity;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateContent;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateSystem;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR_OLD;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR_V1;
import static org.literacybridge.core.tbloader.TBLoaderConstants.OPERATIONAL_DATA;
import static org.literacybridge.core.tbloader.TBLoaderConstants.SYS_DATA_TXT;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_LANGUAGES_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_LISTS_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_MESSAGES_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.UNKNOWN;

class TBLoaderCoreV1 extends TBLoaderCore {
    // V1 user recording file format. Match like C-00120ABC*, or *_9_*.a18 or *_9-0_*.a18
    private static final Pattern UF_PATTERN = Pattern.compile("(?i)^(([abc]-\\p{XDigit}{8})|.*(_9_|_9-0_)).*\\.a18$");

    TBLoaderCoreV1(Builder builder) {
        super(builder);

        // This is the path name for the "TalkingBookData" from this TB. In particular, this is the path
        // name for the directory that will contain the collected data, and then the .zip file of that data.
        // like TalkingBookData/{Deployment name}/{tbloader id}/{community name}/{tb serial no}
        RelativePath talkingBookDataParentPath = new RelativePath(
            TBLoaderConstants.TALKING_BOOK_DATA,           // "TalkingBookData"
            mOldDeploymentInfo.getDeploymentName(),   // like "DEMO-2016-1"
            builder.mTbLoaderConfig.getTbLoaderId(),   // like "000c"
            mOldDeploymentInfo.getCommunity(),        // like "demo-seattle"
            mBuilder.mTbDeviceInfo.getSerialNumber());  // like "B-000C1234"


        // Like 2016y12m25d01h23m45s-000c. Also known as the "synch" directory.
        String mCollectionTempName = mLegacyFormatUpdateTimestamp + "-" + builder.mTbLoaderConfig.getTbLoaderId();

        // like TalkingBookData/{Deployment name}/{tbloader id}/{community name}/{tb serial no}/{timestamp}-{tbloader id}
        // like "2016y12m25d01h23m45s-000c"
        mTalkingBookDataDirectoryPath = new RelativePath(
            talkingBookDataParentPath, mCollectionTempName);
        // like "2016y12m25d01h23m45s-000c"
        mTalkingBookDataZipPath = new RelativePath(
            talkingBookDataParentPath,
            mCollectionTempName + ".zip");

    }

    protected final RelativePath mTalkingBookDataDirectoryPath;
    protected final RelativePath mTalkingBookDataZipPath;

    private TbFile projectCollectedData;
    private synchronized TbFile getProjectCollectedData() {
        if (projectCollectedData == null) {
            projectCollectedData = mCollectedDataDirectory.open(mOldDeploymentInfo.getProjectName());
            projectCollectedData.mkdirs();
        }
        return projectCollectedData;
    }

    TbFile imagesDir;
    TbFile tempTbDataDir;
    TbFile tempTbDataZip;
    TbFile collectedOpDataDir;
    TbFile collectedTbDataDir;
    TbFile collectedTbDataZip;
    TbFile collectedUfDataDir;

    @Override
    protected synchronized TbFile getImagesDir() {
        if (this.imagesDir == null) {
            TbFile imagesDir = mDeploymentDirectory.open(IMAGES_SUBDIR_V1);
            if (!imagesDir.isDirectory()) {
                imagesDir = mDeploymentDirectory.open(IMAGES_SUBDIR_OLD);
            }
            this.imagesDir = imagesDir;
        }
        return this.imagesDir;
    }

    @Override
    protected synchronized TbFile getTempTbDataDir() {
        if (tempTbDataDir == null) {
            mTempDirectory = mTbLoaderConfig.getTempDirectory();
            tempTbDataDir = mTempDirectory.open(mTalkingBookDataDirectoryPath);
            if (!tempTbDataDir.exists()) {
                tempTbDataDir.mkdirs();
            }
        }
        return tempTbDataDir;
    }

    @Override
    protected synchronized TbFile getTempTbDataZip() {
        if (tempTbDataZip == null) {
            // Same name and location as the tempDirectory, but a file with a .zip extension.
            tempTbDataZip = mTempDirectory.open(mTalkingBookDataZipPath);
            if (!tempTbDataZip.getParent().exists()) {
                tempTbDataZip.getParent().mkdirs();
            }
        }
        return tempTbDataZip;
    }

    @Override
    protected synchronized TbFile getCollectedOpDataDir() {
        if (collectedOpDataDir == null) {
            collectedOpDataDir = mCollectedDataDirectory         // like /Users/alice/Amplio/collectiondir
                .open(mOldDeploymentInfo.getProjectName())  // {tbloaderConfig.project}
                .open(OPERATIONAL_DATA)                     // "OperationalData"
                .open(mTbLoaderConfig.getTbLoaderId())      // {tbloaderConfig.tbLoaderId}
                .open("tbData");
            if (!collectedOpDataDir.exists()) {
                collectedOpDataDir.mkdirs();
            }
        }
        return collectedOpDataDir;
    }

    @Override
    protected synchronized TbFile getCollectedTbDataDir() {
        if (collectedTbDataDir == null) {
            collectedTbDataDir = mCollectedDataDirectory.open(mOldDeploymentInfo.getProjectName());
            if (!collectedTbDataDir.exists()) {
                collectedTbDataDir.mkdirs();
            }
        }
        return collectedTbDataDir;
    }

    @Override
    protected synchronized TbFile getCollectedTbDataZip() {
        if (collectedTbDataZip == null) {
            collectedTbDataZip = getProjectCollectedData().open(mTalkingBookDataZipPath);
            if (!collectedTbDataZip.getParent().exists()) {
                collectedTbDataZip.getParent().mkdirs();
            }
        }
        return collectedTbDataZip;
    }

    @Override
    protected synchronized TbFile getCollectedUfDataDir() {
        if (collectedUfDataDir == null) {
            // Build the user recordings destination path.
            collectedUfDataDir = getProjectCollectedData()           // like .../"tbcd1234/collected-data/UWR"
                .open(TBLoaderConstants.USER_RECORDINGS) // "UserRecordings"
                .open(mOldDeploymentInfo.getDeploymentName())  // like "UNICEF-2016-14"
                .open(mTbLoaderConfig.getTbLoaderId())         // like "000C"
                .open(mOldDeploymentInfo.getCommunity());      // like "VING VING"
        }
        return collectedUfDataDir;
    }

    String opLogSuffix;
    String opCsvFilename;
    @Override
    protected synchronized String getOpLogSuffix() {
        if (opLogSuffix == null) {
            // This is a format for a date format used a lot in TB statistics.
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy'y'MM'm'dd'd'", Locale.US);
            String strDate = sdfDate.format(new Date());
            opCsvFilename = String.format("tbData-%s-%s-%s.csv",
                TbLoaderLogger.VERSION_TBDATA,
                strDate,
                mTbLoaderConfig.getTbLoaderId());
            opLogSuffix = String.format("-%s-%s.log",
                strDate,
                mTbLoaderConfig.getTbLoaderId());
        }
        return opLogSuffix;
    }

    @Override
    protected String getOpCsvFilename() {
        if (opCsvFilename == null) {
            getOpLogSuffix();
        }
        return opCsvFilename;
    }

    /**
     * Copies a subset of files from the Talking Book. For logs, and for troubleshooting purposes.
     *
     * @throws IOException if we can't copy one of the files.
     */
    protected void gatherDeviceFiles() throws IOException {// And Copy from tbRoot to tempRoot, with filter.
        startStep(gatherDeviceFiles);
        // rem Collecting Usage Data
        // xcopy ${device_drive} "${syncpath}" /E /Y /EXCLUDE:software\scripts\exclude.txt /C
        //    /e : Copies all subdirectories, even if they are empty. Use /e with the /s and /t command-line options.
        //    /y : Suppresses prompting to confirm that you want to overwrite an existing destination file.
        //    /c : Ignores errors.
        // Filter to exclude certain named files or directories, anything starting with ".", or ending with ".img" or ".old".
        TbFile.CopyFilter copyFilesFilter = new TbFile.CopyFilter() {
            final Set<String> excludedNames = new HashSet<>(Arrays.asList("languages",
                "audio",
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
                // Hidden file? Skip them.
                if (name.charAt(0) == '.') {
                    return false;
                }
                // Image or backup file? Skip them.
                return !name.endsWith(".img") && !name.endsWith(".old");
            }
        };

        mStepBytesCount += TbFile.copyDir(mTalkingBookRoot, mTalkingBookDataRoot, copyFilesFilter, mCopyListener);

        finishStep();
    }

    /**
     * Copy user recordings from the Talking Book to the collected data directory.
     *
     * @throws IOException if a recording can't be copied.
     */
    protected void gatherUserRecordings() throws IOException {
        startStep(gatherUserRecordings);
        // rem Collecting User Recordings
        // mkdir "${recording_path}"
        // xcopy "${device_drive}\messages\audio\*_9_*.a18" "${recording_path}" /C
        // xcopy "${device_drive}\messages\audio\*_9-0_*.a18" "${recording_path}" /C

        // Build the user recordings source path.
        TbFile recordingsSrc = mTalkingBookRoot.open(TBLoaderConstants.TB_AUDIO_PATH);  // "messages/audio"
        // Build the user recordings destination path.
        TbFile recordingsDst = getCollectedUfDataDir();

        // If there is a deployment.properties file on the device, we'll copy it as UF_FILENAME.properties for
        // every UF file.
        boolean haveDeploymentProperties = getDeploymentPropertiesStringForUserFeedback() != null;

        // As user feedback files are copied, create a {recording-name}.properties file with the deployment
        // properties from when the recording was made.
        TbFile.CopyProgress localListener = (fromFile, toFile) -> {
            mCopyListener.copying(fromFile, toFile);
            if (haveDeploymentProperties) {
                try {
                    String infoName = FilenameUtils.getBaseName(toFile.getName()) + ".properties";
                    TbFile infoFile = toFile.getParent().open(infoName);
                    String deploymentPropertiesString = getDeploymentPropertiesStringForUserFeedback();
                    eraseAndOverwriteFile(infoFile, deploymentPropertiesString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };

        TbFile.CopyFilter copyRecordingsFilter = file -> {
            String name = file.getName();
            return UF_PATTERN.matcher(name).matches();
        };
        if (recordingsSrc.exists()) {
            mStepBytesCount += TbFile.copyDir(recordingsSrc, recordingsDst, copyRecordingsFilter, localListener);
        }

        finishStep();
    }

    /**
     * Erases files from the log, log-archive, and statistics directories on the Talking Book.
     */
    protected void clearStatistics() throws IOException {
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

        // If there is a system/sysdata.txt, copy it to the root. This will trigger the TB
        // to re-initialize the flash, and as a side effect, clear the flash statistics.
        TbFile system = mTalkingBookRoot.open("system");
        TbFile sysdata = system.open(SYS_DATA_TXT);
        if (system.isDirectory() && sysdata.exists()) {
            TbFile rootSysData = mTalkingBookRoot.open(SYS_DATA_TXT);
            TbFile.copy(sysdata, rootSysData);
            mClearedFlashStatistics = true;
        }

        finishStep();
    }

    /**
     * Erases user recordings from the Talking Book.
     */
    protected void clearUserRecordings() {
        startStep(clearUserRecordings);

        // rem Deleting User Recordings
        // del ${device_drive}\messages\audio\*_9_*.a18 /Q
        // del ${device_drive}\messages\audio\*_9-0_*.a18 /Q
        TbFile audioDirectory = mTalkingBookRoot.open(TBLoaderConstants.TB_AUDIO_PATH);
        String[] names = audioDirectory.list((parent, name) -> UF_PATTERN.matcher(name).matches());
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
    protected  void clearFeedbackCategories() {
        String[] names;
        startStep(clearFeedbackCategories);

        // rem Deleting User Feedback Category
        // for /f %f in ('dir ${device_drive}\messages\lists /b /AD') do del ${device_drive}\messages\lists\%f\9*.txt /Q
        TbFile listsDirectory = mTalkingBookRoot.open(TB_LISTS_PATH);
        names = listsDirectory.list();
        if (names != null) {
            for (String name : names) {
                TbFile listDirectory = listsDirectory.open(name);
                String[] toDelete = listDirectory.list((parent, name1) ->
                    name1.startsWith("9") && name1.toLowerCase().endsWith(".txt"));
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
     * Attempts to reformat the Talking Book if that's needed.
     * Attempts to relabel the Talking Book, if the label doesn't match the serial number.
     *
     * @return A Result if the TB needs reformatting, but can't be (ie, still needs reformatting.)
     * Returns null if format succeeded or not needed.
     * @throws IOException if the format or relabel fails.
     */
    protected Result reformatRelabel() throws IOException {
        Result result = null;
        boolean goodCard;
        if (mTbHasDiskCorruption) {
            mProgressListener.step(reformatting);
            if (!mTbLoaderConfig.hasCommandLineUtils()) {
                // distinction without a difference... has corruption, reformat didn't fail because
                // no reformat was attempted.
                result = new Result(0, false, true, Result.FORMAT_OP.noFormat, false);
            } else {
                goodCard = mTbLoaderConfig.getCommandLineUtils().formatDisk(mTbDeviceInfo.getRootFile()
                    .getAbsolutePath(), mTbDeviceInfo.getSerialNumber().toUpperCase());
                if (!goodCard) {
                    mProgressListener.log("Reformat failed");
                    mFormatOp = Result.FORMAT_OP.failed;
                    result = new Result(0, false, true, Result.FORMAT_OP.failed, false);
                } else {
                    mFormatOp = Result.FORMAT_OP.succeeded;
                    mProgressListener.log(String.format("Reformatted card, %s", getStepTime()));
                }
            }
        } else {
            if (!mNewDeploymentInfo.getSerialNumber().equalsIgnoreCase(mTbDeviceInfo.getLabelWithoutDriveLetter())) {
                if (!mTbLoaderConfig.hasCommandLineUtils()) {
                    mProgressListener.log("Skipping relabeling; not supported on this OS.");
                } else {
                    mProgressListener.step(relabelling);
                    mTbLoaderConfig.getCommandLineUtils().relabel(mTbDeviceInfo.getRootFile().getAbsolutePath(),
                        mNewDeploymentInfo.getSerialNumber());
                }
            }
        }
        return result;
    }

    /**
     * Clears old files from / /system/, and /languages/.
     * <p>
     * Derived from the "update.txt" file.
     *
     */
    @Override
    protected void clearSystemFiles() {
        startStep(clearSystem);

        mStepFileCount += mTalkingBookRoot.open("archive").delete(TbFile.Flags.recursive);
        mStepFileCount += mTalkingBookRoot.open("LOST.DIR").delete(TbFile.Flags.recursive);
        mStepFileCount += mTalkingBookRoot.open(TB_MESSAGES_PATH).delete(TbFile.Flags.recursive);
        mStepFileCount += mTalkingBookRoot.open(TB_LANGUAGES_PATH).delete(TbFile.Flags.recursive);

        // Clean files from root
        String[] names = mTalkingBookRoot.list((parent, name) -> {
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
        });
        if (names != null) {
            for (String name : names) {
                mProgressListener.detail(name);
                mStepFileCount += mTalkingBookRoot.open(name).delete(TbFile.Flags.recursive);
            }
        }

        // Delete files from /system
        TbFile system = mTalkingBookRoot.open("system");
        names = system.list((parent, name) -> {
            name = name.toLowerCase();
            return name.endsWith(".dep") ||
                name.endsWith(".grp") ||
                name.endsWith(".loc") ||
                name.endsWith(".pkg") ||
                name.endsWith(".prj") ||
                name.endsWith(".rtc") ||
                name.endsWith(".srn") ||
                name.endsWith(".txt");
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
        toDelete = mTalkingBookRoot.open(TB_LANGUAGES_PATH).open("control.bin");
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
    protected void updateSystemFiles() throws IOException {
        startStep(updateSystem);

        TbFile system = mTalkingBookRoot.open("system");

        // software\robocopy content\${new_deployment}\basic ${device_drive} /E /NP /XD .* /XA:H /XF *.srn *.rev
        //      /E    : Copy subdirectories, including Empty ones.
        //      /NP   : No ProgressListener - don't display percentage copied.
        //      /XD   : eXclude Directories matching given names/paths.
        //      /XA:H : eXclude files with any of the given Attributes set.
        //      /XF   : eXclude Files matching given names/paths/wildcards.
        String currentFirmware = mOldDeploymentInfo.getFirmwareRevision();
        boolean needFirmware = !(mBuilder.mAcceptableFirmware.contains(currentFirmware)) ||
            currentFirmware.equals(UNKNOWN) || mBuilder.mRefreshFirmware;
        if (needFirmware) {
            TbFile.CopyFilter firmwareFilter = file -> {
                String name = file.getName();
                // Copy anything that isn't .srn and isn't .rev. Should be only .img files.
                return !name.endsWith(".srn") && !name.endsWith(".rev");
            };
            TbFile firmwareDir = mDeploymentDirectory.open(TBLoaderConstants.CONTENT_BASIC_SUBDIR);
            if (!firmwareDir.isDirectory()) mDeploymentDirectory.open("firmware.v1");
            if (!firmwareDir.isDirectory()) mDeploymentDirectory.open("firmware");
            mStepBytesCount += TbFile.copyDir(firmwareDir,
                mTalkingBookRoot,
                firmwareFilter,
                mCopyListener);
        } else {
            mProgressListener.log(String.format(Locale.US,
                "Keeping firmware version %s (%s is latest)",
                currentFirmware,
                mNewDeploymentInfo.getFirmwareRevision()));
        }
        Calendar cal = Calendar.getInstance();
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        String dateInMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        String year = String.valueOf(cal.get(Calendar.YEAR));

        String projectName = mNewDeploymentInfo.getProjectName().toUpperCase();
        String communityName = mNewDeploymentInfo.getCommunity().toUpperCase();
        String srn = mNewDeploymentInfo.getSerialNumber().toUpperCase();
        String deploymentName = mNewDeploymentInfo.getDeploymentName().toUpperCase();
        String packageName = String.join(",", mNewDeploymentInfo.getPackageNames()).toUpperCase(); // aka 'image'
        String sysDataTxt = String.format("SRN:%s%s", srn, MSDOS_LINE_ENDING) +
            String.format("IMAGE:%s%s", packageName, MSDOS_LINE_ENDING) +
            String.format("UPDATE:%s%s", deploymentName, MSDOS_LINE_ENDING) +
            String.format("LOCATION:%s%s", communityName, MSDOS_LINE_ENDING) +
            String.format("YEAR:%s%s", year, MSDOS_LINE_ENDING) +
            String.format("MONTH:%s%s", month, MSDOS_LINE_ENDING) +
            String.format("DATE:%s%s", dateInMonth, MSDOS_LINE_ENDING) +
            String.format("PROJECT:%s%s", projectName, MSDOS_LINE_ENDING);
        eraseAndOverwriteFile(mTalkingBookRoot.open(TBLoaderConstants.SYS_DATA_TXT), sysDataTxt);
        // A side effect of the sysdata.txt file is to clear the flash statistics.
        mClearedFlashStatistics = true;
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
        //---------------------------------------------------------------------------------------------
        // TODO: remove these lines
        String mUpdateTimestamp = TBLoaderUtils.getDateTime(new Date());  // 2017Y09M28D22H31M52S
        // Like 2016y12m25d01h23m45s-000c. Also known as the "synch" directory.
        String mCollectionTempName = mUpdateTimestamp + "-" + mBuilder.mTbLoaderConfig.getTbLoaderId();
        //---------------------------------------------------------------------------------------------
        eraseAndOverwriteFile(system.open("last_updated.txt"), mCollectionTempName);
        eraseAndOverwriteFile(system.open(projectName + ".prj"), projectName);
        eraseAndOverwriteFile(system.open("notest.pcb"), ".");

        // 'properties' format file, with useful information for statistics gathering (next time around).
        createDeploymentPropertiesFile();

        finishStep();
    }

    /**
     * Updates content files on the Talking Book.
     * <p>
     * Derived from the "community.txt" file.
     *
     * @throws IOException if a file can't be copied.
     */
    protected void updateContent() throws IOException {
        startStep(updateContent);

        // Remember the zero-byte marker files here. Fix them up later.
        Map<String, TbFile> shadowedFiles = new HashMap<>();

        // The real files to replace zero-byte marker files.
        TbFile shadowFilesDir = mDeploymentDirectory.open("shadowFiles");

        // Iterate over the images to be copied.
        for (String imageName : mNewDeploymentInfo.getPackageNames()) {
            // Where files are copied from.
            TbFile imagePath = getImagesDir().open(imageName);

            // Directories in which to look for zero-byte marker files.
            TbFile audioShadowedDir = imagePath.open("messages").open("audio");
            TbFile languagesShadowedDir = imagePath.open("languages");
            List<String> shadowedDirs = new ArrayList<>();
            shadowedDirs.add(audioShadowedDir.getAbsolutePath());
            shadowedDirs.add(languagesShadowedDir.getAbsolutePath());

            // Filter to intercept zero-byte marker files, and track them for copying from their cache.
            Set<String> pathsToProcessLater = new HashSet<>();
            String profilesTxtPath = imagePath.open("system").open("profiles.txt").getAbsolutePath();
            String listDirPath = imagePath.open("messages").open("lists").open("1").getAbsolutePath();
            pathsToProcessLater.add(profilesTxtPath);
            pathsToProcessLater.add(listDirPath);
            TbFile.CopyFilter markerInterceptor = new ContentCopyFilter(imagePath, shadowedDirs, shadowFilesDir, shadowedFiles, pathsToProcessLater);

            if (imagePath.exists()) {
                // Copies most of the content, records 0-byte files that are shadows of real files, and skips
                // the profiles and profiles.txt, which will be merged later.
                mStepBytesCount += TbFile.copyDir(imagePath, mTalkingBookRoot, markerInterceptor, mCopyListener);
            }
        }

        // If we found zero-byte files that need to be replaced with real content, do that now.
        if (shadowedFiles.size()>0) {
            for (Map.Entry<String,TbFile> e : shadowedFiles.entrySet()) {
                TbFile targetFile = mTalkingBookRoot.open(RelativePath.parse(e.getKey()));
                TbFile sourceFile = e.getValue();
                // The single-file copy won't create directories, and it may not have been created in the
                // copy, so ensure it exists.
                TbFile targetDir = targetFile.getParent();
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                TbFile.copy(sourceFile, targetFile);
            }
        }

        // Copy the "lists" directories, renaming appropriately. Merge the profiles.txt files into one.
        copyListsAndProfiles();

        finishStep();
    }

    /**
     * Copies the "lists" files (the _activelist.txt and the txt files referred to in that file) to the target.
     * The sources are all named "lists/1", and the targets will be named "lists/1", "lists/2", etc.
     *
     * Merges the profiles.txt files into a single file.
     * @throws IOException if any file can't be read or written.
     */
    private void copyListsAndProfiles() throws IOException {
        int currentListIndex = 1;
        StringBuilder profilesTxtStr = new StringBuilder();
        for (String imageName : mNewDeploymentInfo.getPackageNames()) {
            String profileName = Integer.toString(currentListIndex);
            // Where files are copied from.
            TbFile imagePath = getImagesDir().open(imageName);

            TbFile listsSource = imagePath.open("messages").open("lists").open("1");
            TbFile listsTarget = mTalkingBookRoot.open("messages").open("lists").open(profileName);
            mStepBytesCount += TbFile.copyDir(listsSource, listsTarget, null, mCopyListener);

            try (
                InputStream is = imagePath.open("system").open("profiles.txt").openFileInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr)) {
                String line = br.readLine();
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    // A test only puts in a single value, not a comma separated list. We can pass it through unchanged.
                    parts[2] = profileName;
                }
                profilesTxtStr.append(String.join(",", parts)).append('\n');
            }
            currentListIndex += 1;
        }
        eraseAndOverwriteFile(mTalkingBookRoot.open("system").open("profiles.txt"), profilesTxtStr.toString());
    }


    /**
     * Updates community specific content files on the Talking Book.
     * <p>
     * Derived from the "community.txt" file.
     *
     * @throws IOException if a file can't be copied.
     */
    protected void updateCommunity() throws IOException {
        startStep(updateCommunity);
        // Keep 10.a18 and foo.grp files; skip recipient.id
        TbFile.CopyFilter filter = file -> {
            if (file.isDirectory()) return true;
            String name = file.getName().toLowerCase();
            return name.endsWith(".a18") || name.endsWith("*.grp");
        };

        TbFile communityPath = mDeploymentDirectory.open(TBLoaderConstants.COMMUNITIES_SUBDIR).open(
            mNewDeploymentInfo.getCommunity());
        if (communityPath.exists()) {
            mStepBytesCount += TbFile.copyDir(communityPath,
                mTalkingBookRoot, filter, mCopyListener);
        }

        finishStep();
    }

    /**
     * Examine the file system on the Talking Book, to see if it seems to have the right files.
     *
     * @return True if the TB looks good.
     */
    protected boolean verifyTalkingBook() {
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
    protected void forceFirmwareRefresh() {// rename firmware at root to system.img to force TB to update itself
        if (mBuilder.mRefreshFirmware) {
            mProgressListener.log("Forcing firmware refresh");
            TbFile firmware = mTalkingBookRoot.open(mNewDeploymentInfo.getFirmwareRevision() + ".img");
            TbFile newFirmware = mTalkingBookRoot.open("system.img");
            firmware.renameTo(newFirmware.getAbsolutePath());
        }
    }

}
