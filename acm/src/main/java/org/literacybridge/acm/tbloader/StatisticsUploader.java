package org.literacybridge.acm.tbloader;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.core.fs.ZipUnzip;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatisticsUploader {
    private final TBLoader tbLoader;

    private final File collectionWorkDir;
    private final File uploadQueueDir;
    private final File uploadQueueCDDir;
    private final File uploadQueueDeviceDir;

    StatisticsUploader(TBLoader tbLoader,
        File collectionWorkDir,
        File uploadQueueDir,
        File uploadQueueCDDir,
        File uploadQueueDeviceDir) {

        this.tbLoader = tbLoader;
        this.collectionWorkDir = collectionWorkDir;
        this.uploadQueueDir = uploadQueueDir;
        this.uploadQueueCDDir = uploadQueueCDDir;
        this.uploadQueueDeviceDir = uploadQueueDeviceDir;
    }

    /**
     * Look for directories in collectionWorkDir. For any that are found, zip their contents into
     * a single file, and move it to the upload queue. Then, if the upload worker has not been
     * started, start it.
     */
    void zipAndUpload() {
        File[] uploadables = collectionWorkDir.listFiles();
        if (uploadables != null) {
            for (File uploadable : uploadables) {
                try {
                    if (uploadable.isDirectory()) {
                        File zipFile = new File(collectionWorkDir, uploadable.getName() + ".zip");
                        ZipUnzip.zip(uploadable, zipFile, true);
                        FileUtils.deleteDirectory(uploadable);
                        FileUtils.moveFileToDirectory(zipFile, uploadQueueDeviceDir, true);
                    } else {
                        FileUtils.moveFileToDirectory(uploadable, uploadQueueDeviceDir, true);
                    }
                } catch (IOException e) {
                    // This really shouldn't happen. If it does, then what?
                }
            }
        }
        if (uploadWorker == null) { // && testDeployment.isSelected()) {
            uploadWorker = new UploadWorker();
            uploadWorker.execute();
        } else {
            tbLoader.updateUploadStatus(new StatisticsUploader.UploadStatus(getUploadQueue()));
        }
    }

    /**
     * Helper class to upload stats and user feedback to S3.
     */
    class UploadWorker extends SwingWorker<UploadStatus, UploadStatus> {
        final Path uploadQueuePath = Paths.get(uploadQueueDir.getAbsolutePath());

        @Override
        protected StatisticsUploader.UploadStatus doInBackground() throws Exception {
            final String bucket = "acm-stats";
            final Authenticator authInstance = Authenticator.getInstance();
            List<File> queue = getUploadQueue();
            while (!isCancelled()) {
                if (queue.size() == 0) {
                    queue = getUploadQueue();
                }
                publish(new StatisticsUploader.UploadStatus(queue));
                if (!authInstance.isOnline()) {
                    // Currently, we never go offline->online. But if the implementation changes
                    // such that it does, this code will at least work.
                    Thread.sleep(60000);
                } else if (queue.size() > 0) {
                    File next = queue.remove(0);
                    Path keyPath = Paths.get(next.getAbsolutePath());
                    Path relativePath = uploadQueuePath.relativize(keyPath);
                    String key = relativePath.toString();
                    System.out.printf("%s => ", key);
                    key = key.replaceAll("\\\\", "/");
                    System.out.printf("%s\n", key);
                    if (authInstance.getAwsInterface().uploadS3Object(bucket, key, next)) {
                        next.delete();
                    }
                    Thread.sleep(2000);
                } else {
                    Thread.sleep(10000);
                }
            }
            return null;
        }

        protected void process(List<StatisticsUploader.UploadStatus> list) {
            StatisticsUploader.UploadStatus progress = list.get(list.size() - 1);
            tbLoader.updateUploadStatus(progress);
        }
    }
    UploadWorker uploadWorker;


    /**
     * Scans the upload queue for anything waiting to be uploaded.
     * @return a list of files in the upload queue.
     */
    private List<File> getUploadQueue() {
        List<File> result = new ArrayList<>();
        StatisticsUploader.addFiles(uploadQueueCDDir, result, true);
        result.sort(Comparator.comparingLong(File::length));
        return result;
    }

    /**
     * Helper for recursively getting the files in a directory.
     * @param file a file to be added, or a directory to be scanned recursively.
     * @param list of files to be appended to.
     */
    static void addFiles(File file, List<File> list, boolean removeEmpty) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] dirList = file.listFiles();
            if (dirList.length == 0 && removeEmpty) {
                file.delete();
            } else {
                for (File f : dirList)
                    addFiles(f, list, removeEmpty);
            }
        } else {
            list.add(file);
        }
    }

    /**
     * Object to hold upload status: number of files, number of bytes.
     */
    static class UploadStatus {
        List<File> queued;
        int nFiles;
        long nBytes;

        public UploadStatus(List<File> files) {
            nFiles = files.size();
            nBytes = files.stream().map(File::length).reduce(0L, Long::sum);
        }
    }


}
