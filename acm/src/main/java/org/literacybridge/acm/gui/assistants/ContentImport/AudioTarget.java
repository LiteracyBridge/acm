package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;

import java.util.ArrayList;
import java.util.List;

public abstract class AudioTarget extends Target {
    // The ACM Playlist into which the item will be imported.
    protected Playlist playlist;
    protected AudioItem item;

    AudioTarget(Playlist playlist) {
        this.playlist = playlist;
    }

    // Name of the item in the ACM db.
    public abstract String getTitle();

    /**
     * Some audio targets have multiple possible names, specifically the long form prompt for
     * a playlist may be named like "Playlist title - invitation", "Playlist title - description",
     * or one of several other titles. This is very important when we want to match a file name
     * against a playlist title long form name; the file will have one of several names, and
     * whatever name it has, we want to be able to match that file against the playlist name.
     *
     * This method lets an audio target provide a list of titles by which it is willing to be
     * named. An ordinary content message will provide only one. A playlist short-form title will
     * also provide only one. But a playlist long-form title will provide several.
     *
     * The default implementation returns a list consisting of a single title.
     * 
     * @return a list of one or more titles for this audio item.
     */
    public List<String> getTitles() {
        List<String> result = new ArrayList<>();
        result.add(getTitle());
        return result;
    }

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
