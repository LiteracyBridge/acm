package org.literacybridge.acm.importexport;

import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.IOException;

class WavMdExporter extends BaseMetadataExporter {
    WavMdExporter(AudioItem audioItem, File targetFile) {
        super(audioItem, targetFile, AudioItemRepository.AudioFormat.WAV);
    }

    @Override
    void saveWithMetadata(File audioNoMetadata) throws IOException {
        IOUtils.copy(audioNoMetadata, targetFile);
        // No metadata for a .wav file.
    }
}
