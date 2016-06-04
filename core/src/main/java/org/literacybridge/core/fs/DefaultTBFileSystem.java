package org.literacybridge.core.fs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DefaultTBFileSystem extends TBFileSystem {
  private final File root;

  private DefaultTBFileSystem(File root) {
    this.root = root;
  }

  public static DefaultTBFileSystem open(File root) {
    return new DefaultTBFileSystem(root);
  }

  @Override
  public boolean fileExists(RelativePath relativePath) throws IOException {
    return resolve(relativePath).exists();
  }

  @Override
  public void doCreateNewFile(RelativePath relativePath, InputStream content)
      throws IOException {
    File file = resolve(relativePath);
    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      copy(content, out);
    }
  }

  @Override
  public void doCreateNewDirectory(RelativePath relativePath)
      throws IOException {
    resolve(relativePath).mkdir();
  }

  @Override
  public void doDeleteFile(RelativePath relativePath) throws IOException {
    resolve(relativePath).delete();
  }

  @Override
  public InputStream openFileInputStream(RelativePath relativePath)
      throws IOException {
    return new FileInputStream(resolve(relativePath));
  }

  @Override
  public OutputStream openFileOutputStream(RelativePath relativePath)
      throws IOException {
    return new FileOutputStream(resolve(relativePath));
  }

  private File resolve(RelativePath relativePath) {
    return RelativePath.resolve(root, relativePath);
  }

  @Override
  public String getRootPath() {
    return root.getAbsolutePath();
  }

  @Override
  public String[] list(RelativePath relativePath) {
    return resolve(relativePath).list();
  }

  @Override
  public String[] list(RelativePath relativePath,
      final FilenameFilter filter) {
    return resolve(relativePath).list(new java.io.FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return filter.accept(DefaultTBFileSystem.this, RelativePath.getRelativePath(
            root.getAbsolutePath(), dir.getAbsolutePath()), name);
      }
    });
  }

  @Override
  public long fileLength(RelativePath relativePath) throws IOException {
    return resolve(relativePath).length();
  }

  @Override
  public boolean isDirectory(RelativePath relativePath) {
    return resolve(relativePath).isDirectory();
  }

  @Override
  protected void doDeleteDirectory(RelativePath relativePath)
      throws IOException {
    resolve(relativePath).delete();
  }

  @Override
  public boolean isHidden(RelativePath relativePath) {
    return resolve(relativePath).isHidden();
  }
}
