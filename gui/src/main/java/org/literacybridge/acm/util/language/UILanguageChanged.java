package org.literacybridge.acm.util.language;

import java.util.Locale;

public class UILanguageChanged {

	private Locale newLocale;
	private Locale oldLocale;
	
	public UILanguageChanged(Locale newLocale, Locale oldLocale) {
		this.newLocale = newLocale;
		this.oldLocale = oldLocale;
	}

	public Locale getNewLocale() {
		return newLocale;
	}

	public Locale getOldLocale() {
		return oldLocale;
	}
}
