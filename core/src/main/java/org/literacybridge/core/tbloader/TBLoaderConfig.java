package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;

public class TBLoaderConfig {
  // this device is the computer/tablet/phone that is running the TB Loader
  private final String deviceID;
  private final String project;
  private final String srnPrefix;
  private final String homePath;
  private final TBFileSystem dropboxFS;
  private final RelativePath collectedDataPath;
  private final TBFileSystem tempFS;

  private TBLoaderConfig(String deviceID, String project, String srnPrefix, String homePath,
      TBFileSystem dropboxFS, RelativePath collectedDataPath, TBFileSystem tempFS) {
    this.deviceID = deviceID;
    this.project = project;
    this.srnPrefix = srnPrefix;
    this.homePath = homePath;
    this.dropboxFS = dropboxFS;
    this.collectedDataPath = collectedDataPath;
    this.tempFS = tempFS;
  }

  public String getDeviceID() {
    return deviceID;
  }

  public String getProject() {
    return project;
  }

  public String getSrnPrefix() {
    return srnPrefix;
  }

  public String getHomePath() {
    return homePath;
  }

  public TBFileSystem getDropboxFileSystem() {
    return dropboxFS;
  }

  public TBFileSystem getTempFileSystem() {
    return tempFS;
  }

  public RelativePath getCollectedDataPath() {
    return collectedDataPath;
  }

  public static class Builder {
    private String deviceID;
    private String project;
    private String srnPrefix;
    private String homePath;
    private TBFileSystem dropboxFS;
    private RelativePath collectedDataPath;
    private TBFileSystem tempFS;

    public final TBLoaderConfig build() {
      return new TBLoaderConfig(deviceID, project, srnPrefix, homePath, dropboxFS, collectedDataPath, tempFS);
    }

    public Builder withDeviceID(String deviceID) {
      this.deviceID = deviceID;
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

    public Builder withHomePath(String homePath) {
      this.homePath = homePath;
      return this;
    }

    public Builder withDropbox(TBFileSystem dropboxFS, RelativePath collectedDataPath) {
      this.dropboxFS = dropboxFS;
      this.collectedDataPath = collectedDataPath;
      return this;
    }

    public Builder withTempFileSystem(TBFileSystem tempFS) {
      this.tempFS = tempFS;
      return this;
    }
  }
}
