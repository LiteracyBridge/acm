package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.metadata.LBMetadataIDs;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.collect.Lists;

public class CSVExporter {
	private final static String CATEGORY_COLUMN_NAME = "CATEGORIES";
	private final static String PLAYLIST_COLUMN_NAME = "PLAYLISTS";
	
	public static void export(LocalizedAudioItem[] audioItems, File targetFile) throws IOException {
		CSVWriter writer = new CSVWriter(new FileWriter(targetFile), ',');
		
		List<MetadataField<?>> columns = Lists.newArrayList(LBMetadataIDs.FieldToIDMap.keySet());
		// create a stable sort order based on the metadata field ids
		Collections.sort(columns, new Comparator<MetadataField<?>>() {
			@Override public int compare(MetadataField<?> o1, MetadataField<?> o2) {
				return LBMetadataIDs.FieldToIDMap.get(o1).compareTo(LBMetadataIDs.FieldToIDMap.get(o2));
			}
		});
		
		// +2 for categories and playlists
		final int numColumns = columns.size() + 2;
		String[] values = new String[numColumns];
		
		// first write header
		for (int i = 0; i < numColumns - 2; i++) {
			values[i] = columns.get(i).getName();
		}
		
		values[numColumns - 2] = CATEGORY_COLUMN_NAME;
		values[numColumns - 1] = PLAYLIST_COLUMN_NAME;
		
		writer.writeNext(values);
		
		for (LocalizedAudioItem audioItem : audioItems) {
			Metadata metadata = audioItem.getMetadata();
			for (int i = 0; i < numColumns - 2; i++) {
				MetadataField<?> field = columns.get(i);
				values[i] = getStringValue(metadata, field);
			}
			values[numColumns - 2] = UIUtils.getCategoryListAsString(audioItem.getParentAudioItem());
			values[numColumns - 1] = UIUtils.getPlaylistAsString(audioItem.getParentAudioItem());
			writer.writeNext(values);
		}
		
		writer.close();
	}
	
	private static <T> String getStringValue(Metadata metadata, MetadataField<T> field) {
		List<MetadataValue<T>> values = metadata.getMetadataValues(field);
		if (values == null || values.isEmpty()) {
			return "";
		}
		
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			builder.append(values.get(i).getValue());
			if (i < values.size() - 1) {
				builder.append(",");
			}
		}
		
		return builder.toString();
	}
}
