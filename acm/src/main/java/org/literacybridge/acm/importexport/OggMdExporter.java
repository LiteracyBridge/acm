package org.literacybridge.acm.importexport;

import adamb.vorbis.CommentField;
import adamb.vorbis.VorbisCommentHeader;
import adamb.vorbis.VorbisIO;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.IOException;

import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.store.MetadataSpecification.LB_DATE_RECORDED;
import static org.literacybridge.acm.store.MetadataSpecification.LB_PRIMARY_SPEAKER;

class OggMdExporter extends BaseMetadataExporter {
    OggMdExporter(AudioItem audioItem, File targetFile) {
        super(audioItem, targetFile, AudioItemRepository.AudioFormat.OGG);
    }

    private void addMetadata() throws IOException {
        VorbisCommentHeader comments = new VorbisCommentHeader();
        comments.vendor = "Amplio";

        gatherMetadata((name, value) -> comments.fields.add(new CommentField(name, value)));

        // Add metadata that will be shown by more tools.
        Metadata metadata = audioItem.getMetadata();
        comments.fields.add(new CommentField("TITLE", metadata.get(DC_TITLE)));
        comments.fields.add(new CommentField("ARTIST", metadata.get(LB_PRIMARY_SPEAKER)));
        comments.fields.add(new CommentField("DATE", metadata.get(LB_DATE_RECORDED)));

        VorbisIO.writeComments(targetFile, comments);
    }

    @Override
    void saveWithMetadata(File audioNoMetadata) throws IOException {
        IOUtils.copy(audioNoMetadata, targetFile);

        addMetadata();
    }
}
