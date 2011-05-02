package org.literacybridge.acm.demo;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.core.DataRequestResult;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;

public class DemoRepository {
	private Taxonomy taxonomy;
	private int[] facetCountArray;
	
	private List<AudioItem> audioItems = new LinkedList<AudioItem>();
	
	
	public DemoRepository() {
		createSampleTaxonomy();
		createSampleAudioItems();
//		createFacetCounts();
	}
	
	public IDataRequestResult getDataRequestResult() {
		
		IDataRequestResult result = new DataRequestResult(taxonomy.getRootCategory(), null, null, audioItems);
		return result;
	}
	
	private void createSampleTaxonomy() {
		// first create a taxonomy of categories
                this.taxonomy = Taxonomy.getTaxonomy();
                
//                if (this.taxonomy.getId() == null) {
//		
//                    // first setup health category
//                    Category root = taxonomy.getRootCategory();
//                    
//                    Category health = new Category("1");
//                    health.setDefaultCategoryDescription("Health", "This is the top level category for health-related content.");
//                    health.setLocalizedCategoryDescription(Locale.GERMAN, "Gesundheit", "Diese Hauptkategory enthaelt Inhalte zum Thema Gesundheit.");
//                    taxonomy.addChild(root, health);
//                    
//                    Category agriculture = new Category("2");
//                    agriculture.setDefaultCategoryDescription("Agriculture", "This is the top level category for agriculture-related content.");
//                    agriculture.setLocalizedCategoryDescription(Locale.GERMAN, "Agrarwirtschaft", "Diese Hauptkategory enthaelt Inhalte zum Thema Agrarwirtschaft.");
//                    taxonomy.addChild(root, agriculture);
//                    
//                    Category health_hiv = new Category("3");
//                    health_hiv.setDefaultCategoryDescription("HIV", "Content about HIV/AIDS.");
//                    health_hiv.setLocalizedCategoryDescription(Locale.GERMAN, "HIV", "Diese Kategory enthaelt Inhalte zum Thema HIV/AIDS.");
//                    taxonomy.addChild(health, health_hiv);
//                    
//                    Category health_malaria = new Category("4");
//                    health_malaria.setDefaultCategoryDescription("Malaria", "Content about Malaria.");
//                    health_malaria.setLocalizedCategoryDescription(Locale.GERMAN, "Malaria", "Diese Kategory enthaelt Inhalte zum Thema Malaria.");
//                    taxonomy.addChild(health, health_malaria);
//                    
//                    Category agriculture_crops = new Category("5");
//                    agriculture_crops.setDefaultCategoryDescription("Crops", "Content about growing crops.");
//                    agriculture_crops.setLocalizedCategoryDescription(Locale.GERMAN, "Getreide", "Diese Kategory enthaelt Inhalte zum Thema Getreideanbau.");
//                    taxonomy.addChild(agriculture, agriculture_crops);
//                    
//                    taxonomy.commit();
//                }
	}
	
	private void createSampleAudioItems() {
//                if (AudioItem.getFromDatabase("1-1") != null) {
//                    return;
//                }

                Random rnd = new Random(1);
		AudioItem[] items = new AudioItem[] {
				new AudioItem("1-1"),  // from device 1, recording 1
				new AudioItem("1-2"),  // from device 1, recording 2
				new AudioItem("3-5"),  
				new AudioItem("4-4"),  
				new AudioItem("4-5"),  
				new AudioItem("8-1"),  
				new AudioItem("8-7"),
				new AudioItem("8-9"),
				new AudioItem("9-1"),  
				new AudioItem("9-7"),
				new AudioItem("9-9")
		};
		
		String[] titles_en = new String[] {
				"Improving maize using manure for fertilizer",
				"Preparing to sow groundnuts before the rainy season",
				"Vaccinate poultry to prevent disease", 
				"Why you should not burn your fields",
				"Picking the right time to sow beans",
				"The importance of colostrum to new born babies",
				"Recognizing symptoms of Malaria",
				"Using bed nets to prevent Malaria",
				"Myths about HIV/AIDS",
				"Understanding the national health insurance program",
				"Family member authority to send sick children to the hospital"	
		};

		String[] titles_de = new String[] {
				"Verbesserter Maisanbau mit Guelle als Duenger",
				"Vorbereitungen zum Erdnussanbau vor Beginn der Regenzeit",
				"Gefluegelimpfungen zur Krankheitsvorbeugung", 
				"Warum man die Felder nicht abbrennen sollte",
				"Die richtige Zeit um Bohnensamen zu saehen",
				"Die Bedeutung von Vormilch fuer Neugeborene",
				"Malaria-Symptome erkennnen",
				"Bettnetze zur Malariavorbeugung",
				"Geruechte ueber HIV/AIDS",
				"Das nationale Krankenversicherungprogramm verstehen",
				"Welche Rechte haben Familienmitglieder um Kinder in Krankenhaeuser zu bringen?"	
		};

		
		String[] authors = new String[] {
				"Zambo Kamingnyelle",
				"Zambo Kamingnyelle",
				"Bampuo Pogdaa",
				"Da-ure Fidelis Awonodomo",
				"Hamidu Lare",
				"Dery Anthony",
				"Irenaeus Suglo",
				"Kuubataanono Gervaise",
				"Dakurah Nuokye",
				"Dakurah Nuokye",
				"Dakurah Nuokye"
		};
		
		
		
		int i = 0;
		for (AudioItem item : items) {
			LocalizedAudioItem germanItem = new LocalizedAudioItem(item.getUuid() + "-de", Locale.GERMAN);
			item.addLocalizedAudioItem(germanItem);
			Metadata germanMetadata = germanItem.getMetadata();
			germanMetadata.setMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(titles_de[i]));
			germanMetadata.setMetadataField(MetadataSpecification.DC_CREATOR, new MetadataValue<String>(authors[i]));
		    germanMetadata.setMetadataField(MetadataSpecification.LB_COPY_COUNT, new MetadataValue<Integer>(1 + rnd.nextInt(50)));
			
			LocalizedAudioItem englishItem = new LocalizedAudioItem(item.getUuid() + "-en", Locale.ENGLISH);
			item.addLocalizedAudioItem(englishItem);
			Metadata englishMetadata = englishItem.getMetadata();
			englishMetadata.setMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(titles_en[i]));
			englishMetadata.setMetadataField(MetadataSpecification.DC_CREATOR, new MetadataValue<String>(authors[i]));
			englishMetadata.setMetadataField(MetadataSpecification.LB_COPY_COUNT, new MetadataValue<Integer>(1 + rnd.nextInt(50)));

			
                        List<Category> categoryList = taxonomy.getCategoryList();
                        int catID = rnd.nextInt(3);
			item.addCategory(categoryList.get(catID));
			
             //           item.commit();
                        
			//this.audioItems.add(item);
			i++;
		}
		
	}
	
	private void createFacetCounts() {
                int maxCategories = this.taxonomy.getCategoryList().size();
		this.facetCountArray = new int[maxCategories + 1];
		Random rnd = new Random();
		
		for (AudioItem item : this.audioItems) {
			List<Category> categories = item.getCategoryList();
			for (Category c : categories) {
				do {
					this.facetCountArray[rnd.nextInt(facetCountArray.length)]++;
					c = c.getParent();
				} while (c != null);
			}
		}
	}
	
}
