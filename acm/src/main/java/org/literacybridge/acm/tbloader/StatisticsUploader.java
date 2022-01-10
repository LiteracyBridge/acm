package org.literacybridge.acm.tbloader;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.core.fs.ZipUnzip;

import javax.swing.SwingWorker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatisticsUploader {
    private final TBLoader tbLoader;

    private final File uploadQueueDir;

    private boolean suppressUpload = ACMConfiguration.getInstance().isSuppressStatisticsUpload();

    StatisticsUploader(TBLoader tbLoader,
        File uploadQueueDir) {

        this.tbLoader = tbLoader;
        this.uploadQueueDir = uploadQueueDir;
    }

    /**
     * Enqueue for upload any files in fromDirectory. Any sub-directories in fromDirectory
     * will be zipped to a file with the sub-directory name + ".zip", then enqueued.
     *
     * @param fromDirectory A directory from which to enqueue files.
     * @param keyPrefix     A prefix to prepend the file name to construct the S3 key, for
     *                      example "collected-data.v2/tbcd000c"
     */
    void zipAndEnqueue(File fromDirectory, String keyPrefix) {
        if (!fromDirectory.isDirectory()) {
            throw new IllegalArgumentException("'fromDirectory' must be a directory");
        }
        File uploadTargetDir = new File(uploadQueueDir, keyPrefix);
        File[] uploadables = fromDirectory.listFiles();
        if (uploadables != null) {
            for (File uploadable : uploadables) {
                try {
                    if (uploadable.isDirectory()) {
                        File zipFile = new File(fromDirectory, uploadable.getName() + ".zip");
                        ZipUnzip.zip(uploadable, zipFile, true);
                        FileUtils.deleteDirectory(uploadable);
                        FileUtils.moveFileToDirectory(zipFile, uploadTargetDir, true);
                    } else {
                        FileUtils.moveFileToDirectory(uploadable, uploadTargetDir, true);
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
                    // Given an uploadQueue ~/Users/alice/Amplio/uploadQueue, and a file in that directory
                    // collected-data/tbcd000c/20220110T085217.123Z.zip, upload the file to
                    // s3://acm-stats/collected-data/tbcd000c/2022-110T085217.123Z.zip
                    File nextFile = queue.remove(0);
                    Path keyPath = Paths.get(nextFile.getAbsolutePath());
                    Path relativePath = uploadQueuePath.relativize(keyPath);
                    String key = relativePath.toString();
                    System.out.printf("%s => ", key);
                    key = key.replaceAll("\\\\", "/");
                    System.out.printf("%s\n", key);
                    if (suppressUpload) {
                        System.out.println("** upload suppressed by config; deleting **");
                        nextFile.delete();
                    } else if (authInstance.getAwsInterface().uploadS3Object(bucket, key, nextFile)) {
                        nextFile.delete();
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
     *
     * @return a list of files in the upload queue.
     */
    private List<File> getUploadQueue() {
        List<File> result = new ArrayList<>();
        StatisticsUploader.addFiles(uploadQueueDir, result, true);
        result.sort(Comparator.comparingLong(File::length));
        return result;
    }

    /**
     * Helper for recursively getting the files in a directory.
     *
     * @param file        a file to be added, or a directory to be scanned recursively.
     * @param list        of files to be appended to.
     * @param removeEmpty if true, rmdir empty directories.
     */
    private static void addFiles(File file, List<File> list, boolean removeEmpty) {
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
