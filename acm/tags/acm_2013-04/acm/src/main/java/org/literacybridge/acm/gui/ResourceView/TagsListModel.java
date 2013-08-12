package org.literacybridge.acm.gui.ResourceView;

import java.util.List;

import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.gui.util.SortedListModel;

public class TagsListModel extends SortedListModel<TagsListModel.TagLabel> {
	public TagsListModel(List<PersistentTag> allTags) {
		for (PersistentTag tag : allTags) {
			add(new TagLabel(tag));
		}
	}

	public static final class TagLabel implements Comparable<TagLabel> {
		private PersistentTag tag;
		
		private TagLabel(PersistentTag tag) {
			this.tag = tag;
		}
		
		public PersistentTag getTag() {
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
