package org.literacybridge.acm.utils;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Predicate;
import org.apache.commons.io.FilenameUtils;

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
      final int code = s.charAt(i);

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
          int utf32 = s.charAt(i + 1);
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
   * Convert UTF8 bytes into UTF16 characters. If offset is non-zero, conversion
   * starts at that starting point in utf8, re-using the results from the
   * previous call up until offset.
   */
  public static String readUTF8(DataInput in) throws IOException {
    int length = readLittleEndian16(in);

    char[] out = new char[length];

    int outUpto = 0, upto = 0;

    while (upto < length) {

      final int b = in.readByte() & 0xff;
      upto++;
      final int ch;

      if (b < 0xc0) {
        assert b < 0x80;
        ch = b;
      } else if (b < 0xe0) {
        ch = ((b & 0x1f) << 6) + (in.readByte() & 0x3f);
        upto++;
      } else if (b < 0xf0) {
        ch = ((b & 0xf) << 12) + ((in.readByte() & 0x3f) << 6)
            + (in.readByte() & 0x3f);
        upto += 2;
      } else {
        assert b < 0xf8;
        ch = ((b & 0x7) << 18) + ((in.readByte() & 0x3f) << 12)
            + ((in.readByte() & 0x3f) << 6) + (in.readByte() & 0x3f);
        upto += 3;
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

  public static String getFileExtension(File file) {
    return FilenameUtils.getExtension(file.getName());
  }

  public static void copy(File fromFile, File toFile, boolean keepLastModified)
      throws IOException {
    copy(fromFile, toFile, fromFile.length());
    if (keepLastModified) {
      toFile.setLastModified(fromFile.lastModified());
    }
  }

  public static void copy(File fromFile, File toFile) throws IOException {
    copy(fromFile, toFile, fromFile.length());
  }

  public static void copy(File fromFile, File toFile, long numBytes)
      throws IOException {
    FileInputStream from = null;
    FileOutputStream to = null;
    try {
      from = new FileInputStream(fromFile);
      to = new FileOutputStream(toFile);
      byte[] buffer = new byte[4096];

      while (true) {
        int numToRead = buffer.length;
        if (numToRead > numBytes) {
          numToRead = (int) numBytes;
        }
        from.read(buffer, 0, numToRead);
        to.write(buffer, 0, numToRead); // write
        numBytes -= numToRead;
        if (numBytes <= 0) {
          break;
        }
      }
    } finally {
      if (from != null)
        try {
          from.close();
        } catch (IOException e) {
          // ignore
        } finally {
          if (to != null)
            try {
              to.close();
            } catch (IOException e) {
              // ignore
            }
        }
    }
  }

  public static void visitFiles(final File root,
      final FilenameFilter fileNameFilter, final Predicate<File> predicate) {
    File[] files = root.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory()
            || fileNameFilter.accept(root, pathname.getName());
      }
    });

    for (File file : files) {
      if (file.isDirectory()) {
        visitFiles(file, fileNameFilter, predicate);
      } else {
        predicate.apply(file);
      }
    }
  }

  public static void deleteRecursive(File path) {
    if (path.isDirectory()) {
      File[] files = path.listFiles();
      for (File f : files) {
        deleteRecursive(f);
      }
    }
    path.delete();
  }

  /**
   * Opens a file by name, but ignores the case of the spelling on case-
   * sensitive file systems.
   *
   * @param parentDirectory
   *          A File representing a directory that may contain a child file or
   *          directory.
   * @param child
   *          The name of a possible child file or directory, but not
   *          necessarily with the correct casing, for instance,
   *          TalkingBookData vs talkingbookdata.
   * @return A File representing the child.
   */
  public static final File FileIgnoreCase(File parentDirectory, final String child) {
    File retval = new File(parentDirectory, child);
    // If the file doesn't exist as-cased, search for a spelling that does match.
    if (!retval.exists()) {
      File[] candidates = parentDirectory.listFiles(new FilenameFilter() {
        boolean found = false;

        @Override
        public boolean accept(File dir, String name) {
          // Accept the first file that matches case insenstively.
          if (!found && name.equalsIgnoreCase(child)) {
            found = true;
            return true;
          }
          return false;
        }
      });
      // If candidates contains a file, we know it exists, so use it.
      if (candidates != null && candidates.length == 1) {
        retval = candidates[0];
      }
    }
    return retval;
  }

    /**
     * Given a file, create the parent directories if they don't already exist.
     * @param file whose parent directory should be created.
     */
    public static void ensureDirectoryExists(File file) {
      File parent = file.getParentFile();
      if (!parent.exists()) {
          parent.mkdirs();
      }
    }

    /**
     * Reads a file, line by line. '#' introduces a comment. comments & whitespace are trimmed.
     * Places the lines into a collection, so this is really good for includelists, excludelists, etc.
     * @param file File of lines
     * @param lines Place lines into this collection.
     * @throws IOException if there is an error reading the file.
     */
    public static void readLines(File file, Collection<String> lines) throws IOException {
        //read file into stream, try-with-resources
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("#");
                if (parts==null || parts.length<1) continue;
                line = parts[0].trim();
                if (line.length() < 1) continue;
                lines.add(line);
            }
        }
    }




}
