package org.literacybridge.acm.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

public abstract class MetadataSerializer {
    public abstract void serialize(Collection<Category> categories, Metadata metadata, DataOutput out) throws IOException;
    public abstract void deserialize(Metadata metadata, Collection<Category> categories, DataInput in) throws IOException;
}
