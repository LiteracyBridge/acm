package org.literacybridge.core.tbloader;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TbFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import static org.literacybridge.core.fs.TbFile.Flags.contentRecursive;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.*;

@SuppressWarnings("RedundantThrows")
class TBLoaderCoreV2 extends TBLoaderCore {

    public static final String BOOTCOUNT_TXT = "bootcount.txt";
    public static final String LOGS_DIR = "log";
    public static final String STATS_DIR = "stats";
    public static final String SYSTEM_DIR = "system";
    public static final String RECORDINGS_DIR = "recordings";
    public static final String SET_RTC_TXT = "SetRTC.txt";
    public static final String DONT_SET_RTC_TXT = "dontSetRTC.txt";

    TBLoaderCoreV2(Builder builder) {
        super(builder);
    }

    TbFile tempTbDataDir;
    TbFile tempTbDataZip;
    TbFile collectedOpDataDir;
    TbFile collectedTbDataDir;
    TbFile collectedTbDataZip;
    TbFile collectedUfDataDir;

    @Override
    protected synchronized TbFile getTempTbDataDir() {
        if (tempTbDataDir == null) {
            mTempDirectory = mTbLoaderConfig.getTempDirectory();
            tempTbDataDir = mTempDirectory.open(TBLoaderConstants.TALKING_BOOK_DATA);
            tempTbDataDir.mkdirs();
        }
        return tempTbDataDir;
    }

    @Override
    protected synchronized TbFile getTempTbDataZip() {
        if (tempTbDataZip == null) {
            tempTbDataZip = mTempDirectory.open(TBLoaderConstants.TALKING_BOOK_DATA+".zip");
            if (!tempTbDataZip.getParent().exists()) {
                tempTbDataZip.getParent().mkdirs();
            }
        }
        return tempTbDataZip;
    }

    @Override
    protected synchronized TbFile getCollectedOpDataDir() {
        if (collectedOpDataDir == null) {
            collectedOpDataDir = mCollectedDataDirectory.open("OperationalData");
            if (!collectedOpDataDir.exists()) {
                collectedOpDataDir.mkdirs();
            }
        }
        return collectedOpDataDir;
    }

    @Override
    protected synchronized TbFile getCollectedTbDataDir() {
        if (collectedTbDataDir == null) {
            collectedTbDataDir = mCollectedDataDirectory.open(TBLoaderConstants.TALKING_BOOK_DATA);
            if (!collectedTbDataDir.exists()) {
                collectedTbDataDir.mkdirs();
            }
        }
        return collectedTbDataDir;
    }

    @Override
    protected synchronized TbFile getCollectedTbDataZip() {
        if (collectedTbDataZip == null) {
            collectedTbDataZip = mCollectedDataDirectory.open(TBLoaderConstants.TALKING_BOOK_DATA+".zip");
            if (!collectedTbDataZip.getParent().exists()) {
                collectedTbDataZip.getParent().mkdirs();
            }
        }
        return collectedTbDataZip;
    }

    @Override
    protected synchronized TbFile getCollectedUfDataDir() {
        if (collectedUfDataDir == null) {
            collectedUfDataDir = mCollectedDataDirectory.open("userrecordings");
        }
        return collectedUfDataDir;
    }

    @Override
    protected void fixupDirectories() throws IOException {
        startStep(fixupDirectories);
        final Set<String> namesToFix = new HashSet<>(Arrays.asList("LOG",
                "STATS",
                "SYSTEM"));
        String[] names = mTalkingBookRoot.list((parent, name) -> namesToFix.contains(name));
        for (String name : names) {
            TbFile toFix = mTalkingBookRoot.open(name);
            String newName = mTalkingBookRoot.getAbsolutePath() + File.separatorChar + name + "_tl";
            toFix.renameTo(newName);
            toFix = mTalkingBookRoot.open(name+"_tl");
            newName = mTalkingBookRoot.getAbsolutePath() + File.separatorChar + name.toLowerCase();
            toFix.renameTo(newName);
        }

        finishStep();
    }


    @Override
    protected void gatherDeviceFiles() throws IOException {
        startStep(gatherDeviceFiles);
        // Filter to exclude certain named files or directories, anything starting with ".", or ending with ".img" or ".old".
        TbFile.CopyFilter copyFilesFilter = new TbFile.CopyFilter() {
            final Set<String> excludedNames = new HashSet<>(Arrays.asList("$recycle.bin",
                    RECORDINGS_DIR,
                "android",
                "config.bin",
                "lost.dir",
                "system volume information"));
            final Set<String> audioExtensions = new HashSet<>(Arrays.asList("wav", "mp3", "a18", "m3t", "ogg"));
            @Override
            public boolean accept(TbFile file) {
                String name = file.getName().toLowerCase();
                if (excludedNames.contains(name)) return false;
                if (name.charAt(0) == '.') return false;
                // Audio file? Skip them.
                String extension = FilenameUtils.getExtension(name);
                return !audioExtensions.contains(extension);
            }
        };
        mStepBytesCount += TbFile.copyDir(mTalkingBookRoot, mTalkingBookDataRoot, copyFilesFilter, mCopyListener);
        finishStep();
    }

