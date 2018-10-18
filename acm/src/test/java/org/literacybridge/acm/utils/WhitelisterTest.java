package org.literacybridge.acm.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.acm.config.CategoryFilter;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Taxonomy;
import org.literacybridge.acm.store.TaxonomyLoader;
import org.literacybridge.acm.utils.Whitelister.OPTIONS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class WhitelisterTest {
    private static final String FILENAME = "something.whitelist";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testLoading() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir);

        Whitelister whitelister = new Whitelister(whitelistFile);
        assertNotNull("Expected to load whitelist", whitelister);
    }

    @Test
    public void testHasFilter() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "some-line");

        Whitelister whitelister = new Whitelister(whitelistFile);
        assertNotNull("Expected to load whitelist", whitelister);
        assertTrue("Expected hasFilter", whitelister.hasFilters());
    }

    @Test
    public void testHasNegFilter() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "~some-line");

        Whitelister whitelister = new Whitelister(whitelistFile);
        assertNotNull("Expected to load whitelist", whitelister);
        assertTrue("Expected hasFilter", whitelister.hasFilters());
    }

    @Test
    public void testWhitelist() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "line-one", "line-two");

        Whitelister whitelister = new Whitelister(whitelistFile);
        assertTrue("Expected 'line-one' to match", whitelister.isIncluded("line-one"));
        assertTrue("Expected 'line-two' to match", whitelister.isIncluded("line-two"));
        assertFalse("Expected 'line-three' to NOT match", whitelister.isIncluded("line-three"));
    }

    @Test
    public void testWhitelistCaseInsensitive() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "line-one", "line-two");

        Whitelister whitelister = new Whitelister(whitelistFile);
        assertTrue("Expected 'LINE-ONE' to match", whitelister.isIncluded("LINE-ONE"));
        assertTrue("Expected 'LINE-TWO' to match", whitelister.isIncluded("LINE-TWO"));
        assertFalse("Expected 'LINE-THREE' to NOT match", whitelister.isIncluded("LINE-THREE"));
    }

    @Test
    public void testWhitelistRegex() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "line-[a-zA-Z]*", "number-[0-9]*$");

        Whitelister whitelister = new Whitelister(whitelistFile, OPTIONS.regex);
        assertTrue("Expected 'line-one' to match", whitelister.isIncluded("line-one"));
        assertTrue("Expected 'line-qwerty' to match", whitelister.isIncluded("line-qwerty"));
        assertFalse("Expected 'line-3-a' to  NOT match", whitelister.isIncluded("line-3-a"));
        assertTrue("Expected 'number-3' to  match", whitelister.isIncluded("number-3"));
        assertFalse("Expected 'number-3-a' to  NOT match", whitelister.isIncluded("number-3-a"));
    }

    @Test
    public void testBlacklist() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "~line-one", "!line-two");

        Whitelister whitelister = new Whitelister(whitelistFile);
        assertFalse("Expected 'line-one' to NOT match", whitelister.isIncluded("line-one"));
        assertFalse("Expected 'line-two' to NOT match", whitelister.isIncluded("line-two"));
        assertTrue("Expected 'line-three' to match", whitelister.isIncluded("line-three"));
    }

    @Test
    public void testBlackAndWhitelist() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "~line-one", "line-one", "line-three");

        Whitelister whitelister = new Whitelister(whitelistFile);
        assertFalse("Expected 'line-one' to NOT match", whitelister.isIncluded("line-one"));
        assertFalse("Expected 'line-two' to NOT match", whitelister.isIncluded("line-two"));
        assertTrue("Expected 'line-three' to match", whitelister.isIncluded("line-three"));
    }

    @Test
    public void testBlackAndWhitelistRegex() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "TEST-[0-9]+.*", "~TEST-18-1");

        Whitelister whitelister = new Whitelister(whitelistFile, OPTIONS.regex);
        assertTrue("Expected 'TEST-18' to match", whitelister.isIncluded("TEST-18"));
        assertTrue("Expected 'TEST-18-2' to match", whitelister.isIncluded("TEST-16-2"));
        assertFalse("Expected 'TEST-18-1' to NOT match", whitelister.isIncluded("TEST-18-1"));
        assertTrue("Expected 'TEST-18-10' to match", whitelister.isIncluded("TEST-10-10"));
    }

    @Test
    public void testBlackAndWhitelistRegex2() throws IOException {
        File tempDir = tmp.newFolder();
        File whitelistFile = writeFile(tempDir, "TEST-1[78](-.*)?", "~TEST-18-1*");

        Whitelister whitelister = new Whitelister(whitelistFile, OPTIONS.regex);
        assertTrue("Expected 'TEST-18' to match", whitelister.isIncluded("TEST-18"));
        assertTrue("Expected 'TEST-18-2' to match", whitelister.isIncluded("TEST-18-2"));
        assertFalse("Expected 'TEST-18-1' to NOT match", whitelister.isIncluded("TEST-18-1"));
        assertFalse("Expected 'TEST-18-11' to NOT match", whitelister.isIncluded("TEST-18-11"));
        assertFalse("Expected 'TEST-18-111' to NOT match", whitelister.isIncluded("TEST-18-111"));
        assertTrue("Expected 'TEST-18-10' to match", whitelister.isIncluded("TEST-18-10"));
        assertTrue("Expected 'TEST-18-101' to match", whitelister.isIncluded("TEST-18-101"));
    }




    /**
     * Writes a whitelist file.
     *
     * @param tempDir Where to create the file
     * @return the file that was created.
     * @throws IOException if the file can't be created or written
     */
    private File writeFile(File tempDir, String... lines) throws IOException {
        File whitelistFile = new File(tempDir, FILENAME);
        try (PrintWriter out = new PrintWriter(whitelistFile)) {
            for (String line : lines)
                out.println(line);
        }
        return whitelistFile;
    }
}

