package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.importexport.AudioFileImporter.AudioItemProcessor;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataStore;

import static org.literacybridge.acm.store.MetadataSpecification.DC_IDENTIFIER;

public class AudioImporter {
    private static final Logger LOG = Logger.getLogger(AudioImporter.class.getName());

    public enum Option {
        addNewOnly     // Do not update existing items, only add new ones.
    };

    private static AudioImporter instance = new AudioImporter();
    public static AudioImporter getInstance() {
        return instance;
    }

    private FileFilter audioFilesFilter;

     // Map of file extensions to ctor of the class that imports that extension.
    private Map<String, Function<File, AudioFileImporter>> cmap = new HashMap<>();

    private AudioImporter() {
        Set<String> extensions = new HashSet<String>();

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
        String extension = getFileExtension(audioFile);
        Function<File, AudioFileImporter> ctor = cmap.get(extension);
        if (ctor != null) {
            return ctor.apply(audioFile);
        }
        return null;
    }

    /**
     * Perform the actual work of importing an audio file.
     * @param file The file to be imported.
     * @param processor Optional processor to examine the file after import.
     * @throws IOException If the file can not be read or imported.
     */
    private AudioItem importFileWithOptions(File file, AudioItemProcessor processor, Option... optionsArg)
        throws IOException
    {
        AudioItem result = null;
        Set<Option> options = new HashSet<>(Arrays.asList(optionsArg));
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }

        if (file.isDirectory()) {
            throw new IllegalArgumentException(file.toString() + " is a directory.");
        }

        AudioFileImporter importer = getImporter(file);
        if (importer == null) {
            throw new UnsupportedOperationException(getFileExtension(file) + " not supported.");
        }
        String title = FilenameUtils.removeExtension(file.getName());

        // Determine if this is a new or existing audio item.
        Metadata metadata = importer.getMetadata();
        String id = metadata.get(DC_IDENTIFIER);
        AudioItem item = store.getAudioItem(id);

        // If the item was not found by id, try filename as title.
        if (item == null) {
            item = store.getAudioItem(title);
            // If still not found, look for an "id" tacked on to the filename.
            if (item == null) {
                int pos = title.indexOf(AudioExporter.FILENAME_SEPARATOR);
                if (pos != -1) {
                    id = title.substring(pos + AudioExporter.FILENAME_SEPARATOR.length());
                    item = store.getAudioItem(id);
                }
            }
        }

        // If the audio item already exists, refresh the existing item with new content.
        if (item != null) {
            if (options.contains(Option.addNewOnly)) {
                // just skip if we have an item with the same id already
                System.out.println(String.format("File '%s' is already in database; skipping",
                    file.getName()));
                result = item;
            } else {
                try {
                    ACMConfiguration.getInstance()
                        .getCurrentDB()
                        .getRepository()
                        .updateAudioItem(item, file);
                    store.commit(item);
                    result = item;
                } catch (Exception e) {
                    LOG.log(Level.WARNING,
                        "Unable to update files for audioitem with id=" + title, e);
                }
            }
        } else {
            // Otherwise, the item didn't already exist, so import the new audio item
            result = importer.importSingleFile(processor);
        }

        return result;
    }

    /**
     * Import a file and add the given category to the file.
     * @param file The file to be imported.
     * @param category Category to be added.
     * @throws IOException If the file can't be read or imported.
     */
    private AudioItem importFile(File file, Category category) throws IOException {
        return importFileWithOptions(file, (item) -> {if (item != null) item.addCategory(category);});
    }

    /**
     * Imports an audio file into the repository.
     * @param file The file to be imported.
     * @param processor An optional processor given an opportunity to modify the item post import.
     * @param options Zero or more options to modify the import.
     * @throws IOException If the file can't be converted or imported.
     */
    public AudioItem importFile(File file, AudioItemProcessor processor, Option... options)
        throws IOException
    {
        return importFileWithOptions(file, processor, options);
    }

    /**
     * Imports an audio file into the repository.
     * @param file The file to be imported.
     * @throws IOException If the file can't be converted or imported.
     */
    public AudioItem importFile(File file) throws IOException {
        return importFileWithOptions(file, (AudioItemProcessor)null);
    }

    /**
     * Imports a list of files, to some category, with progress notification.
     * @param files A Collection<File> of files to import.
     * @param category A Category with which to decorate the content.
     * @param progress A method taking two Integer parameters, to receive progress "n of m" before each file.
     */
    public void importFiles(Collection<File> files, Category category, BiFunction<Integer, Integer, Boolean> progress) {
        List<File> filesToImport = new LinkedList<>();
        files.forEach(f -> gatherFiles(f, true, filesToImport));
        int numImported = 0;
        for (File f : filesToImport) {
            try {
                importFile(f, category);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to import file " + f, e);
            }
            boolean okToContinue=progress.apply(++numImported, filesToImport.size());
            if (!okToContinue) {
                break;
            }
        }
    }

    private void gatherFiles(File dir, boolean recursive, List<File> filesToImport) {
        if (!dir.isDirectory()) {
            filesToImport.add(dir);
        } else {
            File[] files = dir.listFiles(audioFilesFilter);
            Collections.addAll(filesToImport, files);

            if (recursive) {
                File[] subdirs = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });

                for (File subDir : subdirs) {
                    gatherFiles(subDir, recursive, filesToImport);
                }
            }
        }
    }

    private static String getFileExtension(File file) {
        return FilenameUtils.getExtension(file.getName()).toLowerCase();
    }

    private static FileFilter getFileExtensionFilter(final Set<String> extensions) {
        return file -> {
            if (file.isDirectory()) {
                return false;
            }
            String extension = getFileExtension(file);
            return extensions.contains(extension);
        };
    }
}
