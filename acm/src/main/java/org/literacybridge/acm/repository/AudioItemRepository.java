package org.literacybridge.acm.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.literacybridge.acm.audioconverter.api.A18Format;
import org.literacybridge.acm.audioconverter.api.A18Format.AlgorithmList;
import org.literacybridge.acm.audioconverter.api.A18Format.useHeaderChoice;
import org.literacybridge.acm.audioconverter.api.AudioConversionFormat;
import org.literacybridge.acm.audioconverter.api.ExternalConverter;
import org.literacybridge.acm.audioconverter.api.MP3Format;
import org.literacybridge.acm.audioconverter.api.OggFormat;
import org.literacybridge.acm.audioconverter.api.WAVFormat;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.LBMetadataSerializer;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.OsUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.literacybridge.acm.repository.FileRepositoryInterface.Repository;

/**
 * This repository manages all audio files associated with the audio items that
 * the ACM stores. Metadata of the audio items is stored separately, not in this
 * repository.
 */
public class AudioItemRepository {
    private final static File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    public static final class DuplicateItemException extends Exception {
        public DuplicateItemException(String msg) {
            super(msg);
        }
    }

    public static final class UnsupportedFormatException extends Exception {
        UnsupportedFormatException(String msg) {
            super(msg);
        }
    }

    private final CachingRepository audioFileRepository;

    public AudioItemRepository(CachingRepository audioFileRepository) {
        this.audioFileRepository = audioFileRepository;
    }

    /**
     * An enum of all supported audio formats.
     */
    public enum AudioFormat {
        A18("a18", new A18Format(128, 16000, 1, AlgorithmList.A1800, useHeaderChoice.No)),
        WAV("wav", new WAVFormat(128, 16000, 1)),
        MP3("mp3", new MP3Format(128, 16000, 1)),
        OGG("ogg", new OggFormat(128, 16000, 1));

        private final String fileExtension;
        private final AudioConversionFormat audioConversionFormat;

        AudioFormat(String fileExtension, AudioConversionFormat audioConversionFormat) {
            this.fileExtension = fileExtension;
            this.audioConversionFormat = audioConversionFormat;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public AudioConversionFormat getAudioConversionFormat() {
            return audioConversionFormat;
        }
    }

    private static final Map<String, AudioFormat> EXTENSION_TO_FORMAT = Maps.newHashMap();
    static {
        for (AudioFormat format : AudioFormat.values()) {
            EXTENSION_TO_FORMAT.put(format.getFileExtension().trim().toLowerCase(), format);
        }
    }

    private final ExternalConverter externalConverter = new ExternalConverter();

