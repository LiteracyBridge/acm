package org.literacybridge.acm.importexport;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataStore;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.literacybridge.acm.store.MetadataSpecification.DC_IDENTIFIER;

public class AudioImporter {
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(AudioImporter.class.getName());

    public enum Option {
        addNewOnly,     // Do not update existing items, only add new ones.
        updateOnly      // Only update the specified item.
    }

    private static final AudioImporter instance = new AudioImporter();
    public static AudioImporter getInstance() {
        return instance;
    }

    private final FileFilter audioFilesFilter;
    public FileFilter getImportableFilesFilter() {
        return audioFilesFilter;
    }

    // Map of file extensions to ctor of the class that imports that extension.
    private final Map<String, Function<File, BaseMetadataImporter>> cmap = new HashMap<>();

    private AudioImporter() {
        Set<String> extensions = new HashSet<>();

        // We could also use reflection to do this on (nothing but the) class objects.
        // Performance would not be an issue, because the import overwhelms construction, but
        // this is a bit more strongly typed. It is less convenient to add new importers.
        registerImporter(A18MdImporter.getSupportedFileExtensions(), A18MdImporter::new, extensions);
        registerImporter(MP3MdImporter.getSupportedFileExtensions(), MP3MdImporter::new, extensions);
        registerImporter(WavMdImporter.getSupportedFileExtensions(), WavMdImporter::new, extensions);
        registerImporter(OggMdImporter.getSupportedFileExtensions(), OggMdImporter::new, extensions);

        audioFilesFilter = getFileExtensionFilter(extensions);
    }
    private void registerImporter(String[] extensions, Function<File, BaseMetadataImporter> ctor, Set<String> extensionSet) {
        for (String extension : extensions) {
            extension = extension.toLowerCase();
            extensionSet.add(extension);
            cmap.put(extension, ctor);
        }
    }

    public static AudioItem createAudioItemForFile(File file) {
        return new A18MdImporter(file).createAudioItem();
    }

    /**
     * Given a file, return an importer that can import it, if any.
     * @param audioFile The file to be imported.
     * @return The proper importer, if any, else null.
     */
    private BaseMetadataImporter getImporter(File audioFile) {
        String extension = FilenameUtils.getExtension(audioFile.getName()).toLowerCase();
        Function<File, BaseMetadataImporter> ctor = cmap.get(extension);
        if (ctor != null) {
            return ctor.apply(audioFile);
        }
        return new AnyMdImporter(audioFile);
    }

    /**
     * Perform the actual work of importing an audio file.
     * @param file The file to be imported.
     * @param processor Optional processor to examine the file after import.
     * @param optionsArg Optional list of Option.
     * @throws IOException If the file can not be read or imported.
     */
    private AudioItem importFileWithOptions(File file, AudioItemProcessor processor, AudioItem existingItem, Option... optionsArg)
            throws IOException, AudioItemRepository.UnsupportedFormatException, BaseAudioConverter.ConversionException, AudioItemRepository.DuplicateItemException
    {
        Set<Option> options = new HashSet<>(Arrays.asList(optionsArg));
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }

        if (file.isDirectory()) {
            throw new IllegalArgumentException(file + " is a directory.");
        }

        AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
        BaseMetadataImporter importer;
        AudioItemRepository.ImportFunc importFn = repository::updateExistingAudioItemFromFile;
        // Determine if this is a new or existing audio item.
        AudioItem audioItem = null;
        if (options.contains(Option.updateOnly)) {
            if (existingItem == null) {
                throw new IllegalArgumentException("'existingItem' must not be null when 'updateOnly' is specified.");
            }
            audioItem = existingItem;
        } else {
            // If updating is acceptable, try to get the AudioItem via the file name.
            if (!options.contains(Option.addNewOnly)) {
                String id = getIdFromFilename(file);
                if (id != null) {
                    audioItem = store.getAudioItem(id);
                }
            }
            // If not acceptable, or no item, create a new one.
            if (audioItem == null) {
                importer = getImporter(file);
                audioItem = importer.createAudioItem();
                importFn = repository::addNewAudioItemFromFile;
            }
        }
        // Be certain that the DC_IDENTIFIER == the AudioItem.id
        Metadata metadata = audioItem.getMetadata();
        metadata.put(DC_IDENTIFIER, audioItem.getId());

