package org.literacybridge.acm.store;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.acm.config.CategoryFilter;

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

public class TaxonomyLoaderTest {
    public static final int TEST_REVISION = 999999;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testLoadingDefault() throws IOException {
        File tempDir = tmp.newFolder();
        Taxonomy builtin = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load default Taxonomy", builtin);
        assertEquals("Expected to find Taxonomy Root",
            TaxonomyLoader.LB_TAXONOMY_UID,
            builtin.getRootCategory().getId());
        assertNotNull("Expected to find '9-0'", builtin.getCategory("9-0"));
    }

// Enable this "test" to print out a more human-readable version of the taxonomy.
    @Test
    public void saveFriendlyVersion() throws IOException {
        File tempDir = tmp.newFolder();
        Taxonomy builtin = Taxonomy.createTaxonomy(tempDir);
        printTaxonomy(builtin);
    }

    @Test
    public void testLoadingOverride() throws IOException {
        File tempDir = tmp.newFolder();
        writePrivateYaml(tempDir);
        Taxonomy override = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load override Taxonomy", override);
        assertEquals("Expected to find Taxonomy Root",
            TaxonomyLoader.LB_TAXONOMY_UID,
            override.getRootCategory().getId());
        assertEquals("Expected to find TEST_REVISION", TEST_REVISION, (int) override.getRevision());

        assertNull("Expected to not find '9-0'", override.getCategory("9-0"));
        Category cat = override.getCategory("12345-99");
        assertNotNull("Expected to find '12345-99'", cat);
        assertEquals("Expected '12345-99' to be at order 9", 9, cat.getOrder());

        cat = override.getCategory("outoforder");
        assertNotNull("Expected to find 'outoforder'", cat);
        assertEquals("Expected 'outoforder' to be at position 1", 1, cat.getOrder());
        assertEquals("Expected '12345-00' to be at position 0", 0, override.getCategory("12345-00").getOrder());
        assertEquals("Expected '12345-22' to be at position 2", 2, override.getCategory("12345-22").getOrder());
        assertEquals("Expected '12345-33' to be at position 3", 3, override.getCategory("12345-33").getOrder());
        assertEquals("Expected '12345-44' to be at position 4", 4, override.getCategory("12345-44").getOrder());
    }

    @Test
    public void testNonassignable() throws IOException {
        File tempDir = tmp.newFolder();
        writePrivateYaml(tempDir);
        Taxonomy override = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load override Taxonomy", override);
        assertEquals("Expected to find Taxonomy Root",
            TaxonomyLoader.LB_TAXONOMY_UID,
            override.getRootCategory().getId());
        assertEquals("Expected to find TEST_REVISION", TEST_REVISION, (int) override.getRevision());

        Category cat = override.getCategory("0-1");
        assertNotNull("Expected to find '0-1'", cat);
        assertTrue("Expected '0-1' to be visible", cat.isVisible());
        cat = override.getCategory("0-1-1");
        assertNotNull("Expected to find '0-1-1'", cat);
        assertTrue("Expected '0-1-1' to be visible", cat.isVisible());

        cat = override.getCategory("0-2");
        assertNotNull("Expected to find '0-2'", cat);
        assertTrue("Expected '0-2' to be visible", cat.isVisible());
        cat = override.getCategory("0-2-1");
        assertNotNull("Expected to find '0-2-1'", cat);
        assertTrue("Expected '0-2-1' to be visible", cat.isVisible());

        cat = override.getCategory("0-3");
        assertNotNull("Expected to find '0-3'", cat);
        assertFalse("Expected '0-3' to not be visible", cat.isVisible());
        cat = override.getCategory("0-3-1");
        assertNotNull("Expected to find '0-3-1'", cat);
        assertFalse("Expected '0-3-1' to not be visible", cat.isVisible());
    }

