package org.literacybridge.core.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUnzip {
  File baseInDir;

    /**
     * Simple status of the unzip operation. Accumulates the uncompressed sizes of the files as
     * 'current', and the size of the .zip as 'total'. This is inaccurate, because it ignores
     * directories and the entries themselves. For many directories, or small files, it may be way
     * off, but for zips with large files, it is fairly accurate.
     */
  public interface UnzipListener {
        /**
         * Called with progress of the unzip operation.
         * @param current Bytes unzipped so far.
         * @param total Size of the zip file.
         * @return false if the operation should cancel; true to continue.
         */
        boolean progress(long current, long total);
  }

  private ZipUnzip(File baseDirectory) {
    baseInDir = baseDirectory;
  }

    private void addDirectory(ZipOutputStream zout, File fileSource,
        boolean includeBaseDir, boolean includeChildren) throws IOException {
        if (includeBaseDir || fileSource != baseInDir) {
            String relativeDirName = baseInDir.toURI().relativize(fileSource.toURI())
                .getPath();
            zout.putNextEntry(new ZipEntry(relativeDirName));
        }
        if (!includeChildren) {return;}
        File[] files = fileSource.listFiles();
        if (files == null) {
            // Nothing to do.
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                addDirectory(zout, file, false, true);
                continue;
            }
            try {
                String relativeFileName = baseInDir.toURI().relativize(file.toURI()).getPath();
                byte[] buffer = new byte[1024];
                FileInputStream fin = new FileInputStream(file);
                zout.putNextEntry(new ZipEntry(relativeFileName));
                int length;
                while ((length = fin.read(buffer)) > 0) {
                    zout.write(buffer, 0, length);
                }
                zout.closeEntry();
                fin.close();
            } catch (IOException ioe) {
                try {
                    System.out.println("IOException adding file in ZipUnzip: " + file.getAbsolutePath());
                } catch (Exception ex) {
                    // Ignore any exception printing exception message.
                    System.out.println("IOException in ZipUnzip.addDirectory: " + ioe.getMessage());
                }
                throw ioe;
            }
        }
    }

  public static void zip(File inDir, File outFile) throws IOException {
    zip(inDir, outFile, false);
  }

  public static void zip(File inDir, File outFile, boolean includeBaseDir)
      throws IOException {
    ZipUnzip zipper;
    if (includeBaseDir) {
      zipper = new ZipUnzip(inDir.getParentFile());
    } else {
      zipper = new ZipUnzip(inDir);
    }
    outFile.delete();
    outFile.getParentFile().mkdirs();
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outFile));
    zipper.addDirectory(zout, inDir, includeBaseDir, true);
    zout.close();
    // System.out.println("Zip file has been created!");
  }

  public static void zip(File inDir, File outFile, boolean includeBaseDir,
      String[] subdirs) throws IOException {
    ZipUnzip zipper;
    if (includeBaseDir) {
      zipper = new ZipUnzip(inDir.getParentFile());
    } else {
      zipper = new ZipUnzip(inDir);
    }
    outFile.delete();
    outFile.getParentFile().mkdirs();
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outFile));
    zipper.addDirectory(zout, inDir, includeBaseDir, false);
    for (String dir : subdirs) {
      File f = new File(inDir, dir);
      if (f.exists()) {
        zipper.addDirectory(zout, f, false, true);
      }
    }
    zout.close();
    // System.out.println("Zip file has been created!");
  }

  public static void unzip(File inFile, File outDir) throws IOException {
    unzip(inFile, outDir, null);
  }

    /**
     * Unzip the given file into the given directory. If a listener is provided,
     * call it with status updates.
     * @param inFile The .zip file.
     * @param outDir Where to unzip the files.
     * @param listener Optional callback for status.
     * @throws IOException if can't write to a file.
     */
    public static void unzip(File inFile, File outDir, UnzipListener listener) throws IOException {
        File parentDir;
        long current = 0;
        long total = inFile.length();
        boolean cancelled = false;

        parentDir = outDir;
        try (ZipFile zfile = new ZipFile(inFile)) {
            Enumeration<? extends ZipEntry> entries = zfile.entries();
            if (listener != null) {
                listener.progress(current, total);
            }
            byte[] buffer = new byte[1024];
            String expectedFilePrefix = outDir.getCanonicalPath();
            while (entries.hasMoreElements() && !cancelled) {
                ZipEntry entry = entries.nextElement();
                File file = new File(parentDir, entry.getName());
                String canonicalPath = file.getCanonicalPath();
                if (!canonicalPath.startsWith(expectedFilePrefix)) {
                    throw new IOException("Unexpected path contained in zip file");
                }
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (InputStream in = zfile.getInputStream(entry); OutputStream out = new FileOutputStream(file)) {
                        while (true) {
                            int readCount = in.read(buffer);
                            if (readCount < 0) {
                                break;
                            }
                            out.write(buffer, 0, readCount);
                        }
                    }
                }
                if (listener != null) {
                    current += entry.getCompressedSize();
                    cancelled = !listener.progress(current, total);
                }
            }
        }
    }

}
