package org.literacybridge.acm.gui.ResourceView;

import org.literacybridge.acm.gui.util.SortedListModel;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;

public class TagsListModel extends SortedListModel<TagsListModel.TagLabel> {
  public TagsListModel(Iterable<Playlist> playlists,
      SearchResult searchResult) {
    for (Playlist playlist : playlists) {
      add(new TagLabel(playlist, searchResult.getFacetCount(playlist)));
    }
  }

  public static final class TagLabel implements Comparable<TagLabel> {
    private final Playlist tag;
    private final int facetCount;

    private TagLabel(Playlist tag, int facetCount) {
      this.tag = tag;
      this.facetCount = facetCount;
    }

    public Playlist getTag() {
      return tag;
    }

    public int getFacetCount() {
      return facetCount;
    }

    @Override
    public String toString() {
      return facetCount == 0 ? tag.getName()
          : tag.getName() + " [" + facetCount + "]";
    }

    @Override
    public int compareTo(TagLabel other) {
      // display the tags in reverse sort order
      return -tag.getName().compareToIgnoreCase(other.getTag().getName());
    }
  }
}
