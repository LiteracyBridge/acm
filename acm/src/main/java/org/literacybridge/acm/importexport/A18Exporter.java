package org.literacybridge.acm.importexport;

import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.IOException;

class A18Exporter extends AudioFileExporter {
    A18Exporter(AudioItem audioItem, File targetFile) {
        super(audioItem, targetFile, AudioItemRepository.AudioFormat.A18);
    }

    @Override
    void saveWithMetadata(File audioNoMetadata) throws IOException {
        IOUtils.copy(audioNoMetadata, targetFile);

        AudioItemRepository.appendMetadataToA18(audioItem, targetFile);
    }
}
