package org.literacybridge.acm.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TALKING_BOOK_DATA;
import static org.literacybridge.core.tbloader.TBLoaderConstants.USER_RECORDINGS;

/**
 * Created by bill on 10/4/16.
 */
public class MoveStatsTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testEmptyDirs() throws IOException {
        File sourceDir = tmp.newFolder();
        File[] statsDirs = sourceDir.listFiles(new MoveStats.TalkingBookDataFilter());
        assertEquals("Size of statsDirs should equal 0", 0, statsDirs.length);
    }

    @Test
    public void testEmptyCollectedData() throws IOException {
        File sourceDir = tmp.newFolder("collected-data");
        File[] statsDirs = sourceDir.listFiles(new MoveStats.TalkingBookDataFilter());
        assertEquals("Size of statsDirs should equal 0", 0, statsDirs.length);
    }

    @Test
    public void testNonEmptyCollectedData() throws IOException {
        File sourceDir = tmp.newFolder();
        File tbloaderDir = new File(sourceDir, "tbcd000c");
        File collectedData = new File(tbloaderDir, "collected-data");
        File projectDir = new File(collectedData, "TEST");
        File talkingBookData = new File(projectDir, TALKING_BOOK_DATA);
        talkingBookData.mkdirs();
        File[] statsDirs = sourceDir.listFiles(new MoveStats.TalkingBookDataFilter());
        assertEquals("Size of statsDirs should equal 1", 1, statsDirs.length);
    }

    @Test
    public void testNonEmptyCollectedDataWithBadProject() throws IOException {
        File sourceDir = tmp.newFolder();
        File tbloaderDir = new File(sourceDir, "tbcd000c");
        File collectedData = new File(tbloaderDir, "collected-data");
        File projectDir = new File(collectedData, "TEST");
        File talkingBookData = new File(projectDir, TALKING_BOOK_DATA);
        talkingBookData.mkdirs();
        File projectDir2 = new File(collectedData, "TOAST");
        File notTalkingBookData = new File(projectDir2, "nottalkingbookdata");
        notTalkingBookData.mkdirs();
        File[] statsDirs = sourceDir.listFiles(new MoveStats.TalkingBookDataFilter());
        assertEquals("Size of statsDirs should equal 1", 1, statsDirs.length);
    }

    @Test
    public void testWithUserRecordings() throws IOException {
        File sourceDir = tmp.newFolder();
        File tbloaderDir = new File(sourceDir, "tbcd000c");
        File collectedData = new File(tbloaderDir, "collected-data");
        File projectDir = new File(collectedData, "TEST");
        File talkingBookData = new File(projectDir, TALKING_BOOK_DATA);
        talkingBookData.mkdirs();
        File userRecordings = new File(projectDir, USER_RECORDINGS);
        File updateDir = new File(userRecordings, "2016-12");
        File audioFile = new File(updateDir, "foo.a18");
        updateDir.mkdirs();
        FileOutputStream fos = new FileOutputStream(audioFile);
        fos.write('a');
        fos.close();
        File[] statsDirs = sourceDir.listFiles(new MoveStats.TalkingBookDataFilter());
        assertEquals("Size of statsDirs should equal 1", 1, statsDirs.length);
        File[] fbDirs = sourceDir.listFiles(new MoveStats.UserRecordingsFilter());
        assertEquals("Size of feedback dirs should equal 1", 1, fbDirs.length);
    }

}
