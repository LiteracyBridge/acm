package org.literacybridge.acm.gui.assistants.util;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.api.AudioConversionFormat;
import org.literacybridge.acm.audioconverter.api.ExternalConverter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.literacybridge.acm.store.MetadataSpecification.DC_IDENTIFIER;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;

public class AudioUtils {

    public static void copyOrConvert(String title, String languagecode, File fromFile, File toFile)
        throws BaseAudioConverter.ConversionException, IOException
    {
        Metadata metadata = AudioImporter.getInstance().getExistingMetadata(fromFile);
        // Don't expect to usually find existing metadata.
        if (metadata == null) {
            metadata = new Metadata();
        }
        Collection<Category> categories = new ArrayList<>();
        // get a new id, even if the object already had one.
        String id = ACMConfiguration.getInstance().getNewAudioItemUID();
        metadata.put(DC_IDENTIFIER, id);
        metadata.put(DC_LANGUAGE, languagecode);
        metadata.put(DC_TITLE, title);
        Category communities = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getCategory(Constants.CATEGORY_COMMUNITIES);
        categories.add(communities);

        if (FilenameUtils.getExtension(fromFile.getName()).equalsIgnoreCase(AudioItemRepository.AudioFormat.A18.getFileExtension())) {
            AudioItemRepository.copyA18WithoutMetadata(fromFile, toFile);
        } else {
            AudioConversionFormat audioConversionFormat = AudioItemRepository.AudioFormat.A18.getAudioConversionFormat();
            new ExternalConverter().convert(fromFile, toFile, audioConversionFormat, true);
        }

        AudioItemRepository.appendMetadataToA18(metadata, categories, toFile);
    }


}
