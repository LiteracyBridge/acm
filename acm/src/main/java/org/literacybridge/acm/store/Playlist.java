package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Playlist extends Committable {
  private final String id;
  private String name;
  private final List<String> audioItems;

  Playlist(String id) {
    this.id = id;
    audioItems = Lists.newArrayList();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addAudioItem(AudioItem item) {
    addAudioItem(item.getId());
  }

  public void addAudioItem(String id) {
    audioItems.add(id);
  }

  public void addAudioItem(int index, AudioItem item) {
    addAudioItem(index, item.getId());
  }

  public void addAudioItem(int index, String id) {
    audioItems.add(index, id);
  }

  public void removeAudioItem(String id) {
    audioItems.remove(id);
  }

  public int getAudioItemPosition(String id) {
    return audioItems.indexOf(id);
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
      t.getIndex().deletePlaylist(id, t);
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
    private String id;
    private String name;
    private Playlist playlistPrototype;

    private Builder() {
    }

    public Builder withId(String id) {
      this.id = id;
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

    public Builder addAudioItem(String id, int position) {
      items.put(position, id);
      return this;
    }

    public Playlist build() {
      final Playlist playlist = (playlistPrototype == null) ? new Playlist(id)
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
