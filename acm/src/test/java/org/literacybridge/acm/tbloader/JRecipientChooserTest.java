package org.literacybridge.acm.tbloader;

import org.junit.Test;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class JRecipientChooserTest {



    @Test
    public void nullTest() {
        // Ok.
    }

    @Test
    public void testGetRecipients() {
        ProgramSpec programSpec = getProgramSpec();
        RecipientList recipients = programSpec.getRecipients();

        assertNotNull("Recipients", recipients);
        assertEquals("Expected to find 12 recipients.", 12, recipients.size());
    }

    @Test
    public void testCreateChooser() {
        ProgramSpec programSpec = getProgramSpec();

        JRecipientChooser chooser = new JRecipientChooser(programSpec);
        assertNotNull(chooser);
    }

    @Test
    public void testSelectWithPath() {
        ProgramSpec programSpec = getProgramSpec();
        List<String> path = Arrays.asList("USA", "WA", "Seattle", "Demo Seattle", "", "");
        JRecipientChooser chooser = new JRecipientChooser(programSpec);
        assertNotNull(chooser);

        boolean haveSelection = chooser.setSelectionWithPath(path);
        assertTrue("Expected setSelection to work.", haveSelection);

        Recipient recipient = chooser.getSelectedRecipient();
        assertNotNull("Expected to get back a recipient", recipient);
        assertEquals("Expected to get back country as "+path.get(0), path.get(0), recipient.getCountry());
        assertEquals("Expected to get back region as "+path.get(1), path.get(1), recipient.getRegion());
        assertEquals("Expected to get back district as "+path.get(2), path.get(2), recipient.getDistrict());
        assertEquals("Expected to get back community as "+path.get(3), path.get(3), recipient.getCommunityname());
    }

    @Test
    public void testSelectWithBadPath() {
        ProgramSpec programSpec = getProgramSpec();
        List<String> path = Arrays.asList("USA", "WA", "Yakima", "Demo Seattle", "", "");
        JRecipientChooser chooser = new JRecipientChooser(programSpec);
        assertNotNull(chooser);

        boolean haveSelection = chooser.setSelectionWithPath(path);
        assertFalse("Expected setSelection to fail.", haveSelection);

        Recipient recipient = chooser.getSelectedRecipient();
        assertNull("Expected to get back no recipient", recipient);
    }

    private ProgramSpec getProgramSpec() {
        String testFileName = "progspec1/content.csv";
        File testFile = new File(JRecipientChooserTest.class.getClassLoader().getResource(testFileName).getFile());

        return new ProgramSpec(testFile.getParentFile());
    }

}
