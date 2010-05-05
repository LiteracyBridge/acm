package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.literacybridge.acm.categories.Taxonomy.Category;

public abstract class MetadataSerializer {
	public abstract void serialize(Set<Category> categories, Metadata metadata, DataOutput out) throws IOException;
	public abstract Metadata deserialize(Set<Category> categories, DataInput in) throws IOException;
}
