package org.literacybridge.acm.gui.resourcebundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.store.LBMetadataIDs;
import org.literacybridge.acm.store.MetadataField;

import com.google.common.collect.UnmodifiableIterator;

public class LabelProvider {

  public static final String CATEGORY_ROOT_LABEL = "CATEGORY_ROOT_LABEL";
  public static final String PLAYLIST_ROOT_LABEL = "PLAYLIST_ROOT_LABEL";
  public static final String NEW_PLAYLIST_LABEL = "NEW_PLAYLIST_LABEL";
  public static final String LANGUAGES_ROOT_LABEL = "LANGUAGES_ROOT_LABEL";
  public static final String DEVICES_ROOT_LABEL = "DEVICES_ROOT_LABEL";
  public static final String OPTIONS_ROOT_LABEL = "OPTIONS_ROOT_LABEL";
  public static final String OPTIONS_USER_LANGUAGE = "OPTIONS_USER_LANGUAGE";
  public static final String PLACEHOLDER_TEXT = "WATERMARK_SEARCH";

  public static final String AUDIO_ITEM_TABLE_COLUMN_TITLE = "AUDIO_ITEM_TABLE_COLUMN_TITLE";
  public static final String AUDIO_ITEM_TABLE_COLUMN_DURATION = "AUDIO_ITEM_TABLE_COLUMN_DURATION";
  public static final String AUDIO_ITEM_TABLE_COLUMN_CREATOR = "AUDIO_ITEM_TABLE_COLUMN_CREATOR";
  public static final String AUDIO_ITEM_TABLE_COLUMN_MESSAGE_FORMAT = "AUDIO_ITEM_TABLE_COLUMN_MESSAGE_FORMAT";
  public static final String AUDIO_ITEM_TABLE_COLUMN_LANGUAGE = "AUDIO_ITEM_TABLE_COLUMN_LANGUAGE";
  public static final String AUDIO_ITEM_TABLE_COLUMN_PLAYLIST_ORDER = "AUDIO_ITEM_TABLE_COLUMN_PLAYLIST_ORDER";
  public static final String AUDIO_ITEM_TABLE_COLUMN_COPY_COUNT = "AUDIO_ITEM_TABLE_COLUMN_COPY_COUNT";
  public static final String AUDIO_ITEM_TABLE_COLUMN_OPEN_COUNT = "AUDIO_ITEM_TABLE_COLUMN_OPEN_COUNT";
  public static final String AUDIO_ITEM_TABLE_COLUMN_COMPLETION_COUNT = "AUDIO_ITEM_TABLE_COLUMN_COMPLETION_COUNT";
  public static final String AUDIO_ITEM_TABLE_COLUMN_SURVEY1_COUNT = "AUDIO_ITEM_TABLE_COLUMN_SURVEY1_COUNT";
  public static final String AUDIO_ITEM_TABLE_COLUMN_APPLY_COUNT = "AUDIO_ITEM_TABLE_COLUMN_APPLY_COUNT";
  public static final String AUDIO_ITEM_TABLE_COLUMN_NOHELP_COUNT = "AUDIO_ITEM_TABLE_COLUMN_NOHELP_COUNT";
  public static final String AUDIO_ITEM_TABLE_COLUMN_CATEGORIES = "AUDIO_ITEM_TABLE_COLUMN_CATEGORIES";
  public static final String AUDIO_ITEM_TABLE_COLUMN_SOURCE = "AUDIO_ITEM_TABLE_COLUMN_SOURCE";
  public static final String AUDIO_ITEM_TABLE_COLUMN_DATE_FILE_MODIFIED = "AUDIO_ITEM_TABLE_COLUMN_DATE_FILE_MODIFIED";
  public static final String AUDIO_ITEM_TABLE_COLUMN_SDG_GOALS = "AUDIO_ITEM_TABLE_COLUMN_SDG_GOALS";
    public static final String AUDIO_ITEM_TABLE_COLUMN_SDG_TARGETS = "AUDIO_ITEM_TABLE_COLUMN_SDG_TARGETS";
    public static final String AUDIO_ITEM_TABLE_COLUMN_CONTENTID = "DC_IDENTIFIER";
    public static final String AUDIO_ITEM_TABLE_COLUMN_AUDIOITEMID = "Audio Item ID";
    public static final String AUDIO_ITEM_TABLE_COLUMN_FILENAME = "File Name";

