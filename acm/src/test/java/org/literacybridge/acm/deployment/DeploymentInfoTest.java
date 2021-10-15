package org.literacybridge.acm.deployment;

import org.junit.Test;
import org.literacybridge.acm.deployment.DeploymentInfo.PackageInfo;

import java.io.IOException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;

public class DeploymentInfoTest {
    private static class TD {
        String programid;
        int deploymentNumber;
        String languageCode;
        String variant;
        String expectedName;

        public TD(String programid, int deploymentNumber, String languageCode, String variant, String expectedName) {
            this.programid = programid;
            this.deploymentNumber = deploymentNumber;
            this.languageCode = languageCode;
            this.variant = variant;
            this.expectedName = expectedName;
        }
    }
    private static final TD[] testData = new TD[] {
        new TD("TEST", 1, "en", "", "TEST-1-en"),
        new TD("LONGER-TEST", 1, "eng", "", "LONGER-TEST-1-eng"),
        new TD("LONGER-TEST-2", 1, "eng", "", "LONGER-TEST-21eng"),
        new TD("LONGER-TEST-2", 10, "eng", "", "LNGR-TST-210eng"),
        new TD("LONGER-TEST-2", 10, "eng", "a", "LNGR-TST-210enga"),
        new TD("LONGER-TEST-2", 10, "eng", "aa", "LNGR-TST-210engaa"),
        new TD("LONGER-TEST-2", 10, "eng", "xyz", "LNGRTST210engxyz"),
        new TD("LONGER-TEST-2", 10, "eng", "dcba", "LNGRTST210engdcba"),
        new TD("LONGER-TEST-2", 10, "eng", "lmnop", "LNGRTST10englmnop"),
        new TD("LONGER-TEST-2-AND-LONGER-STILL", 10, "eng", "lmnop", "LNGRTST10englmnop"),
    };

    @Test
    public void testCreate() {
        DeploymentInfo di = new DeploymentInfo("MY-PROGRAM-ID", 1, Calendar.getInstance());
        PackageInfo pi = di.addPackage("eng", "a");
        String sn = pi.getShortName();

        for (TD td : testData) {
            di = new DeploymentInfo(td.programid, td.deploymentNumber, Calendar.getInstance());
            pi = di.addPackage(td.languageCode, td.variant);
            assertEquals("Expected short name", td.expectedName, pi.getShortName());
        }
    }

}
