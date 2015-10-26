package org.literacybridge.acm.gui;

import java.util.Map;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;

import com.google.common.collect.Maps;

public class AudioItemCache {
    private final Map<String, AudioItem> cache = Maps.newHashMap();
    private final MetadataStore store;

    public AudioItemCache(MetadataStore store) {
        this.store = store;
    }

    public synchronized AudioItem get(String uuid) {
        AudioItem audioItem = cache.get(uuid);
        if (audioItem == null) {
            audioItem = store.getAudioItem(uuid);
            cache.put(uuid, audioItem);
        }

        return audioItem;
    }

    public synchronized void add(AudioItem item) {
        cache.put(item.getUuid(), item);
    }

    public synchronized void invalidate(String uuid) {
        cache.remove(uuid);
    }
}
