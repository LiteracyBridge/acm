package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.DuplicateItemException;
import org.literacybridge.acm.repository.AudioItemRepository.UnsupportedFormatException;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.RFC3066LanguageCode;

public class WavImporter extends AudioFileImporter {
  private Metadata metadata = new Metadata();
  private Set<Category> categories = new HashSet<>();
  WavImporter(File audioFile) {
    super(audioFile);
  }


  @Override
  protected Metadata getMetadata() {
    // Title is all we would have, and the base class will add that for us, so
    // just return the empty metadata.
    return metadata;
  }

  @Override
  protected Set<Category> getCategories() {
    // Unless & until we implement categories in MP3 files, this will be empty.
    return categories;
  }

  static String[] getSupportedFileExtensions() {
    return new String[] { "wav" };
  }
}
