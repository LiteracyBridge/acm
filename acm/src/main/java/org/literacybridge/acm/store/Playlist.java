package org.literacybridge.acm.store;

import java.util.List;

public interface Playlist extends Persistable {

    List<AudioItem> getAudioItemList();

    String getUuid();

    String getName();

    void setName(String name);

    int getPosition(AudioItem audioItem);

    void setPosition(AudioItem audioItem, int position);

}
