package org.literacybridge.core.fs;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class TBFileSystem implements Cloneable {
  private static final Logger LOG = Logger.getLogger(TBFileSystem.class.getName());

  private static final int COPY_BUFFER_SIZE = 4096;

  private final ThreadLocal<byte[]> copyBuffers;

  public TBFileSystem() {
    this.copyBuffers = new ThreadLocal<byte[]>() {
      @Override
      protected byte[] initialValue() {
        return new byte[COPY_BUFFER_SIZE];
      }
    };
  }

  public void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = copyBuffers.get();
    int bytesRead = 0;
    while((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
    }

    out.flush();
  }

  public static void copy(TBFileSystem sourceFS, RelativePath sourcePath,
                          TBFileSystem targetFS, RelativePath targetPath) throws IOException {
    copy(sourceFS, sourcePath, targetFS, targetPath, null);
  }

  public static void copy(TBFileSystem sourceFS, RelativePath sourcePath,
                          TBFileSystem targetFS, RelativePath targetPath,
                          CopyFileProgressListener progressListener) throws IOException {
    copy(sourceFS, sourcePath, targetFS, targetPath, false, progressListener);
  }

  public static void copy(TBFileSystem sourceFS, RelativePath sourcePath,
                          TBFileSystem targetFS, RelativePath targetPath, boolean sync) throws IOException {
    copy(sourceFS, sourcePath, targetFS, targetPath, false, null);
  }

  public static void copy(TBFileSystem sourceFS, RelativePath sourcePath,
                          TBFileSystem targetFS, RelativePath targetPath, boolean sync,
                          CopyFileProgressListener progressListener) throws IOException {

    LOG.log(Level.INFO, "Copying from " + sourcePath + " to " + targetPath);
    if (sourceFS.fileExists(sourcePath) && targetFS.fileExists(targetPath)) {
      if (sync) {
        long sourceLength = sourceFS.fileLength(sourcePath);
        long targetLength = targetFS.fileLength(targetPath);
        if (sourceLength > 4096 && sourceLength == targetLength) {
          // compare first and last byte
          try (InputStream content1 = sourceFS.openFileInputStream(sourcePath);
               InputStream content2 = targetFS.openFileInputStream(targetPath)) {
            if (content1.read() == content2.read()) {
              long skip = sourceLength - 2;
              content1.skip(skip);
              content2.skip(skip);
              if (content1.read() == content2.read()) {
                return;
              }
            }
          }
        }
      }
      targetFS.deleteFile(targetPath);
    }

    InputStream content = sourceFS.openFileInputStream(sourcePath);
    if (progressListener != null) {
      long totalBytes = sourceFS.fileLength(sourcePath);
      content = new ProgressInputStream(content, totalBytes, progressListener);
    }
    targetFS.createNewFile(targetPath, content, false);
  }

  private static void gatherFiles(TBFileSystem fs, RelativePath path,
      FilenameFilter fileFilter, FilenameFilter dirFilter, Set<RelativePath> gathered) throws IOException {
    if (fs.isDirectory(path)) {
      if (dirFilter == null || dirFilter.accept(fs, path.getParent(), path.getLastSegment())) {
        String[] files = fs.list(path);
        for (String file : files) {
          gatherFiles(fs, new RelativePath(path, file), fileFilter, dirFilter, gathered);
        }
      }
    } else if (fileFilter == null || fileFilter.accept(fs, path.getParent(), path.getLastSegment())) {
      gathered.add(path);
    }

  }

  public static void copyDir(TBFileSystem sourceFS, RelativePath sourcePath,
      TBFileSystem targetFS, RelativePath targetPath, FilenameFilter fileFilter,
      FilenameFilter dirFilter) throws IOException {
    copyDir(sourceFS, sourcePath, targetFS, targetPath, fileFilter, dirFilter, false, true);
  }

  public static void copyDir(TBFileSystem sourceFS, RelativePath sourcePath,
                             TBFileSystem targetFS, RelativePath targetPath,
                             FilenameFilter fileFilter,
                             FilenameFilter dirFilter, boolean sync,
                             boolean doNotDeleteExtraFilesInDest) throws IOException {
    doCopyDir(TBFileSystem.getView(sourceFS, sourcePath), RelativePath.EMPTY,
            TBFileSystem.getView(targetFS, targetPath), RelativePath.EMPTY,
            fileFilter, dirFilter, sync, doNotDeleteExtraFilesInDest);
  }

  private static void doCopyDir(TBFileSystem sourceFS, RelativePath sourcePath,
                                TBFileSystem targetFS, RelativePath targetPath,
                                FilenameFilter fileFilter,
                                FilenameFilter dirFilter, boolean sync,
                                boolean doNotDeleteExtraFilesInDest) throws IOException {

    Set<RelativePath> filesToDelete = null;
    if (sync && !doNotDeleteExtraFilesInDest) {
      filesToDelete = new HashSet<>();
      gatherFiles(targetFS, targetPath, fileFilter, dirFilter, filesToDelete);
    }

    doCopyDir(sourceFS, sourcePath, targetFS, targetPath, fileFilter, dirFilter, sync, filesToDelete);

    if (!doNotDeleteExtraFilesInDest && sync && filesToDelete != null && !filesToDelete.isEmpty()) {
      IOException first = null;
      for (RelativePath fileToDelete : filesToDelete) {
        try {
          targetFS.deleteFile(fileToDelete);
        } catch (IOException e) {
          first = e;
        }
      }
      if (first != null) {
        throw first;
      }
    }
  }

  private static void doCopyDir(TBFileSystem sourceFS, RelativePath sourcePath,
      TBFileSystem targetFS, RelativePath targetPath,
      FilenameFilter fileFilter,
      FilenameFilter dirFilter, boolean sync, Set<RelativePath> filesToDelete) throws IOException {
    if (sourceFS.isDirectory(sourcePath)) {
      if (dirFilter == null || dirFilter.accept(sourceFS, sourcePath.getParent(), sourcePath.getLastSegment())) {
        targetFS.mkdirs(targetPath);
        String[] files = sourceFS.list(sourcePath);
        for (String file : files) {
          doCopyDir(sourceFS, new RelativePath(sourcePath, file), targetFS, new RelativePath(targetPath, file), fileFilter, dirFilter, sync, filesToDelete);
        }
      }
    } else if (fileFilter == null || fileFilter.accept(sourceFS, sourcePath.getParent(), sourcePath.getLastSegment())) {
      if (filesToDelete != null) {
        filesToDelete.remove(sourcePath);
      }
      copy(sourceFS, sourcePath, targetFS, targetPath, sync);
    }
  }

  public static void deleteRecursive(TBFileSystem sourceFS, RelativePath path) throws IOException {
    if (sourceFS.isDirectory(path)) {
      for (String file : sourceFS.list(path)) {
        deleteRecursive(sourceFS, new RelativePath(path, file));
      }
    } else {
      sourceFS.deleteFile(path);
    }
  }

  public abstract boolean isDirectory(RelativePath relativePath)
      throws IOException;

  public abstract boolean fileExists(RelativePath relativePath)
      throws IOException;

  public abstract long fileLength(RelativePath relativePath)
      throws IOException;

  public final void createNewFile(RelativePath relativePath, InputStream content, boolean overwrite)
      throws IOException {
    if (fileExists(relativePath)) {
      if (!overwrite) {
        throw new FileAlreadyExistsException(relativePath.toString());
      } else {
        deleteFile(relativePath);
      }
    }
    doCreateNewFile(relativePath, content);
  }

  public final void mkdir(RelativePath relativePath)
      throws IOException {
    if (fileExists(relativePath)) {
      throw new FileAlreadyExistsException(relativePath.asString());
    }
    doCreateNewDirectory(relativePath);
  }

  public final void mkdirs(RelativePath relativePath)
      throws IOException {
    RelativePath path = new RelativePath();
    for (String segment : relativePath.getSegments()) {
      path = new RelativePath(path, segment);
      if (!fileExists(path)) {
        doCreateNewDirectory(path);
      }
    }
  }

  public final void rmdir(RelativePath relativePath) throws IOException {
    if (!fileExists(relativePath)) {
      throw new FileNotFoundException(relativePath.asString());
    }

    if (!isDirectory(relativePath)) {
      throw new IOException(relativePath.asString() + " is not a directory.");
    }

    if (list(relativePath).length > 0) {
      throw new IOException(relativePath.asString() + " is not empty.");
    }

    doDeleteDirectory(relativePath);
  }

  public final void deleteFile(RelativePath relativePath) throws IOException {
    doDeleteFile(relativePath);
  }

  protected abstract void doCreateNewFile(RelativePath relativePath, InputStream content)
      throws IOException;

  protected abstract void doCreateNewDirectory(RelativePath relativePath)
      throws IOException;

  protected abstract void doDeleteFile(RelativePath relativePath)
      throws IOException;

  protected abstract void doDeleteDirectory(RelativePath relativePath)
      throws IOException;

  public abstract InputStream openFileInputStream(RelativePath relativePath)
      throws IOException;

  public abstract OutputStream openFileOutputStream(RelativePath relativePath)
      throws IOException;

  public abstract String[] list(RelativePath relativePath) throws IOException;

  public abstract String[] list(RelativePath relativePath,
      FilenameFilter filter) throws IOException;

  public abstract boolean isHidden(RelativePath relativePath)
      throws IOException;

  public abstract String getRootPath();

  @Override
  public final String toString() {
    return getRootPath();
  }

  public interface FilenameFilter {
    boolean accept(TBFileSystem fs, RelativePath dir, String name);
  }

  public static final FilenameFilter ACCEPT_ALL = new FilenameFilter() {
    @Override
    public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
      return true;
    }
  };

  public static TBFileSystem getView(TBFileSystem fs, RelativePath root) {
    return new View(fs, root);
  }

  public static class View extends TBFileSystem {
    private final TBFileSystem fs;
    private final RelativePath root;

    private View(TBFileSystem fs, RelativePath root) {
      this.fs = fs;
      this.root = root;
    }

    private RelativePath resolve(RelativePath relativePath) {
      return RelativePath.concat(root, relativePath);
    }

    @Override
    public boolean isDirectory(RelativePath relativePath) throws IOException {
      return fs.isDirectory(resolve(relativePath));
    }

    @Override
    public boolean fileExists(RelativePath relativePath) throws IOException {
      return fs.fileExists(resolve(relativePath));
    }

    @Override
    public long fileLength(RelativePath relativePath) throws IOException {
      return fs.fileLength(resolve(relativePath));
    }

    @Override
    protected void doCreateNewFile(RelativePath relativePath, InputStream content) throws IOException {
      fs.doCreateNewFile(resolve(relativePath), content);
    }

    @Override
    protected void doCreateNewDirectory(RelativePath relativePath) throws IOException {
      fs.doCreateNewDirectory(resolve(relativePath));
    }

    @Override
    protected void doDeleteFile(RelativePath relativePath) throws IOException {
      fs.doDeleteFile(resolve(relativePath));
    }

    @Override
    protected void doDeleteDirectory(RelativePath relativePath) throws IOException {
      fs.doDeleteDirectory(resolve(relativePath));
    }

    @Override
    public InputStream openFileInputStream(RelativePath relativePath) throws IOException {
      return fs.openFileInputStream(resolve(relativePath));
    }

    @Override
    public OutputStream openFileOutputStream(RelativePath relativePath) throws IOException {
      return fs.openFileOutputStream(resolve(relativePath));
    }

    @Override
    public String[] list(RelativePath relativePath) throws IOException {
      return fs.list(resolve(relativePath));
    }

    @Override
    public String[] list(RelativePath relativePath, FilenameFilter filter) throws IOException {
      return fs.list(resolve(relativePath), filter);
    }

    @Override
    public boolean isHidden(RelativePath relativePath) throws IOException {
      return fs.isHidden(resolve(relativePath));
    }

    @Override
    public String getRootPath() {
      return new RelativePath(fs.getRootPath(), root.asString()).asString();
    }
  }

  private static final class ProgressInputStream extends FilterInputStream {
    private final CopyFileProgressListener listener;
    private final long totalBytes;
    private long bytesRead;

    protected ProgressInputStream(InputStream in, long totalBytes,
                                  CopyFileProgressListener listener) {
      super(in);
      this.listener = listener;
      this.totalBytes = totalBytes;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
      int n = super.read(buffer, byteOffset, byteCount);
      bytesRead += n;
      listener.onProgressUpdate(bytesRead, totalBytes);
      return n;
    }
  }

  public interface CopyFileProgressListener {
    void onProgressUpdate(long bytesRead, long bytesTotal);
  }
}
