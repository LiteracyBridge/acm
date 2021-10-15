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

    // A class implementing various dos utilities. (In particular, RemoveDrive.exe.)
    private final FileSystemUtilities fileSystemUtilities;

    // The user email, if known.
    private final String userEmail;

    // The name by which the user is addressed, if known.
    private final String userName;

    private TBLoaderConfig(Builder builder) {
        this.tbLoaderId = builder.tbLoaderId;
        this.collectedDataDirectory = builder.collectedDataDirectory;
        this.tempDirectory = builder.tempDirectory;
        this.fileSystemUtilities = builder.fileSystemUtilities;
        this.userEmail = builder.userEmail;
        this.userName = builder.userName;
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

    public FileSystemUtilities getCommandLineUtils() {
        return fileSystemUtilities;
    }

    String getUserEmail() {
        return userEmail;
    }

    String getUserName() {
        return userName!=null ? userName : userEmail;
    }

    public static class Builder {
        private String tbLoaderId;
        private TbFile collectedDataDirectory;
        private TbFile tempDirectory;
        private FileSystemUtilities fileSystemUtilities;
        private String userEmail = null;
        private String userName = null;

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
            if (fileSystemUtilities == null)
                fileSystemUtilities = new FileSystemUtilities();
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

        public Builder withFileSystemUtilities(FileSystemUtilities fileSystemUtilities) {
            this.fileSystemUtilities = fileSystemUtilities;
            return this;
        }

        public Builder withUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }
    }
}
