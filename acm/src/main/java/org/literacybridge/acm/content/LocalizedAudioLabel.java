package org.literacybridge.acm.content;

public class LocalizedAudioLabel {
    private String label;
    private String description;

    public LocalizedAudioLabel(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return this.label;
    }

    public String getDescription() {
        return this.description;
    }


    @Override
    public String toString() {
        return this.label;
    }
}
