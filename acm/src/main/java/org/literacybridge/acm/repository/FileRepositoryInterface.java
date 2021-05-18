package org.literacybridge.acm.repository;

import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

interface FileRepositoryInterface {
    /**
     * Resolves the given audio item to a File inside the Repository. Note that the File need not
     * currently exist; this is the path that the file will have if it does exist.
     * @param audioItem The audio item for which the File is desired.
     * @param format The desired audio format, such as A18 or MP3.
     * @param writeAccess If true, the caller wishes to write to the file.
     * @return A File object representing the audio file, should it ever exist.
     */
    File resolveFile(AudioItem audioItem, AudioItemRepository.AudioFormat format, boolean writeAccess);

    /**
     * Gets a list of all of the audio item files in the repository.
     * @return List of IDs.
     */
    List<String> getAudioItemIds();

    /**
     * Deletes the storage associated with the given audio item id. Note that there
     * may not be a corresponding audio item.
     * @param id The id to be deleted.
     * @return True if the deletion succeeded, false otherwise.
     */
    void delete(String id);

    long size(String id);

    default Path basePath() { return new File(".").toPath(); }
    default Path resolveDirectoryPath(String id) {
        return basePath().resolve(contentDirName+File.separator+id);
    }
    default Path resolveFilePath(String id, AudioItemRepository.AudioFormat format) {
        return resolveDirectoryPath(id).resolve(id + "." + format.getFileExtension());
    }
    static String contentDirName = "org" + File.separator + "Literacybridge";
}
