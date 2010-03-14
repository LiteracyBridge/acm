package org.literacybridge.acm.rcp.util;

import java.util.Locale;

public class LanguageUtil {

	// current default language for itmes shown in the UI
	private static Locale defaultLanguage = Locale.ENGLISH;
	
	public static Locale GetCurrentLanguage() {
		return defaultLanguage;
	}
	
	public static void setCurrentLanguage(Locale newDefaultLanguage) {
		defaultLanguage = newDefaultLanguage;
	}
	
}
