package org.literacybridge.acm.demo;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;

public class DemoPrintRepository {
	public static void main(String args[]) throws Exception {
		Persistence.initialize();
		DemoRepository demo = new DemoRepository();
		IDataRequestResult result = demo.getDataRequestResult();
		
		System.out.println("================================================================");
		System.out.println("Output in GERMAN locale:");
		System.out.println("================================================================");
		System.out.println();
		print(Locale.GERMAN, result);
		printFacetCounts(Locale.GERMAN, result);
		
		System.out.println();
		System.out.println();
		System.out.println("================================================================");
		System.out.println("Output in ENGLISH locale:");
		System.out.println("================================================================");
		System.out.println();
		print(Locale.ENGLISH, result);
		printFacetCounts(Locale.ENGLISH, result);
	}
	
	private static void print(Locale locale, IDataRequestResult result) throws Exception {
		List<AudioItem> audioItems = result.getAudioItems();
		for (AudioItem item : audioItems) {
			StringBuilder cats = new StringBuilder();
			List<Category> categories = item.getCategoryList();
			for (Category c : categories) {
				cats.append(c.getCategoryName(locale));
				cats.append(", ");
			}
			
			LocalizedAudioItem localizedItem = item.getLocalizedAudioItem(locale);
			Metadata metadata = localizedItem.getMetadata();
			System.out.println("title = " + metadata.getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue()
					+ "\t\tcreator = " + metadata.getMetadataValues(MetadataSpecification.DC_CREATOR).get(0).getValue()
					+ "\t\tlang = " + metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE).get(0).getValue()
					+ "\t\tcategories = " + cats.toString());
		}
		
	}
	
	public static void printFacetCounts(Locale locale, IDataRequestResult result) {
		Category cat = result.getRootCategory();
		if (cat.hasChildren()) {
			printChildren(result, cat, locale, 0);
		}
		System.out.println();
	}
	
	private static void printChildren(IDataRequestResult result, Category cat, Locale locale, int depth) {
		for (Category child : cat.getChildren()) {
			for (int i = 0; i < depth; i++) {
				System.out.print('\t');
			}
			System.out.println(child.getCategoryName(locale) + " (" + result.getFacetCount(child) + ")");
			if (child.hasChildren()) {
				printChildren(result, child, locale, depth+1);
			}
		}
		
	}

}
