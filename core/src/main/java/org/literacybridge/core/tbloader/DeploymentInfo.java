package org.literacybridge.core.tbloader;

public class DeploymentInfo {
  private final String serialNumber;   // This has zero to do with deployment
  private final String projectName;
  private final String packageName;
  private final String deploymentName;
  private final String lastUpdatedText;
  private final String firmwareRevision;
  private final String community;

  public DeploymentInfo(String serialNumber, String projectName, String packageName,
      String deploymentName, String lastUpdatedText, String firmwareRevision,
      String community) {
    this.serialNumber = serialNumber;
    this.projectName = projectName;
    this.packageName = packageName;
    this.deploymentName = deploymentName;
    this.lastUpdatedText = lastUpdatedText;
    this.firmwareRevision = firmwareRevision;
    this.community = community;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public String getProjectName() { return projectName; }

  public String getPackageName() {
    return packageName;
  }

  public String getDeploymentName() {
    return deploymentName;
  }

  public String getLastUpdatedText() {
    return lastUpdatedText;
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
        + "\nPackage name: " + packageName
        + "\nDeployment name: " + deploymentName
        + "\nLast updated: " + lastUpdatedText
        + "\nFirmware revision: " + firmwareRevision
        + "\nCommunity: " + community;
  }
}
