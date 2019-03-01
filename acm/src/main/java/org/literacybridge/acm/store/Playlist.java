package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Playlist extends Committable {
  private final String uuid;
  private String name;
  private final List<String> audioItems;

  Playlist(String uuid) {
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

  public void addAudioItem(AudioItem item) {
    addAudioItem(item.getUuid());
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

  public Collection<String> getAudioItemList() {
    return audioItems;
  }

  public int getNumAudioItems() {
    return audioItems.size();
  }

  @Override
  public void prepareCommit(MetadataStore store,
      List<Committable> additionalObjects) {
    if (isDeleteRequested()) {
      for (String item : audioItems) {
        AudioItem audioItem = store.getAudioItem(item);
        audioItem.removePlaylist(this);
        additionalObjects.add(audioItem);
      }
    } else {
      for (String item : audioItems) {
        AudioItem audioItem = store.getAudioItem(item);
        audioItem.addPlaylist(this);
        additionalObjects.add(audioItem);
      }
    }
  }

  @Override
  public boolean doCommit(Transaction t) throws IOException {
    if (isDeleteRequested()) {
      t.getIndex().deletePlaylist(uuid, t);
      return false;
    } else {
      return t.getIndex().updatePlaylistName(this, t);
    }
  }

  @Override
  public void doRollback(Transaction t) throws IOException {
    t.getIndex().refresh(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private SortedMap<Integer, String> items = Maps.newTreeMap();
    private String uuid;
    private String name;
    private Playlist playlistPrototype;

    private Builder() {
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withPlaylistPrototype(Playlist playlist) {
      this.playlistPrototype = playlist;
      return this;
    }

    public Builder addAudioItem(String uuid, int position) {
      items.put(position, uuid);
      return this;
    }

    public Playlist build() {
      final Playlist playlist = (playlistPrototype == null) ? new Playlist(uuid)
          : playlistPrototype;
      playlist.audioItems.clear();
      playlist.setName(name);
      for (String audioItem : items.values()) {
        playlist.addAudioItem(audioItem);
      }
      return playlist;
    }
  }
}
