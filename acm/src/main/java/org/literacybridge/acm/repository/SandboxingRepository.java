package org.literacybridge.acm.repository;

import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.sandbox.Sandbox;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class SandboxingRepository implements FileRepositoryInterface {

    final private Sandbox sandbox;
    final private File contentDir;

    SandboxingRepository(Sandbox sandbox, File contentDir) {
        this.sandbox = sandbox;
        this.contentDir = contentDir;
    }

    public boolean isSandboxedFile(File file) {
        return sandbox.isSandboxedFile(file);
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
    @Override
    public File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
        Path path = resolveDirectoryPath(audioItem.getId()).resolve(audioItem.getId() + "." + format.getFileExtension());
        return writeAccess ? sandbox.outputFile(path) : sandbox.inputFile(path);
    }

    @Override
    public List<String> getAudioItemIds() {
        List<String> result = new ArrayList<>();
        Path contentPath = contentDir.toPath().resolve(contentDirName);
        File contentDir = new File(this.contentDir, "org" + File.separator + "literacybridge");
        if (sandbox.isDirectory(contentPath)) {
            // The sub-directories in the content directory are the ids. Only count them if they contain files.
            for (Path subPath : sandbox.listPaths(contentPath)) {
                if (sandbox.listPaths(subPath).size() > 0) {
                    result.add(subPath.getFileName().toString());
                }
            }
        }
        return result;
    }

    @Override
    public void delete(String id) {
        Path path = resolveDirectoryPath(id);
        sandbox.removeRecursive(path);
    }

    @Override
    public long size(String id) {
        Path path = resolveDirectoryPath(id);
        Collection<Path> children = sandbox.listPaths(path);
        long size = children.stream().map(Path::toFile).map(File::length).reduce(0L, Long::sum);
        return size;
    }

    @Override
    public Path basePath() {
        return this.contentDir.toPath();
    }

}
