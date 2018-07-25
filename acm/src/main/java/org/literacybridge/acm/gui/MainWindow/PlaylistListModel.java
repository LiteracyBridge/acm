package org.literacybridge.acm.gui.MainWindow;

import org.literacybridge.acm.gui.util.SortedListModel;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;

class PlaylistListModel extends SortedListModel<PlaylistListModel.PlaylistLabel> {
  PlaylistListModel(Iterable<Playlist> playlists, SearchResult searchResult) {
    for (Playlist playlist : playlists) {
      add(new PlaylistLabel(playlist, searchResult.getFacetCount(playlist)));
    }
  }

  public static final class PlaylistLabel implements Comparable<PlaylistLabel> {
    private final Playlist playlist;
    private final int facetCount;

    private PlaylistLabel(Playlist playlist, int facetCount) {
      this.playlist = playlist;
      this.facetCount = facetCount;
    }

    Playlist getPlaylist() {
      return playlist;
    }

    int getFacetCount() {
      return facetCount;
    }

    @Override
    public String toString() {
      return facetCount == 0 ? playlist.getName()
          : playlist.getName() + " [" + facetCount + "]";
    }

    @Override
    public int compareTo(PlaylistLabel other) {
      // display the tags in reverse sort order
      return -playlist.getName().compareToIgnoreCase(other.getPlaylist().getName());
    }
  }
}
