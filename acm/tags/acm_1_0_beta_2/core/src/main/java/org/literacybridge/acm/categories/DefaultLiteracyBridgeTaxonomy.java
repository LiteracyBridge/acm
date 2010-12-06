package org.literacybridge.acm.categories;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.yaml.snakeyaml.Yaml;

public class DefaultLiteracyBridgeTaxonomy {
	public final static String LB_TAXONOMY_UID = "LB_TAX_1.0";
	
	public final static String YAML_FILE_NAME = "lb_taxonomy.yaml";
	public final static String YAML_CAT_NAME_FIELD = "name";
	public final static String YAML_CAT_DESC_FIELD = "description";
	public final static String YAML_CAT_CHILDREN_FIELD = "children";

	private static Category addCategory(Taxonomy taxonomy, Category parent, String id, String name, String desc) {
        Category cat = new Category(id);
        cat.setDefaultCategoryDescription(name, desc);
        taxonomy.addChild(parent, cat);
        return cat;
	}
	
	public static void main(String[] args) throws Exception {
		Taxonomy taxonomy = new Taxonomy();
		createTaxonomy(taxonomy);
		Category root = taxonomy.getRootCategory();
		print(root);
	}
	
	public static void createTaxonomy(Taxonomy taxonomy) {
		InputStream input = DefaultLiteracyBridgeTaxonomy.class.getResourceAsStream("/" + YAML_FILE_NAME);
		try {
		    Yaml yaml = new Yaml();
		    loadYaml(taxonomy, yaml.load(input), taxonomy.getRootCategory());
		} finally {
		    try {
		    	input.close();
		    } catch (IOException e) {
		    	// ignore - at least we tried
		    }
		}
	}
	
	private static void loadYaml(Taxonomy taxonomy, Object o, Category parent) {
		Map<String, Object> data = (Map<String, Object>) o;
	    for (Entry<String, Object> entry : data.entrySet()) {
	    	String catID = entry.getKey();
	    	Map<String, Object> catData = (Map<String, Object>) entry.getValue();
	    	String catName = (String) catData.get(YAML_CAT_NAME_FIELD);
	    	String catDesc = (String) catData.get(YAML_CAT_DESC_FIELD);
	    	if (catDesc == null) {
	    		catDesc = "";
	    	}
	    	Category cat = addCategory(taxonomy, parent, catID, catName, catDesc);
	    	Object children = catData.get(YAML_CAT_CHILDREN_FIELD);
	    	if (children != null) {
	    		loadYaml(taxonomy, children, cat);
	    	}
	    }
	}
	
	private static void print(Category cat) {
		System.out.println(cat.getUuid() + " : " + cat.getCategoryName(Locale.ENGLISH));
		List<Category> children = cat.getChildren();
		if (children != null) {
			for (Category c : children) {
				print(c);
			}
		}
	}
	
}
