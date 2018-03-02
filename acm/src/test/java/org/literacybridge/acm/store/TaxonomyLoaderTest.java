package org.literacybridge.acm.store;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

public class TaxonomyLoaderTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testLoadingDefault() throws IOException {
        File tempDir = tmp.newFolder();
        Taxonomy builtin = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load default Taxonomy", builtin);
        assertEquals("Expected to find Taxonomy Root", TaxonomyLoader.LB_TAXONOMY_UID, builtin.getRootCategory().getId());
        assertNotNull("Expected to find '9-0'", builtin.getCategory("9-0"));
    }

    @Test
    public void testLoadingOverride() throws IOException {
        File tempDir = tmp.newFolder();
        writePrivateYaml(tempDir);
        Taxonomy override = Taxonomy.createTaxonomy(tempDir);
        assertNotNull("Expected to load default Taxonomy", override);
        assertEquals("Expected to find Taxonomy Root", TaxonomyLoader.LB_TAXONOMY_UID, override.getRootCategory().getId());

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

    /**
     * Creates a dummy taxonomy with a very high version number.
     * @param tempDir Where to create the lb_taxonomy.yaml file
     * @throws IOException if the file can't be created or written
     */
    private void writePrivateYaml(File tempDir) throws IOException {
        File taxonomyFile = new File(tempDir, TaxonomyLoader.YAML_FILE_NAME);
        try (PrintWriter out = new PrintWriter(taxonomyFile)) {
            out.println("taxonomy:");
            out.println("  revision: 999999");
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
        }
    }

}
