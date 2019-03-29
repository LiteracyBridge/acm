package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

import java.util.Objects;

public class ImportableAudioItem {
    private String title;
    // The ACM Playlist into which the item will be imported.
    private Playlist playlist;
    private AudioItem item;
    // If there is an audio item AND a matching file, is it OK to replace this item?
    private boolean replaceOk = false;

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

    public void setReplaceOk(boolean replaceOk) {
        this.replaceOk = replaceOk;
    }
    public boolean isReplaceOk() {
        return replaceOk;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public boolean isImportable() {
        return !hasAudioItem() || isReplaceOk();
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
