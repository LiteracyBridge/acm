package org.literacybridge.acm.importexport;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.metadata.UnknownUserTextValue;
import org.cmc.music.myid3.MyID3;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.Taxonomy;

import static org.literacybridge.acm.store.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.store.MetadataSpecification.LB_DATE_RECORDED;
import static org.literacybridge.acm.store.MetadataSpecification.LB_PRIMARY_SPEAKER;

public class MP3Importer extends AudioFileImporter {
    private Metadata metadata = null;
    private Set<Category> categories = null;

    MP3Importer(File audioFile) {
        super(audioFile);
    }

    @Override
    protected Metadata getMetadata() {
        if (metadata == null) {
            Metadata loadedMetadata = new Metadata();

            try {
                // Get the ID3 tags, convert to a more friendly format.
                MusicMetadataSet musicMetadataSet = new MyID3().read(audioFile);
                IMusicMetadata musicMetadata = musicMetadataSet.getSimplified();
                
                // Import any LB defined metadata fields that exist in the OGG file.
                List<UnknownUserTextValue> privateTags = (List<UnknownUserTextValue>)musicMetadata.getUnknownUserTextValues();
                Map<String, String> mp3Metadata = new HashMap<>();
                for (UnknownUserTextValue utv : privateTags) {
                    String value = utv.value;
                    // Support multiple by joining with semicolon; we may revisit this in the future.
                    if (mp3Metadata.containsKey(utv.key)) {
                        value = mp3Metadata.get(utv.key) + ";" + value;
                    }
                    mp3Metadata.put(utv.key, value);
                }
                setMetadataFromMap(loadedMetadata, mp3Metadata);

                // If no DC_TITLE, use the MP3 title. If no MP3 title, super class will parse filename.
                if (!loadedMetadata.containsField(DC_TITLE) && !isEmpty(musicMetadata.getSongTitle())) {
                    loadedMetadata.put(DC_TITLE, musicMetadata.getSongTitle());
                }

                if (!loadedMetadata.containsField(LB_PRIMARY_SPEAKER) && !isEmpty(musicMetadata.getArtist())) {
                    loadedMetadata.put(LB_PRIMARY_SPEAKER, musicMetadata.getArtist());
                }
                if (!loadedMetadata.containsField(DC_PUBLISHER) && !isEmpty(musicMetadata.getPublisher())) {
                    loadedMetadata.put(DC_PUBLISHER, musicMetadata.getPublisher());
                }
                if (!loadedMetadata.containsField(LB_DATE_RECORDED)) {
                    Number year = musicMetadata.getYear();
                    if (year != null && year.intValue() >= 1970) {
                        String date = String.format("%04d", year.intValue());
                        loadedMetadata.put(LB_DATE_RECORDED, date);
                    }
                }

                // See if there is a CATEGORIES comment.
                Taxonomy taxonomy = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getTaxonomy();
                Set<Category> loadedCategories;
                String categoriesString = mp3Metadata.get("CATEGORIES");
                if (!isEmpty(categoriesString)) {
                    loadedCategories = Arrays.stream(categoriesString.split(";"))
                            .map(taxonomy::getCategory)
                            .collect(Collectors.toSet());
                } else {
                    loadedCategories = new HashSet<>();
                }


                metadata = loadedMetadata;
                categories = loadedCategories;
            } catch (Exception e) {
                // Ignore, return null
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
    return new String[] { "mp3" };
  }
}
