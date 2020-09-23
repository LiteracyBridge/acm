package org.literacybridge.acm.repository;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.audioconverter.api.ExternalConverter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionSourceMissingException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.importexport.AudioExporter;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.CUSTOM_GREETING;
import static org.literacybridge.acm.repository.FileRepositoryInterface.Repository;

/**
 * This repository manages all audio files associated with the audio items that
 * the ACM stores. Metadata of the audio items is stored separately, not in this
 * repository.
 */
public class AudioItemRepositoryImpl implements AudioItemRepository {
    private final static File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private final static Pattern categoryPattern = Pattern.compile("^\\$?\\d+(-\\d+)+$");
    private WavFilePreCaching caching = null;

    public static AudioItemRepositoryImpl buildAudioItemRepository(DBConfiguration dbConfiguration) {
        String wavExt = "." + AudioItemRepository.AudioFormat.WAV.getFileExtension();
        FileSystemGarbageCollector fsgc = new FileSystemGarbageCollector(
            dbConfiguration.getCacheSizeInBytes(),
            (file, name) -> name.toLowerCase().endsWith(wavExt));
        // The localCacheRepository lives in ~/LiteracyBridge/ACM/cache/ACM-FOO. It is used for all
        // non-A18 files. When .wav files (but not, say, mp3s) exceed max cache size, they'll be gc-ed.
        // The localCacheRepository lives in ~/LiteracyBridge/ACM/cache/ACM-FOO. It is used for all
        // non-A18 files. When .wav files (but not, say, mp3s) exceed max cache size, they'll be gc-ed.
        FileSystemRepository localCacheRepository = new FileSystemRepository(dbConfiguration.getLocalCacheDirectory(), fsgc);
        // If there is no sandbox directory, all A18s are read from and written to this directory. If there
        // IS a sandbox directory, then A18s are written there, and read from here if they're not in the
        // sandbox. (That behaviour is broken because there is no mechanism to clean out stale items from
        // the sandbox.)
        FileSystemRepository globalSharedRepository = new FileSystemRepository(dbConfiguration.getProgramContentDir());
        // If the ACM is opened in "sandbox" mode, all A18s are written here. A18s are read from here if
        // present, but read from the global shared repository if absent from the sandbox. Note that
        // if not sandboxed, this one will be null.
        FileSystemRepository sandboxRepository
            = dbConfiguration.isSandboxed() ? new FileSystemRepository(dbConfiguration.getSandboxDirectory()) : null;

        // The caching repository directs resolve requests to one of the three above file based
        // repositories.
        Collection<AudioFormat> nativeFormats = dbConfiguration.getNativeAudioFormats()
                .stream()
                .map(AudioItemRepositoryImpl::audioFormatForExtension)
                .collect(Collectors.toSet());

        CachingRepository cachingRepository
            = new CachingRepository(localCacheRepository, globalSharedRepository, sandboxRepository, nativeFormats);
        return new AudioItemRepositoryImpl(cachingRepository);
    }

    private final CachingRepository audioFileRepository;

