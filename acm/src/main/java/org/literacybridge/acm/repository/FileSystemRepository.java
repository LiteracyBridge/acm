package org.literacybridge.acm.repository;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.mutable.MutableLong;
import org.literacybridge.acm.db.AudioItem;
import org.literacybridge.acm.utils.IOUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class FileSystemRepository extends AudioItemRepository {
    public static class FileSystemGarbageCollector {
        private final long maxSizeInBytes;
        private final FilenameFilter filesToDelete;

        public FileSystemGarbageCollector(long maxSizeInBytes,
                FilenameFilter filesToDelete) {
            this.maxSizeInBytes = maxSizeInBytes;
            this.filesToDelete = filesToDelete;
        }

        private long calculateCurrentSizeInBytes(File repositoryRoot) throws IOException {
            final MutableLong sizeInBytes = new MutableLong();
            IOUtils.visitFiles(repositoryRoot, filesToDelete, new Predicate<File>() {
                @Override public boolean apply(File file) {
                    sizeInBytes.add(file.length());
                    return true;
                }
            });
            return sizeInBytes.longValue();
        }

        public GCInfo needsGc(File repositoryRoot) throws IOException {
            long currentSize = calculateCurrentSizeInBytes(repositoryRoot);
            return new GCInfo(currentSize > maxSizeInBytes, currentSize, maxSizeInBytes);
        }

        public void gc(File repositoryRoot) throws IOException {
            GCInfo gcInfo = needsGc(repositoryRoot);
            long sizeInBytes = gcInfo.getCurrentSizeInBytes();

            if (gcInfo.isGcRecommended()) {
                final List<File> allFiles = Lists.newArrayList();
                IOUtils.visitFiles(repositoryRoot, filesToDelete, new Predicate<File>() {
                    @Override public boolean apply(File file) {
                        allFiles.add(file);
                        return true;
                    }
                });

                Collections.sort(allFiles, new Comparator<File>() {
                    @Override public int compare(File f1, File f2) {
                        return new Long(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });

                // make sure never to delete the most recent file
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
    }

    private final File baseDir;
    private final FileSystemGarbageCollector garbageCollector;

    public FileSystemRepository(File baseDir) {
        this(baseDir, null);
    }

    public FileSystemRepository(File baseDir, FileSystemGarbageCollector garbageCollector) {
        this.baseDir = baseDir;
        this.garbageCollector = garbageCollector;
    }

    @Override
    protected File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
        return new File(resolveDirectory(audioItem, format), audioItem.getUuid() + "." + format.getFileExtension());
    }

    protected File resolveDirectory(AudioItem audioItem, AudioFormat format) {
        // TODO: For now we just use the unique ID of the audio item; in the future, we might want to use
        // a different way to construct the path

        StringBuilder builder = new StringBuilder();
        builder.append(baseDir.getAbsolutePath());
        builder.append(File.separator);
        builder.append("org");
        builder.append(File.separator);
        builder.append("literacybridge");
        builder.append(File.separator);
        builder.append(audioItem.getUuid());

        String path = builder.toString();
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    @Override
    public GCInfo needsGc() throws IOException {
        if (garbageCollector == null) {
            return new GCInfo(false, 0, 0);
        }

        return garbageCollector.needsGc(baseDir);
    }

    @Override
    public void gc() throws IOException {
        if (garbageCollector != null) {
            garbageCollector.gc(baseDir);
        }
    }
}
