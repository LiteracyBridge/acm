package org.literacybridge.acm.store;

import java.util.Collection;
import java.util.List;

/**
 * An AudioItem is a unique audio entity, identified by its audioItemID.
 * It is optionally associated with one or more categories and must have
 * at least one {@link LocalizedAudioItem}. If multiple translations of content
 * represented by this AudioItem are available, then there must an instance
 * of {@link LocalizedAudioItem} per translation referenced by this AudioItem.
 *
 */
public abstract class AudioItem implements Persistable {
    public abstract String getUuid();

    public abstract void setUuid(String uuid);

    public abstract void addCategory(Category category);

    public abstract boolean hasCategory(Category category);

    public abstract void addPlaylist(Playlist playlist);

    public abstract boolean hasPlaylist(Playlist playlist);

    public abstract void removePlaylist(Playlist playlist);

    public abstract void removeCategory(Category category);

    public abstract void removeAllCategories();

    public abstract List<Category> getCategoryList();

    public abstract List<Category> getCategoryLeavesList();

    public abstract Collection<Playlist> getPlaylists();

    public abstract Metadata getMetadata();

    public abstract String getRevision();
}
