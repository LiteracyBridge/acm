package org.literacybridge.acm.store;

import org.literacybridge.acm.config.CategoryFilter;
import org.literacybridge.acm.store.Category.CategoryBuilder;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
// @formatter:off
/**
 * Loads a literacy bridge taxonomy from a YAML file. The file is structured as follows:
 * taxonomy:
 *   revision: <version>
 *   categories:
 *     '0':
 *       name: Other Messages
 *       order: 998
 *       children:
 *         '0-0' :
 *         name: General Other
 *         order: 0
 *         . . .
 *     . . .
 *
 * Note to self: don't ever use YAML again. Significant whitespace is known to be a bad idea.
 */
// @formatter:on

public class TaxonomyLoader {
    public final static String LB_TAXONOMY_UID = "LB_TAX_1.0";

    public final static String YAML_FILE_NAME = "lb_taxonomy.yaml";
    private final static String YAML_REVISION_FIELD = "revision";
    private final static String YAML_CATEGORIES_FIELD = "categories";
    private final static String YAML_TAXONOMY_FIELD = "taxonomy";
    private final static String YAML_CAT_NAME_FIELD = "name";
    private final static String YAML_CAT_AKA_FIELD = "aka";
    private final static String YAML_CAT_ORDER_FIELD = "order";
    private final static String YAML_CAT_NON_ASSIGNABLE_FIELD = "nonassignable"; // Can a message be assigned to this category?
    private final static String YAML_CAT_USERFEEDBACK_BUCKETS = "feedbackbuckets"; // Add UF buckets?
    private final static String YAML_CAT_CHILDREN_FIELD = "children";

    private final static String[][] FEEDBACK_BUCKETS = {
        {"-1", "Question"},
        {"-2", "Endorsement"},
        {"-3", "Suggestion"},
        {"-4", "Complaint"},
        {"-5", "Comment"}
    };

    /**
     * Find the highest-versioned of the built-in taxonomy and a lb_taxonomy.yaml that may
     * exist in the given directory. Load that into the given taxonomy object.
     *
     * @param categoryFilter
     * @param programHomeDir possibly containing a lb_taxonomy.yaml file
     * @param taxonomy     A taxonomy object containing only a root entry.
     */
    public static void loadLatestTaxonomy(CategoryFilter categoryFilter,
        File programHomeDir,
        Taxonomy taxonomy) {
        TaxonomyLoader loader = new TaxonomyLoader(categoryFilter, programHomeDir, taxonomy);
        loader.loadLatestTaxonomy();
    }

    private final File programHomeDir;
    private final Taxonomy taxonomy;
    private final CategoryFilter categoryFilter;

    private TaxonomyLoader(CategoryFilter categoryFilter,
        File programHomeDir,
        Taxonomy taxonomy) {
        this.programHomeDir = programHomeDir;
        this.taxonomy = taxonomy;
        this.categoryFilter = categoryFilter;
    }

    private void loadLatestTaxonomy() {
        // Note that if there is an includelist, and it does not have "LB_TAX_1.0" in it,
        // then anything that is to be visible must be explicitly included.
        Category rootCategory = taxonomy.getRootCategory();
        rootCategory.updateVisibility(categoryFilter.getVisibilityFor(rootCategory, null));

        VersionedTaxonomyData taxonomyData = findLatest();
        taxonomy.setRevision(taxonomyData.revision);
        Map<String, Object> categories = taxonomyData.categories;
        parseYamlData(categories, rootCategory);
    }

    /**
     * Versioned, raw Taxonomy data.
     */
    private class VersionedTaxonomyData {
        private final int revision;
        private final Map<String, Object> categories;

        private VersionedTaxonomyData(int revision, Map<String, Object> categories) {
            this.revision = revision;
            this.categories = categories;
        }
    }

    /**
     * Get's the latest of a) the built in taxonomy or b) an (optional) lb_taxonomy.yaml in
     * the project directory.
     * @return The latest taxonomy data.
     */
    private VersionedTaxonomyData findLatest() {
        // Load the built-in taxonomy
        VersionedTaxonomyData versionedTaxonomyData = loadYamlFromStream(TaxonomyLoader.class.getResourceAsStream(
            "/" + YAML_FILE_NAME));

        // check if there is a newer one in the acmDirectory.
        File acmTaxonomyFile = new File(programHomeDir, YAML_FILE_NAME);
        if (acmTaxonomyFile.exists()) {
            try (FileInputStream in = new FileInputStream(acmTaxonomyFile)) {

                VersionedTaxonomyData localTaxonomy = loadYamlFromStream(in);
                if (localTaxonomy.revision > versionedTaxonomyData.revision) {
                    versionedTaxonomyData = localTaxonomy;
                }
            } catch (MarkedYAMLException e) {
                System.out.println(e.toString());
                // ignore and use the packaged taxonomy
            } catch (Exception e) {
                Exception ex = e;
                System.out.println(e.getMessage());
                // ignore and use the packaged taxonomy
            }

        }

        return versionedTaxonomyData;
    }