    @Test
    public void testWhitelisting() throws IOException {
        File tempDir = tmp.newFolder();
        writePrivateYaml(tempDir);
        writeWhitelist(tempDir);
        Taxonomy override = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load override Taxonomy", override);
        assertEquals("Expected to find Taxonomy Root",
            TaxonomyLoader.LB_TAXONOMY_UID,
            override.getRootCategory().getId());
        assertEquals("Expected to find TEST_REVISION", TEST_REVISION, (int) override.getRevision());

        Category cat = override.getCategory("0-1");
        assertNotNull("Expected to find '0-1'", cat);
        assertFalse("Expected '0-1' to not be visible", cat.isVisible());
        cat = override.getCategory("0-1-1");
        assertNotNull("Expected to find '0-1-1'", cat);
        assertFalse("Expected '0-1-1' to not be visible", cat.isVisible());

        cat = override.getCategory("0-2");
        assertNotNull("Expected to find '0-2'", cat);
        assertTrue("Expected '0-2' to be visible", cat.isVisible());
        cat = override.getCategory("0-2-1");
        assertNotNull("Expected to find '0-2-1'", cat);
        assertTrue("Expected '0-2-1' to be visible", cat.isVisible());

        cat = override.getCategory("0-3");
        assertNotNull("Expected to find '0-3'", cat);
        assertFalse("Expected '0-3' to not be visible", cat.isVisible());
        cat = override.getCategory("0-3-1");
        assertNotNull("Expected to find '0-3-1'", cat);
        assertFalse("Expected '0-3-1' to not be visible", cat.isVisible());

        assertTrue("Should require whitelist semantics", CategoryFilter.requiresWhitelistSemantics(override));
        List<String> wlData = CategoryFilter.buildWhitelistData(override);
        System.out.println(wlData);
    }

    @Test
    public void testBlacklisting() throws IOException {
        File tempDir = tmp.newFolder();
        writePrivateYaml(tempDir);
        writeBlacklist(tempDir);
        Taxonomy override = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load override Taxonomy", override);
        assertEquals("Expected to find Taxonomy Root",
            TaxonomyLoader.LB_TAXONOMY_UID,
            override.getRootCategory().getId());
        assertEquals("Expected to find TEST_REVISION", TEST_REVISION, (int) override.getRevision());

        Category cat = override.getCategory("0-1");
        assertNotNull("Expected to find '0-1'", cat);
        assertTrue("Expected '0-1' to be visible", cat.isVisible());
        cat = override.getCategory("0-1-1");
        assertNotNull("Expected to find '0-1-1'", cat);
        assertFalse("Expected '0-1-1' to not be visible", cat.isVisible());

        cat = override.getCategory("0-2");
        assertNotNull("Expected to find '0-2'", cat);
        assertTrue("Expected '0-2' to be visible", cat.isVisible());
        cat = override.getCategory("0-2-1");
        assertNotNull("Expected to find '0-2-1'", cat);
        assertTrue("Expected '0-2-1' to be visible", cat.isVisible());

        cat = override.getCategory("0-3");
        assertNotNull("Expected to find '0-3'", cat);
        assertFalse("Expected '0-3' to not be visible", cat.isVisible());
        cat = override.getCategory("0-3-1");
        assertNotNull("Expected to find '0-3-1'", cat);
        assertFalse("Expected '0-3-1' to not be visible", cat.isVisible());

        assertFalse("Shouldn't require whitelist semantics", CategoryFilter.requiresWhitelistSemantics(override));
        List<String> wlData = CategoryFilter.buildWhitelistData(override);
        System.out.println(wlData);
    }

