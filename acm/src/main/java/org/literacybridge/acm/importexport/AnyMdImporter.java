package org.literacybridge.acm.importexport;

import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Will attempt to import anything, using ffmpeg.
 */
class AnyMdImporter extends BaseMetadataImporter {
    private final Metadata metadata = new Metadata();
    private final Set<Category> categories = new HashSet<>();

    AnyMdImporter(File audioFile) {
        super(audioFile);
    }

    @Override
    protected Metadata getMetadata() {
        // Title is all we would have, and the base class will add that for us, so
        // just return the empty metadata.
        // TODO: use ffmpeg to extract any metadata.
        return metadata;
    }

    @Override
    protected Set<Category> getCategories() {
        // Just another kind of metadata, and we don't have any.
        return categories;
    }
}
