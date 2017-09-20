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

    // Was (or will) the deployment for testing purposes.
    private final boolean testDeployment;

    public static class DeploymentInfoBuilder {
        private static final String UNKNOWN = "UNKNOWN";
        private String serialNumber = UNKNOWN;
        private String projectName = UNKNOWN;
        private String deploymentName = UNKNOWN;
        private String packageName = UNKNOWN;
        private String updateDirectory = null;
        private String updateTimestamp = null;
        private String firmwareRevision = UNKNOWN;
        private String community = UNKNOWN;
        private boolean testDeployment = false;

        public DeploymentInfoBuilder withSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public DeploymentInfoBuilder withProjectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public DeploymentInfoBuilder withDeploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public DeploymentInfoBuilder withPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public DeploymentInfoBuilder withUpdateDirectory(String updateDirectory) {
            this.updateDirectory = updateDirectory;
            return this;
        }

        public DeploymentInfoBuilder withUpdateTimestamp(String updateTimestamp) {
            this.updateTimestamp = updateTimestamp;
            return this;
        }

        public DeploymentInfoBuilder withFirmwareRevision(String firmwareRevision) {
            this.firmwareRevision = firmwareRevision;
            return this;
        }

        public DeploymentInfoBuilder withCommunity(String community) {
            this.community = community;
            return this;
        }

        public DeploymentInfoBuilder asTestDeployment(boolean testDeployment) {
            this.testDeployment = testDeployment;
            return this;
        }

        // Build from an existing DeploymentInfo
        public DeploymentInfoBuilder fromDeploymentInfo(DeploymentInfo di) {
            serialNumber = di.serialNumber;
            projectName = di.projectName;
            deploymentName = di.deploymentName;
            packageName = di.packageName;
            updateDirectory = di.updateDirectory;
            updateTimestamp = di.updateTimestamp;
            firmwareRevision = di.firmwareRevision;
            community = di.community;
            testDeployment = di.testDeployment;
            return this;
        }

        // Set the project name, if it is not already set.
        public DeploymentInfoBuilder withFallbackProjectName(String fallbackName) {
            if (projectName == null || projectName.equalsIgnoreCase("UNKNOWN")) {
                projectName = fallbackName;
            }
            return this;
        }

        public DeploymentInfo build() {
            return new DeploymentInfo(this);
        }
    }
    private DeploymentInfo(DeploymentInfoBuilder builder) {
        this.serialNumber = builder.serialNumber;
        this.projectName = builder.projectName;
        this.deploymentName = builder.deploymentName;
        this.packageName = builder.packageName;
        this.updateDirectory = builder.updateDirectory;
        this.updateTimestamp = builder.updateTimestamp;
        this.firmwareRevision = builder.firmwareRevision;
        this.community = builder.community;
        this.testDeployment = builder.testDeployment;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }

    public String getProjectName() {
        return projectName;
    }

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

    public boolean isTestDeployment() { return testDeployment; }

    @Override
    public String toString() {
        return "Serial number: " + serialNumber
                + "\nProject name: " + projectName
                + "\nDeployment name: " + deploymentName
                + "\nPackage name: " + packageName
                + "\nupdated timestamp: " + updateTimestamp
                + "\nFirmware revision: " + firmwareRevision
                + "\nCommunity: " + community
                + "\nTest: " + (Boolean)testDeployment;
    }
}
