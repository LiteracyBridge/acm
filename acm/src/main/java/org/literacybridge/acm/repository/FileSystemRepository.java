package org.literacybridge.acm.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;

public class FileSystemRepository implements FileRepositoryInterface {

    private final File baseDir;
    private final FileSystemGarbageCollector garbageCollector;

    public FileSystemRepository(File baseDir) {
        this(baseDir, null);
    }

    public FileSystemRepository(File baseDir, FileSystemGarbageCollector garbageCollector) {
        this.baseDir = baseDir;
        this.garbageCollector = garbageCollector;
    }

    /**
     * Creates a File in which may be stored, or to store an audio file. The directory path is
     * constructed by resolveDirectory, and the complete path name will be like:
     * org/literacybridge/{file-id}/{file-id}.a18
     *
     * @param audioItem   The audio item for which to construct the path to the File.
     * @param format      The audio format, like A18 or MP3
     * @param writeAccess (unused) If write access is desired.
     * @return A File object representing the physical file.
     */
    public File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
        return new File(resolveDirectory(audioItem.getUuid()),
            audioItem.getUuid() + "." + format.getFileExtension());
    }

    /**
     * Creates a path under which to store a file, in the baseDir directory. The baseDir may be like
     * ~/Dropbox/ACM-FOO/content/ (globalSharedRepository)
     * ~/LiteracyBridge/ACM/cache/ACM-FOO/ (localCacheRepository)
     * ~/LiteracyBridge/ACM/temp/ACM-FOO/content/ (sandboxRepository
     * <p>
     * The path will be like
     * org/literacybridge/{file-id}
     *
     * @param id The id of the audio item for which to construct the path to the containing directory.
     * @return A File representing the containing directory.
     */
    private File resolveDirectory(String id) {
        // TODO: For now we just use the unique ID of the audio item; in the future,
        // we might want to use
        // a different way to construct the path

        StringBuilder builder = new StringBuilder();
        builder.append(baseDir.getAbsolutePath());
        builder.append(File.separator);
        builder.append("org");
        builder.append(File.separator);
        builder.append("literacybridge");
        builder.append(File.separator);
        builder.append(id);

        String path = builder.toString();
        File dir = new File(path);

        return dir;
    }

    public List<String> getAudioItemIds(Repository repo) {
        List<String> result = new ArrayList<>();
        File contentDir = new File(baseDir, "org" + File.separator + "literacybridge");
        if (contentDir.exists() && contentDir.isDirectory()) {
            for (File audioDir : contentDir.listFiles()) {
                if (audioDir.exists() && audioDir.isDirectory()) {
                    String[] audioFiles = audioDir.list();
                    if (audioFiles != null && audioFiles.length > 0) {
                        result.add(audioDir.getName());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void delete(String id) {
        File audioDir = resolveDirectory(id);
        FileUtils.deleteQuietly(audioDir);
    }

    @Override
    public long size(String id) {
        File audioDir = resolveDirectory(id);
        long total = 0;
        if (audioDir.exists() && audioDir.isDirectory()) {
            for (File f : audioDir.listFiles())
                total += f.length();
        }
        return total;
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
}
