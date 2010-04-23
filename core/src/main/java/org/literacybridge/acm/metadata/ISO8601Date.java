package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ISO8601Date {
	private long date;

	// TODO: it should be possible to pass-in a String that gets converted
	public ISO8601Date(long date) {
		this.date = date;
	}
	
	public static MetadataValue<ISO8601Date> deserialize(DataInput in) throws IOException {
		long value = in.readLong();
		return new MetadataValue<ISO8601Date>(new ISO8601Date(value));
	}
	
	public static void serialize(DataOutput out, MetadataValue<ISO8601Date> value) throws IOException {
		out.writeLong(value.getValue().date);
	}

}
