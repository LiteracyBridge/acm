package org.literacybridge.acm.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.acm.config.CategoryFilter;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Taxonomy;
import org.literacybridge.acm.store.TaxonomyLoader;
import org.literacybridge.acm.utils.Includelister.OPTIONS;

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

public class IncludelisterTest {
    private static final String FILENAME = "something.includelist";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testLoading() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir);

        Includelister includelister = new Includelister(includelistFile);
        assertNotNull("Expected to load includelist", includelister);
    }

    @Test
    public void testHasFilter() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "some-line");

        Includelister includelister = new Includelister(includelistFile);
        assertNotNull("Expected to load includelist", includelister);
        assertTrue("Expected hasFilter", includelister.hasFilters());
    }

    @Test
    public void testHasNegFilter() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "~some-line");

        Includelister includelister = new Includelister(includelistFile);
        assertNotNull("Expected to load includelist", includelister);
        assertTrue("Expected hasFilter", includelister.hasFilters());
    }

    @Test
    public void testIncludelist() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "line-one", "line-two");

        Includelister includelister = new Includelister(includelistFile);
        assertTrue("Expected 'line-one' to match", includelister.isIncluded("line-one"));
        assertTrue("Expected 'line-two' to match", includelister.isIncluded("line-two"));
        assertFalse("Expected 'line-three' to NOT match", includelister.isIncluded("line-three"));
    }

    @Test
    public void testIncludelistCaseInsensitive() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "line-one", "line-two");

        Includelister includelister = new Includelister(includelistFile);
        assertTrue("Expected 'LINE-ONE' to match", includelister.isIncluded("LINE-ONE"));
        assertTrue("Expected 'LINE-TWO' to match", includelister.isIncluded("LINE-TWO"));
        assertFalse("Expected 'LINE-THREE' to NOT match", includelister.isIncluded("LINE-THREE"));
    }

    @Test
    public void testIncludelistCaseSensitive() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "line-one", "LiNe-OnE");

        Includelister includelister = new Includelister(includelistFile, OPTIONS.caseSensitive);
        assertFalse("Expected 'LINE-ONE' to NOT match", includelister.isIncluded("LINE-ONE"));
        assertTrue("Expected 'line-one' to match", includelister.isIncluded("line-one"));
        assertTrue("Expected 'LiNe-OnE' to match", includelister.isIncluded("LiNe-OnE"));
        assertFalse("Expected 'LINE-THREE' to NOT match", includelister.isIncluded("LINE-THREE"));
    }

    @Test
    public void testIncludelistRegex() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "line-[a-zA-Z]*", "number-[0-9]*$");

        Includelister includelister = new Includelister(includelistFile, OPTIONS.regex);
        assertTrue("Expected 'line-one' to match", includelister.isIncluded("line-one"));
        assertTrue("Expected 'line-qwerty' to match", includelister.isIncluded("line-qwerty"));
        assertFalse("Expected 'line-3-a' to  NOT match", includelister.isIncluded("line-3-a"));
        assertTrue("Expected 'number-3' to  match", includelister.isIncluded("number-3"));
        assertFalse("Expected 'number-3-a' to  NOT match", includelister.isIncluded("number-3-a"));
    }

    @Test
    public void testIncludelistRegexCaseSensitive() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "line-[a-zA-Z]*", "number-[0-9]*$");

        Includelister includelister = new Includelister(includelistFile, OPTIONS.regex, OPTIONS.caseSensitive);
        assertTrue("Expected 'line-one' to match", includelister.isIncluded("line-one"));
        assertTrue("Expected 'line-qwerty' to match", includelister.isIncluded("line-qwerty"));
        assertFalse("Expected 'line-3-a' to  NOT match", includelister.isIncluded("line-3-a"));
        assertTrue("Expected 'number-3' to  match", includelister.isIncluded("number-3"));
        assertFalse("Expected 'number-3-a' to  NOT match", includelister.isIncluded("number-3-a"));
    }

    @Test
    public void testExcludelist() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "~line-one", "!line-two");

        Includelister includelister = new Includelister(includelistFile);
        assertFalse("Expected 'line-one' to NOT match", includelister.isIncluded("line-one"));
        assertFalse("Expected 'line-two' to NOT match", includelister.isIncluded("line-two"));
        assertTrue("Expected 'line-three' to match", includelister.isIncluded("line-three"));
    }

    @Test
    public void testBlackAndIncludelist() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "~line-one", "line-one", "line-three");

        Includelister includelister = new Includelister(includelistFile);
        assertFalse("Expected 'line-one' to NOT match", includelister.isIncluded("line-one"));
        assertFalse("Expected 'line-two' to NOT match", includelister.isIncluded("line-two"));
        assertTrue("Expected 'line-three' to match", includelister.isIncluded("line-three"));
    }

    @Test
    public void testBlackAndIncludelistRegex() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "TEST-[0-9]+.*", "~TEST-18-1");

        Includelister includelister = new Includelister(includelistFile, OPTIONS.regex);
        assertTrue("Expected 'TEST-18' to match", includelister.isIncluded("TEST-18"));
        assertTrue("Expected 'TEST-18-2' to match", includelister.isIncluded("TEST-16-2"));
        assertFalse("Expected 'TEST-18-1' to NOT match", includelister.isIncluded("TEST-18-1"));
        assertTrue("Expected 'TEST-18-10' to match", includelister.isIncluded("TEST-10-10"));
    }

    @Test
    public void testBlackAndIncludelistRegex2() throws IOException {
        File tempDir = tmp.newFolder();
        File includelistFile = writeFile(tempDir, "TEST-1[78](-.*)?", "~TEST-18-1*");

        Includelister includelister = new Includelister(includelistFile, OPTIONS.regex);
        assertTrue("Expected 'TEST-18' to match", includelister.isIncluded("TEST-18"));
        assertTrue("Expected 'TEST-18-2' to match", includelister.isIncluded("TEST-18-2"));
        assertFalse("Expected 'TEST-18-1' to NOT match", includelister.isIncluded("TEST-18-1"));
        assertFalse("Expected 'TEST-18-11' to NOT match", includelister.isIncluded("TEST-18-11"));
        assertFalse("Expected 'TEST-18-111' to NOT match", includelister.isIncluded("TEST-18-111"));
        assertTrue("Expected 'TEST-18-10' to match", includelister.isIncluded("TEST-18-10"));
        assertTrue("Expected 'TEST-18-101' to match", includelister.isIncluded("TEST-18-101"));
    }




    /**
     * Writes an includelist file.
     *
     * @param tempDir Where to create the file
     * @return the file that was created.
     * @throws IOException if the file can't be created or written
     */
    private File writeFile(File tempDir, String... lines) throws IOException {
        File includelistFile = new File(tempDir, FILENAME);
        try (PrintWriter out = new PrintWriter(includelistFile)) {
            for (String line : lines)
                out.println(line);
        }
        return includelistFile;
    }
}