  public static final class KeyValuePair<K, V> {
    private final K key;
    private final V value;

    public KeyValuePair(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }
  }

  private static final String BUNDLE_NAME = "labels";
  private static final String METAFIELD_PROPERTY_PREFIX = "METAFIELD_";

  private final static Map<Locale, ResourceBundle> bundles = new HashMap<>();

  private static ResourceBundle getResourceBundle(Locale locale) {
    ResourceBundle bundle = bundles.get(locale);
    if (bundle == null) {
      bundle = loadUTF8Bundle(BUNDLE_NAME, locale);
      bundles.put(locale, bundle);
    }
    return bundle;
  }

  public static String getLabel(MetadataField<?> field) {
    return getLabel(field, LanguageUtil.getUILanguage());
  }

  @Deprecated // Use the overload without locale
  public static String getLabel(MetadataField<?> field, Locale locale) {
    int fieldID = LBMetadataIDs.FieldToIDMap.get(field);
    String resourceName = METAFIELD_PROPERTY_PREFIX + fieldID;

    ResourceBundle bundle = getResourceBundle(locale);
    return bundle.getString(resourceName);
  }

  public static Iterator<KeyValuePair<MetadataField<?>, String>> getMetaFieldLabelsIterator() {
    return getMetaFieldLabelsIterator(LanguageUtil.getUILanguage());
  }

  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated // Use the overload without locale
  public static Iterator<KeyValuePair<MetadataField<?>, String>> getMetaFieldLabelsIterator(
      final Locale locale) {
    final Iterator<MetadataField<?>> fieldIterator = LBMetadataIDs.FieldToIDMap
        .keySet().iterator();
    return new UnmodifiableIterator<KeyValuePair<MetadataField<?>, String>>() {

      @Override
      public boolean hasNext() {
        return fieldIterator.hasNext();
      }

      @Override
      public KeyValuePair<MetadataField<?>, String> next() {
        MetadataField<?> field = fieldIterator.next();
        String value = getLabel(field, locale);
        return new KeyValuePair<>(field, value);
      }

    };
  }

  /**
   * Helper because we always pass LanguageUtil.getUILanguage() anyway...
   * @param propertyName Name of the label for which we want the value.
   * @return Value of the label.
   */
  public static String getLabel(String propertyName) {
     return getLabel(propertyName, LanguageUtil.getUILanguage());
  }

  @Deprecated // Use the overload without locale
  public static String getLabel(String propertyName, Locale locale) {
    try {
      return getResourceBundle(locale).getString(propertyName);
    } catch (MissingResourceException e) {
      try {
        return getResourceBundle(Locale.ENGLISH).getString(propertyName);
      } catch (MissingResourceException e1) {
        return propertyName;
      }
    }
  }

  public static void main(String[] args) {
    Iterator<KeyValuePair<MetadataField<?>, String>> it = getMetaFieldLabelsIterator(
        Locale.ENGLISH);
    while (it.hasNext()) {
      System.out.println(it.next().getValue());
    }
  }

  private static ResourceBundle loadUTF8Bundle(String bundleName,
      Locale locale) {
    return ResourceBundle.getBundle(bundleName, locale,
        new ResourceBundle.Control() {
          @Override
          public List<String> getFormats(String baseName) {
            if (baseName == null)
              throw new NullPointerException();
            return Collections.singletonList("properties");
          }

          @Override
          public ResourceBundle newBundle(String baseName, Locale locale,
              String format, ClassLoader loader, boolean reload)
              throws
              IOException {
            if (baseName == null || locale == null || format == null
                || loader == null)
              throw new NullPointerException();
            ResourceBundle bundle = null;
            if (format.equals("properties")) {
              String bundleName = toBundleName(baseName, locale);
              String resourceName = toResourceName(bundleName, format);
              InputStream stream = null;
              if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                  URLConnection connection = url.openConnection();
                  if (connection != null) {
                    // Disable caches to get fresh data for
                    // reloading.
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                  }
                }
              } else {
                stream = loader.getResourceAsStream(resourceName);
              }
              if (stream != null) {
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                bundle = new PropertyResourceBundle(reader);
                reader.close();
              }
            }
            return bundle;
          }
        });

  }
}
