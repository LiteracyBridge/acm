package org.literacybridge.acm.gui.fx.dashboard;


import org.literacybridge.acm.store.AudioItem;

import java.util.Locale;
import java.util.stream.Stream;

public class AudioLibraryRow {
    private final AudioItem audioItem;

    public AudioLibraryRow(AudioItem audioItem) {
        this.audioItem = audioItem;
    }

    public AudioItem getAudioItem() {
        return audioItem;
    }

    public String getTitle() {
        return audioItem.getTitle();
    }

    public String getId() {
        return audioItem.getId();
    }

    public String getDuration() {
        return audioItem.getDuration();
    }

    public String getCategory() {
        return "Agriculture Cat";
    }

    public String getSource() {
        return "Unknown";
    }

    public String getLanguage() {
        return audioItem.getLanguageCode();
    }

    public String getDate() {
        return "2026-01-01";
    }

    // True if the search text appears in the title, category, source or language, ignoring case.
    // Empty search text matches every row.
    public boolean matches(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return true;
        }
        String needle = searchText.trim().toLowerCase(Locale.ROOT);
        return Stream.of(getTitle(), getCategory(), getSource(), getLanguage())
                .anyMatch(value -> value != null && value.toLowerCase(Locale.ROOT).contains(needle));
    }
}