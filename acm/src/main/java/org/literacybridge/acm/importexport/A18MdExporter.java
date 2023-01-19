package org.literacybridge.acm.importexport;

import org.literacybridge.acm.repository.A18Utils;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.IOException;

class A18MdExporter extends BaseMetadataExporter {
    A18MdExporter(AudioItem audioItem, File targetFile) {
        super(audioItem, targetFile, AudioItemRepository.AudioFormat.A18);
    }

    @Override
    void saveWithMetadata(File audioNoMetadata) throws IOException {
        IOUtils.copy(audioNoMetadata, targetFile);

        A18Utils.appendMetadataToA18(audioItem, targetFile);
    }
}
