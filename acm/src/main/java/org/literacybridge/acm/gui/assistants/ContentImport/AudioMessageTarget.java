package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;

import java.util.Objects;

public class AudioMessageTarget extends AudioTarget {
    protected MessageSpec message;

    AudioMessageTarget(MessageSpec message, Playlist playlist) {
        super(playlist);
        this.message = message;
    }

    public String getTitle() {
        return message.title;
    }
    public String getPromptString() {
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

    @Override
    public MessageSpec getMessageSpec() {
        return message;
    }
    @Override
    public ContentSpec.PlaylistSpec getPlaylistSpec() {
        return getMessageSpec().getPlaylist();
    }

    @Override
    public int getPlaylistSpecOrdinal() {
        return getMessageSpec().getPlaylist().getOrdinal();
    }
    @Override
    public int getMessageSpecOrdinal() {
        return getMessageSpec().getOrdinal();
    }

    @Override
    public boolean isMessage() {
        return true;
    }
    @Override
    public boolean isPlaylist() {
        return false;
    }

    @Override
    public String toString() {
        return message.title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioMessageTarget that = (AudioMessageTarget) o;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, playlist, item);
    }

}