    @Test
    public void testMixedlisting() throws IOException {
        File tempDir = tmp.newFolder();
        writePrivateYaml(tempDir);
        writeMixedlist(tempDir);
        Taxonomy override = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load override Taxonomy", override);
        assertEquals("Expected to find Taxonomy Root",
            TaxonomyLoader.LB_TAXONOMY_UID,
            override.getRootCategory().getId());
        assertEquals("Expected to find TEST_REVISION", TEST_REVISION, (int) override.getRevision());

        Category cat = override.getCategory("0-1");
        assertNotNull("Expected to find '0-1'", cat);
        assertTrue("Expected '0-1' to be visible", cat.isVisible());
        cat = override.getCategory("0-1-1");
        assertNotNull("Expected to find '0-1-1'", cat);
        assertFalse("Expected '0-1-1' to be not visible", cat.isVisible());
        cat = override.getCategory("0-1-2");
        assertNotNull("Expected to find '0-1-2'", cat);
        assertTrue("Expected '0-1-2' to be visible", cat.isVisible());

        cat = override.getCategory("0-2");
        assertNotNull("Expected to find '0-2'", cat);
        assertFalse("Expected '0-2' to not be visible", cat.isVisible());
        cat = override.getCategory("0-2-1");
        assertNotNull("Expected to find '0-2-1'", cat);
        assertFalse("Expected '0-2-1' to not be visible", cat.isVisible());

        cat = override.getCategory("0-3");
        assertNotNull("Expected to find '0-3'", cat);
        assertFalse("Expected '0-3' to not be visible", cat.isVisible());
        cat = override.getCategory("0-3-1");
        assertNotNull("Expected to find '0-3-1'", cat);
        assertFalse("Expected '0-3-1' to not be visible", cat.isVisible());

        assertTrue("Should require whitelist semantics", CategoryFilter.requiresWhitelistSemantics(override));
        List<String> wlData = CategoryFilter.buildWhitelistData(override);
        System.out.println(wlData);
    }

    @Test
    public void testFeedbackBuckets() throws IOException {
        File tempDir = tmp.newFolder();
        writePrivateYaml(tempDir);
        writeMixedlist(tempDir);
        Taxonomy override = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load override Taxonomy", override);
        assertEquals("Expected to find Taxonomy Root",
            TaxonomyLoader.LB_TAXONOMY_UID,
            override.getRootCategory().getId());
        assertEquals("Expected to find TEST_REVISION", TEST_REVISION, (int) override.getRevision());

        Category cat = override.getCategory("90-1-1");
        assertNotNull("Expected to find '90-1-1'", cat);
        cat = override.getCategory("90-1-1-1");
        assertNotNull("Expected to find '90-1-1-1'", cat);
        assertEquals("Expected 90-1-1-1 to be 'Question'", "Question", cat.getCategoryName());

        cat = override.getCategory("90-1-1-2");
        assertNotNull("Expected to find '90-1-1-2'", cat);
        assertEquals("Expected 90-1-1-2 to be 'Question'", "Endorsement", cat.getCategoryName());

        cat = override.getCategory("90-1-1-3");
        assertNotNull("Expected to find '90-1-1-3'", cat);
        assertEquals("Expected 90-1-1-3 to be 'Question'", "Suggestion", cat.getCategoryName());

        cat = override.getCategory("90-1-1-4");
        assertNotNull("Expected to find '90-1-1-4'", cat);
        assertEquals("Expected 90-1-1-4 to be 'Question'", "Complaint", cat.getCategoryName());

        cat = override.getCategory("90-1-1-5");
        assertNotNull("Expected to find '90-1-1-5'", cat);
        assertEquals("Expected 90-1-1-5 to be 'Question'", "Comment", cat.getCategoryName());

        // Test turning feedbackbuckets off:
        cat = override.getCategory("90-1-2");
        assertNotNull("Expected to find '90-1-2'", cat);
        cat = override.getCategory("90-1-2-1");
        assertNull("Expected to not find '90-1-2-1'", cat);
    }

