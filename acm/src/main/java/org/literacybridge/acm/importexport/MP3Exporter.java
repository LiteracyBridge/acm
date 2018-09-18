package org.literacybridge.acm.importexport;

import org.apache.commons.io.FilenameUtils;
import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.UnknownUserTextValue;
import org.cmc.music.myid3.MyID3;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.literacybridge.acm.store.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.store.MetadataSpecification.LB_GOAL;
import static org.literacybridge.acm.store.MetadataSpecification.LB_KEYWORDS;
import static org.literacybridge.acm.store.MetadataSpecification.LB_NOTES;
import static org.literacybridge.acm.store.MetadataSpecification.LB_PRIMARY_SPEAKER;

class MP3Exporter extends AudioFileExporter {
    MP3Exporter(AudioItem audioItem, File targetFile) {
        super(audioItem, targetFile, AudioItemRepository.AudioFormat.MP3);
    }

    private File getTmpFile() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        String filename = FilenameUtils.removeExtension(targetFile.getName()) + "-tmp";
        File tmpFile = new File(tmpDir,filename + "." + targetFormat.getFileExtension());
        int counter = 0;
        while (tmpFile.exists()) {
            tmpFile = new File(tmpDir,filename + "-" + ++counter + "." + targetFormat.getFileExtension());
        }
        return tmpFile;
    }

    static boolean isEmpty(String string) {
        return string==null || string.trim().length()==0;
    }

    @Override
    @SuppressWarnings("unchecked")
    void saveWithMetadata(File audioNoMetadata) throws IOException {
        MusicMetadata metadataToWrite = MusicMetadata.createEmptyMetadata();
        Vector<UnknownUserTextValue> mdTags = new Vector<>();
        gatherMetadata((name, value) -> {mdTags.add(new UnknownUserTextValue(name, value));});
        metadataToWrite.setUnknownUserTextValues(mdTags);

        // Add metadata that will be shown by more tools.
        Metadata metadata = audioItem.getMetadata();
        metadataToWrite.setSongTitle(metadata.get(DC_TITLE));
        if (!isEmpty(metadata.get(LB_PRIMARY_SPEAKER)))
            metadataToWrite.setArtist(metadata.get(LB_PRIMARY_SPEAKER));
        if (!isEmpty(metadata.get(DC_PUBLISHER)))
            metadataToWrite.setPublisher(metadata.get(DC_PUBLISHER));
        List<String> comments = new ArrayList<>();
        comments.add(metadata.get(LB_GOAL));
        comments.add(metadata.get(LB_NOTES));
        comments.add(metadata.get(LB_KEYWORDS));
        Vector<String> vector = (Vector<String>) comments.stream().filter(str -> !isEmpty(str)).collect(Collectors.toCollection((Supplier<Vector>) Vector::new));
        metadataToWrite.setComments(vector);

        MyID3 tagWriter = new MyID3();
        try {
            tagWriter.write(audioNoMetadata, targetFile, null, metadataToWrite);
        } catch (ID3WriteException e) {
            e.printStackTrace();
        }
    }
}
