package org.literacybridge.androidtbloader.uploader;

import java.io.File;
import java.util.Comparator;
import java.util.Date;

/**
 * Class to describe an item in the upload queue. Used in a PriorityQueue.
 */
class UploadItem {
    File file;
    Date timestamp;
    long size;
    long startTime;
    long elapsedMillis=-1;
    boolean success;

    UploadItem(File file) {
        this.file = file;
        this.timestamp = new Date(file.lastModified());
        this.size = file.length();
    }

    void uploadStarted() {
        startTime = System.currentTimeMillis();
    }
    private void uploadEnded(boolean success) {
        this.timestamp = new Date(System.currentTimeMillis());
        elapsedMillis = Math.max(System.currentTimeMillis() - startTime, 0);
        this.success = success;
    }
    void uploadFailed() {
        uploadEnded(false);
    }
    void uploadSucceeded() {
        uploadEnded(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        UploadItem that = (UploadItem) o;

        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    /**
     * Comparator to assist using the class in a PriorityQueue.
     */
    static class QueuedUploadItemComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem lhs, UploadItem rhs) {
            // The smaller file is the higher priority, so it should compare as 'less'.
            long diff = lhs.size - rhs.size;
            if (diff < 0) return -1;
            if (diff > 0) return 1;
            return 0;
        }
    }
}