        /**
         * Creates a dummy taxonomy with a very high version number.
         *
         * @param tempDir Where to create the lb_taxonomy.yaml file
         * @throws IOException if the file can't be created or written
         */
    private void writePrivateYaml(File tempDir) throws IOException {
        File taxonomyFile = new File(tempDir, TaxonomyLoader.YAML_FILE_NAME);
        try (PrintWriter out = new PrintWriter(taxonomyFile)) {
            out.println("taxonomy:");
            out.println(String.format("  revision: %d", TEST_REVISION));
            out.println("  categories:");
            out.println("    '12345' :");
            out.println("      name: First category name");
            out.println("      children:");
            out.println("         '12345-00' :");
            out.println("           name: At zero");
            out.println("         '12345-99' :");
            out.println("           order: 9");
            out.println("           name: At ten");
            out.println("         outoforder:");
            out.println("           order: 1");
            out.println("           name: At one");
            out.println("         '12345-22' :");
            out.println("           name: At two");
            out.println("         '12345-33' :");
            out.println("           name: At three");
            out.println("         '12345-44' :");
            out.println("           name: AT four");
            out.println("    '0' :");
            out.println("      name: Parent");
            out.println("      children:");
            out.println("        '0-1' :");
            out.println("          name: zero-one");
            out.println("          children:");
            out.println("            '0-1-1' :");
            out.println("              name: zero-one-one");
            out.println("            '0-1-2' :");
            out.println("              name: zero-one-two");
            out.println("        '0-2' :");
            out.println("          name: zero-two");
            out.println("          children:");
            out.println("            '0-2-1' :");
            out.println("              name: zero-two-one");
            out.println("        '0-3' :");
            out.println("          name: zero-three");
            out.println("          nonassignable: true");
            out.println("          children:");
            out.println("            '0-3-1' :");
            out.println("              name: zero-three-one");
            out.println("    '90' :");
            out.println("      name: UF");
            out.println("      feedbackbuckets: true");
            out.println("      children:");
            out.println("        '90-1' :");
            out.println("          name: ninety-one");
            out.println("          children:");
            out.println("            '90-1-1' :");
            out.println("              name: ninety-one-one");
            out.println("            '90-1-2' :");
            out.println("              name: ninety-one-two");
            out.println("              feedbackbuckets: false");
        }
    }

    private void writeWhitelist(File tempDir) throws IOException {
        File taxonomyFile = new File(tempDir, CategoryFilter.WHITELIST_FILENAME);
        try (PrintWriter out = new PrintWriter(taxonomyFile)) {
            out.println("0-2");
        }
    }
    private void writeBlacklist(File tempDir) throws IOException {
        File taxonomyFile = new File(tempDir, CategoryFilter.WHITELIST_FILENAME);
        try (PrintWriter out = new PrintWriter(taxonomyFile)) {
            out.println("~ 0-1-1");
        }
    }
    private void writeMixedlist(File tempDir) throws IOException {
        File taxonomyFile = new File(tempDir, CategoryFilter.WHITELIST_FILENAME);
        try (PrintWriter out = new PrintWriter(taxonomyFile)) {
            out.println("0-1");
            out.println("~ 0-1-1");
        }
    }

    private void printTaxonomy(Taxonomy tax) throws FileNotFoundException {
        String s = File.separator;
        File taxonomyFile = new File("src"+s+"main"+s+"resources"+s+"taxonomy.txt");
        Category uf = tax.getCategory("90");
        try (PrintWriter out = new PrintWriter(taxonomyFile)) {
            printCategory(tax.getRootCategory(), "", out, uf);
        }
    }
    private void printCategory(Category cat, String prefix, PrintWriter out, Category uf) {
        // Don't print leaf nodes of Categorized Feedback. They're generated. Yes, this is hacky.
        // Hey, this is a pseudo-test to generate a human readable listing of categories.
        if (cat.hasChildren() || !cat.isChildOf(uf)) {
            out.printf("%s%s  (%s)%n", prefix, cat.getCategoryName(), cat.getId());
            for (Category child : cat.getSortedChildren()) {
                printCategory(child, prefix + "  ", out, uf);
            }
        }
    }

}

