package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class MetadataSerializer {
	public abstract void serialize(Metadata metadata, DataOutput out) throws IOException;
	public abstract Metadata deserialize(DataInput in) throws IOException;
}
