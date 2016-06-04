package org.literacybridge.core;

public abstract class ProgressListener {
  public abstract void updateProgress(int progressPercent, String progressUpdate);
  public abstract void addDetail(String detail);
}
