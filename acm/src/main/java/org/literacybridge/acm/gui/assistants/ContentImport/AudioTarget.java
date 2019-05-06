package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;

import java.util.Objects;

public abstract class AudioTarget extends Target {
    // The ACM Playlist into which the item will be imported.
    protected Playlist playlist;
    protected AudioItem item;

    public AudioTarget(Playlist playlist) {
        this.playlist = playlist;
    }

    public abstract String getTitle();

    public void setItem(AudioItem item) {
        this.item = item;
    }
    public AudioItem getItem() {
        return item;
    }
    public abstract boolean hasAudioItem();

    public Playlist getPlaylist() {
        return playlist;
    }
    private MessageSpec getMessage() {
        return null;
    }

    public abstract int getPlaylistSpecOrdinal();
    public abstract int getMessageSpecOrdinal();

    public abstract boolean isMessage();
    public abstract boolean isPlaylist();

}
