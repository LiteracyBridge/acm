package org.literacybridge.acm.gui.ResourceView;

import java.util.List;

import org.literacybridge.acm.db.Playlist;
import org.literacybridge.acm.gui.util.SortedListModel;

public class TagsListModel extends SortedListModel<TagsListModel.TagLabel> {
    public TagsListModel(List<Playlist> playlists) {
        for (Playlist playlist : playlists) {
            add(new TagLabel(playlist));
        }
    }

    public static final class TagLabel implements Comparable<TagLabel> {
        private Playlist tag;

        private TagLabel(Playlist tag) {
            this.tag = tag;
        }

        public Playlist getTag() {
            return tag;
        }

        @Override public String toString() {
            return tag.getName();
        }

        @Override public int compareTo(TagLabel other) {
            // display the tags in reverse sort order
            return -tag.getName().compareToIgnoreCase(other.getTag().getName());
        }
    }
}