    /**
     * Given an input stream, attempt to load a Taxonomy from it. The stream is a YAML file, with
     * one member, named "taxonomy", itself containing two members, a "revision" member (ie, a
     * version), and a "categories" member, containing a map of categoryid: category objects:
     *
     * @param input stream with a Taxonomy.
     * @return the Taxonomy from the stream.
     */
    private VersionedTaxonomyData loadYamlFromStream(InputStream input) {
        Yaml yaml = new Yaml();
        // Load the YAML, get the one and only member.
        Map<String, Object> data = (Map<String, Object>) yaml.load(input);
        Map<String, Object> taxonomyMap = (Map<String, Object>) data.get(YAML_TAXONOMY_FIELD);
        // Metadata about the Taxonomy (only the version number)
        int revision = (Integer) taxonomyMap.get(YAML_REVISION_FIELD);
        // Finally the Taxonomy itself
        Map<String, Object> categories = (Map<String, Object>) taxonomyMap.get(YAML_CATEGORIES_FIELD);
        return new VersionedTaxonomyData(revision, categories);
    }

    /**
     * Helper to add the default "addFeedbackBuckets=false" parameter.
     * @param subCategories map of id: category to be parsed into the taxonomy.
     * @param parent category under which to place the parsed categories.
     */
    private void parseYamlData(Map<String, Object> subCategories, Category parent) {
        parseYamlData(subCategories, parent, false);
    }
    /**
     * Parses the nested object maps loaded from a Taxonomy YAML into a Taxonomy object.
     *  @param subCategories map of id: category to be parsed into the Taxonomy.
     * @param parent        category under which to place the parsed categories.
     * @param addFeedbackBuckets If true, add feedback buckets to the leaf nodes.
     */
    private void parseYamlData(Map<String, Object> subCategories,
        Category parent,
        boolean addFeedbackBuckets)
    {

        Integer nextOrder = 0;
        Set<Integer> usedOrders = new HashSet<>();

        for (Entry<String, Object> entry : subCategories.entrySet()) {
            String catID = entry.getKey();

            @SuppressWarnings("unchecked")
            Map<String, Object> catData = (Map<String, Object>) entry.getValue();
            String catName = (String) catData.get(YAML_CAT_NAME_FIELD);

            // If no "order" value, use the next sequential value. If duplicate, increment.
            Integer order = null;
            try {
                order = (Integer) catData.get(YAML_CAT_ORDER_FIELD);
            } catch (Exception e) { /* ignore */ }
            order = (order == null) ? nextOrder : order;
            while (usedOrders.contains(order)) { order += 1; }
            usedOrders.add(order);
            nextOrder = order + 1;

            // If no "nonassignable" value, inherit from parent. Defaults to 'false'.
            Object field = catData.get(YAML_CAT_NON_ASSIGNABLE_FIELD);
            boolean nonAssignable = parent.isNonAssignable();
            try {
                if (field != null) {
                    nonAssignable = (Boolean) field;
                }
            } catch (Exception ex) {
                // ignore.
            }

            boolean addBuckets = addFeedbackBuckets;
            field = catData.get(YAML_CAT_USERFEEDBACK_BUCKETS);
            try {
                if (field != null) {
                    addBuckets = (Boolean) field;
                }
            } catch (Exception ex) {
                // Ignore, and use passed-in value.
            }

            List<String> aliases = null;
            field = catData.get(YAML_CAT_AKA_FIELD);
            try {
                if (field != null) {
                    aliases = Arrays.stream(((String) field).split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                }
            } catch (Exception ex) {
                // Ignore and use no AKA.
            }


            Category cat = new CategoryBuilder(catID).withName(catName)
                .withOrder(order)
                .nonAssignable(nonAssignable)
                .alsoKnownAs(aliases)
                .withVisibility(categoryFilter.getVisibilityFor(catID, parent))
                .build();
            taxonomy.addChild(parent, cat);
            Object children = catData.get(YAML_CAT_CHILDREN_FIELD);
            if (children != null) {
                //noinspection unchecked
                parseYamlData((Map<String, Object>) children, cat, addBuckets);
            } else if (addBuckets) {
                addFeedbackBuckets(cat);
            }
        }
    }

    /**
     * Adds the feedback buckets to a taxonomy node. Removes much boilerplate from the taxonomy
     * file.
     * @param parent Category node to get buckets.
     */
    private void addFeedbackBuckets(Category parent) {
        int order = 1;
        boolean nonAssignable = parent.isNonAssignable();
        for (String[] fb : FEEDBACK_BUCKETS) {
            String id = parent.getId() + fb[0];
            Category cat = new CategoryBuilder(id)
                .withName(fb[1])
                .withOrder(order++)
                .nonAssignable(nonAssignable)
                .withVisibility(categoryFilter.getVisibilityFor(id, parent))
                .autogenerated()
                .build();
            taxonomy.addChild(parent, cat);
        }
    }

    private static void print(Category cat) {
        Collection<? extends Category> children = cat.getChildren();
        if (children != null) {
            for (Category c : children) {
                print(c);
            }
        }
    }

}
