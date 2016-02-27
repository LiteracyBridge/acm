package org.literacybridge.acm.gui;

import java.util.Map;
import java.util.SortedMap;

import org.literacybridge.acm.store.AudioItem;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class AudioItemCache {
    private final static class CacheKey {
        final String indexKey;  // references an index (SortedMap)
        final String sortKey;   // references an entry within the index

        public CacheKey(String indexKey, String sortKey) {
            this.indexKey = indexKey;
            this.sortKey = sortKey;
        }

        @Override public int hashCode() {
            return 31 * indexKey.hashCode() + sortKey.hashCode();
        }

        @Override public boolean equals(Object o) {
            CacheKey other = (CacheKey) o;
            return this.indexKey.equals(other.indexKey) && this.sortKey.equals(other.sortKey);
        }
    }

    private final static class IndexEntry {
        final String key;
        final AudioItem audioItem;

        public IndexEntry(String key, AudioItem audioItem) {
            this.key = key;
            this.audioItem = audioItem;
        }
    }

    private final Map<String, AudioItem> cache = Maps.newLinkedHashMap();
    private final Map<String, SortedMap<String, IndexEntry>> sortedIndexes = Maps.newHashMap();

    public Iterable<AudioItem> getAudioItems(String sortKey, Predicate<String> filter) {
        SortedMap<String, IndexEntry> index = sortedIndexes.get(sortKey);
        return Iterables.filter(Iterables.transform(index.values(), new Function<IndexEntry, AudioItem>() {
            @Override public AudioItem apply(IndexEntry item) {
                return item.audioItem;
            }
        }), Predicates.compose(filter, new Function<AudioItem, String>() {

            @Override public String apply(AudioItem item) {
                return item.getUuid();
            }
        }));
    }

    public Iterable<AudioItem> getAudioItems() {
        return cache.values();
    }

    private String getSortKey(AudioItem audioItem, String field) {
        return null;
    }

    public void updateAudioItem(AudioItem item) {

    }

    public synchronized AudioItem get(String uuid) {
        return cache.get(uuid);
    }

    public synchronized void update(AudioItem item) {
        cache.put(item.getUuid(), item);
    }

    public synchronized void invalidate(String uuid) {
        cache.remove(uuid);
    }
}
