package org.literacybridge.acm.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

/**
 * Loads a literacy bridge taxonomy from a YAML file. The file is structured as follows:
 * taxonomy:
 *   revision: <version>
 *   categories:
 *      '0':
 *          name: Other Messages
 *          order: 998
 *          children:
 *              '0-0' :
 *                  name: General Other
 *                  order: 0
 *              . . .
 *      . . .
 */
public class TaxonomyLoader {
  public final static String LB_TAXONOMY_UID = "LB_TAX_1.0";

  public final static String YAML_FILE_NAME = "lb_taxonomy.yaml";
  private final static String YAML_REVISION_FIELD = "revision";
  private final static String YAML_CATEGORIES_FIELD = "categories";
  private final static String YAML_TAXONOMY_FIELD = "taxonomy";
  private final static String YAML_CAT_NAME_FIELD = "name";
  private final static String YAML_CAT_ORDER_FIELD = "order";
  private final static String YAML_CAT_NON_ASSIGNABLE_FIELD = "nonassignable"; // Can a message be assigned to this category?
  private final static String YAML_CAT_CHILDREN_FIELD = "children";

    /**
     * Versioned, raw Taxonomy data.
     */
  public static class TaxonomyData {
    private final int revision;
    private final Map<String, Object> categories;

    private TaxonomyData(int revision, Map<String, Object> categories) {
      this.revision = revision;
      this.categories = categories;
    }

      /**
       * Parse this set of categories into the given taxonomy.
       * @param taxonomy to receive the categories.
       */
    public void createTaxonomy(Taxonomy taxonomy) {
      TaxonomyLoader.parseYamlData(taxonomy, categories, taxonomy.getRootCategory());
    }
  }

    /**
     * Creates a Category object, and adds it to the Taxonomy as a child of the given Parent.
     * @param taxonomy to receive the new Category.
     * @param parent of the new Category.
     * @param id of the new Category, like "9-0"
     * @param name of the new Category, like "Feedback From Users"
     * @param order of the Category within a list of it and its peers
     * @param nonAssignable If true, will not be available to assign to a message
     * @return the new Category object
     */
  private static Category addCategory(
      Taxonomy taxonomy,
      Category parent,
      String id,
      String name,
      int order,
      boolean nonAssignable) {
    Category cat = new Category(id);
    cat.setName(name);
    cat.setOrder(order);
    cat.setNonAssignable(nonAssignable);
    taxonomy.addChild(parent, cat);
    return cat;
  }

    /**
     * Return the highest-versioned of the built-in taxonomy and a lb_taxonomy.yaml that may
     * exist in the given directory.
     * @param acmDirectory possibly containing a lb_taxonomy.yaml file
     * @return the latest of the built-in and found taxonomy file's contents.
     */
  public static TaxonomyData loadLatestTaxonomy(File acmDirectory) {
      // Load the built-in taxonomy
    TaxonomyData taxonomy = loadYamlFromStream(TaxonomyLoader.class
        .getResourceAsStream("/" + YAML_FILE_NAME));

    // check if there is a newer one in the acmDirectory.
    File acmTaxonomyFile = new File(acmDirectory, YAML_FILE_NAME);
    if (acmTaxonomyFile.exists()) {
      try (FileInputStream in = new FileInputStream(acmTaxonomyFile)) {

        TaxonomyData localTaxonomy = loadYamlFromStream(in);
        if (localTaxonomy.revision > taxonomy.revision) {
          taxonomy = localTaxonomy;
        }
      } catch (Exception e) {
          Exception ex = e;
        // ignore and use the packaged taxonomy
      }
    }

    return taxonomy;
  }

    /**
     * Given an input stream, attempt to load a Taxonomy from it. The stream is a YAML file, with
     * one member, named "taxonomy", itself containing two members, a "revision" member (ie, a
     * version), and a "categories" member, containing a map of categoryid: category objects:
     * @param input stream with a Taxonomy.
     * @return the Taxonomy from the stream.
     */
  private static TaxonomyData loadYamlFromStream(InputStream input) {
      Yaml yaml = new Yaml();
      // Load the YAML, get the one and only member.
      Map<String, Object> data = (Map<String, Object>) yaml.load(input);
      Map<String, Object> taxonomyMap = (Map<String, Object>) data.get(YAML_TAXONOMY_FIELD);
      // Metadata about the Taxonomy (only the version number)
      int revision = (Integer) taxonomyMap.get(YAML_REVISION_FIELD);
      // Finally the Taxonomy itself
      Map<String, Object> categories = (Map<String, Object>) taxonomyMap.get(YAML_CATEGORIES_FIELD);
      return new TaxonomyData(revision, categories);
  }

    /**
     * Parses the nested object maps loaded from a Taxonomy YAML into a Taxonomy object.
     * @param taxonomy object to be populated.
     * @param subCategories map of id: category to be parsed into the Taxonomy.
     * @param parent category under which to place the parsed categories.
     */
  private static void parseYamlData(
      Taxonomy taxonomy,
      Map<String, Object> subCategories,
      Category parent) {

      Integer nextOrder = 0;
      Set<Integer> usedOrders = new HashSet<>();

    for (Entry<String, Object> entry : subCategories.entrySet()) {
      String catID = entry.getKey();

      Map<String, Object> catData = (Map<String, Object>) entry.getValue();
      String catName = (String) catData.get(YAML_CAT_NAME_FIELD);

      // If no "order" value, use the next sequential value. If duplicate, increment.
      Integer order = null;
      try {
          order = (Integer) catData.get(YAML_CAT_ORDER_FIELD);
      } catch (Exception e) { /* ignore */ }
      order = (order==null) ? nextOrder : order;
      while (usedOrders.contains(order)) { order += 1; }
      usedOrders.add(order);
      nextOrder = order + 1;
      
      // If no "nonassignable" value, inherit from parent. Defaults to 'false'.
      Object field = catData.get(YAML_CAT_NON_ASSIGNABLE_FIELD);
      boolean nonAssignable = parent.isNonAssignable();
      try {
          if (field != null) {
              nonAssignable = (Boolean)field;
          }
      } catch (Exception ex) {
          // ignore.
      }
      Category cat = addCategory(taxonomy, parent, catID, catName, order, nonAssignable);
      Object children = catData.get(YAML_CAT_CHILDREN_FIELD);
      if (children != null) {
        parseYamlData(taxonomy, (Map<String, Object>) children, cat);
      }
    }
  }

  private static void print(Category cat) {
    Iterable<Category> children = cat.getChildren();
    if (children != null) {
      for (Category c : children) {
        print(c);
      }
    }
  }

}
