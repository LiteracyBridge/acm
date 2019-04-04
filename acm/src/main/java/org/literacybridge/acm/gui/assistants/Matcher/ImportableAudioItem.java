package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;

import java.util.Objects;

public class ImportableAudioItem {
    private MessageSpec message;
    // The ACM Playlist into which the item will be imported.
    private Playlist playlist;
    private AudioItem item;
    // If there is an audio item AND a matching file, is it OK to replace this item?
    private boolean replaceOk = false;

    public ImportableAudioItem(MessageSpec message, Playlist playlist) {
        this.message = message;
        this.playlist = playlist;
    }

    public String getTitle() {
        return message.title;
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
    public MessageSpec getMessage() {
        return message;
    }

    public boolean isImportable() {
        return !hasAudioItem() || isReplaceOk();
    }

    @Override
    public String toString() {
        return message.title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportableAudioItem that = (ImportableAudioItem) o;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, playlist, item);
    }

}