        ////////////////////////////////////////////////////////////////////////////////////////////
        // A U D I O   I M P O R T   H A P P E N S   H E R E
        //
        // Here is where the actual conversion and import happens.
        importFn.accept(audioItem, file);
        //
        ////////////////////////////////////////////////////////////////////////////////////////////

        // let caller tweak audio item
        if (processor != null) {
            processor.process(audioItem);
        }

        store.commit(audioItem);

        return audioItem;
    }

    public void updateAudioItemFromFile(AudioItem existingItem, File file, AudioItemProcessor processor)
            throws AudioItemRepository.UnsupportedFormatException,
            BaseAudioConverter.ConversionException, IOException, AudioItemRepository.DuplicateItemException {
        importFileWithOptions(file, processor, existingItem, Option.updateOnly);
    }

    /**
     * Imports an audio file into the repository.
     * @param file The file to be imported.
     * @param processor An optional processor given an opportunity to modify the item post import.
     * @throws IOException If the file can't be converted or imported.
     */
    public AudioItem importAudioItemFromFile(File file, AudioItemProcessor processor)
            throws IOException, AudioItemRepository.UnsupportedFormatException, BaseAudioConverter.ConversionException, AudioItemRepository.DuplicateItemException {
        return importFileWithOptions(file, processor, null, Option.addNewOnly);
    }

    /**
     * Imports an audio file into the repository. If the name of the file is like "${title}___${id}",
     * then this is updating an existing item, which must exist in this repository.
     *
     * Specifically intended for interactively drag-and-drop adding files to the ACM.
     * @param file to be imported.
     * @param processor An optional processor that is given an opportunity to modify the item post import.
     * @throws BaseAudioConverter.ConversionException If the file's audio format can't be converted.
     * @throws AudioItemRepository.AudioItemRepositoryException If something other than the audio format can't be converted.
     * @throws IOException If there's an IO error in the conversion.
     */
    public void importDroppedFile(File file, AudioItemProcessor processor)
            throws BaseAudioConverter.ConversionException, AudioItemRepository.AudioItemRepositoryException, IOException {
        Option option = Option.addNewOnly;
        AudioItem item = null;
        String id = getIdFromFilename(file);
        if (id != null) {
            item = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getAudioItem(id);
            if (item == null) {
                throw new AudioItemRepository.MissingItemException("Audio item for update not found");
            }
            option = Option.updateOnly;
        }
        importFileWithOptions(file, processor, item, option);
    }

    /**
     * Add the given file to the ACM database.
     * @param file to be imported.
     * @param processor An optional processor that is given an opportunity to modify the item post import.
     * @throws BaseAudioConverter.ConversionException If the file's audio format can't be converted.
     * @throws AudioItemRepository.AudioItemRepositoryException If something other than the audio format can't be converted.
     * @throws IOException If there's an IO error in the conversion.
     */
    public void importOrUpdateAudioItemFromFile(File file, AudioItemProcessor processor)
            throws AudioItemRepository.AudioItemRepositoryException, BaseAudioConverter.ConversionException, IOException {
        importFileWithOptions(file, processor, null);
    }

    private static String getIdFromFilename(File audioFile) {
        String id = null;
        // When audio is exported as "title + id", the format is "${title}___${id}".
        String titleFromFilename = FilenameUtils.removeExtension(audioFile.getName());
        int pos = titleFromFilename.indexOf(AudioExporter.AUDIOITEM_ID_SEPARATOR);
        if (pos != -1) {
            id = titleFromFilename.substring(pos + AudioExporter.AUDIOITEM_ID_SEPARATOR.length());
        }
        return id;
    }

    /**
     * Don't import anything, but read metadata from an existing file.
     * @param file A file.
     * @return Any metadata in the file.
     */
    public Metadata getExistingMetadata(File file) {
        BaseMetadataImporter importer = getImporter(file);

        return importer.getMetadata();
    }

    private static FileFilter getFileExtensionFilter(final Set<String> extensions) {
        return file -> {
            if (file.isDirectory()) {
                return false;
            }
            String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
            return extensions.contains(extension);
        };
    }

    /**
     * An interface to give import callers an opportunity to examine / tweak an audio file
     * as it is imported.
     */
    public interface AudioItemProcessor {
        void process(AudioItem item);
    }
}
