package org.literacybridge.acm.gui.fx.dashboard;


public class AudioLibraryRow {
    private final String title;
    private final String duration;
    private final String category;
    private final String source;
    private final String language;
    private final String date;

    public AudioLibraryRow(String title, String duration, String category,
                           String source, String language, String date) {
        this.title = title;
        this.duration = duration;
        this.category = category;
        this.source = source;
        this.language = language;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }

    public String getCategory() {
        return category;
    }

    public String getSource() {
        return source;
    }

    public String getLanguage() {
        return language;
    }

    public String getDate() {
        return date;
    }
}