package core.spec;

import org.junit.Test;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ProgramSpec;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;

public class ContentSpecTest {

    @Test
    public void testProgramSpecDir() {
        String testSpecName = "progspec1";
        File testFile = new File(ContentSpecTest.class.getClassLoader().getResource(testSpecName).getFile());

        assertTrue("Expected progspec1 to exist", testFile.exists());
        assertTrue("Expected progspec1 to be a directory", testFile.isDirectory());
    }

    @Test
    public void testGetContent() {
        String testFileName = "progspec1/content.csv";
        File testFile = new File(ContentSpecTest.class.getClassLoader().getResource(testFileName).getFile());

        ProgramSpec programSpec = new ProgramSpec(testFile.getParentFile());
        ContentSpec contentSpec = programSpec.getContentSpec();

        assertNotNull("ContentSpec", contentSpec);
        assertNotEquals("Expected to find deployments", contentSpec
            .getDeploymentSpecs().size(), 0);
    }

}
