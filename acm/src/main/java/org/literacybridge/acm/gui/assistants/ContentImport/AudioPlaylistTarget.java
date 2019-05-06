package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;

import java.io.File;
import java.util.Objects;

public class AudioPlaylistTarget extends AudioTarget {
    private String title;
    private ContentSpec.PlaylistSpec playlistSpec;
    private boolean isLong;
    private File file;

    public AudioPlaylistTarget(ContentSpec.PlaylistSpec playlistSpec, String title, boolean isLong, Playlist playlist) {
        super(playlist);
        this.playlistSpec = playlistSpec;
        this.title = title;
        this.isLong = isLong;
    }

    public String getTitle() {
        return title;
    }

    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }

    public boolean hasAudioItem() {
        return this.item != null || this.file != null;
    }
    @Override
    public boolean targetExists() { return hasAudioItem(); }

    public Playlist getPlaylist() {
        return playlist;
    }
    public MessageSpec getMessage() {
        return null;
    }

    public ContentSpec.PlaylistSpec getPlaylistSpec() {
        return playlistSpec;
    }

    public int getPlaylistSpecOrdinal() {
        return playlistSpec.getOrdinal();
    }
    // The playlist prompts sort before the messages.
    public int getMessageSpecOrdinal() {
        return isLong ? -1 : -2;
    }
    public boolean isLong() {
        return isLong;
    }

    @Override
    public boolean isMessage() {
        return false;
    }
    @Override
    public boolean isPlaylist() {
        return true;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioPlaylistTarget that = (AudioPlaylistTarget) o;
        return this.isLong==that.isLong && playlistSpec.equals(that.playlistSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playlistSpec, isLong, playlist, item);
    }

}
