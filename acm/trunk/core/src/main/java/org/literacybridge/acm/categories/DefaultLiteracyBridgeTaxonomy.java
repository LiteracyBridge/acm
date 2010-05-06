package org.literacybridge.acm.categories;

import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.db.Persistence;

public class DefaultLiteracyBridgeTaxonomy {
/*
 
 Agriculture
	Crops
		Maize
		Millet
		Beans
		Groundnuts
		Rice
	Livestock
		Chickens/Fowl
		Sheep
		Goats
		Cows
		Donkeys
		
 Health
	HIV/Aids
	Malaria
	Tuberculosis
	Other infectious diseases
	Antenatal care
	Neonatal care
	Hygiene and sanitation
	Family planning

 Stories
	spoken books
	oral stories
	
 Education
	Adult literacy
	School lessons
		Pre-Kindergarten
		Kindergarten
		1st year
		2nd year
		...
		13th year
 	Science
	Math
	Business
	environmental studies
	social studies
	Languages
		English
		Dagaare
		Sisaala

 Business
	accounting
	marketing
	entrepreneurship
	finance

 Governance

 Music

 Diary

 Other messages 
 
 */
	
	public final static String LB_TAXONOMY_UID = "LB_TAX_1.0";
	
	public static void createTaxonomy(Taxonomy taxonomy) {
		Category root = taxonomy.getRootCategory();
		
		// top level categories
		Category other = addCategory(taxonomy, root, "0", "Other Messages", "");
		Category agriculture = addCategory(taxonomy, root, "1", "Agriculture", "");
		Category health = addCategory(taxonomy, root, "2", "Health", "");
		Category education = addCategory(taxonomy, root, "3", "Education", "");
		Category stories = addCategory(taxonomy, root, "4", "Stories", "");
		Category business = addCategory(taxonomy, root, "5", "Business", "");
		Category governance = addCategory(taxonomy, root, "6", "Governance", "");
		Category music = addCategory(taxonomy, root, "7", "Music", "");
		Category diary = addCategory(taxonomy, root, "8", "Diary", "");
		

		// agriculture
		Category crops = addCategory(taxonomy, agriculture, "1-1", "Crops", "");
			Category maize = addCategory(taxonomy, crops, "1-1-1", "Maize", "");
			Category millet = addCategory(taxonomy, crops, "1-1-2", "Millet", "");
			Category beans = addCategory(taxonomy, crops, "1-1-3", "Beans", "");
			Category groundnuts = addCategory(taxonomy, crops, "1-1-4", "Groundnuts", "");
			Category rice = addCategory(taxonomy, crops, "1-1-5", "Rice", "");
			
		Category livestock = addCategory(taxonomy, agriculture, "1-2", "Livestock", "");
			Category chickens = addCategory(taxonomy, livestock, "1-2-1", "Chickens/Fowl", "");
			Category sheep = addCategory(taxonomy, livestock, "1-2-2", "Sheep", "");
			Category goats = addCategory(taxonomy, livestock, "1-2-3", "Goats", "");
			Category cows = addCategory(taxonomy, livestock, "1-2-4", "Cows", "");
			Category donkeys = addCategory(taxonomy, livestock, "1-2-5", "Donkeys", "");
		
		// health
		Category hiv = addCategory(taxonomy, health, "2-1", "HIV/Aids", "");
		Category malaria = addCategory(taxonomy, health, "2-2", "Malaria", "");
		Category tuberculosis = addCategory(taxonomy, health, "2-3", "Tuberculosis", "");			
		Category other_infectious = addCategory(taxonomy, health, "2-4", "Other infectious diseases", "");
		Category antenatal = addCategory(taxonomy, health, "2-5", "Antenatal care", "");
		Category neonatal = addCategory(taxonomy, health, "2-6", "Neonatal care", "");
		Category hygiene = addCategory(taxonomy, health, "2-7", "Hygiene and sanitation", "");
		Category family = addCategory(taxonomy, health, "2-8", "Family planning", "");
		

		// education
		Category adult = addCategory(taxonomy, education, "3-1", "Adult literacy", "");
		Category school = addCategory(taxonomy, education, "3-2", "School Lessons", "");
			Category preKindergarten = addCategory(taxonomy, school, "3-2-1", "Pre-Kindergarten", "");
			Category kindergarten = addCategory(taxonomy, school, "3-2-2", "Kindergarten", "");
			addCategory(taxonomy, school, "3-2-3", "1st year", "");
			addCategory(taxonomy, school, "3-2-4", "2nd year", "");
			addCategory(taxonomy, school, "3-2-5", "3rd year", "");
			for (int i = 4; i <= 13; i++) {
				addCategory(taxonomy, school, "3-2-" + (i + 2), i + "th year", "");
			}
		Category science = addCategory(taxonomy, education, "3-3", "Science", "");
		Category math = addCategory(taxonomy, education, "3-4", "Math", "");
		Category scienceBusiness = addCategory(taxonomy, education, "3-5", "Business", "");
		Category environmental = addCategory(taxonomy, education, "3-6", "Environmental studies", "");
		Category social = addCategory(taxonomy, education, "3-7", "Social studies", "");
		Category languages = addCategory(taxonomy, education, "3-8", "Languages", "");
			Category english = addCategory(taxonomy, languages, "3-8-1", "English", "");
			Category dagaare = addCategory(taxonomy, languages, "3-8-2", "Dagaare", "");
			Category sisaala = addCategory(taxonomy, languages, "3-8-3", "Sisaala", "");

		// stories
		Category spoken = addCategory(taxonomy, stories, "4-1", "Spoken books", "");
		Category oral = addCategory(taxonomy, stories, "4-2", "Oral stories", "");

			
		// business
		Category accounting = addCategory(taxonomy, business, "5-1", "Accounting", "");
		Category marketing = addCategory(taxonomy, business, "5-2", "Marketing", "");
		Category entrepreneurship = addCategory(taxonomy, business, "5-3", "Entrepreneurship", "");			
		Category finance = addCategory(taxonomy, business, "5-4", "Finance", "");
	}
	
	private static Category addCategory(Taxonomy taxonomy, Category parent, String id, String name, String desc) {
        Category cat = new Category(id);
        cat.setDefaultCategoryDescription(name, desc);
        taxonomy.addChild(parent, cat);
        return cat;
	}
	
	public static void main(String[] args) throws Exception {
		Persistence.initialize();
		Taxonomy tax = Taxonomy.getTaxonomy();
		Category root = tax.getRootCategory();
		print(root);
		
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
