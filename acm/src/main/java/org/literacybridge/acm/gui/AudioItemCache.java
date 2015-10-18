package org.literacybridge.acm.gui;

import java.util.Map;

import org.literacybridge.acm.db.AudioItem;

import com.google.common.collect.Maps;

public class AudioItemCache {
    private Map<String, AudioItem> cache = Maps.newHashMap();

    public synchronized AudioItem get(String uuid) {
        AudioItem audioItem = cache.get(uuid);
        if (audioItem == null) {
            audioItem = AudioItem.getFromDatabase(uuid);
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
