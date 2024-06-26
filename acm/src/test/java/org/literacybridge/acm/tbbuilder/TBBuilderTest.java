package org.literacybridge.acm.tbbuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class TBBuilderTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testRevisionIncrementing() {
        // Ordinary increment.
        String newRev = Publish.incrementRevision("a");
        assertEquals("\"a\" should be incremented to \"b\".", "b", newRev);

        // Overflow to 2 letters.
        newRev = Publish.incrementRevision("z");
        assertEquals("\"z\" should be incremented to \"aa\".", "aa", newRev);

        // Ordinary increment two letters.
        newRev = Publish.incrementRevision("aa");
        assertEquals("\"aa\" should be incremented to \"ab\".", "ab", newRev);

        // Increment with a carry.
        newRev = Publish.incrementRevision("az");
        assertEquals("\"az\" should be incremented to \"ba\".", "ba", newRev);

        // Overflow to 3 letters.
        newRev = Publish.incrementRevision("zz");
        assertEquals("\"zz\" should be incremented to \"aaa\".", "aaa", newRev);

        // Overflow to many letters (4 letters already gives us nearly 1/2 million revisions).
        newRev = Publish.incrementRevision("zzzz");
        assertEquals("\"zzzz\" should be incremented to \"aaaaa\".", "aaaaa", newRev);

        // Carry to the second letter.
        newRev = Publish.incrementRevision("bbbz");
        assertEquals("\"bbbz\" should be incremented to \"bbca\".", "bbca", newRev);

        // Carry through several letters.
        newRev = Publish.incrementRevision("cdezzz");
        assertEquals("\"cdezzz\" should be incremented to \"cdfaaa\".", "cdfaaa", newRev);
    }

    @Test
    public void testIncrementFailures() {
        try {
            String newRev = Publish.incrementRevision("A");
            fail("Should not be able to increment \"A\".");
        } catch (Exception ignored) {
            // expected.
        }

        try {
            String newRev = Publish.incrementRevision("123");
            fail("Should not be able to increment \"123\".");
        } catch (Exception ignored) {
            // expected.
        }

        try {
            String newRev = Publish.incrementRevision("Aa");
            fail("Should not be able to increment \"Aa\".");
        } catch (Exception ignored) {
            // expected.
        }

        try {
            String newRev = Publish.incrementRevision("a1a");
            fail("Should not be able to increment \"a1a\".");
        } catch (Exception ignored) {
            // expected.
        }

        try {
            String newRev = Publish.incrementRevision("-");
            fail("Should not be able to increment \"-\".");
        } catch (Exception ignored) {
            // expected.
        }
    }

    @Test
    public void testIncrementFile() throws Exception {
        File publishedDir = folder.newFolder("published");

        String deploymentName = "Random-name-123";
        String oldRev = "a";
        File revFile = new File(publishedDir, deploymentName+"-"+oldRev+".rev");
        revFile.createNewFile();

        String newRev = Publish.getNextDeploymentRevision(publishedDir, deploymentName);
        assertEquals("New revisions should be \"b\".", "b", newRev);
    }

    @Test
    public void testNoPrevRevision() throws Exception {
        File publishedDir = folder.newFolder("published");

        String deploymentName = "Other-file-3.14";

        String newRev = Publish.getNextDeploymentRevision(publishedDir, deploymentName);
        assertEquals("First revisions should be \"a\".", "a", newRev);
   }

    @Test
    public void testMultipleIncrementRevision() throws Exception {
        File publishedDir = folder.newFolder("published");

        String deploymentName = "some-1.rEallY.23-skidoo$_$_@weird-name";

        String newRev = Publish.getNextDeploymentRevision(publishedDir, deploymentName);
        assertEquals("First revisions should be \"a\".", "a", newRev);

        newRev = Publish.getNextDeploymentRevision(publishedDir, deploymentName);
        assertEquals("Second revisions should be \"b\".", "b", newRev);

        for (int i='c'; i<='z'; i++) {
            String expected = String.valueOf((char)i);
            newRev = Publish.getNextDeploymentRevision(publishedDir, deploymentName);
            assertEquals("Subsequent revisions should increment.", expected, newRev);
        }

        newRev = Publish.getNextDeploymentRevision(publishedDir, deploymentName);
        assertEquals("Eventually, revisions should increment to \"aa\".", "aa", newRev);
    }

//    @Test
//    public void testShouldFail() {
//        String newRev = Publish.incrementRevision("Aa");
//    }
}
