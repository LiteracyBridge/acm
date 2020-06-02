package org.literacybridge.acm.repository;

import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.io.IOException;
import java.util.List;

interface FileRepositoryInterface {
    enum Repository {
        global,
        cache,
        sandbox
    }

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
     * Gets information about the GC state of the Repository.
     * @return A GCInfo object.
     * @throws IOException if the file system can't be perused.
     */
    FileSystemGarbageCollector.GCInfo getGcInfo() throws IOException;

    /**
     * Performs a GC on the Repository, if the Repository supports collection.
     * @throws IOException If a file error occurs.
     */
    void gc() throws IOException;

    /**
     * Gets a list of all of the audio item files in the repository.
     * @return List of IDs.
     */
    List<String> getAudioItemIds(Repository repo);

    /**
     * Deletes the storage associated with the given audio item id. Note that there
     * may not be a corresponding audio item.
     * @param id The id to be deleted.
     * @return True if the deletion succeeded, false otherwise.
     */
    void delete(String id);

    long size(String id);
}
