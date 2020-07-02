package core.tbloader;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.ProgressListener;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood2;

/**
 * Created by bill on 5/11/16.
 */
public class TBLoaderIntegrationTest {
    private final static String FS = File.separator;

    private static final String location = "Other";
    private static final String testUser = "tester";
    private static final String tbcdId = "1234";
    private static final String newSrn = "B-12341234";
    private static final String newProject = "PROJ2";
    private static final String newDepl = "PROJ2-18-2";
    private static final String newPkg = "DEMO-2016-2-EN";
    private static final String newTimestamp = "Tue Feb 27 00:00:00 HST 2018";
    private static final String newFirmware = "r1999";
    private static final String newCommunity = "demo-Seattle";
    private static final String recipientid = "123456789abc";
    private static final String srnPrefix = "B-";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void tempDirIsDirectory() throws IOException {
        File tempDir = tmp.newFolder();
        assertEquals("tempDir should be a directory.", true, tempDir.isDirectory());
    }

    @Test
    public void tempDirIsEmpty() throws IOException {
        File tempDir = tmp.newFolder();
        File[] tempFiles = tempDir.listFiles();
        assertNotNull("Files[] should not be null", tempFiles);
        assertEquals("Files[] should be empty.", 0, tempFiles.length);
    }

    @Test
    public void tbLoaderConfig() throws IOException {
        TbFile tempDir = new FsFile(tmp.newFolder());
        TbFile collectedDataDir = new FsFile(tmp.newFolder());

        TBLoaderConfig tbLoaderConfig = new TBLoaderConfig.Builder()
            .withTbLoaderId(tbcdId)
            .withCollectedDataDirectory(collectedDataDir)
            .withTempDirectory(tempDir)
            .withWindowsUtilsDirectory(null)
            .withUserEmail(testUser)
            .build();

        assertEquals(tbLoaderConfig.getTbLoaderId(), tbcdId);
        assertEquals(tbLoaderConfig.getTempDirectory(), tempDir);
        assertEquals(tbLoaderConfig.getCollectedDataDirectory(), collectedDataDir);
        assertEquals(tbLoaderConfig.getWindowsUtilsDirectory(), null);
    }

    @Test
    public void deploymentInfo() {
        String expected = "Serial number: " + newSrn + "\n"
            + "Project name: " + newProject + "\n"
            + "Deployment name: " + newDepl + "\n"
            + "Package name: " + newPkg + "\n"
            + "updated timestamp: " + newTimestamp + "\n"
            + "Firmware revision: " + newFirmware + "\n"
            + "Community: " + newCommunity + "\n"
            + "Recipientid: " + recipientid + "\n"
            + "Test: false";

        DeploymentInfo newDeploymentInfo = getNewDeploymentInfo(newSrn);
        String actual = newDeploymentInfo.toString();
        assertEquals(actual, expected);
    }

    @Test
    public void whereAmI() {
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        assertNotNull(s);
    }

    @Test
    public void simulateTbUpdate() throws IOException {
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        File cwd = new File(s);
        FsFile sourceImage = new FsFile(new File(cwd,
            "testData"+FS+"TB-Loaders"+FS+"DEMO"+FS+"content"+FS+"DEMO-2016-2"));
        File tbRootImage = new File(cwd, "testData"+FS+"tbImages"+FS+"DEMObefore");
        File tbExpectedAfterImage = new File(cwd, "testData"+FS+"tbImages"+FS+"DEMOafter");

        File testRoot = tmp.newFolder();
        File tbRoot = new File(testRoot, "tbroot");
        assertTrue("Couldn't create subdirectory for virtual TB.", tbRoot.mkdirs());

        FileUtils.copyDirectory(tbRootImage, tbRoot);

        TBDeviceInfo oldTbDevice = new TBDeviceInfo(new FsFile(tbRoot), null, srnPrefix);
        String srn = oldTbDevice.getSerialNumber();
        if (srn.equalsIgnoreCase(TBLoaderConstants.NEED_SERIAL_NUMBER) ||
            !isSerialNumberFormatGood(srnPrefix, srn) ||
            !isSerialNumberFormatGood2(srn)) {
            oldTbDevice.setSerialNumber(newSrn);
            srn = newSrn;
        }

        DeploymentInfo oldDeploymentInfo = oldTbDevice.createDeploymentInfo(newProject);
        DeploymentInfo newDeploymentInfo = getNewDeploymentInfo(srn);

        TbFile tempDir = new FsFile(new File(testRoot, "tempdir"));
        TbFile collectedDataDir = new FsFile(new File(testRoot, "collected-data"));

        TBLoaderConfig tbLoaderConfig = new TBLoaderConfig.Builder()
            .withTbLoaderId(tbcdId)
            .withCollectedDataDirectory(collectedDataDir)
            .withTempDirectory(tempDir)
            .withWindowsUtilsDirectory(null)
            .withUserEmail(testUser)
            .build();

        StatusDisplay statusDisplay = new StatusDisplay();

        TBLoaderCore tbLoader = new TBLoaderCore.Builder()
            .withTbLoaderConfig(tbLoaderConfig)
            .withTbDeviceInfo(oldTbDevice)
            .withDeploymentDirectory(sourceImage)
            .withOldDeploymentInfo(oldDeploymentInfo)
            .withNewDeploymentInfo(newDeploymentInfo)
            .withLocation(location)
            .withRefreshFirmware(false)
            .withProgressListener(statusDisplay)
            .build();

        TBLoaderCore.Result res = tbLoader.update();
        assertNotNull(res);

        compareDirectories(tbRoot, tbExpectedAfterImage);

        checkDeploymentProperties(tbRoot, srn);
    }

