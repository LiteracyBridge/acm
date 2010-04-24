package org.literacybridge.acm.demo;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.resourcebundle.LabelProvider.KeyValuePair;

public class DemoPrintRepository {
	public static void main(String args[]) throws Exception {
		Persistence.initialize();
		DemoRepository demo = new DemoRepository();
		IDataRequestResult result = demo.getDataRequestResult();

		System.out.println("================================================================");
		System.out.println("Taxonomy:");
		System.out.println("================================================================");
		System.out.println();
		printTaxonomy(Locale.ENGLISH, result);
		
		
		System.out.println("================================================================");
		System.out.println("Output in GERMAN locale:");
		System.out.println("================================================================");
		System.out.println();
		print(Locale.GERMAN, result);
		//printFacetCounts(Locale.GERMAN, result);
		
		System.out.println();
		System.out.println();
		System.out.println("================================================================");
		System.out.println("Output in ENGLISH locale:");
		System.out.println("================================================================");
		System.out.println();
		print(Locale.ENGLISH, result);
		//printFacetCounts(Locale.ENGLISH, result);
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
			
			Iterator<KeyValuePair<MetadataField<?>, String>> fieldsIterator = LabelProvider.getMetaFieldLabelsIterator(locale);
			while (fieldsIterator.hasNext()) {
				KeyValuePair<MetadataField<?>, String> field = fieldsIterator.next();
				String valueList = getCommaSeparatedList(metadata, field.getKey());
				if (valueList != null) {
					System.out.print(field.getValue() + " = " + valueList + "  \t");
				}
			}
			
			System.out.println();
//			System.out.println("title = " + metadata.getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue()
//					+ "\t\tcreator = " + metadata.getMetadataValues(MetadataSpecification.DC_CREATOR).get(0).getValue()
//					+ "\t\tlang = " + metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE).get(0).getValue()
//					+ "\t\tcategories = " + cats.toString());
		}
		
	}
	
	private static <T> String getCommaSeparatedList(Metadata metadata, MetadataField<T> field) {
		StringBuilder builder = new StringBuilder();
		List<MetadataValue<T>> fieldValues = metadata.getMetadataValues(field);
		if (fieldValues != null) {
			for (int i = 0; i < fieldValues.size(); i++) {
				builder.append(fieldValues.get(i).getValue());
				if (i != fieldValues.size() - 1) {
					builder.append(", ");
				}
			}
			return builder.toString();
		} else {
			return null;
		}
	}
	
	public static void printFacetCounts(Locale locale, IDataRequestResult result) {
		Category cat = result.getRootCategory();
		if (cat.hasChildren()) {
			printChildren(result, cat, locale, 0, true);
		}
		System.out.println();
	}
	
	private static void printChildren(IDataRequestResult result, Category cat, Locale locale, int depth, boolean printCount) {
		for (Category child : cat.getChildren()) {
			for (int i = 0; i < depth; i++) {
				System.out.print('\t');
			}
			System.out.print(child.getCategoryName(locale));
			if (printCount) {
				System.out.println(" (" + result.getFacetCount(child) + ")");
			} else {
				System.out.println();	
			}
			if (child.hasChildren()) {
				printChildren(result, child, locale, depth+1, printCount);
			}
		}
		
	}

	public static void printTaxonomy(Locale locale, IDataRequestResult result) {
		Category cat = result.getRootCategory();
		if (cat.hasChildren()) {
			printChildren(result, cat, locale, 0, false);
		}
		System.out.println();
	}
}
