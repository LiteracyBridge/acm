package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;

import java.util.Objects;

public class AudioTarget extends Target {
    private MessageSpec message;
    // The ACM Playlist into which the item will be imported.
    private Playlist playlist;
    private AudioItem item;

    public AudioTarget(MessageSpec message, Playlist playlist) {
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
    @Override
    public boolean targetExists() { return hasAudioItem(); }

    public Playlist getPlaylist() {
        return playlist;
    }
    public MessageSpec getMessage() {
        return message;
    }


    @Override
    public String toString() {
        return message.title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioTarget that = (AudioTarget) o;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, playlist, item);
    }

}
