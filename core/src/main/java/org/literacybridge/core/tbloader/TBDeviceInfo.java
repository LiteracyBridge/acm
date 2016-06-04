package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.TBFileSystem;

public final class TBDeviceInfo {
  private final TBFileSystem fs;
  private final String label;
  private String serialNumber;
  private boolean corrupted;
  private final String datetime;
  private final String syncdir;

  public TBDeviceInfo(TBFileSystem fs, String label, String tbLoaderDeviceID) {
    this.fs = fs;
    this.label = label.trim();
    this.corrupted = false;
    this.serialNumber = "";
    this.datetime = TBLoaderUtils.getDateTime();
    this.syncdir = this.datetime + "-" + tbLoaderDeviceID;
  }

  public TBFileSystem getFileSystem() {
    return fs;
  }

  public String getLabel() {
    return label;
  }

  public String getLabelWithoutDriveLetter() {
    String labelNoDriveLetter = this.label;
    int index = labelNoDriveLetter.lastIndexOf('(');
    if (index > 0) {
      labelNoDriveLetter = labelNoDriveLetter.substring(0, index - 1);
    }
    return labelNoDriveLetter;
  }

  public void setCorrupted(boolean corrupted) {
    this.corrupted = corrupted;
  }

  public boolean isCorrupted() {
    return corrupted;
  }

  public String getDateTime() {
    return datetime;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public String getSyncDir() {
    return syncdir;
  }

  @Override
  public String toString() {
    if (label.isEmpty()) {
      return fs.toString();
    }
    return label;
  }
}
