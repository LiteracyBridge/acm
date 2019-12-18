package org.literacybridge.acm.gui.util.language;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Category;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LanguageUtil {
  private static final Logger LOG = Logger
            .getLogger(LanguageUtil.class.getName());

  // current default language for items shown in the UI
  private static Locale defaultLanguage = Locale.ENGLISH;

  // fallback language if default language is not available for a certain item
  private static Locale fallbackLanguage = Locale.ENGLISH;

  // UI language
  private static Locale uiLanguage = Locale.ENGLISH;

  // The language that the user has chosen to show the name of a
  // !LocalizedAudioItems!
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

  public static String getBestMatchingLocalizedLabel(Category category,
      Locale wish) {
    return category.getCategoryName();
  }

  public static Locale getUILanguage() {
    return uiLanguage;
  }

  public static void setUILanguage(Locale newUILocale) {
    uiLanguage = newUILocale;
  }

  public static String getLocalizedLanguageName(Locale locale) {
    String label = ACMConfiguration.getInstance().getCurrentDB()
        .getLanguageLabel(locale);
    if (label == null) {
      if (locale == null) {
        LOG.log(Level.SEVERE, "Unexpected null value for locale");
        label = "--";
      } else {
        label = locale.getDisplayLanguage(getUILanguage());
      }
    }
    return label;
  }

  public static String getLanguageNameWithCode(Locale locale) {
      String label = getLocalizedLanguageName(locale);
      String iso639 = locale.getLanguage();
      if (StringUtils.isNotEmpty(iso639)) {
          label += " ("+iso639+")";
      }
      return label;
  }

}
