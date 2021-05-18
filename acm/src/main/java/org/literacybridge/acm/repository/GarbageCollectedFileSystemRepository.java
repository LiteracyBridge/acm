package org.literacybridge.acm.repository;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.mutable.MutableLong;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.BackgroundTaskManager;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class GarbageCollectedFileSystemRepository extends FileSystemRepository {
    private final FileSystemGarbageCollector garbageCollector;
    private WavFilePreCaching caching = null;

    GarbageCollectedFileSystemRepository(File baseDir, long cacheSizeInBytes) {
        super(baseDir);
        String wavExt = "." + AudioItemRepository.AudioFormat.WAV.getFileExtension();

        this.garbageCollector = new FileSystemGarbageCollector(
            cacheSizeInBytes,
            (file, name) -> name.toLowerCase().endsWith(wavExt));
    }

    public synchronized void setupWavCaching(Predicate<Long> gcQuery) throws IOException {
        if (caching == null) {
            caching = new WavFilePreCaching();
        }
        FileSystemGarbageCollector.GCInfo gcInfo = getGcInfo();

        if (gcInfo.isGcRecommended()) {
            long sizeMB = gcInfo.getCurrentSizeInBytes() / 1024 / 1024;
            if (!caching.hasUncachedA18Files()) {
                if (gcQuery.test(sizeMB)) {
                    gc();
                }
            }
        }

        if (ACMConfiguration.getInstance().getCurrentDB().isShouldPreCacheWav()) {
            caching.cacheNewA18Files();
        }
    }


    public FileSystemGarbageCollector.GCInfo getGcInfo() throws IOException {
        if (garbageCollector == null) {
            return new FileSystemGarbageCollector.GCInfo(false, 0, 0);
        }

        return garbageCollector.getGcInfo(baseDir);
    }

    public void gc() throws IOException {
        if (garbageCollector != null) {
            garbageCollector.gc(baseDir);
        }
    }

    /**
     * A helper to pre-cache .wav files, so that they're immediately available for playback.
     *
     * As of May 2021, this feature is unused.
     */
    public static class WavFilePreCaching {

        private final Set<AudioItem> uncachedAudioItems = new HashSet<>();

        public WavFilePreCaching() {
            findUncachedWaveFiles();
        }

        public boolean hasUncachedA18Files() {
            return !uncachedAudioItems.isEmpty();
        }

        public void cacheNewA18Files() {
            Application.getApplication().getTaskManager().execute(new Task());
        }

        private void findUncachedWaveFiles() {
            System.out.print("Finding uncached wave files...");
            AudioItemRepository repository = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getRepository();

            for (AudioItem audioItem : ACMConfiguration.getInstance()
                .getCurrentDB()
                .getMetadataStore()
                .getAudioItems()) {
                if (repository.findAudioFileWithFormat(audioItem, AudioItemRepository.AudioFormat.WAV) != null) {
                    uncachedAudioItems.add(audioItem);
                }
            }
            System.out.println("done");
        }

        private class Task extends BackgroundTaskManager.ExtendedSwingWorker<Void, Void> {
            String itemBeingProcessed;

            /*
             * Main task. Executed in background thread.
             */
            @Override
            public Void doInBackground() {
                int progress = 0;
                // Initialize progress property.
                setProgress(0);
                Iterator<AudioItem> it = uncachedAudioItems.iterator();
                while (it.hasNext() && !isCancelled()) {
                    AudioItem item = it.next();
                    try {
                        itemBeingProcessed = item.getId();
                        System.out.println("Converting " + itemBeingProcessed);
                        ACMConfiguration.getInstance()
                            .getCurrentDB()
                            .getRepository()
                            .getAudioFile(item, AudioItemRepository.AudioFormat.WAV);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    progress++;
                    setProgress((int) ((float) progress / uncachedAudioItems.size() * 100.0));
                }
                itemBeingProcessed = "done";
                return null;
            }

            @Override
            public String toString() {
                String s = "Converting ";
                if (itemBeingProcessed != null) {
                    s += itemBeingProcessed;
                }
                return s;
            }
        }
    }

    /**
     * A helper to remove old .wav files to free up disk space.
     */
    static class FileSystemGarbageCollector {
        private final long maxSizeInBytes;
        private final FilenameFilter filesToDelete;

        FileSystemGarbageCollector(long maxSizeInBytes, FilenameFilter filesToDelete) {
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
}
