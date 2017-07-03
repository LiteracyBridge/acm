package org.literacybridge.acm.importexport;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.importexport.FileImporter.Importer;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.DuplicateItemException;
import org.literacybridge.acm.repository.AudioItemRepository.UnsupportedFormatException;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.LBMetadataIDs;
import org.literacybridge.acm.store.LBMetadataSerializer;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.utils.IOUtils;

public class A18Importer extends FileImporter.Importer {
  public static AudioItem loadMetadata(MetadataStore store, File file) throws IOException {
    DataInputStream in = null;

    try {
      in = new DataInputStream(
          new BufferedInputStream(new FileInputStream(file)));
      int bytesToSkip = IOUtils.readLittleEndian32(in);

      Metadata loadedMetadata = new Metadata();

      Set<Category> categories = new HashSet<Category>();
      if (bytesToSkip + 4 < file.length()) {
        try {
          in.skipBytes(bytesToSkip);
          LBMetadataSerializer serializer = new LBMetadataSerializer();
          serializer.deserialize(loadedMetadata, store.getTaxonomy(),
              categories, in);
        } catch (IOException e) {
          // do nothing
        }
      }

      AudioItem audioItem = null;

      if (loadedMetadata.getNumberOfFields() == 0) {
        // legacy mode
        audioItem = store
            .newAudioItem(ACMConfiguration.getInstance().getNewAudioItemUID());
        Metadata metadata = audioItem.getMetadata();
        String fileName = file.getName();
        metadata.setMetadataField(MetadataSpecification.DTB_REVISION,
            new MetadataValue<String>("1"));
        metadata.setMetadataField(MetadataSpecification.DC_IDENTIFIER,
            new MetadataValue<String>(audioItem.getUuid()));
        metadata.setMetadataField(MetadataSpecification.DC_LANGUAGE,
            new MetadataValue<RFC3066LanguageCode>(
                new RFC3066LanguageCode(Locale.ENGLISH.getLanguage())));

        metadata.setMetadataField(MetadataSpecification.DC_TITLE,
            new MetadataValue<String>(
                fileName.substring(0, fileName.length() - 4)));

      } else {
        audioItem = store.newAudioItem(loadedMetadata
            .getMetadataValue(MetadataSpecification.DC_IDENTIFIER).getValue());
        Metadata metadata = audioItem.getMetadata();

        // Add metadata the file already had, if any.
        metadata.addValuesFrom(loadedMetadata);

        if (metadata
            .getMetadataValue(MetadataSpecification.DTB_REVISION) == null) {
          metadata.setMetadataField(MetadataSpecification.DTB_REVISION,
              new MetadataValue<String>("0"));
        }
      }

      // add categories the file had already, if any
      for (Category cat : categories) {
        audioItem.addCategory(cat);
      }

      return audioItem;
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

    @Override
    protected void importSingleFile(MetadataStore store, File file,
                                    FileImporter.AudioItemProcessor processor) throws IOException {
      try {
        AudioItem audioItem = loadMetadata(store, file);

        // TODO: handle updating the file by making use of revisions
        if (store.getAudioItem(audioItem.getUuid()) != null) {
            // just skip for now if we have an item with the same id already
            System.out.println(String.format("File '%s' is already in database; skipping", file.getName()));
            return;
        }

        AudioItemRepository repository = ACMConfiguration.getInstance()
                .getCurrentDB().getRepository();
        repository.storeAudioFile(audioItem, file);
          // let caller tweak audio item
          if (processor != null) {
              processor.process(audioItem);
          }

          store.commit(audioItem);

      } catch (UnsupportedFormatException e) {
          throw new IOException(e);
      } catch (DuplicateItemException e) {
          throw new IOException(e);
      }
    }

  @Override
  protected String[] getSupportedFileExtensions() {
    return new String[] { ".a18" };
  }
}
