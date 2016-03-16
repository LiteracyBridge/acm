package org.literacybridge.acm.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

/**
 * Unit test for simple DropboxFinder.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {DropboxFinderTest.class, DropboxFinder.class })
public class DropboxFinderTest
{

    /**
     * Rigourous Test :-)
     */
    @Test
    public void testDetectWindows()
    {
        PowerMockito.mockStatic(DropboxFinder.class);
        PowerMockito.when(DropboxFinder.onWindows()).thenReturn(true);

        DropboxFinder dbFinder = new DropboxFinder();
        assertTrue(DropboxFinder.onWindows());
    }

    @Test
    public void testDetectOSx()
    {
        PowerMockito.mockStatic(DropboxFinder.class);
        PowerMockito.when(DropboxFinder.onWindows()).thenReturn(false);

        DropboxFinder dbFinder = new DropboxFinder();
        assertFalse(DropboxFinder.onWindows());
    }

    @Test
    public void testMissingEnvironment()
    {
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
    public void testGetInfoFilePathForNix()
    {
        DropboxFinder dbFinder = new DropboxFinder();
        PowerMockito.mockStatic(System.class);
        // Pretend we're on Mac
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Mac OSX");
        PowerMockito.when(System.getProperty("user.home")).thenReturn("/home/LB");
        PowerMockito.mockStatic(DropboxFinder.class);
        PowerMockito.when(DropboxFinder.onWindows()).thenReturn(false);

        // If, by mistake, we think we're on Windows, we'll use LOCALAPPDATA to find the path:
        PowerMockito.when(System.getenv("APPDATA")).thenReturn("r:\\Users\\LB\\AppData\\Roaming");
        PowerMockito.when(System.getenv("LOCALAPPDATA")).thenReturn("r:\\Users\\LB\\AppData\\Local");

        File infoFile = dbFinder.getInfoFile();
        String expected = File.separator + "home" + File.separator + "LB" + File.separator + ".dropbox" + File.separator + "info.json";
        assertEquals(expected, infoFile.getPath());
    }

    @Test
    public void testGetInfoFilePathForWindows()
    {
        DropboxFinder dbFinder = new DropboxFinder();
        PowerMockito.mockStatic(System.class);
        // Pretend we're on Windows
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows");
        PowerMockito.when(System.getenv("APPDATA")).thenReturn("r:\\Users\\LB\\AppData\\Roaming");
        PowerMockito.when(System.getenv("LOCALAPPDATA")).thenReturn("r:\\Users\\LB\\AppData\\Local");
        PowerMockito.mockStatic(DropboxFinder.class);
        PowerMockito.when(DropboxFinder.onWindows()).thenReturn(true);

        // If, by mistake, we think we're on OS/X or Linux, we'll use this to find the path:
        PowerMockito.when(System.getProperty("user.home")).thenReturn("/home/LB");

        File infoFile = dbFinder.getInfoFile();
        String expected = "r:\\Users\\LB\\AppData\\Local" + File.separator + "Dropbox" + File.separator + "info.json";
        System.out.printf("Expecting %s\ngot%s\n", expected, infoFile.getPath());
        assertEquals(expected, infoFile.getPath());
    }

    @Test
    public void testParseDrobpoxPersonal()
    {
        DropboxFinder dbFinder = new DropboxFinder();
        //String jsonString = "{\"business\": {\"path\": \"/Users/bill/Dropbox (Literacy Bridge)\", \"host\": 4929547026}}";
        String jsonString = "{\"personal\": {\"path\": \"/Users/LB/Dropbox\", \"host\": 4929547026}}";
        InputStream jsonStream = null;
        try {
            jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            assertTrue(false); // fail the test.
        }

        String path;
        path = dbFinder.getDropboxPathFromInputStream(jsonStream);

        assertEquals("/Users/LB/Dropbox", path);
    }

    @Test
    public void testParseDrobpoxBusiness()
    {
        DropboxFinder dbFinder = new DropboxFinder();
        String jsonString = "{\"business\": {\"path\": \"/Users/LB/Dropbox (Literacy Bridge)\", \"host\": 4929547026}}";
        //String jsonString = "{\"personal\": {\"path\": \"/Users/LB/Dropbox\", \"host\": 4929547026}}";
        InputStream jsonStream = null;
        try {
            jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            assertTrue(false); // fail the test.
        }

        String path;
        path = dbFinder.getDropboxPathFromInputStream(jsonStream);

        assertEquals("/Users/LB/Dropbox (Literacy Bridge)", path);
    }

    @Test
    public void testParseDrobpoxBoth()
    {
        DropboxFinder dbFinder = new DropboxFinder();
        String jsonString = "{\"business\": {\"path\": \"/Users/LB/Dropbox (Literacy Bridge)\", \"host\": 4929547026}," +
                "\"personal\": {\"path\": \"/Users/LB/Dropbox\", \"host\": 4929547026}}";
        InputStream jsonStream = null;
        try {
            jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            assertTrue(false); // fail the test.
        }

        String path;
        path = dbFinder.getDropboxPathFromInputStream(jsonStream);

        assertEquals("/Users/LB/Dropbox (Literacy Bridge)", path);
    }

    @Test
    public void testParseBadJson()
    {
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
