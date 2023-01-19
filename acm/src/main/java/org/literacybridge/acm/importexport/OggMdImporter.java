package org.literacybridge.acm.importexport;

import adamb.vorbis.CommentField;
import adamb.vorbis.VorbisCommentHeader;
import adamb.vorbis.VorbisIO;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.Taxonomy;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.store.MetadataSpecification.LB_DATE_RECORDED;
import static org.literacybridge.acm.store.MetadataSpecification.LB_PRIMARY_SPEAKER;

class OggMdImporter extends BaseMetadataImporter {
    private Metadata metadata = null;
    private Set<Category> categories = null;

    OggMdImporter(File audioFile) {
        super(audioFile);
    }

    @Override
    protected Metadata getMetadata() {
        if (metadata == null) {
            try {
                Metadata loadedMetadata = new Metadata();

                // Get the Vorbis comments, convert to a more usable structure.
                VorbisCommentHeader vch = VorbisIO.readComments(audioFile);
                List<CommentField> vorbisComments = vch.fields;

                // Import any LB defined metadata fields that exist in the OGG file.
                Map<String, String> oggMetadata = new HashMap<>();
                for (CommentField field : vorbisComments) {
                    String value = field.value;
                    // Support multiple by joining with semicolon; we may revisit this in the future.
                    if (oggMetadata.containsKey(field.name)) {
                        value = oggMetadata.get(field.name) + ";" + value;
                    }
                    oggMetadata.put(field.name, value);
                }
                setMetadataFromMap(loadedMetadata, oggMetadata);

                // If no DC_TITLE, use the OGG title. If no OGG title, super class will parse filename.
                if (!loadedMetadata.containsField(DC_TITLE) && !StringUtils.isBlank(oggMetadata.get("TITLE"))) {
                    loadedMetadata.put(DC_TITLE, oggMetadata.get("TITLE"));
                }
                // If no LB_PRIMARY_SPEAKER, use OGG artist.
                if (!loadedMetadata.containsField(LB_PRIMARY_SPEAKER) && !StringUtils.isBlank(oggMetadata.get("ARTIST"))) {
                    loadedMetadata.put(LB_PRIMARY_SPEAKER, oggMetadata.get("ARTIST"));
                }
                // If no LB_DATE_RECORDED, use OGG date.
                if (!loadedMetadata.containsField(LB_DATE_RECORDED) && !StringUtils.isBlank(oggMetadata.get("DATE"))) {
                    loadedMetadata.put(LB_DATE_RECORDED, oggMetadata.get("DATE"));
                }

                // See if there is a CATEGORIES comment.
                Taxonomy taxonomy = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getTaxonomy();
                String categoriesString = oggMetadata.get("CATEGORIES");
                Set<Category> loadedCategories = Arrays.stream(categoriesString.split(";"))
                    .map(taxonomy::getCategory)
                    .collect(Collectors.toSet());

                metadata = loadedMetadata;
                categories = loadedCategories;
            } catch (Exception e) {
                // Ignore and leave metadata empty.
            }
        }
        return metadata;
    }

    @Override
    protected Set<Category> getCategories() {
        if (categories == null) {
            getMetadata(); // categories is a side effect
        }
        return categories;
    }

  static String[] getSupportedFileExtensions() {
    return new String[] { "ogg" };
  }
}
