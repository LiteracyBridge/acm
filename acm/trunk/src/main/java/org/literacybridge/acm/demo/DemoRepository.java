package org.literacybridge.acm.demo;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.core.DataRequestResult;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;

public class DemoRepository {
	private Taxonomy taxonomy;
	private int[] facetCountArray;
	
	private List<AudioItem> audioItems = new LinkedList<AudioItem>();
	
	
	public DemoRepository() {
		createSampleTaxonomy();
		createSampleAudioItems();
		createFacetCounts();
	}
	
	public IDataRequestResult getDataRequestResult() {
		
		IDataRequestResult result = new DataRequestResult(taxonomy, facetCountArray, audioItems);
		return result;
	}
	
	private void createSampleTaxonomy() {
		// first create a taxonomy of categories
		this.taxonomy = new Taxonomy();
		
		// first setup health category
		Category root = taxonomy.getRootCategory();
		
		Category health = new Category(1);
		health.setCategoryDescription(Locale.ENGLISH, "Health", "This is the top level category for health-related content.");
		health.setCategoryDescription(Locale.GERMAN, "Gesundheit", "Diese Hauptkategory enthaelt Inhalte zum Thema Gesundheit.");
		taxonomy.addChild(root, health);
		
		Category agriculture = new Category(2);
		agriculture.setCategoryDescription(Locale.ENGLISH, "Agriculture", "This is the top level category for agriculture-related content.");
		agriculture.setCategoryDescription(Locale.GERMAN, "Agrarwirtschaft", "Diese Hauptkategory enthaelt Inhalte zum Thema Agrarwirtschaft.");
		taxonomy.addChild(root, agriculture);
		
		Category health_hiv = new Category(3);
		health_hiv.setCategoryDescription(Locale.ENGLISH, "HIV", "Content about HIV/AIDS.");
		health_hiv.setCategoryDescription(Locale.GERMAN, "HIV", "Diese Kategory enthaelt Inhalte zum Thema HIV/AIDS.");
		taxonomy.addChild(health, health_hiv);
		
		Category health_malaria = new Category(4);
		health_malaria.setCategoryDescription(Locale.ENGLISH, "Malaria", "Content about Malaria.");
		health_malaria.setCategoryDescription(Locale.GERMAN, "Malaria", "Diese Kategory enthaelt Inhalte zum Thema Malaria.");
		taxonomy.addChild(health, health_malaria);
		
		Category agriculture_crops = new Category(5);
		agriculture_crops.setCategoryDescription(Locale.ENGLISH, "Crops", "Content about growing crops.");
		agriculture_crops.setCategoryDescription(Locale.GERMAN, "Getreide", "Diese Kategory enthaelt Inhalte zum Thema Getreideanbau.");
		taxonomy.addChild(agriculture, agriculture_crops);
	}
	
	private void createSampleAudioItems() {
		Random rnd = new Random(1);

		AudioItem[] items = new AudioItem[] {
				new AudioItem("1-1"),  // from device 1, recording 1
				new AudioItem("1-2"),  // from device 1, recording 2
				new AudioItem("3-5"),  
				new AudioItem("4-4"),  
				new AudioItem("4-5"),  
				new AudioItem("8-1"),  
				new AudioItem("8-7"),
				new AudioItem("8-9")
		};
		
		int i = 1;
		for (AudioItem item : items) {
			LocalizedAudioItem germanItem = new LocalizedAudioItem(item.getUUId() + "-de", Locale.GERMAN, null);
			item.addLocalizedAudioItem(Locale.GERMAN, germanItem);
			Metadata germanMetadata = germanItem.getMetadata();
			germanMetadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>("Deutscher Titel " + i));
			germanMetadata.addMetadataField(MetadataSpecification.DC_CREATOR, new MetadataValue<String>("Michael Busch"));
			germanMetadata.addMetadataField(MetadataSpecification.DC_LANGUAGE, 
					new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode("de")));
			
			LocalizedAudioItem englishItem = new LocalizedAudioItem(item.getUUId() + "-en", Locale.ENGLISH, null);
			item.addLocalizedAudioItem(Locale.ENGLISH, englishItem);
			Metadata englishMetadata = englishItem.getMetadata();
			englishMetadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>("English title " + i));
			englishMetadata.addMetadataField(MetadataSpecification.DC_CREATOR, new MetadataValue<String>("Cliff Schmidt"));
			englishMetadata.addMetadataField(MetadataSpecification.DC_LANGUAGE, 
					new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode("en")));
			
			int catID = 3 + rnd.nextInt(3);
			item.addCategory(taxonomy.getCategory(catID));
			
			this.audioItems.add(item);
			i++;
		}
		
	}
	
	private void createFacetCounts() {
		int maxCatID = this.taxonomy.getMaxCategoryID();
		this.facetCountArray = new int[maxCatID + 1];
		
		for (AudioItem item : this.audioItems) {
			Set<Category> categories = item.getCategories();
			for (Category c : categories) {
				do {
					this.facetCountArray[c.getID()]++;
					c = c.getParent();
				} while (c != null);
			}
		}
	}
	
}