    private void checkDeploymentProperties(File tbRoot, String srn) {
        Properties props = new Properties();
        File propsFile = new File(tbRoot, "system"+FS+"deployment.properties");
        try (InputStream is = new FileInputStream(propsFile)) {
            props.load(is);
            assertEquals("Expect TB SRN to match.", srn, props.getProperty(TBLoaderConstants.TALKING_BOOK_ID_PROPERTY));
            assertEquals("Expect project to match.", newProject, props.getProperty(TBLoaderConstants.PROJECT_PROPERTY));
            assertEquals("Expect deployment to match.", newDepl, props.getProperty(TBLoaderConstants.DEPLOYMENT_PROPERTY));
            assertEquals("Expect package to match.", newPkg, props.getProperty(TBLoaderConstants.PACKAGE_PROPERTY));
            assertEquals("Expect community to match.", newCommunity, props.getProperty(TBLoaderConstants.COMMUNITY_PROPERTY));
            assertEquals("Expect firmware to match.", newFirmware, props.getProperty(TBLoaderConstants.FIRMWARE_PROPERTY));
            assertNotNull("Expect timestamp to exist.", props.getProperty(TBLoaderConstants.TIMESTAMP_PROPERTY));
            assertEquals("Expect recipientid to match.", recipientid, props.getProperty(TBLoaderConstants.RECIPIENTID_PROPERTY));
            //assertEquals("", , props.getProperty(TBLoaderConstants.TEST_DEPLOYMENT_PROPERTY));
            assertEquals("Expect username to match.", testUser, props.getProperty(TBLoaderConstants.USERNAME_PROPERTY));
            assertEquals("Expect TBCDID to match.", tbcdId, props.getProperty(TBLoaderConstants.TBCDID_PROPERTY));
            assertEquals("Expect 'new SRN' to be false", "false", props.getProperty(TBLoaderConstants.NEW_SERIAL_NUMBER_PROPERTY));
            assertEquals("Expect location to match.", location, props.getProperty(TBLoaderConstants.LOCATION_PROPERTY));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void compareDirectories(File tbRoot, File tbRootImage) {
        Map<String, Integer> actualMap = readFiles(tbRoot, Paths.get(tbRoot.getAbsolutePath()));
        Map<String, Integer> expectedMap = readFiles(tbRootImage,
            Paths.get(tbRootImage.getAbsolutePath()));

        for (Map.Entry<String, Integer> e : expectedMap.entrySet()) {
            String key = e.getKey();
            Integer value = e.getValue();
            if (!actualMap.containsKey(key)) {
                TestCase.assertTrue("Expected to find file " + key, false);
            } else if (!actualMap.get(key).equals(value)) {
                assertEquals("Expected values equal in file " + key, value, actualMap.get(key));
            }
        }
        for (Map.Entry<String, Integer> e : actualMap.entrySet()) {
            String key = e.getKey();
            if (!expectedMap.containsKey(key)) {
                TestCase.assertTrue("Unexpected file found: " + key, false);
            }
        }

    }

    private String[] excludedFilesList = {
        "system"+FS+"last_updated.txt",          // 2018y03m01d15h15m33s-1234
        "system"+FS+"deployment.properties",     // java properties file regarding deployment
        "system"+FS+"DEMO-SEATTLE.loc",          // DEMO-SEATTLE
        "inspect",                          // presence is all that matters
        "0h1m0s.rtc",
        // I have a suspicion this is supposed to be somethign else
        "system"+FS+"PROJ2-18-2.dep",            // presence
        "system"+FS+"B-000C035A.srn",            // presence
        "system"+FS+"notest.pcb",                // presence
        "sysdata.txt",
        // harder to parse, less complete version of deployment.properties
        "system"+FS+"PROJ2.prj"                  // presence
    };

    private Set<String> excludedFiles = null;
    private int errorCounter = 10000; // to assign non-valid numbers when there's an error.

    private Map<String, Integer> readFiles(File root, Path relativeRoot) {
        if (excludedFiles == null) {
            excludedFiles = new HashSet<>();
            excludedFiles.addAll(Arrays.asList(excludedFilesList));
        }
        Map<String, Integer> result = new HashMap<>();

        File[] files = root.listFiles();
        assertNotNull("Argument to readFiles should have been a directory", files);
        for (File f : files) {
            if (f.isDirectory()) {
                result.putAll(readFiles(f, relativeRoot));
            } else {
                String relativePath = relativeRoot.relativize(Paths.get(f.getAbsolutePath()))
                    .toString();
                if (excludedFiles.contains(relativePath)) continue;
                try (InputStream in = new FileInputStream(f);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                    String value = br.readLine().trim();
                    result.put(relativePath, Integer.parseInt(value));
                } catch (Exception e) {
                    result.put(relativePath, ++errorCounter);
                }
            }
        }

        return result;
    }

    private DeploymentInfo getNewDeploymentInfo(String srn) {
        DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
            .withSerialNumber(srn)
            .withNewSerialNumber(false)
            .withProjectName(newProject)
            .withDeploymentName(newDepl)
            .withPackageName(newPkg)
            .withUpdateDirectory(null)
            .withUpdateTimestamp(newTimestamp)
            .withFirmwareRevision(newFirmware)
            .withCommunity(newCommunity)
            .withRecipientid(recipientid)
            .asTestDeployment(false);
        return builder.build();
    }

    class StatusDisplay extends ProgressListener {
        @Override
        public void step(Steps step) {

        }

        @Override
        public void detail(String value) {

        }

        @Override
        public void log(String value) {

        }

        @Override
        public void log(boolean append, String value) {

        }
    }

}
