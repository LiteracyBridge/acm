package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

import org.literacybridge.acm.store.MetadataStore.Transaction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Playlist implements Persistable {
    // TODO: make uuid final once deprecated method setUuid() is removed
    private String uuid;
    private String name;
    private final List<String> audioItems;

    public Playlist(String uuid) {
        this.uuid = uuid;
        audioItems = Lists.newArrayList();
    }

    @Deprecated
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addAudioItem(String uuid) {
        audioItems.add(uuid);
    }

    public void removeAudioItem(String uuid) {
        audioItems.remove(uuid);
    }

    public int getAudioItemPosition(String uuid) {
        return audioItems.indexOf(uuid);
    }

    public Iterable<String> getAudioItemList() {
        return audioItems;
    }

    public int getNumAudioItems() {
        return audioItems.size();
    }

    @Override
    public void commitTransaction(Transaction t) throws IOException {
        t.getIndex().updatePlaylistName(this, t);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SortedMap<Integer, String> items = Maps.newTreeMap();
        private String uuid;
        private String name;

        private Builder() {}

        public Builder withUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder addAudioItem(String uuid, int position) {
            items.put(position, uuid);
            return this;
        }

        public Playlist build() {
            Playlist playlist = new Playlist(uuid);
            playlist.setName(name);
            for (String audioItem : items.values()) {
                playlist.addAudioItem(audioItem);
            }
            return playlist;
        }
    }
}
