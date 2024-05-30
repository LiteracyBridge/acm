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
}
