package org.literacybridge.acm.db;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

import com.google.common.collect.Lists;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
class DBPlaylist implements Playlist {
    private final PersistentTag playlist;

    public DBPlaylist(String name) {
        this(new PersistentTag());
        setName(name);
        playlist.commit();
    }

    public DBPlaylist(PersistentTag playlist) {
        this.playlist = playlist;
        setName(playlist.getName());
    }

    @Override
    public List<AudioItem> getAudioItemList() {
        return DBMetadataStore.toAudioItemList(playlist.getPersistentAudioItemList());
    }

    @Override
    public String getUuid() {
        return playlist.getUuid();
    }

    @Override
    public String getName() {
        return playlist.getName();
    }

    public Integer getId() {
        return playlist.getId();
    }

    @Override
    public void setName(String name) {
        playlist.setTitle(name);
        playlist.setUuid(name);
    }

    @Override
    public int getPosition(AudioItem audioItem) {
        return PersistentTagOrdering.getFromDatabase(((DBAudioItem) audioItem).getPersistentAudioItem(), playlist).getPosition();
    }

    @Override
    public void setPosition(AudioItem audioItem, int position) {
        PersistentTagOrdering ordering =
                PersistentTagOrdering.getFromDatabase(((DBAudioItem) audioItem).getPersistentAudioItem(), playlist);
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

    public static Playlist getFromDatabase(String uid) {
        return new DBPlaylist(PersistentTag.getFromDatabase(uid));
    }

    public static List<Playlist> toPlaylists(Collection<PersistentTag> tags) {
        List<Playlist> playlists = Lists.newArrayListWithCapacity(tags.size());
        for (PersistentTag tag : tags) {
            playlists.add(new DBPlaylist(tag));
        }
        return playlists;
    }

    @Override
    public <T> T commit(EntityManager em) {
        return playlist.commit(em);
    }
}
