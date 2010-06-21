package org.literacybridge.acm.resourcebundle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.literacybridge.acm.metadata.LBMetadataIDs;
import org.literacybridge.acm.metadata.MetadataField;

import com.google.common.collect.UnmodifiableIterator;

public class LabelProvider {
		
	public static final String CATEGORY_ROOT_LABEL = "CATEGORY_ROOT_LABEL";
	public static final String DEVICES_ROOT_LABEL = "DEVICES_ROOT_LABEL";
	public static final String OPTIONS_ROOT_LABEL = "OPTIONS_ROOT_LABEL";
	public static final String OPTIONS_USER_LANGUAGE = "OPTIONS_USER_LANGUAGE";
	public static final String WATERMARK_SEARCH = "WATERMARK_SEARCH";
	
	public static final String AUDIO_ITEM_TABLE_COLUMN_TITLE = "AUDIO_ITEM_TABLE_COLUMN_TITLE";
	public static final String AUDIO_ITEM_TABLE_COLUMN_CREATOR = "AUDIO_ITEM_TABLE_COLUMN_CREATOR";
	public static final String AUDIO_ITEM_TABLE_COLUMN_LANGUAGE = "AUDIO_ITEM_TABLE_COLUMN_LANGUAGE";
	public static final String AUDIO_ITEM_TABLE_COLUMN_PLAY_COUNT = "AUDIO_ITEM_TABLE_COLUMN_PLAY_COUNT";
	public static final String AUDIO_ITEM_TABLE_COLUMN_CATEGORIES = "AUDIO_ITEM_TABLE_COLUMN_CATEGORIES";
	
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
			bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
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
		return getResourceBundle(locale).getString(propertyName);
	}
	
	public static void main(String[] args) {
		Iterator<KeyValuePair<MetadataField<?>, String>> it = getMetaFieldLabelsIterator(Locale.ENGLISH);
		while (it.hasNext()) {
			System.out.println(it.next().getValue());
		}
	}
}