    /**
     * Returns true, if this audio item is stored in any supported format in this
     * repository.
     */
    public synchronized boolean hasAudioItem(AudioItem audioItem) {
        for (AudioFormat format : AudioFormat.values()) {
            if (hasAudioItemFormat(audioItem, format)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true, if this audio item is stored in the given format in the
     * repository.
     */
    synchronized boolean hasAudioItemFormat(AudioItem audioItem, AudioFormat format) {
        File file = getAudioFile(audioItem, format);
        return file != null && file.exists();
    }

    /**
     * Store a new audioItem in the repository.
     * <p>
     * Throws {@link DuplicateItemException} if the item already exists in this
     * repository. For storing different audio formats of the same audio item in
     * this repository call convert() instead of calling this method multiple
     * times.
     */
    public synchronized File storeAudioFile(AudioItem audioItem, File externalFile)
        throws UnsupportedFormatException, IOException
    {
        AudioFormat format = determineFormat(externalFile);
        if (format == null) {
            throw new UnsupportedFormatException(
                "Unsupported or unrecognized audio format for file: " + externalFile);
        }

        File toFile = resolveFile(audioItem, format, true);
        IOUtils.ensureDirectoryExists(toFile);

        if (format == AudioFormat.A18) {
            // we only store the audio itself in the repo, as we keep the metadata
            // separately in the database;
            // therefore strip metadata section herea
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(
                externalFile)));
            int numBytes = IOUtils.readLittleEndian32(in);
            in.close();
            IOUtils.copy(externalFile, toFile, numBytes + 4);
        } else {
            IOUtils.copy(externalFile, toFile);
            try {
                // convert file to A18 format right away
                convert(audioItem, AudioFormat.A18);
            } catch (ConversionException e) {
                throw new IOException(e);
            }
        }

        // update the duration in the audioitem's metadata section
        A18DurationUtil.updateDuration(audioItem);

        return toFile;
    }

    /**
     * Returns a handle to the audio file for the specified audioItem. Returns
     * null, if the audio item is not stored with the given format in this
     * repository;
     */
    public synchronized File getAudioFile(AudioItem audioItem, AudioFormat format) {
        cleanStaleAudioFiles(audioItem);

        File file = resolveFile(audioItem, format, false);
        return file.exists() ? file : null;
    }

    /**
     * Converts the audio item into the specified targetFormat and returns a
     * handle to the newly created file. The new file will be within the repository.
     */
    public synchronized File convert(AudioItem audioItem, AudioFormat targetFormat)
        throws ConversionException, IOException
    {
        cleanStaleAudioFiles(audioItem);

        File audioFile = resolveFile(audioItem, targetFormat, true);
        if (!audioFile.exists() && targetFormat == AudioFormat.A18) {
            // Before creating a new a18 in the temp folder from a cached wave file,
            // just copy over the a18 file in dropbox over there if it exists.
            File audioFileShared = resolveFile(audioItem, targetFormat, false);
            if (audioFileShared.exists()) {
                IOUtils.ensureDirectoryExists(audioFile);
                IOUtils.copy(audioFileShared, audioFile, true);
            }
        }
        if (audioFile.exists()) {
            return audioFile;
        }

        // we prefer to convert from WAV if possible
        File sourceFile = getAudioFile(audioItem, AudioFormat.WAV);
        if (sourceFile == null) {
            // no WAV, try any other format
            for (AudioFormat sourceFormat : AudioFormat.values()) {
                sourceFile = getAudioFile(audioItem, sourceFormat);
                if (sourceFile != null) {
                    break;
                }
            }
        }

        if (OsUtils.WINDOWS && sourceFile != null) {
            IOUtils.ensureDirectoryExists(audioFile);
            externalConverter.convert(sourceFile,
                audioFile.getParentFile(),
                TMP_DIR,
                targetFormat.getAudioConversionFormat(),
                false);
        }

        return audioFile;
    }

    /**
     * Updates all stores audio files associated with the given AudioItem.
     * <p>
     * First the file in the given format is imported and the old version of that
     * format is, if it exists, overwritten. Then this new version is converted
     * into all formats that were previously stored for that audio item.
     */
    public synchronized void updateAudioItem(AudioItem audioItem, File externalFile)
        throws ConversionException, IOException, UnsupportedFormatException
    {
        if (determineFormat(externalFile) == null) {
            throw new UnsupportedFormatException(
                "Unsupported or unrecognized audio format for file: " + externalFile);
        }

        // Determine in which formats the item is currently stored
        Set<AudioFormat> existingFormats = Sets.newHashSet();
        for (AudioFormat format : AudioFormat.values()) {
            if (hasAudioItemFormat(audioItem, format)) {
                existingFormats.add(format);
            }
        }

        // now delete the old files
        delete(audioItem);

        // store the new sourceFile
        storeAudioFile(audioItem, externalFile);

        // Restore to all previously stored formats (why?)
        for (AudioFormat format : existingFormats) {
            convert(audioItem, format);
        }
    }

    /**
     * Deletes all files associated with an audioitem from the repository.
     */
    public synchronized void delete(AudioItem audioItem) {
        // we need to loop over all formats, because the different formats
        // could be stored in different directories (e.g. local cache)
        for (AudioFormat format : AudioFormat.values()) {
            File file = resolveFile(audioItem, format, true);
            if (file != null) {
                IOUtils.deleteRecursive(file.getParentFile());
            }
        }
    }

    public synchronized void exportA18WithMetadata(AudioItem audioItem, File targetDirectory)
        throws ConversionException, IOException
    {
        // File fromFile = convert(audioItem, AudioFormat.A18); // this will create
        // a new a18 in the temp folder, which isn't necessary for export
        File fromFile = resolveFile(audioItem, AudioFormat.A18, false);
        if (fromFile == null) {
            throw new IOException("AudioItem " + audioItem.getUuid() + " not found in repository.");
        }

        exportA18WithMetadataToFile(audioItem, new File(targetDirectory, fromFile.getName()));
    }

    public synchronized void exportA18WithMetadataToFile(AudioItem audioItem, File targetFile)
        throws ConversionException, IOException
    {
        // File fromFile = convert(audioItem, AudioFormat.A18); // this will create
        // a new a18 in the temp folder, which isn't necessary for export
        File fromFile = resolveFile(audioItem, AudioFormat.A18, false);

        if (!fromFile.exists()) {
            fromFile = convert(audioItem, AudioFormat.A18);
        }
        if (fromFile == null) {
            throw new IOException("AudioItem " + audioItem.getUuid() + " not found in repository.");
        }
        IOUtils.copy(fromFile, targetFile);
        appendMetadataToA18(audioItem, targetFile);
    }

    /**
     * Returns a handle to the audio file in the given format. Does not guarantee
     * that the file exists.
     */
    private File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
        return audioFileRepository.resolveFile(audioItem, format, writeAccess);
    }

