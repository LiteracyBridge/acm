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
    private final Map<String, Function<File, AudioFileImporter>> cmap = new HashMap<>();

    private AudioImporter() {
        Set<String> extensions = new HashSet<>();

        // We could also use reflection to do this on (nothing but the) class objects.
        // Performance would not be an issue, because the import overwhelms construction, but
        // this is a bit more strongly typed. It is less convenient to add new importers.
        registerImporter(A18Importer.getSupportedFileExtensions(), A18Importer::new, extensions);
        registerImporter(MP3Importer.getSupportedFileExtensions(), MP3Importer::new, extensions);
        registerImporter(WavImporter.getSupportedFileExtensions(), WavImporter::new, extensions);
        registerImporter(OggImporter.getSupportedFileExtensions(), OggImporter::new, extensions);

        audioFilesFilter = getFileExtensionFilter(extensions);
    }
    private void registerImporter(String[] extensions, Function<File, AudioFileImporter> ctor, Set<String> extensionSet) {
        for (String extension : extensions) {
            extension = extension.toLowerCase();
            extensionSet.add(extension);
            cmap.put(extension, ctor);
        }
    }

    /**
     * Given a file, return an importer that can import it, if any.
     * @param audioFile The file to be imported.
     * @return The proper importer, if any, else null.
     */
    private AudioFileImporter getImporter(File audioFile) {
        String extension = FilenameUtils.getExtension(audioFile.getName()).toLowerCase();
        Function<File, AudioFileImporter> ctor = cmap.get(extension);
        if (ctor != null) {
            return ctor.apply(audioFile);
        }
        return new AnyImporter(audioFile);
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
        AudioItem result;
        Set<Option> options = new HashSet<>(Arrays.asList(optionsArg));
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }

        if (file.isDirectory()) {
            throw new IllegalArgumentException(file.toString() + " is a directory.");
        }

        // Determine if this is a new or existing audio item.
        AudioItem item = null;
        if (options.contains(Option.updateOnly)) {
            item = existingItem;
        }
        if (item == null) {
            String title = FilenameUtils.removeExtension(file.getName());
            int pos = title.indexOf(AudioExporter.AUDIOITEM_ID_SEPARATOR);
            if (pos != -1) {
                String id = title.substring(pos + AudioExporter.AUDIOITEM_ID_SEPARATOR.length());
                item = store.getAudioItem(id);
            }
        }

        // If the audio item already exists, refresh the existing item with new content.
        if (item != null) {
            if (options.contains(Option.addNewOnly)) {
                // just skip if we have an item with the same id already
                System.out.println(String.format("File '%s' is already in database; skipping",
                    file.getName()));
            } else {
                ACMConfiguration.getInstance()
                    .getCurrentDB()
                    .getRepository()
                    .updateAudioItem(item, file);

                // let caller tweak audio item
                if (processor != null) {
                    processor.process(item);
                }

                store.commit(item);
            }
            result = item;
        } else {
            // Otherwise, the item didn't already exist, so import the new audio item
            AudioFileImporter importer = getImporter(file);
            result = importer.importSingleFile(processor);
        }

        return result;
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

    public AudioItem importOrUpdateAudioItemFromFile(File file, AudioItemProcessor processor)
            throws AudioItemRepository.UnsupportedFormatException, BaseAudioConverter.ConversionException, IOException, AudioItemRepository.DuplicateItemException {
        return importFileWithOptions(file, processor, null);
    }

    /**
     * Don't import anything, but read metadata from an existing file.
     * @param file A file.
     * @return Any metadata in the file.
     */
    public Metadata getExistingMetadata(File file) {
        AudioFileImporter importer = getImporter(file);

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
