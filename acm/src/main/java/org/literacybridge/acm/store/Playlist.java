package org.literacybridge.acm.store;

import java.util.List;

import org.literacybridge.acm.store.MetadataStore.Transaction;

import com.google.common.collect.Lists;

public class Playlist implements Persistable {
    private final String uuid;
    private String name;
    private final List<String> audioItems;

    public Playlist(String uuid) {
        this.uuid = uuid;
        audioItems = Lists.newArrayList();
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

    @Override
    public <T extends Transaction> void commitTransaction(T t) {
    }
}
