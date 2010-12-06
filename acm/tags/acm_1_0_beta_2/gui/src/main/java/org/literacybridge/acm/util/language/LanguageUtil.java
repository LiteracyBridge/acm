package org.literacybridge.acm.util.language;

import java.util.Locale;

import org.literacybridge.acm.categories.Taxonomy.Category;

public class LanguageUtil {

	// current default language for items shown in the UI
	private static Locale defaultLanguage = Locale.ENGLISH;
	
	// fallback language if default language is not available for a certain item
	private static Locale fallbackLanguage = Locale.ENGLISH;
	
	// UI language
	private static Locale uiLanguage = Locale.ENGLISH;
	
	// The language that the user has chosen to show the name of a !LocalizedAudioItems!
	// DO NOT USE FOR UI CONTROLS
	public static Locale getUserChoosenLanguage() {
		return defaultLanguage;
	}
	
	public static void setUserChoosenLanguage(Locale newDefaultLanguage) {
		defaultLanguage = newDefaultLanguage;
	}

	public static Locale GetFallbackLanguage() {
		return fallbackLanguage;
	}
	
	public static void setFallbackLanguage(Locale newFallbackLanguage) {
		fallbackLanguage = newFallbackLanguage;
	}
	
	public static String getBestMatchingLocalizedLabel(Category category, Locale wish) {
		// here we must check first if the default language is available !!	
		return category.getCategoryName(wish).getLabel();
	}
	
	public static Locale getUILanguage() {
		return uiLanguage;
	}
	
	public static void setUILanguage(Locale newUILocale) {
		uiLanguage = newUILocale;
	}
	/**
	 * Get 'ISO 3166-1 alpha-2' country codes for a language.
	 * 
	 * This is only a simple mapping!! example: Return 'US' flag for a English language
	 * 
	 * @param local
	 * @return
	 */
	public static String getCountryCodeForLanguage(Locale local) {
		String str = null;
		
		if (Locale.GERMAN == local) {
			return "de";
		} else if (Locale.ENGLISH == local) {
			return "en";
		} else if (Locale.FRENCH == local) {
			return "fr";
		}
		
		return str;
	}
}
