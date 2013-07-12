package org.literacybridge.acm.gui.resourcebundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.literacybridge.acm.metadata.LBMetadataIDs;
import org.literacybridge.acm.metadata.MetadataField;

import com.google.common.collect.UnmodifiableIterator;

public class LabelProvider {
		
	public static final String CATEGORY_ROOT_LABEL = "CATEGORY_ROOT_LABEL";
	public static final String TAGS_ROOT_LABEL = "TAGS_ROOT_LABEL";
	public static final String NEW_TAG_LABEL = "NEW_TAG_LABEL";
	public static final String LANGUAGES_ROOT_LABEL = "LANGUAGES_ROOT_LABEL";
	public static final String DEVICES_ROOT_LABEL = "DEVICES_ROOT_LABEL";
	public static final String OPTIONS_ROOT_LABEL = "OPTIONS_ROOT_LABEL";
	public static final String OPTIONS_USER_LANGUAGE = "OPTIONS_USER_LANGUAGE";
	public static final String WATERMARK_SEARCH = "WATERMARK_SEARCH";
	
	public static final String AUDIO_ITEM_TABLE_COLUMN_TITLE = "AUDIO_ITEM_TABLE_COLUMN_TITLE";
	public static final String AUDIO_ITEM_TABLE_COLUMN_DURATION = "AUDIO_ITEM_TABLE_COLUMN_DURATION";
	public static final String AUDIO_ITEM_TABLE_COLUMN_CREATOR = "AUDIO_ITEM_TABLE_COLUMN_CREATOR";
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
	
	public static final class KeyValuePair<K, V> {
		private final K key;
		private final V value;
		
		public KeyValuePair(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		public K getKey() {return key;}
		public V getValue() {return value;}
	}
	
	private static final String BUNDLE_NAME = "labels";
	private static final String METAFIELD_PROPERTY_PREFIX = "METAFIELD_";
	
	private final static Map<Locale, ResourceBundle> bundles = new HashMap<Locale, ResourceBundle>();
	
	private static final ResourceBundle getResourceBundle(Locale locale) {
		ResourceBundle bundle = bundles.get(locale);
		if (bundle == null) {
			bundle = loadUTF8Bundle(BUNDLE_NAME, locale);
			bundles.put(locale, bundle);
		}
		return bundle;
	}
	
	public static String getLabel(MetadataField<?> field, Locale locale) {
		int fieldID = LBMetadataIDs.FieldToIDMap.get(field);
		String resourceName = METAFIELD_PROPERTY_PREFIX + fieldID;
		
		ResourceBundle bundle = getResourceBundle(locale);
		return bundle.getString(resourceName);
	}
	
	public static Iterator<KeyValuePair<MetadataField<?>, String>> getMetaFieldLabelsIterator(final Locale locale) {
		final Iterator<MetadataField<?>> fieldIterator = LBMetadataIDs.FieldToIDMap.keySet().iterator();
		return new UnmodifiableIterator<KeyValuePair<MetadataField<?>,String>>() {

			@Override
			public boolean hasNext() {
				return fieldIterator.hasNext();
			}

			@Override
			public KeyValuePair<MetadataField<?>, String> next() {
				MetadataField<?> field = fieldIterator.next();
				String value = getLabel(field, locale);
				return new KeyValuePair<MetadataField<?>, String>(field, value);
			}
				
		};
	}
	
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
		Iterator<KeyValuePair<MetadataField<?>, String>> it = getMetaFieldLabelsIterator(Locale.ENGLISH);
		while (it.hasNext()) {
			System.out.println(it.next().getValue());
		}
	}
	
	private static ResourceBundle loadUTF8Bundle(String bundleName, Locale locale) {
		return ResourceBundle.getBundle(bundleName, locale, 
			     new ResourceBundle.Control() {
			         public List<String> getFormats(String baseName) {
			             if (baseName == null)
			                 throw new NullPointerException();
			             return Arrays.asList("properties");
			         }
			         public ResourceBundle newBundle(String baseName,
			                                         Locale locale,
			                                         String format,
			                                         ClassLoader loader,
			                                         boolean reload)
			                          throws IllegalAccessException,
			                                 InstantiationException,
			                                 IOException {
			             if (baseName == null || locale == null
			                   || format == null || loader == null)
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
			                	 InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
			                     bundle = new PropertyResourceBundle(reader);
			                     reader.close();
			                 }
			             }
			             return bundle;
			         }
			     });

	}
}
