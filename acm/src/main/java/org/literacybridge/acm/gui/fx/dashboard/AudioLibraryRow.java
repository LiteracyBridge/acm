package org.literacybridge.acm.gui.fx.dashboard;


import org.literacybridge.acm.store.AudioItem;

public class AudioLibraryRow {
    private final AudioItem audioItem;
//    private final String id;
//    private final String title;
//    private final String duration;
//    private final String category;
//    private final String source;
//    private final String language;
//    private final String date;

    public AudioLibraryRow(AudioItem audioItem) {
        this.audioItem = audioItem;
//        this.title = title;
//        this.duration = duration;
//        this.category = category;
//        this.source = source;
//        this.language = language;
//        this.date = date;
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
}