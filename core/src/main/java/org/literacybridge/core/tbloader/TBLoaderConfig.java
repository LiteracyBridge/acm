package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.TbFile;

import java.util.ArrayList;
import java.util.List;

public class TBLoaderConfig {
    // this device is the computer/tablet/phone that is running the TB Loader
    private final String tbLoaderId;

    // A TbFile representing where the TB-Loader will copy its output (stats, signin recordings), like ~/Dropbox/tbcd000c/collected-data
    private final TbFile collectedDataDirectory;

    // A "TBFileSystem" object representing the root of a temporary gathering point.
    private final TbFile tempDirectory;

    // A directory that can contain dos utilities. (In particular, RemoveDrive.exe.)
    private final TbFile windowsUtilsDirectory;

    // The user email, if known.
    private final String userEmail;

    private TBLoaderConfig(Builder builder) {
        this.tbLoaderId = builder.tbLoaderId;
        this.collectedDataDirectory = builder.collectedDataDirectory;
        this.tempDirectory = builder.tempDirectory;
        this.windowsUtilsDirectory = builder.windowsUtilsDirectory;
        this.userEmail = builder.userEmail;
    }

    public String getTbLoaderId() {
        return tbLoaderId;
    }

    public TbFile getTempDirectory() {
        return tempDirectory;
    }

    public TbFile getCollectedDataDirectory() {
        return collectedDataDirectory;
    }

    public TbFile getWindowsUtilsDirectory() {
        return windowsUtilsDirectory;
    }

    String getUserEmail() {
        return userEmail;
    }

    public static class Builder {
        private String tbLoaderId;
        private TbFile collectedDataDirectory;
        private TbFile tempDirectory;
        private TbFile windowsUtilsDirectory = null;
        private String userEmail = null;

        public final TBLoaderConfig build() {
            List<String> missing = new ArrayList<>();

            if (tbLoaderId == null)
                missing.add("tbLoaderId");
            if (collectedDataDirectory == null)
                missing.add("collected-data directory");
            if (tempDirectory == null)
                missing.add("tempDirectory");
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                    "TBLoaderConfig not initialized with " + missing.toString());
            }
            return new TBLoaderConfig(this);
        }

        public Builder withTbLoaderId(String tbLoaderId) {
            this.tbLoaderId = tbLoaderId;
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

        public Builder withUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }
    }
}
