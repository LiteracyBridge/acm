package org.literacybridge.acm.importexport;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import static org.literacybridge.acm.store.MetadataSpecification.ALL_METADATA_FIELDS;

/**
 * Base class for audio file exporters. Handles most of the operations that are common to all formats.
 */

abstract class AudioFileExporter {
    AudioItem audioItem = null;
    File targetFile;
    AudioItemRepository.AudioFormat targetFormat;

    /**
     * Constructor with the salient information about the export.
     * @param audioItem The audio item to be exported.
     * @param targetFile The filename to which it should be exported, with extension.
     * @param targetFormat The audio format to which to export.
     */
    AudioFileExporter(AudioItem audioItem, File targetFile, AudioItemRepository.AudioFormat targetFormat) {
        this.audioItem = audioItem;
        this.targetFile = targetFile;
        this.targetFormat = targetFormat;
    }

    /**
     * Performs the conversion.
     * @throws IOException If a file can't be read or written.
     * @throws ConversionException If the format conversion fails.
     */
    void export() throws IOException, ConversionException, AudioItemRepository.UnsupportedFormatException {
        AudioItemRepository repository = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getRepository();

        // Get a version of the file in the right format.
        File sourceFile = repository.getAudioFile(audioItem, targetFormat);

        if (sourceFile == null || ! sourceFile.exists()) {
            // Count file that couldn't be exported.
            return;
        }

        saveWithMetadata(sourceFile);
    }

    /**
     * Called after conversion, to let the format specific things happen. Essentially, adding
     * the metadata.
     * @param audioNoMetadata The converted file, without metadata. Resides within the audio
     *                        repository.
     * @throws IOException If the metadata can't be written.
     */
    abstract void saveWithMetadata(File audioNoMetadata) throws IOException;

    /**
     * Gets the metadata and categories from the source file. Passes them, one at a time, to
     * a consumer.
     * @param consumer Called with a name:value for the metadata and categories. The consumer
     *                 can then do whatever is needed, in the context of the caller.
     */
    void gatherMetadata(BiConsumer<String, String> consumer) {
        Metadata metadata = audioItem.getMetadata();
        for (MetadataField<?> field : ALL_METADATA_FIELDS) {
            String name = field.getName();
            String value = metadata.get(field);

            if (!StringUtils.isEmpty(value)) {
                consumer.accept(name, value);
            }
        }

        // Add the categories. An alternative would be to add multiple comments per category.
        List<String> categoryList = audioItem.getCategoryLeavesList().stream()
            .map(Category::getId)
            .collect(Collectors.toList());
        String categoryString = String.join(";", categoryList);
        consumer.accept("CATEGORIES", categoryString);
    }
}
