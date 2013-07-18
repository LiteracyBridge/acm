package org.literacybridge.acm.categories;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.db.PersistentCategory;
import org.yaml.snakeyaml.Yaml;

public class DefaultLiteracyBridgeTaxonomy {
	public final static String LB_TAXONOMY_UID = "LB_TAX_1.0";
	
	public final static String YAML_FILE_NAME = "lb_taxonomy.yaml";
	public final static String YAML_REVISION_FIELD = "revision";
	public final static String YAML_CATEGORIES_FIELD = "categories";
	public final static String YAML_TAXONOMY_FIELD = "taxonomy";
	public final static String YAML_CAT_NAME_FIELD = "name";
	public final static String YAML_CAT_ORDER_FIELD = "order";
	public final static String YAML_CAT_DESC_FIELD = "description";
	public final static String YAML_CAT_CHILDREN_FIELD = "children";

	public static class TaxonomyRevision {
		public final int revision;
		private final Map<String, Object> categories;
		
		private TaxonomyRevision(int revision, Map<String, Object> categories) {
			this.revision = revision;
			this.categories = categories;
		}
		
		public void createTaxonomy(Taxonomy taxonomy, final Map<String, PersistentCategory> existingCategories) {
			DefaultLiteracyBridgeTaxonomy.loadYaml(taxonomy, categories, taxonomy.getRootCategory(), existingCategories);
		}
	}
	
	private static Category addCategory(Taxonomy taxonomy, Category parent, PersistentCategory existingCategory, String id, String name, String desc, int order) {
        Category cat = existingCategory == null ? new Category(id) : new Category(existingCategory);
        cat.setDefaultCategoryDescription(name, desc);
        cat.setOrder(order);
        taxonomy.addChild(parent, cat);
        return cat;
	}
	
	public static void main(String[] args) throws Exception {
		Taxonomy taxonomy = new Taxonomy();
		loadLatestTaxonomy().createTaxonomy(taxonomy, null);
		Category root = taxonomy.getRootCategory();
		print(root);
	}
	
	public static TaxonomyRevision loadLatestTaxonomy() {
		TaxonomyRevision taxonomy = loadTaxonomy(DefaultLiteracyBridgeTaxonomy.class.getResourceAsStream("/" + YAML_FILE_NAME));
		
		// check if there is a newer one in the 
		File userFile = new File(Configuration.getACMDirectory(), YAML_FILE_NAME);
		if (userFile.exists()) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(userFile);
				TaxonomyRevision updatedTaxonomy = loadTaxonomy(in);
				if (updatedTaxonomy.revision > taxonomy.revision) {
					taxonomy = updatedTaxonomy;
				}
			} catch (Exception e) {
				// ignore and use the packaged taxonomy
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		
		// check online for latest taxonomy
		try {
			URL url = new URL("http://literacybridge.googlecode.com/svn/acm/trunk/core/src/main/resources/lb_taxonomy.yaml");
			InputStream in = url.openStream();
			TaxonomyRevision updatedTaxonomy = loadTaxonomy(in);
			if (updatedTaxonomy.revision > taxonomy.revision) {
				taxonomy = updatedTaxonomy;
			}
		} catch (Exception e) {
			// maybe there is no internet connection - go with previous taxonomy
		}		
		
		return taxonomy;
		
	}
	
	private static TaxonomyRevision loadTaxonomy(InputStream input) {
		try {
		    Yaml yaml = new Yaml();
		    Map<String, Object> data = (Map<String, Object>) yaml.load(input);
		    Map<String, Object> taxonomyMap = (Map<String, Object>) data.get(YAML_TAXONOMY_FIELD);
		    int revision = (Integer) taxonomyMap.get(YAML_REVISION_FIELD);
		    Map<String, Object> categories = (Map<String, Object>) taxonomyMap.get(YAML_CATEGORIES_FIELD);
		    return new TaxonomyRevision(revision, categories);
		} finally {
		    try {
		    	input.close();
		    } catch (IOException e) {
		    	// ignore - at least we tried
		    }
		}
	}
	
	private static void loadYaml(Taxonomy taxonomy, Map<String, Object> categories, Category parent, final Map<String, PersistentCategory> existingCategories) {
	    for (Entry<String, Object> entry : categories.entrySet()) {
	    	String catID = entry.getKey();
	    	
	    	PersistentCategory existingCategory = null;
	    	if (existingCategories != null) {
	    		existingCategory =  existingCategories.get(catID);
	    	}
	    	Map<String, Object> catData = (Map<String, Object>) entry.getValue();
	    	String catName = (String) catData.get(YAML_CAT_NAME_FIELD);
	    	String catDesc = (String) catData.get(YAML_CAT_DESC_FIELD);
	    	Integer order = (Integer) catData.get(YAML_CAT_ORDER_FIELD);
	    	if (catDesc == null) {
	    		catDesc = "";
	    	}
	    	Category cat = addCategory(taxonomy, parent, existingCategory, catID, catName, catDesc, order);
	    	Object children = catData.get(YAML_CAT_CHILDREN_FIELD);
	    	if (children != null) {
	    		loadYaml(taxonomy, (Map<String, Object>) children, cat, existingCategories);
	    	}
	    }
	}
	
	public static void print(Category cat) {
		List<Category> children = cat.getChildren();
		if (children != null) {
			for (Category c : children) {
				print(c);
			}
		}
	}
	
}
