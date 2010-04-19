package org.literacybridge.acm.categories;

import org.literacybridge.acm.categories.Taxonomy.Category;

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
		Category agriculture = addCategory(taxonomy, root, "lb.agr", "Agriculture", "");
		Category health = addCategory(taxonomy, root, "lb.hea", "Health", "");
		Category stories = addCategory(taxonomy, root, "lb.sto", "Stories", "");
		Category education = addCategory(taxonomy, root, "lb.edu", "Education", "");
		Category business = addCategory(taxonomy, root, "lb.bus", "Business", "");
		Category governance = addCategory(taxonomy, root, "lb.gov", "Governance", "");
		Category music = addCategory(taxonomy, root, "lb.mus", "Music", "");
		Category diary = addCategory(taxonomy, root, "lb.dia", "Diary", "");
		Category other = addCategory(taxonomy, root, "lb.oth", "Other Messages", "");

		// agriculture
		Category crops = addCategory(taxonomy, agriculture, "lb.agr.cro", "Crops", "");
			Category maize = addCategory(taxonomy, crops, "lb.agr.cro.mai", "Maize", "");
			Category millet = addCategory(taxonomy, crops, "lb.agr.cro.mil", "Millet", "");
			Category beans = addCategory(taxonomy, crops, "lb.agr.cro.bea", "Beans", "");
			Category groundnuts = addCategory(taxonomy, crops, "lb.agr.cro.gro", "Groundnuts", "");
			Category rice = addCategory(taxonomy, crops, "lb.agr.cro.ric", "Rice", "");
			
		Category livestock = addCategory(taxonomy, agriculture, "lb.agr.liv", "Livestock", "");
			Category chickens = addCategory(taxonomy, livestock, "lb.agr.liv.chi", "Chickens/Fowl", "");
			Category sheep = addCategory(taxonomy, livestock, "lb.agr.liv.she", "Sheep", "");
			Category goats = addCategory(taxonomy, livestock, "lb.agr.liv.goa", "Goats", "");
			Category cows = addCategory(taxonomy, livestock, "lb.agr.liv.cow", "Cows", "");
			Category donkeys = addCategory(taxonomy, livestock, "lb.agr.liv.don", "Donkeys", "");
		
		// health
		Category hiv = addCategory(taxonomy, health, "lb.hea.hiv", "HIV/Aids", "");
		Category malaria = addCategory(taxonomy, health, "lb.hea.mal", "Malaria", "");
		Category tuberculosis = addCategory(taxonomy, health, "lb.hea.tub", "Tuberculosis", "");			
		Category other_infectious = addCategory(taxonomy, health, "lb.hea.oth", "Other infectious diseases", "");
		Category antenatal = addCategory(taxonomy, health, "lb.hea.ant", "Antenatal care", "");
		Category neonatal = addCategory(taxonomy, health, "lb.hea.neo", "Neonatal care", "");
		Category hygiene = addCategory(taxonomy, health, "lb.hea.hyg", "Hygiene and sanitation", "");
		Category family = addCategory(taxonomy, health, "lb.hea.fam", "Family planning", "");
		
		// stories
		Category spoken = addCategory(taxonomy, stories, "lb.sto.spo", "Spoken books", "");
		Category oral = addCategory(taxonomy, stories, "lb.sto.ora", "Oral stories", "");

		// education
		Category adult = addCategory(taxonomy, education, "lb.edu.adu", "Adult literacy", "");
		Category school = addCategory(taxonomy, education, "lb.edu.sch", "School Lessons", "");
			Category preKindergarten = addCategory(taxonomy, school, "lb.edu.sch.pre", "Pre-Kindergarten", "");
			Category kindergarten = addCategory(taxonomy, school, "lb.edu.sch.kin", "Kindergarten", "");
			addCategory(taxonomy, school, "lb.edu.sch.y01", "1st year", "");
			addCategory(taxonomy, school, "lb.edu.sch.y02", "2nd year", "");
			addCategory(taxonomy, school, "lb.edu.sch.y03", "3rd year", "");
			for (int i = 4; i <= 13; i++) {
				String id = "y" + (i < 10 ? "0" + i : "" + i); 
				addCategory(taxonomy, school, "lb.edu.sch." + id, i + "th year", "");
			}
		Category science = addCategory(taxonomy, education, "lb.edu.sci", "Science", "");
		Category math = addCategory(taxonomy, education, "lb.edu.mat", "Math", "");
		Category scienceBusiness = addCategory(taxonomy, education, "lb.edu.bus", "Business", "");
		Category environmental = addCategory(taxonomy, education, "lb.edu.env", "Environmental studies", "");
		Category social = addCategory(taxonomy, education, "lb.edu.soc", "social studies", "");
		Category languages = addCategory(taxonomy, education, "lb.edu.lan", "Languages", "");
			Category english = addCategory(taxonomy, languages, "lb.edu.lan.eng", "English", "");
			Category dagaare = addCategory(taxonomy, languages, "lb.edu.lan.dag", "Dagaare", "");
			Category sisaala = addCategory(taxonomy, languages, "lb.edu.lan.sis", "Sisaala", "");

		// business
		Category accounting = addCategory(taxonomy, business, "lb.bus.acc", "accounting", "");
		Category marketing = addCategory(taxonomy, business, "lb.bus.mar", "marketing", "");
		Category entrepreneurship = addCategory(taxonomy, business, "lb.bus.ent", "entrepreneurship", "");			
		Category finance = addCategory(taxonomy, business, "lb.bus.fin", "finance", "");
	}
	
	private static Category addCategory(Taxonomy taxonomy, Category parent, String id, String name, String desc) {
        Category cat = new Category(id);
        cat.setDefaultCategoryDescription(name, desc);
        taxonomy.addChild(parent, cat);
        return cat;
	}
	
}
