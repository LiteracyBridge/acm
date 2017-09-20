package org.literacybridge.core.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class to provide common functionality between File and Android's DocumentFile.
 *
 */

public abstract class TbFile {
    private static final Logger LOG = Logger.getLogger("TbFile");

    /**
     * Flags for TbFile operations. Not all flags are appropriate for all methods, and not
     * all combinations are legal or sensible.
     */
    public enum Flags {
        nil,                // means nothing
        append,             // When opening a file for write, append, not overwrite
        recursive,          // Delete a directory and its contents
        contentRecursive    // Delete a directory's contents, but not the directory
    }

    public interface FilenameFilter {
        boolean accept(TbFile parent, String name);
    }

    private static final int COPY_BUFFER_SIZE = 65536; // 4096;
    private static final ThreadLocal<byte[]> copyBuffers = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            LOG.log(Level.INFO, "TBL!: Allocating a copy buffer");
            return new byte[COPY_BUFFER_SIZE];
        }
    };

    public void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = copyBuffers.get();
        int bytesRead = 0;
        while((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        out.close();
    }

    /**
     * We'd like 'new TbFile(parent, child)', but this'll have to do.
     * @param parent The parent file.
     * @param child Name of the child file.
     * @return
     */
    public static TbFile open(TbFile parent, String child) {
        return parent.open(child);
    }

    /**
     * Factory method to open a child.
     * @param child The child file. Does not necessarily need to exist.
     * @return The TbFile.
     */
    public abstract TbFile open(String child);
    public TbFile open(List<String> childPath) {
        if (childPath.size() == 0) return this;
        // Starting with this file, open the intermediates, and finally the end child.
        TbFile tmp = this;
        for (String child : childPath) {
            tmp = tmp.open(child);
        }
        return tmp;
    }

    public abstract TbFile getParent();

    public abstract String getName();

    public abstract String getAbsolutePath();

    public abstract void renameTo(String newName);

    public abstract boolean exists();

    public abstract boolean isDirectory();

    public abstract boolean mkdir();

    public abstract boolean mkdirs();

    public abstract void createNew(InputStream content, Flags... flags) throws IOException;

    public abstract boolean delete();

    public abstract long length();

    public abstract long lastModified();

    public abstract String[] list();

    public abstract String[] list(FilenameFilter filter);

    public TbFile[] listFiles() { return listFiles(null); };

    public abstract TbFile[] listFiles(final FilenameFilter filter);

    public abstract long getFreeSpace();

    public abstract InputStream openFileInputStream() throws IOException;

    // Is any flag from query in the array of flags?
    private static boolean hasFlag(Flags[] flags, Flags... query) {
        for (Flags q : query)
            for (Flags f : flags)
                if (f == q) return true;
        return false;
    }
    public int delete(Flags... flags) {
        int numDeleted = 0;
        if (!exists()) return numDeleted;
        if (isDirectory() && hasFlag(flags, Flags.recursive, Flags.contentRecursive)) {
            String [] children = list();
            for (String child : children) {
                TbFile f = open(child);
                numDeleted += f.delete(Flags.recursive);
            }
            if (hasFlag(flags, Flags.contentRecursive)) return numDeleted;
        }
        if (delete()) numDeleted++;
        return numDeleted;
    }

    public void deleteDirectory() {
        if (!exists()) return;
        if (isDirectory()) {
            String [] children = list();
            for (String child : children) {
                TbFile f = open(child);
                f.deleteDirectory();
            }
        }
        delete();
    }

    public interface CopyProgress {
        void copying(String filename);
    }

    public abstract static class CopyFilter {
        public abstract boolean accept(TbFile file);
    }

    public static long copy(TbFile src, TbFile dst) throws IOException {
        long bytesCopied = src.length();
//            LOG.log(Level.INFO, String.format("Copying file from %s to %s", src.getName(), dst.getName()));
        try (InputStream content = src.openFileInputStream() ) {
            dst.createNew(content);
        }
        return bytesCopied;
    }

    public static long copyDir(TbFile src, TbFile dst) throws IOException {
        return copyDir(src, dst, null, null);
    }

    public static long copyDir(TbFile src, TbFile dst, CopyFilter filter) throws IOException {
        return copyDir(src, dst, filter, null);
    }

    public static long copyDir(TbFile src, TbFile dst, CopyFilter filter, CopyProgress progress) throws IOException {
        long bytesCopied = 0;
        if (src.isDirectory()) {
            dst.mkdirs();
            String[] children = src.list();
            int numCopied = 0;
            long startTime = System.nanoTime();
//            LOG.log(Level.INFO, String.format("Copying directory (%d items) from %s to %s", children.length, src.getName(), dst.getName()));
            for (String child : children) {
                TbFile srcChild = src.open(child);
                if (filter == null || filter.accept(srcChild)) {
                    numCopied++;
                    bytesCopied += copyDir(srcChild, dst.open(child), filter, progress);
                }
            }
            double seconds = (System.nanoTime()-startTime)/1e9;
//            LOG.log(Level.INFO, String.format("Copied directory (%d items, %d bytes, %.0f B/s) in %.3fs from %s to %s",
//                    numCopied, bytesCopied, bytesCopied/seconds, seconds, src.getName(), dst.getName()));
        } else {
            bytesCopied = src.length();
            long startTime = System.nanoTime();
//            LOG.log(Level.INFO, String.format("Copying file from %s to %s", src.getName(), dst.getName()));
            if (progress != null) progress.copying(dst.getName());
            try (InputStream content = src.openFileInputStream() ) {
                dst.createNew(content);
            }
            double seconds = (System.nanoTime()-startTime)/1e9;
//            LOG.log(Level.INFO, String.format("Copied file (%d bytes, %.0f B/s) in %.3fs from %s to %s",
//                    bytesCopied, bytesCopied/seconds, seconds, src.getName(), dst.getName()));
        }
        return bytesCopied;
    }

}
