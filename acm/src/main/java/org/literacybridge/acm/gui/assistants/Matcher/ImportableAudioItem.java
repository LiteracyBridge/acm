package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

import java.util.Objects;

public class ImportableAudioItem {
    private String title;
    private Playlist playlist;
    private AudioItem item;

    public ImportableAudioItem(String title, Playlist playlist) {
        this.title = title;
        this.playlist = playlist;
    }

    public String getTitle() {
        return title;
    }
    public void setItem(AudioItem item) {
        this.item = item;
    }
    public AudioItem getItem() {
        return item;
    }
    public boolean hasAudioItem() {
        return this.item != null;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportableAudioItem that = (ImportableAudioItem) o;
        return title.equals(that.title) && playlist.equals(that.playlist) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, playlist, item);
    }
}
