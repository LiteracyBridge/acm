package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;

public abstract class AudioTarget extends Target {
    // The ACM Playlist into which the item will be imported.
    protected Playlist playlist;
    protected AudioItem item;

    AudioTarget(Playlist playlist) {
        this.playlist = playlist;
    }

    // Name of the item in the ACM db.
    public abstract String getTitle();
    // To show to the user when telling them about the item.
    public abstract String getPromptString();

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

    public abstract ContentSpec.MessageSpec getMessageSpec();
    public abstract ContentSpec.PlaylistSpec getPlaylistSpec();

    public abstract int getPlaylistSpecOrdinal();
    public abstract int getMessageSpecOrdinal();

    public abstract boolean isMessage();
    public abstract boolean isPlaylist();

}