    /**
     * Returns whether this repository needs to perform gc.
     */
    public FileSystemGarbageCollector.GCInfo getGcInfo() throws IOException {
        return audioFileRepository.getGcInfo();
    }

    /**
     * Can optionally be overwritten by subclasses to performs a garbage
     * collection in the repository to free up disk space.
     */
    public void gc() throws IOException {
        audioFileRepository.gc();
    }

    /**
     * Ensures that no file belonging to an audio item is older than its
     * corresponding a18 file
     */
    private void cleanStaleAudioFiles(AudioItem audioItem) {
        File a18 = resolveFile(audioItem, AudioFormat.A18, false);
        if (a18.exists()) {
            Arrays.stream(AudioFormat.values())
                .filter(format -> format != AudioFormat.A18)
                .forEach(format -> {
                    File file = resolveFile(audioItem, format, true);
                    if (file.exists() && file.lastModified() < a18.lastModified()) {
                        // a18 file is newer - delete it, it will get recreated from the a18
                        // in convert()
                        file.delete();
                        // Try to delete the parent directory. It's OK if it fails.
                        file.getParentFile().delete();
                    }
                });
        }
    }

    public static void appendMetadataToA18(AudioItem audioItem, File a18File) throws IOException {
        // remove locale hack once we get rid of localized audio items
        Metadata metadata = audioItem.getMetadata();

        try (FileOutputStream fos = new FileOutputStream(a18File,true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            DataOutputStream out = new DataOutputStream(bos)) {
            LBMetadataSerializer serializer = new LBMetadataSerializer();
            serializer.serialize(Lists.newArrayList(audioItem.getCategoryList()), metadata, out);
        }
    }

    /**
     * Determines the format of the given file. Returns null, if the format was
     * not recognized.
     */
    private static AudioFormat determineFormat(File file) {
        String extension = IOUtils.getFileExtension(file);
        return audioFormatForExtension(extension);
    }

    public static AudioFormat audioFormatForExtension(String extension) {
        extension = extension.trim().toLowerCase();
        return EXTENSION_TO_FORMAT.get(extension);
    }

    public static class CleanResult {
        public int files;
        public long bytes;

        CleanResult(int files, long bytes) {
            this.files = files;
            this.bytes = bytes;
        }
    }

    public CleanResult cleanUnreferencedFiles() {
        // This gets the list of directory names, which should correspond to item ids.
        List<String> ids = audioFileRepository.getAudioItemIds(Repository.global);
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        // Calculates the cumulative size of all of the files that will be removed.
        // For all ids, take the ones with no backing audio item, get the size, and sum.
        Long size = ids.stream()
            .filter(id->store.getAudioItem(id)==null)
            .map(this.audioFileRepository::size)
            .reduce((long) 0, (a,b)-> a+b);
        // For all ids, if there is no audio item, delete from the repository.
        int n = 0;
        for (String id: ids) {
            AudioItem item = store.getAudioItem(id);
            if (item == null) {
                audioFileRepository.delete(id);
                n++;
            }
        }
        return new CleanResult(n, size);
    }
}
