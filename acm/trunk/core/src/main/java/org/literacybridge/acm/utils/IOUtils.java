package org.literacybridge.acm.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class IOUtils {
	public static final int UNI_SUR_HIGH_START = 0xD800;
	public static final int UNI_SUR_HIGH_END = 0xDBFF;
	public static final int UNI_SUR_LOW_START = 0xDC00;
	public static final int UNI_SUR_LOW_END = 0xDFFF;
	public static final int UNI_REPLACEMENT_CHAR = 0xFFFD;

	private static final long UNI_MAX_BMP = 0x0000FFFF;

	private static final int HALF_BASE = 0x0010000;
	private static final long HALF_SHIFT = 10;
	private static final long HALF_MASK = 0x3FFL;

	public static long readLittleEndian64(DataInput in) throws IOException {
		return ((in.readByte() & 0xFF)) | ((in.readByte() & 0xFF) << 8)
				| ((in.readByte() & 0xFF) << 16) | (in.readByte() & 0xFF << 24)
				| ((in.readByte() & 0xFF) << 32) | (in.readByte() & 0xFF << 40)
				| ((in.readByte() & 0xFF) << 48) | (in.readByte() & 0xFF << 56);
	}

	public static int readLittleEndian32(DataInput in) throws IOException {
		return ((in.readByte() & 0xFF)) | ((in.readByte() & 0xFF) << 8)
				| ((in.readByte() & 0xFF) << 16) | (in.readByte() & 0xFF << 24);
	}

	public static short readLittleEndian16(DataInput in) throws IOException {
		return (short) (((in.readByte() & 0xFF)) | ((in.readByte() & 0xFF) << 8));
	}

	public static void writeLittleEndian64(DataOutput out, long value)
			throws IOException {
		out.writeByte((byte) (value));
		out.writeByte((byte) (value >> 8));
		out.writeByte((byte) (value >> 16));
		out.writeByte((byte) (value >> 24));
		out.writeByte((byte) (value >> 32));
		out.writeByte((byte) (value >> 40));
		out.writeByte((byte) (value >> 48));
		out.writeByte((byte) (value >> 56));
	}

	public static void writeLittleEndian32(DataOutput out, long value)
			throws IOException {
		out.writeByte((byte) (value));
		out.writeByte((byte) (value >> 8));
		out.writeByte((byte) (value >> 16));
		out.writeByte((byte) (value >> 24));
	}

	public static void writeLittleEndian16(DataOutput out, long value)
			throws IOException {
		out.writeByte((byte) (value));
		out.writeByte((byte) (value >> 8));
	}

	/**
	 * Encode characters from this String, starting at offset for length
	 * characters. Returns the number of bytes written to bytesOut.
	 */
	public static void writeAsUTF8(DataOutput out, CharSequence s)
			throws IOException {
		final int maxLen = s.length() * 4;
		byte[] bytes = new byte[maxLen];
		int bytesUpto = 0;

		for (int i = 0; i < s.length(); i++) {
			final int code = (int) s.charAt(i);

			if (code < 0x80) {
				bytes[bytesUpto++] = ((byte) code);
			} else if (code < 0x800) {
				bytes[bytesUpto++] = ((byte) (0xC0 | (code >> 6)));
				bytes[bytesUpto++] = ((byte) (0x80 | (code & 0x3F)));
			} else if (code < 0xD800 || code > 0xDFFF) {
				bytes[bytesUpto++] = ((byte) (0xE0 | (code >> 12)));
				bytes[bytesUpto++] = ((byte) (0x80 | ((code >> 6) & 0x3F)));
				bytes[bytesUpto++] = ((byte) (0x80 | (code & 0x3F)));
			} else {
				// surrogate pair
				// confirm valid high surrogate
				if (code < 0xDC00 && (i < s.length() - 1)) {
					int utf32 = (int) s.charAt(i + 1);
					// confirm valid low surrogate and write pair
					if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) {
						utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
						i++;
						bytes[bytesUpto++] = ((byte) (0xF0 | (utf32 >> 18)));
						bytes[bytesUpto++] = ((byte) (0x80 | ((utf32 >> 12) & 0x3F)));
						bytes[bytesUpto++] = ((byte) (0x80 | ((utf32 >> 6) & 0x3F)));
						bytes[bytesUpto++] = ((byte) (0x80 | (utf32 & 0x3F)));
						continue;
					}
				}
				// replace unpaired surrogate or out-of-order low surrogate
				// with substitution character
				bytes[bytesUpto++] = ((byte) 0xEF);
				bytes[bytesUpto++] = ((byte) 0xBF);
				bytes[bytesUpto++] = ((byte) 0xBD);
			}
		}

		writeLittleEndian16(out, bytesUpto);
		out.write(bytes, 0, bytesUpto);
	}

	/**
	 * Convert UTF8 bytes into UTF16 characters. If offset is non-zero,
	 * conversion starts at that starting point in utf8, re-using the results
	 * from the previous call up until offset.
	 */
	public static String readUTF8(DataInput in) throws IOException {
		int length = readLittleEndian16(in);

		char[] out = new char[length];

		int outUpto = 0, upto = 0;

		while (upto < length) {

			final int b = in.readByte() & 0xff;
			final int ch;

			if (b < 0xc0) {
				assert b < 0x80;
				ch = b;
			} else if (b < 0xe0) {
				ch = ((b & 0x1f) << 6) + (in.readByte() & 0x3f);
			} else if (b < 0xf0) {
				ch = ((b & 0xf) << 12) + ((in.readByte() & 0x3f) << 6)
						+ (in.readByte() & 0x3f);
			} else {
				assert b < 0xf8;
				ch = ((b & 0x7) << 18) + ((in.readByte() & 0x3f) << 12)
						+ ((in.readByte() & 0x3f) << 6)
						+ (in.readByte() & 0x3f);
			}

			if (ch <= UNI_MAX_BMP) {
				// target is a character <= 0xFFFF
				out[outUpto++] = (char) ch;
			} else {
				// target is a character in range 0xFFFF - 0x10FFFF
				final int chHalf = ch - HALF_BASE;
				out[outUpto++] = (char) ((chHalf >> HALF_SHIFT) + UNI_SUR_HIGH_START);
				out[outUpto++] = (char) ((chHalf & HALF_MASK) + UNI_SUR_LOW_START);
			}
		}

		return new String(out, 0, outUpto);
	}

}
