package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AudioPlaylistTarget extends AudioTarget {
    private static final String SHORT_PROMPT = "\"%s\"";
    private static final String LONG_PROMPT = "\"...%s...\"";

    private ContentSpec.PlaylistSpec playlistSpec;
    private boolean isLong;
    private File file;

    AudioPlaylistTarget(ContentSpec.PlaylistSpec playlistSpec, boolean isLong, Playlist playlist) {
        super(playlist);
        this.playlistSpec = playlistSpec;
        this.isLong = isLong;
    }

    public String getTitle() {
        return String.format(isLong?PlaylistPrompts.LONG_TITLE:PlaylistPrompts.SHORT_TITLE, playlistSpec.getPlaylistTitle());
    }

    /**
     * See super class for more details. If this is the long-form playlist title, return a list
     * of all the possible names for the long-form title ("Playlist title - invitation", etc.).
     * If NOT a long-form playlist title, let the super class decide what to do.
     * @return a list of one or more playlist titles.
     */
    public List<String> getTitles() {
        if (!isLong) return super.getTitles();
        String title = playlistSpec.getPlaylistTitle();
        return Arrays.stream(PlaylistPrompts.LONG_TITLE_LIST)
            .map(s -> String.format(s, title))
            .collect(Collectors.toList());
    }

    public String getPromptString() {
        return String.format(isLong?LONG_PROMPT:SHORT_PROMPT, playlistSpec.getPlaylistTitle());
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

    @Override
    public MessageSpec getMessageSpec() {
        return null;
    }
    @Override
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
        return getTitle();
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
