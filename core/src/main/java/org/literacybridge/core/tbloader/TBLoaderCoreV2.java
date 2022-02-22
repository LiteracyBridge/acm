package org.literacybridge.core.tbloader;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TbFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.literacybridge.core.fs.TbFile.Flags.contentRecursive;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearStats;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearSystem;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.clearUserRecordings;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.gatherDeviceFiles;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.gatherUserRecordings;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.reformatting;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.relabelling;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateContent;
import static org.literacybridge.core.tbloader.ProgressListener.Steps.updateSystem;

@SuppressWarnings("RedundantThrows")
class TBLoaderCoreV2 extends TBLoaderCore {
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
    protected void gatherDeviceFiles() throws IOException {
        startStep(gatherDeviceFiles);
        // Filter to exclude certain named files or directories, anything starting with ".", or ending with ".img" or ".old".
        TbFile.CopyFilter copyFilesFilter = new TbFile.CopyFilter() {
            final Set<String> excludedNames = new HashSet<>(Arrays.asList("$recycle.bin",
                "recordings",
                "android",
                "config.bin",
                "lost.dir",
                "system volume information"));
            @Override
            public boolean accept(TbFile file) {
                String name = file.getName().toLowerCase();
                if (excludedNames.contains(name)) return false;
                if (name.charAt(0) == '.') return false;
                // Audio file? Skip them.
                return !name.endsWith(".wav") && !name.endsWith(".mp3") && !name.endsWith(".a18");
            }
        };
        mStepBytesCount += TbFile.copyDir(mTalkingBookRoot, mTalkingBookDataRoot, copyFilesFilter, mCopyListener);
        finishStep();
    }

    @Override
    protected void gatherUserRecordings() throws IOException {
        startStep(gatherUserRecordings);
        // Build the user recordings source path.
        TbFile recordingsSrc = mTalkingBookRoot.open("recordings");
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
        mTalkingBookRoot.open("log").delete(contentRecursive);
        mTalkingBookRoot.open("stats").delete(contentRecursive);
        finishStep();
    }

    @Override
    protected void clearUserRecordings() {
        startStep(clearUserRecordings);
        mTalkingBookRoot.open("recordings").delete(contentRecursive);
        finishStep();
    }

    @Override
    protected void clearFeedbackCategories() {
        // TBD
    }

    private String labelFromArmSn(String armSn) {
        int lastDotIx = armSn.lastIndexOf('.');
        if (lastDotIx >= 0 && lastDotIx<armSn.length()-1) armSn = armSn.substring(lastDotIx+1);
        return armSn;
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
        TbFile system = mTalkingBookRoot.open("system");
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
        mTalkingBookRoot.open("log").mkdir();
        mTalkingBookRoot.open("recordings").mkdir();
        mTalkingBookRoot.open("stats").mkdir();

        // 'properties' format file, with useful information for statistics gathering (next time around).
        createDeploymentPropertiesFile();

        finishStep();
    }

    @Override
    protected void updateSystemTime() throws IOException {
        // Sets the RTC clock.
        eraseAndOverwriteFile(mTalkingBookRoot.open(Arrays.asList("system", "SetRTC.txt")), "");
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
        TbFile system = mTalkingBookRoot.open("system");
        verified = system.open("SetRTC.txt").exists();
        return verified;
    }

    @Override
    protected void forceFirmwareRefresh() {
        // no-op
    }

}
