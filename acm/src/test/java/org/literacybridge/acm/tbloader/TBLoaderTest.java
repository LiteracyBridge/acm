package org.literacybridge.acm.tbloader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class TBLoaderTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // These are created fresh for every test. Populate as needed.
    private File homeDir;  // Like ~/Literacybridge
    private File dbxDir;   // Like ~/Dropbox
    private File tbcdDir;  // Like ~/Dropbox/tbcd0123

    private final static String deviceId = "000c";

    private final static String binaryFn = deviceId + TBLoaderConstants.DEVICE_FILE_EXTENSION;
    private final static String hexFn = deviceId + ".hex";
    private final static String textFn = deviceId + ".txt";

    private void configureDirectories() throws IOException {
        homeDir = folder.newFolder("home");
        dbxDir = folder.newFolder("dbx");
        tbcdDir = new File(dbxDir, TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);
        tbcdDir.mkdirs();
    }

//    @Test
//    public void testCreateSrnFiles() throws IOException {
//        int srNo = 123;
//        configureDirectories();
//        TBLoader.saveSerialNumber(deviceId, srNo, tbcdDir);
//        TBLoader.saveSerialNumber(deviceId, srNo, homeDir);
//
//        File homeBin = new File(homeDir, binaryFn);
//        assertTrue("Expected "+binaryFn+" to exist.", homeBin.exists());
//        assertTrue("Expected "+binaryFn+" to be file.", homeBin.isFile());
//
//        File homeHex = new File(homeDir, hexFn);
//        assertTrue("Expected "+hexFn+" to exist.", homeHex.exists());
//        assertTrue("Expected "+hexFn+" to be file.", homeHex.isFile());
//
//        File homeText = new File(homeDir, textFn);
//        assertFalse("Expected "+textFn+" to not exist.", homeText.exists());
//    }

//    @Test
//    public void testReadSrnFiles() throws IOException {
//        int srNo = 123;
//        configureDirectories();
//        TBLoader.saveSerialNumber(deviceId, srNo, tbcdDir);
//        TBLoader.saveSerialNumber(deviceId, srNo, homeDir);
//
//        int homeNo = TBLoader.loadSerialNumber(deviceId, homeDir);
//        int tbcdNo = TBLoader.loadSerialNumber(deviceId, tbcdDir);
//
//        assertEquals("SRN read should be SRN saved.", srNo, homeNo);
//        assertEquals("SRN read should be SRN saved.", srNo, tbcdNo);
//    }

//    @Test
//    public void testReadSrnBinary() throws IOException {
//        int srNo = 456;
//        configureDirectories();
//        writeBinary(new File(homeDir, binaryFn), srNo);
//
//        int readNo = TBLoader.loadSerialNumber(deviceId, homeDir);
//        assertEquals("SRN read should be SRN saved.", srNo, readNo);
//    }

//    @Test
//    public void testReadSrnHex() throws IOException {
//        int srNo = 789;
//        configureDirectories();
//        writeHex(new File(homeDir, hexFn), srNo);
//
//        int readNo = TBLoader.loadSerialNumber(deviceId, homeDir);
//        assertEquals("SRN read should be SRN saved.", srNo, readNo);
//    }

//    @Test
//    public void testReadSrnTxt() throws IOException {
//        int srNo = 234;
//        configureDirectories();
//
//        writeHex(new File(homeDir, textFn), srNo);
//
//        int readNo = TBLoader.loadSerialNumber(deviceId, homeDir);
//        assertEquals("SRN read should be SRN saved.", srNo, readNo);
//    }

//    @Test
//    public void testReadSrnBinaryLarger() throws IOException {
//        int smallNo = 123;
//        int largeNo = 124;
//        configureDirectories();
//
//        writeHex(new File(homeDir, hexFn), smallNo);
//        writeBinary(new File(homeDir, binaryFn), largeNo);
//
//        int readNo = TBLoader.loadSerialNumber(deviceId, homeDir);
//        assertEquals("SRN read should be SRN saved.", largeNo, readNo);
//    }

//    @Test
//    public void testReadSrnHexLarger() throws IOException {
//        int smallNo = 123;
//        int largeNo = 124;
//        configureDirectories();
//
//        writeHex(new File(homeDir, hexFn), largeNo);
//        writeBinary(new File(homeDir, binaryFn), smallNo);
//
//        int readNo = TBLoader.loadSerialNumber(deviceId, homeDir);
//        assertEquals("SRN read should be SRN saved.", largeNo, readNo);
//    }

//    @Test
//    public void testCleanTxtFile() throws IOException {
//        int srNo = 345;
//        configureDirectories();
//
//        writeHex(new File(homeDir, textFn), srNo+1);
//
//        TBLoader.saveSerialNumber(deviceId, srNo, homeDir);
//
//        File homeTxt = new File(homeDir, textFn);
//        assertFalse("Expected "+textFn+" to not exist.", homeTxt.exists());
//
//        int readNo = TBLoader.loadSerialNumber(deviceId, homeDir);
//        assertEquals("SRN read should be SRN saved.", srNo, readNo);
//    }

//    @Test
//    public void testAllSame() throws IOException {
//        int srNo = 345;
//        configureDirectories();
//
//        TBLoader.saveSerialNumber(deviceId, srNo, homeDir);
//
//        File homeBin = new File(homeDir, binaryFn);
//        int readNo;
//        try (DataInputStream is = new DataInputStream(new FileInputStream(homeBin))) {
//            readNo = is.readInt();
//        }
//        assertEquals("SRN read should be SRN saved.", srNo, readNo);
//
//        File homeHex = new File(homeDir, hexFn);
//        try (FileReader fr = new FileReader(homeHex);
//            BufferedReader br = new BufferedReader(fr)) {
//            String line = br.readLine().trim();
//            if (line.length() == 4) {
//                readNo = Integer.parseInt(line, 16);
//            }
//        }
//        assertEquals("SRN read should be SRN saved.", srNo, readNo);
//    }

    private void writeBinary(File file, int serialnumber) throws IOException {
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
            os.writeInt(serialnumber);
        }
    }
    private void writeHex(File file, int serialnumber) throws IOException {
        try (FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("%04x", serialnumber);
        }
    }
}
