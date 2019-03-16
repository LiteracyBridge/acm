package core.spec;

import org.junit.Test;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.TbFlashData;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;

public class ContentTest {

    @Test
    public void testProgramSpecDir() {
        String testSpecName = "progspec1";
        File testFile = new File(ContentTest.class.getClassLoader().getResource(testSpecName).getFile());

        assertTrue("Expected progspec1 to exist", testFile.exists());
        assertTrue("Expected progspec1 to be a directory", testFile.isDirectory());
    }

    @Test
    public void testGetContent() {
        String testFileName = "progspec1/content.csv";
        File testFile = new File(ContentTest.class.getClassLoader().getResource(testFileName).getFile());

        ProgramSpec programSpec = new ProgramSpec(testFile.getParentFile());
        Content content = programSpec.getContent();

        assertNotNull("Content", content);
        assertNotEquals("Expected to find deployments", content.getDeployments().size(), 0);
    }

}
