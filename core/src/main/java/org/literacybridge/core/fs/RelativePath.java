package org.literacybridge.core.fs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class RelativePath {
  public static final RelativePath EMPTY = new RelativePath();

  private final List<String> pathSegments;

  public RelativePath(String... pathSegments) {
    this.pathSegments = new ArrayList<String>();
    if (pathSegments != null) {
      addPathSegments(pathSegments);
    }
  }

  public RelativePath(List<String> pathSegments) {
    this(pathSegments, 0, pathSegments.size());
  }

  public RelativePath(RelativePath parent, String... pathSegments) {
    this(parent.pathSegments, 0, parent.pathSegments.size());
    addPathSegments(pathSegments);
  }

  private RelativePath(List<String> pathSegments, int offset, int length) {
    this.pathSegments = new ArrayList<String>();
    for (int i = 0; i < length; i++) {
      this.pathSegments.add(pathSegments.get(i + offset));
    }
  }

  private void addPathSegments(String... segments) {
    for (String segment : segments) {
      this.pathSegments.add(segment);
    }
  }

  private void addPathSegments(List<String> segments) {
    this.pathSegments.addAll(segments);
  }

  private void addRelativePath(RelativePath relativePath) {
    addPathSegments(relativePath.pathSegments);
  }

  public String getLastSegment() {
    return pathSegments.size() > 0 ? pathSegments.get(pathSegments.size() - 1) : "";
  }

  public List<String> getSegments() {
    return Collections.unmodifiableList(this.pathSegments);
  }

  public RelativePath getParent() {
    return pathSegments.size() > 1 ? new RelativePath(this.pathSegments, 0, this.pathSegments.size() - 1) : RelativePath.EMPTY;
  }

  public static RelativePath parse(String relativePath) {
    List<String> segments = new ArrayList<String>();
    StringTokenizer tokenizer = new StringTokenizer(relativePath, File.separator);
    while (tokenizer.hasMoreTokens()) {
      segments.add(tokenizer.nextToken());
    }
    return new RelativePath(segments, 0, segments.size());
  }

  public static RelativePath getRelativePath(String root, String absolutePath) {
    if (!absolutePath.startsWith(root)) {
      return null;
    }

    return RelativePath.parse(absolutePath.substring(root.length()));
  }

  public static File resolve(final File root, RelativePath relativePath) {
    File file = root;
    for (String s : relativePath.getSegments()) {
      file = new File(file, s);
    }
    return file;
  }


  public String asString() {
    StringBuilder builder = new StringBuilder();
    for (String segment : pathSegments) {
      builder.append(segment);
      builder.append(File.separatorChar);
    }
    if (builder.length() > 0) {
      // strip last separatorChar
      builder.setLength(builder.length() - 1);
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    return asString();
  }

  @Override
  public int hashCode() {
    int code = 37;
    for (String segment : pathSegments) {
      code = 37 * code + segment.hashCode();
    }

    return code;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof RelativePath)) {
      return false;
    }

    RelativePath otherPath = (RelativePath) other;
    if (pathSegments.size() != otherPath.pathSegments.size()) {
      return false;
    }

    for (int i = 0; i < pathSegments.size(); i++) {
      if (!pathSegments.get(i).equals(otherPath.pathSegments.get(i))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public RelativePath clone() {
    // this constructor creates a new ArrayList
    return new RelativePath(this.pathSegments);
  }

  public static RelativePath concat(RelativePath... paths) {
    RelativePath path = new RelativePath();
    for (RelativePath p : paths) {
      path.addRelativePath(p);
    }
    return path;
  }
}
