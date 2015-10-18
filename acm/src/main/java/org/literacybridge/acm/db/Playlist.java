package org.literacybridge.acm.db;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

public class Playlist implements Persistable {
    private final PersistentTag playlist;

    public Playlist() {
        this(new PersistentTag());
    }

    public Playlist(PersistentTag playlist) {
        this.playlist = playlist;
    }

    public List<AudioItem> getAudioItemList() {
        return AudioItem.toAudioItemList(playlist.getPersistentAudioItemList());
    }

    public String getName() {
        return playlist.getName();
    }

    public Integer getId() {
        return playlist.getId();
    }

    public void setName(String name) {
        playlist.setTitle(name);
    }

    public int getPosition(AudioItem audioItem) {
        return PersistentTagOrdering.getFromDatabase(audioItem.getPersistentAudioItem(), playlist).getPosition();
    }

    public void setPosition(AudioItem audioItem, int position) {
        PersistentTagOrdering ordering =
                PersistentTagOrdering.getFromDatabase(audioItem.getPersistentAudioItem(), playlist);
        ordering.setPosition(position);
        ordering.commit();
    }

    PersistentTag getTag() {
        return playlist;
    }

    @Override
    public <T> T commit() {
        return playlist.commit();
    }

    @Override
    public void destroy() {
        playlist.destroy();
    }

    @Override
    public <T> T refresh() {
        return playlist.refresh();
    }

    public static List<Playlist> getFromDatabase() {
        return toPlaylists(PersistentTag.getFromDatabase());
    }

    public static List<Playlist> toPlaylists(Collection<PersistentTag> tags) {
        List<Playlist> playlists = Lists.newArrayListWithCapacity(tags.size());
        for (PersistentTag tag : tags) {
            playlists.add(new Playlist(tag));
        }
        return playlists;
    }

}
