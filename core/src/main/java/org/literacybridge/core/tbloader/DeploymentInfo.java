package org.literacybridge.core.tbloader;

/**
 * A class to hold information about a deployment on a Talking Book.
 */
public class DeploymentInfo {
    // The Talking Book's "serial number".  This, of course, has nothing to do with a given
    // deployment, but it is convenient to have it with the rest of the data.
    private final String serialNumber;
    // The name of the project previously deployed to the Talking Book. This may not be knowable
    // from the Talking Book, in which case the *new* deployment's project may be substituted.
    private final String projectName;
    // The name of the content update previously deployed to the Talking Book.
    private final String deploymentName;
    // The name of the content package previously deployed to the Talking Book.
    private final String packageName;

    // A string with the directory where the statistics were written last time. It is really only useful because,
    // as a side effect of how that name was generated, it is possible to get the date and time of the last update.
    // Yes, simply storing that date and time would have been better than this.
    private final String updateDirectory;

    // A string with a date of the update.
    private final String updateTimestamp;
    // The firmware version currently on the Talking Book.
    private final String firmwareRevision;
    // The name of the community in which the Talking Book was last deployed.
    private final String community;

    public DeploymentInfo(String serialNumber, String projectName, String deploymentName,
                          String packageName, String updateDirectory, String updateTimestamp, String firmwareRevision,
                          String community) {
        this.serialNumber = serialNumber;
        this.projectName = projectName;
        this.deploymentName = deploymentName;
        this.packageName = packageName;
        this.updateDirectory = updateDirectory;
        this.updateTimestamp = updateTimestamp;
        this.firmwareRevision = firmwareRevision;
        this.community = community;
    }

    /**
     * If there is no DeploymentInfo, all these values are unknown.
     */
    public static final String U = "UNKNOWN";

    public DeploymentInfo() {
        this(U, U, U, U, U, U, U, U);
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getProjectName() { return projectName; }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getUpdateDirectory() {
        return updateDirectory;
    }

    public String getUpdateTimestamp() {
        return updateTimestamp;
    }

    public String getFirmwareRevision() {
        return firmwareRevision;
    }

    public String getCommunity() {
        return community;
    }

    @Override
    public String toString() {
        return "Serial number: " + serialNumber
                + "\nProject name: " + projectName
                + "\nDeployment name: " + deploymentName
                + "\nPackage name: " + packageName
                + "\nupdated timestamp: " + updateTimestamp
                + "\nFirmware revision: " + firmwareRevision
                + "\nCommunity: " + community;
    }
}
