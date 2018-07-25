package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Version {
    public static String buildTimestamp;
    public static String buildDate;
    public static int buildNumber;

    // Initialize the properties
    static {
        Properties props = new Properties();
        //with properties in the same dir as current class
        try {
            props.load(Version.class.getResourceAsStream("/version.properties"));
            buildTimestamp = props.getProperty("BUILD_TIMESTAMP");
            buildDate = props.getProperty("BUILD_DATE");
            buildNumber = Integer.parseInt(props.getProperty("BUILD_NUMBER", "0"));

        } catch (Exception ignored) {
            // Ignore
        }
    }

    public static Boolean isUpToDate() {
        Properties props = new Properties();
        Boolean isUpToDate = null;
        File dbx = new File(DropboxFinder.getDropboxPath());
        String fn = "LB-software/ACM-install/ACM/software/build.properties".replace('/', File.separatorChar);
        File file = new File(dbx, fn);
        try {
            FileInputStream fis = new FileInputStream(file);
            props.load(fis);
            String latestTimestamp = props.getProperty("BUILD_TIMESTAMP");
            isUpToDate = new Boolean(latestTimestamp.equals(buildTimestamp));
        } catch (Exception ignored) {
            // ignore
        }
        return isUpToDate;
    }

}
