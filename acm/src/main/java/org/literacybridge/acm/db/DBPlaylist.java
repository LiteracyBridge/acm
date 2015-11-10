package org.literacybridge.acm.db;

import java.util.Collection;
import java.util.List;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore.Transaction;
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
        ACMConfiguration.getCurrentDB().getMetadataStore().commit(playlist);
    }

    public DBPlaylist(PersistentTag playlist) {
        this.playlist = playlist;
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
        ACMConfiguration.getCurrentDB().getMetadataStore().commit(ordering);
    }

    PersistentTag getTag() {
        return playlist;
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
    public void commitTransaction(Transaction transaction) {
        throw new UnsupportedOperationException("Writing to Derby DB is not supported anymore.");
    }
}
