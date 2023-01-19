package org.literacybridge.acm.importexport;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataStore;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.literacybridge.acm.store.MetadataSpecification.ALL_METADATA_FIELDS;
import static org.literacybridge.acm.store.MetadataSpecification.DC_IDENTIFIER;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.store.MetadataSpecification.DTB_REVISION;

/**
 * Abstract base class for an audio file importer.
 */
abstract class BaseMetadataImporter {

    File audioFile;
    BaseMetadataImporter(File audioFile) {
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

    public AudioItem createAudioItem() {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Metadata loadedMetadata = getMetadata();
        Set<Category> loadedCategories = getCategories();

        // Get a new AudioItem, using a new id.
        String id = ACMConfiguration.getInstance().getNewAudioItemUID();

        // Try to get it from filename. When audio is exported as
        // "title + id", the format is "${title}___${id}"; we can parse that for the id.
        String titleFromFilename = FilenameUtils.removeExtension(audioFile.getName());
        int pos = titleFromFilename.indexOf(AudioExporter.AUDIOITEM_ID_SEPARATOR);
        if (pos != -1) {
            titleFromFilename = titleFromFilename.substring(0, pos);
        }
        AudioItem audioItem = store.newAudioItem(id);
        Metadata metadata = audioItem.getMetadata();

        // If there is existing metadata, add it to the AudioItem. Don't copy the ID.
        if (loadedMetadata != null) {
            // Don't copy any id from the imported item.a
            metadata.addValuesFromOtherWithExclusions(loadedMetadata, DC_IDENTIFIER);
        }

        // Be completely sure that the DC_IDENTIFIER == the AudioItem.id
        metadata.put(DC_IDENTIFIER, audioItem.getId());

        // Add defaults for missing required values.
        if (StringUtils.isBlank(metadata.get(DC_TITLE))) {
            if (StringUtils.isBlank(titleFromFilename)) {
                titleFromFilename = FilenameUtils.removeExtension(audioFile.getName());
                pos = titleFromFilename.indexOf(AudioExporter.AUDIOITEM_ID_SEPARATOR);
                if (pos != -1) {
                    titleFromFilename = titleFromFilename.substring(0, pos);
                }
            }
            metadata.put(DC_TITLE, titleFromFilename);
        }
        if (StringUtils.isBlank(metadata.get(DTB_REVISION))) {
            metadata.put(DTB_REVISION, 1);
        }
        // If no DC_LANGUAGE, use the ACM's first language.
        if (StringUtils.isBlank(metadata.get(DC_LANGUAGE))) {
            List<Locale> languages = ACMConfiguration.getInstance().getCurrentDB().getAudioLanguages();
            String iso639 = languages.size() > 0 ? languages.get(0).getLanguage() : "en";
            metadata.put(DC_LANGUAGE, iso639);
        }
        // Date?

        // add categories the file had already, if any
        if (loadedCategories != null) {
            audioItem.addCategories(loadedCategories);
        }

        return audioItem;

    }

}