    private AudioItemRepositoryImpl(CachingRepository audioFileRepository) {
        this.audioFileRepository = audioFileRepository;
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
    private synchronized boolean hasAudioItem(AudioItem audioItem) {
        for (AudioFormat format : AudioFormat.values()) {
            if (hasAudioFileWithFormat(audioItem, format)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true, if this audio item is stored in the given format in the
     * repository.
     */
    @Override
    public synchronized boolean hasAudioFileWithFormat(AudioItem audioItem, AudioFormat format) {
        File file = findFileWithFormat(audioItem, format);
        return file != null && file.exists();
    }

    /**
     * Store a new audioItem in the repository.
     * <p>
\     * Throws {@link DuplicateItemException} if the item already exists in this
     * repository. For storing different audio formats of the same audio item in
     * this repository call convert() instead of calling this method multiple
     * times.
     */
    @Override
    public synchronized void addAudioItem(AudioItem audioItem, File externalFile)
            throws UnsupportedFormatException, IOException, DuplicateItemException {
        if (hasAudioItem(audioItem)) {
            throw new DuplicateItemException(String.format("Audio item %s already exists for language %s", audioItem.getTitle(), audioItem.getLanguageCode()));
        }
        storeAudioFile(audioItem, externalFile);
    }

    /**
     * Updates all stores audio files associated with the given AudioItem.
     * <p>
     * First the file in the given format is imported and the old version of that
     * format is, if it exists, overwritten. Then this new version is converted
     * into all formats that were previously stored for that audio item.
     */
    @Override
    public synchronized void updateAudioItem(AudioItem audioItem, File externalFile)
            throws ConversionException, IOException, UnsupportedFormatException
    {
        ensureKnownFormat(externalFile);

        // Determine in which formats the item is currently stored
        Set<AudioFormat> existingFormats = Sets.newHashSet();
        for (AudioFormat format : AudioFormat.values()) {
            if (hasAudioFileWithFormat(audioItem, format)) {
                existingFormats.add(format);
            }
        }

        // now delete the old files
        deleteAudioItem(audioItem);

        // store the new sourceFile
        storeAudioFile(audioItem, externalFile);

        // Restore to all previously stored formats
        for (AudioFormat format : existingFormats) {
            convert(audioItem, format);
        }
    }

    /**
     * Store a new File as an audioItem in the repository. The AudioItem may or may
     * not already exist.
     */
    private synchronized void storeAudioFile(AudioItem audioItem, File externalFile)
        throws UnsupportedFormatException, IOException
    {
        AudioFormat format = ensureKnownFormat(externalFile);

        File toFile = resolveFile(audioItem, format, true);
        IOUtils.ensureDirectoryExists(toFile);

        if (format == AudioFormat.A18) {
            // we only store the audio itself in the repo, as we keep the metadata
            // separately in the database;
            // therefore strip metadata section herea
            A18Utils.copyA18WithoutMetadata(externalFile, toFile);
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
        A18Utils.updateDuration(audioItem);
    }

    /**
     * Returns a handle to the audio file for the specified audioItem. Returns
     * null, if the audio item is not stored with the given format in this
     * repository;
     */
    @Override
    public synchronized File findFileWithFormat(AudioItem audioItem, AudioFormat format) {
        File file = resolveFile(audioItem, format, false);
        return file.exists() ? file : null;
    }

    /**
     * Returns the audio file for the specified audio item. If the item does not exist in
     * the requested format, it is converted into that format.
     * @param audioItem The desired audio item.
     * @param format The desired audio format.
     * @return A File containing the audio.
     * @throws IOException If a file can't be read or written.
     * @throws ConversionException If an error occurs while converting.
     */
    @Override
    public synchronized File getAudioFile(AudioItem audioItem, AudioFormat format) throws IOException, ConversionException {
        File file = resolveFile(audioItem, format, false);
        if (file == null || !file.exists()) {
            file = convert(audioItem, format);
        }
        return file;
    }

    /**
     * Converts the audio item into the specified targetFormat and returns a
     * handle to the newly created file. The new file will be within the repository.
     */
    private synchronized File convert(AudioItem audioItem, AudioFormat targetFormat)
        throws ConversionException, IOException
    {

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

        return convertFile(audioItem, (item,format)-> findFileWithFormat((AudioItem)item, format), targetFormat, audioFile.getParentFile());
    }

    private synchronized File convertFile(Object source, BiFunction<Object, AudioFormat, File> sourceFileFinder, AudioFormat targetFormat, File targetDirectory) throws ConversionException {
        File sourceFile = sourceFileFinder.apply(source, AudioFormat.WAV);
        if (sourceFile == null) {
            // no WAV, try any other format
            for (AudioFormat sourceFormat : AudioFormat.values()) {
                sourceFile = sourceFileFinder.apply(source, sourceFormat);
                if (sourceFile != null) {
                    break;
                }
            }
        }

        if (sourceFile == null) {
            throw new ConversionSourceMissingException(String.format("Can't find file to convert for: %s", source.toString()), source.toString());
        }
        // Target file has same name, and is in same directory as source, but with proper extension.
        File targetFile = ExternalConverter.targetFile(sourceFile, targetDirectory, targetFormat.getAudioConversionFormat());
        IOUtils.ensureDirectoryExists(targetFile);
        externalConverter.convert(sourceFile, targetFile,
                TMP_DIR,
                targetFormat.getAudioConversionFormat(), false);

        return targetFile;
    }

    /**
     * Deletes all files associated with an audioitem from the repository.
     */
    @Override
    public synchronized void deleteAudioItem(AudioItem audioItem) {
        // we need to loop over all formats, because the different formats
        // could be stored in different directories (e.g. local cache)
        for (AudioFormat format : AudioFormat.values()) {
            File file = resolveFile(audioItem, format, true);
            if (file != null) {
                IOUtils.deleteRecursive(file.getParentFile());
            }
        }
    }

    @Override
    public String getAudioFilename(AudioItem audioItem, AudioFormat audioFormat) {
        File fromFile = resolveFile(audioItem, audioFormat, false);
        if (fromFile == null) {
            return null;
        }
        return fromFile.getName();
    }

    /**
     * Exports the audio item in the given format. Creates the format if necessary. The file will contain metadata.
     * @param audioItem The audio item to be exported.
     * @param targetFile The file to which to export. NOTE: Regardless of the extension of this file, the native
     *                   extension of the format is used.
     * @param targetFormat The format in which to export.
     * @return Returns the actual file exported.
     * @throws IOException If a file can't be read or written.
     * @throws BaseAudioConverter.ConversionException If an existing file can't be converted to the desired format.
     */
    @Override
    public synchronized File exportAudioFileWithFormat(AudioItem audioItem, File targetFile, AudioFormat targetFormat) throws IOException, ConversionException {
        String defaultExtension = targetFormat.getFileExtension();
        String givenExtension = FilenameUtils.getExtension(targetFile.getName());
        if (!defaultExtension.equalsIgnoreCase(givenExtension)) {
            targetFile = new File(targetFile.getParentFile(),
                                  FilenameUtils.removeExtension(targetFile.getName()) + '.' + defaultExtension);
        }
        AudioExporter exporter = AudioExporter.getInstance();
        exporter.export(audioItem, targetFile, targetFormat);
        return targetFile;
    }

    /**
     *
     * @param prompt The system prompt file name, like "0" or "21".
     * @param targetFile The file to which to export. NOTE: Regardless of any extension of this file, the native
     *                   extension of the format is actually used.
     * @param language The language for which the prompt is to be exported.
     * @param targetFormat The format in which to export.
     * @throws IOException If a file can't be read or written.
     * @throws BaseAudioConverter.ConversionException If an existing file can't be converted to the desired format.
     */
    @Override
    public void exportSystemPromptFileWithFormat(String prompt, File targetFile, String language, AudioFormat targetFormat) throws IOException, ConversionException {
        String defaultExtension = targetFormat.getFileExtension();
        String givenExtension = FilenameUtils.getExtension(targetFile.getName());
        if (!defaultExtension.equalsIgnoreCase(givenExtension)) {
            targetFile = new File(targetFile.getParent(),
                    FilenameUtils.removeExtension(targetFile.getName()) + '.' + defaultExtension);
        }
        File TbOptionsDir = new File(ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir(), "TB_Options");
        File promptsDir = new File(TbOptionsDir, "languages" + File.separator + language);
        // Does the file already exist in the right format?
        File sourceFile = new File(promptsDir, targetFile.getName());
        if (!sourceFile.exists()) {
            // Doesn't exist, so convert it.
            String basename = FilenameUtils.removeExtension(targetFile.getName());
            sourceFile = convertFile(prompt, (name, format) -> {
                File testFile = new File(promptsDir, basename + '.' + format.getFileExtension());
                return testFile.exists() ? testFile : null;
            }, targetFormat, targetFile.getParentFile());
        }
        IOUtils.copy(sourceFile, targetFile);
    }

    /**
     *
     * @param packageName This is needed to find a "prompts.txt" file for ACM database located prompts.
     * @param prompt The prompt id, like "2-0" or "LB-2_uzz71upxwm_11e"
     * @param targetFile The basic name of the file to be exported, like .../2-0.a18 (or .mp3, etc). Will also
     *                   implicitly create the .../i2-0.a18 file.
     * @param language The language being exported. Used to find "2-0" style files in the TB_Options.
     * @param targetFormat The desired audio format, like A18 or MP3.
     * @throws IOException If a file can't be read or written.
     * @throws BaseAudioConverter.ConversionException If an existing file can't be converted to the desired format.
     */
    @Override
    public void exportCategoryPromptPairWithFormat(String packageName,
            String prompt,
            File targetFile,
            String language,
            AudioFormat targetFormat) throws IOException, ConversionException {
        String defaultExtension = targetFormat.getFileExtension();
        String givenExtension = FilenameUtils.getExtension(targetFile.getName());
        if (!defaultExtension.equalsIgnoreCase(givenExtension)) {
            targetFile = new File(targetFile.getParent(),
                    FilenameUtils.removeExtension(targetFile.getName()) + '.' + defaultExtension);
        }
        // Does this look like a TB_Options prompt ("0-1") or like an ACM database prompt ("LB-2_uzz71upxwm_11e")
        File promptsDir;
        Matcher matcher = categoryPattern.matcher(prompt);
        if (matcher.matches()) {
            // It is a TB_Options prompt. Set up the source to point there.
            File TbOptionsDir = new File(ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir(),"TB_Options");
            promptsDir = new File(TbOptionsDir, "languages" + File.separator + language + File.separator + "cat");
        } else {
            // The prompts are from the ACM content. There should be a ".ids" file with the two prompt item ids.
            File packagesDir = new File(ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir(),"packages");
            File packageDir = new File(packagesDir, packageName);
            promptsDir = new File(packageDir, "prompts" + File.separator + language + File.separator + "cat");
            File propsFile = new File(promptsDir, prompt + ".ids");
            if (propsFile.exists()) {
                Properties idProperties = new Properties();
                try (FileInputStream fis = new FileInputStream(propsFile)) {
                    idProperties.load(fis);
                    String nameId = idProperties.getProperty("name");
                    String invitationId = idProperties.getProperty("invitation");
                    MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
                    AudioItem audioItem = store.getAudioItem(nameId);
                    exportAudioFileWithFormat(audioItem, targetFile, targetFormat);

                    File invitationFile = new File(targetFile.getParent(), 'i'+targetFile.getName());
                    audioItem = store.getAudioItem(invitationId);
                    exportAudioFileWithFormat(audioItem, invitationFile, targetFormat);

                    return;
                }
            }
            // There wasn't a .ids file.
        }
        // Process the category announcement.
        // Does the file already exist in the right format?
        File sourceFile = new File(promptsDir, targetFile.getName());
        if (!sourceFile.exists()) {
            // Doesn't exist, so convert it.
            String basename = FilenameUtils.removeExtension(targetFile.getName());
            sourceFile = convertFile(prompt, (name, format) -> {
                File testFile = new File(promptsDir, basename + '.' + format.getFileExtension());
                return testFile.exists() ? testFile : null;
            }, targetFormat, targetFile.getParentFile());
        }
        IOUtils.copy(sourceFile, targetFile);

        // Process the invitation.
        targetFile = new File(targetFile.getParentFile(), 'i'+targetFile.getName());
        sourceFile = new File(promptsDir, targetFile.getName());
        if (!sourceFile.exists()) {
            // Doesn't exist, so convert it.
            String basename = FilenameUtils.removeExtension(targetFile.getName());
            sourceFile = convertFile(prompt, (name, format) -> {
                File testFile = new File(promptsDir, basename + '.' + format.getFileExtension());
                return testFile.exists() ? testFile : null;
            }, targetFormat, targetFile.getParentFile());
        }
        IOUtils.copy(sourceFile, targetFile);
    }

    /**
     * Exports the greeting for one recipient, in the desired audio format.
     * @param sourceDir Where the greeting should be found.
     * @param targetFile The basic name of the file to be exported, like .../1d3d00aa4cdf2368.a18 (or .mp3, etc).
     *                   NOTE: Regardless of the extension of this file, the native extension of the format is used.
     * @param targetFormat The desired audio format, like A18 or MP3.
     * @throws ConversionException if the greeting can't be converted to the desired audio format.
     * @throws IOException if a greeting file can't be read or written.
     */
    @Override
    public void exportGreetingWithFormat(File sourceDir,
            File targetFile,
            AudioFormat targetFormat) throws
                                      ConversionException,
                                      IOException {
        // Normalize output filename for the audio format.
        String defaultExtension = targetFormat.getFileExtension();
        String givenExtension = FilenameUtils.getExtension(targetFile.getName());
        if (!defaultExtension.equalsIgnoreCase(givenExtension)) {
            targetFile = new File(targetFile.getParent(),
                    FilenameUtils.removeExtension(targetFile.getName()) + '.' + defaultExtension);
        }

        File sourceFile = new File(sourceDir, FilenameUtils.removeExtension(CUSTOM_GREETING) + "." + defaultExtension);

        if (!sourceFile.exists()) {
            // Doesn't exist, so convert it.
            String basename = FilenameUtils.removeExtension(targetFile.getName());
            sourceFile = convertFile(null, (name, format) -> {
                File testFile = new File(sourceDir, basename + '.' + format.getFileExtension());
                return testFile.exists() ? testFile : null;
            }, targetFormat, targetFile.getParentFile());
        }
        IOUtils.copy(sourceFile, targetFile);
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

    /**
     * Determines the format of the given file. Throws if format not recognized.
     * @param file to be examined.
     * @return the AudioFormat of the file's extension.
     * @throws UnsupportedFormatException if the extension is not recognized.
     */
    private static AudioFormat ensureKnownFormat(File file) throws UnsupportedFormatException {
        String extension = IOUtils.getFileExtension(file);
        AudioFormat audioFormat = audioFormatForExtension(extension);
        if (audioFormat == null) {
            System.out.print("Unsupported or unrecognized audio format for file: " + file);
            throw new UnsupportedFormatException(
                    "Unsupported or unrecognized audio format for file: " + file);
        }
        return audioFormat;
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
            .reduce((long) 0, Long::sum);
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
