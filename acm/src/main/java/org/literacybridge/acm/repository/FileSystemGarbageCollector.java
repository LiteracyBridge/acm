package org.literacybridge.acm.repository;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.mutable.MutableLong;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

public class FileSystemGarbageCollector {
    private final long maxSizeInBytes;
    private final FilenameFilter filesToDelete;

    public FileSystemGarbageCollector(long maxSizeInBytes, FilenameFilter filesToDelete) {
        this.maxSizeInBytes = maxSizeInBytes;
        this.filesToDelete = filesToDelete;
    }

    private long calculateCurrentSizeInBytes(File repositoryRoot) throws IOException
    {
        final MutableLong sizeInBytes = new MutableLong();
        IOUtils.visitFiles(repositoryRoot, filesToDelete, file -> {
            sizeInBytes.add(file.length());
            return true;
        });
        return sizeInBytes.longValue();
    }

    GCInfo getGcInfo(File repositoryRoot) throws IOException {
        long currentSize = calculateCurrentSizeInBytes(repositoryRoot);
        return new GCInfo(currentSize > maxSizeInBytes,
            currentSize,
            maxSizeInBytes);
    }

    void gc(File repositoryRoot) throws IOException {
        GCInfo gcInfo = getGcInfo(repositoryRoot);
        long sizeInBytes = gcInfo.getCurrentSizeInBytes();

        if (gcInfo.isGcRecommended()) {
            // Get a filtered list of files in the repository subject to collection.
            final List<File> allFiles = Lists.newArrayList();
            IOUtils.visitFiles(repositoryRoot, filesToDelete, file -> {
                allFiles.add(file);
                return true;
            });
            // Sort by oldest to newest.
            allFiles.sort((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

            // Delete the oldest files until we've freed enough. Do not delete the most recent
            // file (though the next-most-recent may be from the same operation at the same time?)
            for (int i = 0; i < allFiles.size() - 1; i++) {
                File file = allFiles.get(i);
                long size = file.length();
                if (file.delete()) {
                    sizeInBytes -= size;
                    if (sizeInBytes <= maxSizeInBytes) {
                        break;
                    }
                }

            }
        }
    }

    public static final class GCInfo {
        private final boolean gcRecommended;
        private final long currentSizeInBytes;
        private final long maxSizeInBytes;

        GCInfo(boolean gcRecommended, long currentSizeInBytes, long maxSizeInBytes) {
            this.gcRecommended = gcRecommended;
            this.currentSizeInBytes = currentSizeInBytes;
            this.maxSizeInBytes = maxSizeInBytes;
        }

        public boolean isGcRecommended() {
            return gcRecommended;
        }

        public long getCurrentSizeInBytes() {
            return currentSizeInBytes;
        }

        public long getMaxSizeInBytes() {
            return maxSizeInBytes;
        }
    }
}
