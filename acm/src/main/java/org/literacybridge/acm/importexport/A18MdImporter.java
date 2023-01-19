package org.literacybridge.acm.importexport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.LBMetadataSerializer;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

class A18MdImporter extends BaseMetadataImporter {
    private Set<Category> categories = null;
    private Metadata metadata = null;

    public A18MdImporter(File audioFile) {
        super(audioFile);
    }

    @Override
    protected Metadata getMetadata() {
        if (metadata == null) {
            try (FileInputStream fis = new FileInputStream(audioFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataInputStream in = new DataInputStream(bis)) {
                int bytesToSkip = IOUtils.readLittleEndian32(in);

                MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
                Metadata loadedMetadata = new Metadata();
                Set<Category> loadedCategories = new HashSet<>();

                if (bytesToSkip + 4 < audioFile.length()) {
                    try {
                        in.skipBytes(bytesToSkip);
                        LBMetadataSerializer serializer = new LBMetadataSerializer();
                        serializer.deserialize(loadedMetadata,
                            store.getTaxonomy(),
                            loadedCategories,
                            in);

                        categories = loadedCategories;
                        metadata = loadedMetadata;
                    } catch (IOException e) {
                        // do nothing; loadedMetadata is probably still empty. Or maybe not.
                    }
                }

            } catch (Exception e) {
                // Ignore, return empty Metadata
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
        return new String[] { "a18" };
    }
}
