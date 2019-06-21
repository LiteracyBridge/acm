package org.literacybridge.acm.gui.MainWindow;

import org.literacybridge.acm.gui.util.SortedListModel;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlaylistListModel extends SortedListModel<PlaylistListModel.PlaylistLabel> {
  private Map<Playlist, Integer> facetMap = new HashMap<>();

  PlaylistListModel(Iterable<Playlist> playlists, SearchResult searchResult) {
    for (Playlist playlist : playlists) {
      add(new PlaylistLabel(playlist));
    }
    updateFacetCounts(searchResult);
  }

  public void updateFacetCounts(SearchResult searchResult) {
    facetMap.clear();
      for (PlaylistLabel playlistLabel : getModel()) {
        facetMap.put(playlistLabel.playlist, searchResult.getFacetCount(playlistLabel.playlist));
      }
  }

  public final class PlaylistLabel implements Comparable<PlaylistLabel> {
    private final Playlist playlist;

    private PlaylistLabel(Playlist playlist) {
      this.playlist = playlist;
    }

    public Playlist getPlaylist() {
      return playlist;
    }

    int getFacetCount() {
      return facetMap.getOrDefault(getPlaylist(), 0);
    }

    @Override
    public String toString() {
      return getFacetCount() == 0 ? playlist.getName()
          : playlist.getName() + " [" + getFacetCount() + "]";
    }

    @Override
    public int compareTo(PlaylistLabel other) {
      // display the tags in reverse sort order
      return -playlist.getName().compareToIgnoreCase(other.getPlaylist().getName());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PlaylistLabel that = (PlaylistLabel) o;
      return getPlaylist().equals(that.getPlaylist());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getPlaylist());
    }
  }
}
