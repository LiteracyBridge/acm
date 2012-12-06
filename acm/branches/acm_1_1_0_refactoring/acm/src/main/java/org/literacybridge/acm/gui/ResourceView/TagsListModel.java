package org.literacybridge.acm.gui.ResourceView;

import java.util.List;

import javax.swing.AbstractListModel;

import org.literacybridge.acm.db.PersistentTag;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class TagsListModel extends AbstractListModel {
	private final List<TagLabel> tags;
	
	public TagsListModel(List<PersistentTag> allTags) {
		tags = Lists.transform(allTags, new Function<PersistentTag, TagLabel>() {
			@Override public TagLabel apply(PersistentTag tag) {
				return new TagLabel(tag);
			}
		});
	}

	@Override
	public Object getElementAt(int i) {
		return tags.get(i); 
	}

	@Override
	public int getSize() {
		return tags.size();
	}
	
	public static final class TagLabel {
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
	}
}
