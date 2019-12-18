package org.literacybridge.acm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for simple DropboxFinder.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DropboxFinderTest.class, DropboxFinder.class })
public class DropboxFinderTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private File mockDropbox(boolean business) throws IOException {
    File dropbox = tmp.newFolder(business?"Dropbox (Amplio)":"Dropbox");
    File LB_software = new File(dropbox, "LB-software");
    File ACM_Install = new File(LB_software, "ACM-Install");
    File ACM = new File(ACM_Install, "ACM");
    File software = new File (ACM, "software");
    software.mkdirs();
    File acm_jar = new File(software, "acm.jar");
    acm_jar.createNewFile();
    return dropbox;
  }

  /**
   * Rigourous Test :-)
   */
  @Test
  public void testDetectWindows() {
    PowerMockito.mockStatic(DropboxFinder.class);
    PowerMockito.when(DropboxFinder.onWindows()).thenReturn(true);

    DropboxFinder dbFinder = new DropboxFinder();
    assertTrue(DropboxFinder.onWindows());
  }

  @Test
  public void testDetectOSx() {
    PowerMockito.mockStatic(DropboxFinder.class);
    PowerMockito.when(DropboxFinder.onWindows()).thenReturn(false);

    DropboxFinder dbFinder = new DropboxFinder();
    assertFalse(DropboxFinder.onWindows());
  }

  @Test
  public void testMissingEnvironment() {
    DropboxFinder dbFinder = new DropboxFinder();
    PowerMockito.mockStatic(System.class);
    // Pretend we're on Windows
    PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows");
    PowerMockito.when(System.getenv("APPDATA")).thenReturn(null);
    PowerMockito.when(System.getenv("LOCALAPPDATA")).thenReturn(null);
    PowerMockito.mockStatic(DropboxFinder.class);
    PowerMockito.when(DropboxFinder.onWindows()).thenReturn(true);

    boolean gotException = false;
    File path = null;
    try {
      path = dbFinder.getInfoFile();
    } catch (RuntimeException e) {
      gotException = true;
    }
    assertTrue(gotException);
  }

  @Test
  public void testGetInfoFilePathForNix() {
    DropboxFinder dbFinder = new DropboxFinder();
    PowerMockito.mockStatic(System.class);
    // Pretend we're on Mac
    PowerMockito.when(System.getProperty("os.name")).thenReturn("Mac OSX");
    PowerMockito.when(System.getProperty("user.home")).thenReturn("/home/LB");
    PowerMockito.mockStatic(DropboxFinder.class);
    PowerMockito.when(DropboxFinder.onWindows()).thenReturn(false);

    // If, by mistake, we think we're on Windows, we'll use LOCALAPPDATA to find
    // the path:
    PowerMockito.when(System.getenv("APPDATA"))
        .thenReturn("r:\\Users\\LB\\AppData\\Roaming");
    PowerMockito.when(System.getenv("LOCALAPPDATA"))
        .thenReturn("r:\\Users\\LB\\AppData\\Local");

    File infoFile = dbFinder.getInfoFile();
    String expected = File.separator + "home" + File.separator + "LB"
        + File.separator + ".dropbox" + File.separator + "info.json";
    assertEquals(expected, infoFile.getPath());
  }

  @Test
  public void testGetInfoFilePathForWindows() {
    DropboxFinder dbFinder = new DropboxFinder();
    PowerMockito.mockStatic(System.class);
    // Pretend we're on Windows
    PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows");
    PowerMockito.when(System.getenv("APPDATA"))
        .thenReturn("r:\\Users\\LB\\AppData\\Roaming");
    PowerMockito.when(System.getenv("LOCALAPPDATA"))
        .thenReturn("r:\\Users\\LB\\AppData\\Local");
    PowerMockito.mockStatic(DropboxFinder.class);
    PowerMockito.when(DropboxFinder.onWindows()).thenReturn(true);

    // If, by mistake, we think we're on OS/X or Linux, we'll use this to find
    // the path:
    PowerMockito.when(System.getProperty("user.home")).thenReturn("/home/LB");

    File infoFile = dbFinder.getInfoFile();
    String expected = "r:\\Users\\LB\\AppData\\Local" + File.separator
        + "Dropbox" + File.separator + "info.json";
    System.out.printf("Expecting %s\ngot%s\n", expected, infoFile.getPath());
    assertEquals(expected, infoFile.getPath());
  }

  @Test
  public void testParseDrobpoxPersonal() throws IOException {
    File dbxDirPer = mockDropbox(true);
    String dbxPathPer = dbxDirPer.getAbsolutePath();
    dbxPathPer = dbxPathPer.replace('\\', '/');
    DropboxFinder dbFinder = new DropboxFinder();
    // String jsonString = "{\"business\": {\"path\": \"/Users/bill/Dropbox
    // (Literacy Bridge)\", \"host\": 4929547026}}";
    String jsonString = "{\"personal\": {\"path\": \""+dbxPathPer+"\", \"host\": 4929547026}}";
    InputStream jsonStream = null;
    try {
      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      assertTrue(false); // fail the test.
    }

    String path;
    path = dbFinder.getDropboxPathFromInputStream(jsonStream);

    assertEquals(dbxPathPer, path);
  }

  @Test
  public void testParseDrobpoxBusiness() throws IOException {
    File dbxDirBus = mockDropbox(true);
    String dbxPathBus = dbxDirBus.getAbsolutePath();
    dbxPathBus = dbxPathBus.replace('\\', '/');
    DropboxFinder dbFinder = new DropboxFinder();
    String jsonString = "{\"business\": {\"path\": \""+dbxPathBus+"\", \"host\": 4929547026}}";
    // String jsonString = "{\"personal\": {\"path\": \"/Users/LB/Dropbox\",
    // \"host\": 4929547026}}";
    InputStream jsonStream = null;
    try {
      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      assertTrue(false); // fail the test.
    }

    String path;
    path = dbFinder.getDropboxPathFromInputStream(jsonStream);

    assertEquals(dbxPathBus, path);
  }

  @Test
  public void testParseDrobpoxBoth() throws IOException {
    File dbxDirPer = mockDropbox(false);
    String dbxPathPer = dbxDirPer.getAbsolutePath();
    dbxPathPer = dbxPathPer.replace('\\', '/');
    File dbxDirBus = mockDropbox(true);
    String dbxPathBus = dbxDirBus.getAbsolutePath();
    dbxPathBus = dbxPathBus.replace('\\', '/');
    DropboxFinder dbFinder = new DropboxFinder();
    String jsonString = "{\"business\": {\"path\": \""+dbxPathBus+"\", \"host\": 4929547026},"
        + "\"personal\": {\"path\": \"/Users/LB/Dropbox\", \"host\": 4929547026}}";
    InputStream jsonStream = null;
    try {
      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      assertTrue(false); // fail the test.
    }

    String path;
    path = dbFinder.getDropboxPathFromInputStream(jsonStream);

    assertEquals(dbxPathBus, path);
  }

  @Test
  public void testParseBadJson() {
    DropboxFinder dbFinder = new DropboxFinder();
    String jsonString = "This is not a JSON string";
    InputStream jsonStream = null;
    try {
      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      assertTrue(false); // fail the test.
    }

    String path;
    path = dbFinder.getDropboxPathFromInputStream(jsonStream);

    assertEquals("", path);
  }

}
