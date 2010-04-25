package org.literacybridge.acm.utils;

import java.io.DataInput;
import java.io.IOException;

public class IOUtils {
	public static long readLittleEndianLong(DataInput in) throws IOException {
	    return ((in.readByte() & 0xFF)) | ((in.readByte() & 0xFF) << 8)
        | ((in.readByte() & 0xFF) <<  16) |  (in.readByte() & 0xFF << 24)
        | ((in.readByte() & 0xFF) <<  32) |  (in.readByte() & 0xFF << 40)
        | ((in.readByte() & 0xFF) <<  48) |  (in.readByte() & 0xFF << 56);
	}

	public static int readLittleEndianInt(DataInput in) throws IOException {
	    return ((in.readByte() & 0xFF)) | ((in.readByte() & 0xFF) << 8)
        | ((in.readByte() & 0xFF) <<  16) |  (in.readByte() & 0xFF << 24);
	}

	public static int readBigEndianInt(DataInput in) throws IOException {
	    return ((in.readByte() & 0xFF) << 24) | ((in.readByte() & 0xFF) << 16)
        | ((in.readByte() & 0xFF) <<  8) |  (in.readByte() & 0xFF);
	}

	
}
