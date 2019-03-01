package org.literacybridge.acm.importexport;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static javax.swing.text.html.parser.DTDConstants.MD;
import static org.literacybridge.acm.store.MetadataSpecification.*;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;

/**
 * Abstract base class for an audio file importer.
 */
abstract class AudioFileImporter {

    File audioFile;
    AudioFileImporter(File audioFile) {
        this.audioFile = audioFile;
    }

    protected abstract Metadata getMetadata();

    static void setMetadataFromMap(Metadata metadata, Map<String, String> audioFileMetadata) {
        for (MetadataField<?> field : ALL_METADATA_FIELDS) {
            String name = field.getName();
            String value = audioFileMetadata.get(field.getName());

            if (!StringUtils.isEmpty(value)) {
                metadata.put(name, value);
            }
        }
    }

    protected abstract Set<Category> getCategories();

    AudioItem importSingleFile(AudioItemProcessor itemProcessor) throws IOException
    {
        AudioItem result = null;
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        try {
            AudioItem audioItem = createAudioItem();

            if (store.getAudioItem(audioItem.getUuid()) != null) {
                // just skip if we have an item with the same id already
                System.out.println(String.format("File '%s' is already in database; skipping",
                    audioFile.getName()));
                return audioItem;
            }

            // let caller tweak audio item
            if (itemProcessor != null) {
                itemProcessor.process(audioItem);
            }

            // Commit now because storeAudioFile will copy the file as an a18 (!), and then update the duration field (!)
            // which runs another transaction.
            store.commit(audioItem);

            AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
            repository.storeAudioFile(audioItem, audioFile);
            result = audioItem;
        } catch (AudioItemRepository.UnsupportedFormatException e) {
            throw new IOException(e);
        }
        return result;
    }


    public AudioItem createAudioItem()
            throws IOException {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Metadata loadedMetadata = getMetadata();
        Set<Category> loadedCategories = getCategories();

        // Get a new AudioItem, using a new or existing id.
        String id = null;
        if (loadedMetadata != null) {
            id = loadedMetadata.get(DC_IDENTIFIER);
        }
        // If no id from metadata, try to get it from filename.
        String titleFromFilename = null;
        if (isEmpty(id)) {
            titleFromFilename = FilenameUtils.removeExtension(audioFile.getName());
            int pos = titleFromFilename.indexOf(AudioExporter.FILENAME_SEPARATOR);
            if (pos != -1) {
                id = titleFromFilename.substring(pos + AudioExporter.FILENAME_SEPARATOR.length());
                titleFromFilename = titleFromFilename.substring(0, pos);
            }
            // If no id from filename, allocate a new one.
            if (isEmpty(id)) {
                id = ACMConfiguration.getInstance().getNewAudioItemUID();
            }
        }
        AudioItem audioItem = store.newAudioItem(id);
        Metadata metadata = audioItem.getMetadata();

        // If there is existing metadata, add it to the AudioItem.
        if (loadedMetadata != null) {
            metadata.addValuesFrom(loadedMetadata);
        }

        // Add defaults for missing required values.
        if (isEmpty(metadata.get(DC_IDENTIFIER))) {
            metadata.put(DC_IDENTIFIER, audioItem.getUuid());
        }
        if (isEmpty(metadata.get(DC_TITLE))) {
            if (isEmpty(titleFromFilename)) {
                titleFromFilename = FilenameUtils.removeExtension(audioFile.getName());
                int pos = titleFromFilename.indexOf(AudioExporter.FILENAME_SEPARATOR);
                if (pos != -1) {
                    titleFromFilename = titleFromFilename.substring(0, pos);
                }
            }
            metadata.put(DC_TITLE, titleFromFilename);
        }
        if (isEmpty(metadata.get(DTB_REVISION))) {
            metadata.put(DTB_REVISION, 1);
        }
        // If no DC_LANGUAGE, use the ACM's first language.
        if (isEmpty(metadata.get(DC_LANGUAGE))) {
            List<Locale> languages = ACMConfiguration.getInstance().getCurrentDB().getAudioLanguages();
            String iso639 = languages.size() > 0 ? languages.get(0).getISO3Language() : "en";
            metadata.put(DC_LANGUAGE, iso639);
        }
        // Date?

        // add categories the file had already, if any
        if (loadedCategories != null) {
            audioItem.addCategories(loadedCategories);
        }

        return audioItem;

    }
    
    static boolean isEmpty(String string) {
        return string==null || string.trim().length()==0;
    }

    /**
     * An interface to give import callers an opportunity to examine / tweak an audio file
     * as it is imported.
     */
    public interface AudioItemProcessor {
        void process(AudioItem item);
    }
}
