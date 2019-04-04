package core.spec;

import org.junit.Test;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.RecipientList;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RecipientTest {

    @Test
    public void testProgramSpecDir() {
        String testSpecName = "progspec1";
        File testFile = new File(RecipientTest.class.getClassLoader().getResource(testSpecName).getFile());

        assertTrue("Expected progspec1 to exist", testFile.exists());
        assertTrue("Expected progspec1 to be a directory", testFile.isDirectory());
    }

    @Test
    public void testGetRecipients() {
        String testFileName = "progspec1/content.csv";
        File testFile = new File(RecipientTest.class.getClassLoader().getResource(testFileName).getFile());

        ProgramSpec programSpec = new ProgramSpec(testFile.getParentFile());
        RecipientList recipients = programSpec.getRecipients();

        assertNotNull("Recipients", recipients);
        assertEquals("Expected to find 12 recipients.", 12, recipients.size());
    }

    @Test
    public void testGetRecipientsDepl1() {
        String testFileName = "progspec1/content.csv";
        File testFile = new File(RecipientTest.class.getClassLoader().getResource(testFileName).getFile());

        ProgramSpec programSpec = new ProgramSpec(testFile.getParentFile());
        RecipientList recipients = programSpec.getRecipientsForDeployment(1);

        assertNotNull("Recipients", recipients);
        assertEquals("Expected to find 8 recipients for Deployment 1", 8, recipients.size());
    }

    @Test
    public void testGetRecipientsDepl2() {
        String testFileName = "progspec1/content.csv";
        File testFile = new File(RecipientTest.class.getClassLoader().getResource(testFileName).getFile());

        ProgramSpec programSpec = new ProgramSpec(testFile.getParentFile());
        RecipientList recipients = programSpec.getRecipients();
        RecipientList recipients2 = programSpec.getRecipientsForDeployment(2);

        assertNotNull("Recipients", recipients2);
        assertEquals("Expected to find 12 recipients for Deployment 2", recipients.size(), recipients2.size());
    }

}