    @Override
    protected void gatherUserRecordings() throws IOException {
        startStep(gatherUserRecordings);
        // Build the user recordings source path.
        TbFile recordingsSrc = mTalkingBookRoot.open(RECORDINGS_DIR);
        // Build the user recordings destination path.
        TbFile recordingsDst = getCollectedUfDataDir();

        // If there is a deployment.properties file on the device, we'll copy it as UF_FILENAME.properties for
        // every UF file.
        String deploymentPropertiesString = getDeploymentPropertiesForUserFeedback();

        // Watch the file progress, and for every file, create a *.properties
        TbFile.CopyProgress localListener = (fromFile, toFile) -> {
            mCopyListener.copying(fromFile, toFile);
            if (deploymentPropertiesString != null) {
                try {
                    String infoName = FilenameUtils.getBaseName(toFile.getName()) + ".properties";
                    TbFile infoFile = toFile.getParent().open(infoName);
                    eraseAndOverwriteFile(infoFile, deploymentPropertiesString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        if (recordingsSrc.exists()) {
            mStepBytesCount += TbFile.copyDir(recordingsSrc, recordingsDst, null, localListener);
        }
        finishStep();
    }

    @Override
    protected void clearStatistics() throws IOException {
        startStep(clearStats);
        mTalkingBookRoot.open(LOGS_DIR).delete(contentRecursive);
        mTalkingBookRoot.open(STATS_DIR).delete(contentRecursive);
        // Delete system/bootcount.txt so that the logs will start with a new number.
        TbFile system = mTalkingBookRoot.open(SYSTEM_DIR);
        system.open(BOOTCOUNT_TXT).delete();
        finishStep();
    }

    @Override
    protected void clearUserRecordings() {
        startStep(clearUserRecordings);
        mTalkingBookRoot.open(RECORDINGS_DIR).delete(contentRecursive);
        finishStep();
    }

    @Override
    protected void clearFeedbackCategories() {
        // TBD
    }

    private String encode(int i) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        StringBuilder result = new StringBuilder();
        i = Math.abs(i);
        while (i > 0) {
            char ch = chars.charAt(i%chars.length());
            result.insert(0, ch);
            i = i / chars.length();
        }
        return result.toString();
    }

    private String labelFromArmSn(String armSn) {
        String[] parts = armSn.split("\\.");
        int x = Integer.parseInt(parts[0], 16);
        int y = Integer.parseInt(parts[1], 16);
        int w = Integer.parseInt(parts[2], 16);
        String r = encode(x) + encode(y) +
                encode(w) +
                parts[3];
        if (r.length() > 11) r = r.substring(0, 11);
        return r;
    }

    @Override
    protected Result reformatRelabel() throws IOException {
        Result result = null;
        String diskLabel = labelFromArmSn(mTbDeviceInfo.getSerialNumber());
        boolean goodCard;
        if (mTbHasDiskCorruption) {
            mProgressListener.step(reformatting);
            if (!mTbLoaderConfig.hasCommandLineUtils()) {
                // distinction without a difference... has corruption, reformat didn't fail because
                // no reformat was attempted.
                result = new Result(0, false, true, Result.FORMAT_OP.noFormat, false);
            } else {
                goodCard = mTbLoaderConfig.getCommandLineUtils().formatDisk(mTbDeviceInfo.getRootFile()
                    .getAbsolutePath(), diskLabel.toUpperCase());
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
            if (StringUtils.isNotBlank(diskLabel) &&
                    !diskLabel.equalsIgnoreCase(mTbDeviceInfo.getLabelWithoutDriveLetter())) {
                if (!mTbLoaderConfig.hasCommandLineUtils()) {
                    mProgressListener.log("Skipping relabeling; not supported on this OS.");
                } else {
                    mProgressListener.step(relabelling);
                    mTbLoaderConfig.getCommandLineUtils().relabel(mTbDeviceInfo.getRootFile().getAbsolutePath(),
                        diskLabel.toUpperCase());
                }
            }
        }
        return result;
    }

    @Override
    protected void clearSystemFiles() throws IOException {
        startStep(clearSystem);
        mStepFileCount += mTalkingBookRoot.open("LOST.DIR").delete(TbFile.Flags.recursive);
        mStepFileCount += mTalkingBookRoot.open("$RECYCLE.BIN").delete(TbFile.Flags.recursive);
        // Clean files from root
        String[] names = mTalkingBookRoot.list((parent, name) -> {
            name = name.toLowerCase();
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
        // Delete all files except QC_PASS.txt and the ID files from /system
        TbFile system = mTalkingBookRoot.open(SYSTEM_DIR);
        Set<String> keepers = new HashSet<>(Arrays.asList("QC_PASS.TXT", "DEVICE_ID.TXT", "FIRMWARE_ID.TXT"));
        names = system.list((parent, name) -> !keepers.contains(name.toUpperCase()));
        if (names != null) {
            for (String name : names) {
                mProgressListener.detail(name);
                system.open(name).delete();
                mStepFileCount++;
            }
        }
        TbFile qcFile = system.open("QC_PASS.TXT");
        if (!qcFile.exists()) {
            eraseAndOverwriteFile(qcFile, "");
        }

        finishStep();
    }

    @Override
    protected void updateSystemFiles() throws IOException {
        startStep(updateSystem);
        mTalkingBookRoot.open(LOGS_DIR).mkdir();
        mTalkingBookRoot.open(RECORDINGS_DIR).mkdir();
        mTalkingBookRoot.open(STATS_DIR).mkdir();

        // 'properties' format file, with useful information for statistics gathering (next time around).
        createDeploymentPropertiesFile();

        finishStep();
    }

    @Override
    protected void updateSystemTime() throws IOException {
        // Sets the RTC clock.
        // Delete system/bootcount.txt so that the logs will start with a new number.
        TbFile system = mTalkingBookRoot.open(SYSTEM_DIR);
        eraseAndOverwriteFile(system.open(SET_RTC_TXT), "");
        system.open(DONT_SET_RTC_TXT).delete();
    }

    @Override
    protected void updateContent() throws IOException {
        startStep(updateContent);

        // Remember the zero-byte marker files here. Fix them up later.
        Map<String, TbFile> shadowedFiles = new HashMap<>();

        // The real files to replace zero-byte marker files.
        TbFile shadowFilesDir = mDeploymentDirectory.open("shadowFiles");

        // Iterate over the images to be copied.
        for (String imageName : mNewDeploymentInfo.getPackageNames()) {
            // Where files are copied from.
            TbFile imagePath = mDeploymentDirectory.open("images.v2").open(imageName);

            // Directories in which to look for zero-byte marker files.
            TbFile shadowedDir = imagePath.open("content");
            List<String> shadowedDirs = new ArrayList<>();
            shadowedDirs.add(shadowedDir.getAbsolutePath());

            // Filter to intercept zero-byte marker files, and track them for copying from their cache.
            Set<String> pathsToProcessLater = new HashSet<>();
            pathsToProcessLater.add(imagePath.open("content").open(PackagesData.PACKAGES_DATA_TXT).getAbsolutePath());
            TbFile.CopyFilter markerInterceptor = new ContentCopyFilter(imagePath, shadowedDirs, shadowFilesDir, shadowedFiles,
                pathsToProcessLater);

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

        // Merge/copy the package_data.txt file(s).
        copyPackageData();

        finishStep();
    }

    private void copyPackageData() throws IOException {
        PackagesData packagesData = new PackagesData(mNewDeploymentInfo.getDeploymentName());
        for (String imageName : mNewDeploymentInfo.getPackageNames()) {
            TbFile imagePath = mDeploymentDirectory.open("images.v2").open(imageName).open("content").open(PackagesData.PACKAGES_DATA_TXT);
            try (InputStream packageDataStream = imagePath.openFileInputStream()) {
                PackagesData.PackagesDataImporter pdi = new PackagesData.PackagesDataImporter(packageDataStream);
                PackagesData imageData = pdi.do_import();
                packagesData.addPackagesData(imageData);
            }
        }
        TbFile tbPackagesData = mTalkingBookRoot.open("content").open(PackagesData.PACKAGES_DATA_TXT);
        OutputStream tbPackagesDataStream = tbPackagesData.createNew();
        packagesData.exportPackageDataFile(tbPackagesDataStream);
    }

    @Override
    protected void updateCommunity() throws IOException {

    }

    @Override
    protected boolean verifyTalkingBook() {
        boolean verified;
        // A file, more or less at random.
        TbFile system = mTalkingBookRoot.open(SYSTEM_DIR);
        verified = system.open(TBLoaderConstants.DEPLOYMENT_PROPERTIES_NAME).exists();
        return verified;
    }

    @Override
    protected void forceFirmwareRefresh() {
        // no-op
    }

}
