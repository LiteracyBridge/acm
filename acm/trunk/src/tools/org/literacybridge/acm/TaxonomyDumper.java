package org.literacybridge.acm;

import java.util.Locale;

import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.categories.Taxonomy.Category;

public class TaxonomyDumper {
	public static void printTaxonomy(Taxonomy taxonomy, Locale locale) {
		Category cat = taxonomy.getRootCategory();
		if (cat.hasChildren()) {
			printChildren(cat, locale, 0);
		}
		System.out.println();
	}
	
	private static void printChildren(Category cat, Locale locale, int depth) {
		for (Category child : cat.getChildren()) {
			for (int i = 0; i < depth; i++) {
				System.out.print('\t');
			}
			System.out.println(child.getCategoryName(locale));
			if (child.hasChildren()) {
				printChildren(child, locale, depth+1);
			}
		}
		
	}
}
