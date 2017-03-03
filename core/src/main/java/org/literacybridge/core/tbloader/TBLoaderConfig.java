package org.literacybridge.core.tbloader;


import org.literacybridge.core.fs.TbFile;

import java.util.ArrayList;
import java.util.List;

public class TBLoaderConfig {
  // this device is the computer/tablet/phone that is running the TB Loader
  private final String tbLoaderId;
  // The project for which TBs are being configured
  private final String project;
  // "A-" or "B-"; old-style or new-style talking books.
  private final String srnPrefix;

  // A TbFile representing where the TB-Loader will copy its output (stats, signin recordings), like ~/Dropbox/tbcd000c/collected-data
  private final TbFile collectedDataDirectory;

  // A "TBFileSystem" object representing the root of a temporary gathering point.
  private final TbFile tempDirectory;

  // A directory that can contain dos utilities. (In particular, RemoveDrive.exe.)
  private final TbFile windowsUtilsDirectory;

  private TBLoaderConfig(String tbLoaderId, String project, String srnPrefix,
                         TbFile collectedDataDirectory, TbFile tempDirectory,
                         TbFile windowsUtilsDirectory) {
    this.tbLoaderId = tbLoaderId;
    this.project = project;
    this.srnPrefix = srnPrefix;
    this.collectedDataDirectory = collectedDataDirectory;
    this.tempDirectory = tempDirectory;
    this.windowsUtilsDirectory = windowsUtilsDirectory;
  }

  public String getTbLoaderId() {
    return tbLoaderId;
  }

  public String getProject() {
    return project;
  }

  public String getSrnPrefix() {
    return srnPrefix;
  }

  TbFile getTempDirectory() {
    return tempDirectory;
  }

  TbFile getCollectedDataDirectory() {
    return collectedDataDirectory;
  }

  TbFile getWindowsUtilsDirectory() {
    return windowsUtilsDirectory;
  }

  public static class Builder {
    private String tbLoaderId;
    private String project;
    private String srnPrefix;
    private TbFile collectedDataDirectory;
    private TbFile tempDirectory;
    private TbFile windowsUtilsDirectory = null;

    public final TBLoaderConfig build() {
      List<String> missing = new ArrayList<>();

      if (tbLoaderId == null) missing.add("tbLoaderId");
      if (project == null) missing.add("project");
      if (srnPrefix == null) missing.add("srnPrefix");
      if (collectedDataDirectory == null) missing.add("collected-data directory");
      if (tempDirectory == null) missing.add("tempDirectory");
      if (!missing.isEmpty()) {
        throw new IllegalStateException("TBLoaderConfig not initialized with " + missing.toString());
      }
      return new TBLoaderConfig(tbLoaderId, project, srnPrefix,
              collectedDataDirectory, tempDirectory, windowsUtilsDirectory);
    }

    public Builder withTbLoaderId(String tbLoaderId) {
      this.tbLoaderId = tbLoaderId;
      return this;
    }

    public Builder withProject(String project) {
      this.project = project;
      return this;
    }

    public Builder withSrnPrefix(String srnPrefix) {
      this.srnPrefix = srnPrefix;
      return this;
    }

    public Builder withCollectedDataDirectory(TbFile collectedDataDirectory) {
      this.collectedDataDirectory = collectedDataDirectory;
      return this;
    }

    public Builder withTempDirectory(TbFile tempDirectory) {
      this.tempDirectory = tempDirectory;
      return this;
    }

    public Builder withWindowsUtilsDirectory(TbFile windowsUtilsDirectory) {
      this.windowsUtilsDirectory = windowsUtilsDirectory;
      return this;
    }
  }
}
