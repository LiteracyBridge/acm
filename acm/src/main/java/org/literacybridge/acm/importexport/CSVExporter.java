package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.LBMetadataIDs;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;

public class CSVExporter {
  private static final Logger LOG = Logger
      .getLogger(CSVExporter.class.getName());

  private final static String CATEGORY_COLUMN_NAME = "CATEGORIES";
  private final static String QUALITY_COLUMN_NAME = "QUALITY";
  private final static String PROJECT_COLUMN_NAME = "PROJECT";

  public static void export(AudioItem[] audioItems, File targetFile)
      throws IOException {
    export(Lists.newArrayList(audioItems), targetFile);
  }

  public static void export(Iterable<AudioItem> audioItems, File targetFile)
      throws IOException {
    String project = ACMConfiguration.getInstance().getCurrentDB()
        .getSharedACMname();
    if (project.toLowerCase().startsWith("acm-")) {
      project = project.substring(4);
    }

    CSVWriter writer = new CSVWriter(new FileWriter(targetFile), ',');

    List<MetadataField<?>> columns = Lists
        .newArrayList(LBMetadataIDs.FieldToIDMap.keySet());
    // create a stable sort order based on the metadata field ids
    Collections.sort(columns, new Comparator<MetadataField<?>>() {
      @Override
      public int compare(MetadataField<?> o1, MetadataField<?> o2) {
        return LBMetadataIDs.FieldToIDMap.get(o1)
            .compareTo(LBMetadataIDs.FieldToIDMap.get(o2));
      }
    });

    // +3 for categories, quality and project
    final int numColumns = columns.size() + 3;
    String[] values = new String[numColumns];

    // first write header
    for (int i = 0; i < numColumns - 3; i++) {
      values[i] = columns.get(i).getName();
    }

    values[numColumns - 3] = CATEGORY_COLUMN_NAME;
    values[numColumns - 2] = QUALITY_COLUMN_NAME;
    values[numColumns - 1] = PROJECT_COLUMN_NAME;

    writer.writeNext(values);

    for (AudioItem audioItem : audioItems) {
      Metadata metadata = audioItem.getMetadata();
      String quality = "l";
      for (int i = 0; i < numColumns - 3; i++) {
        MetadataField<?> field = columns.get(i);
        String value = getStringValue(metadata, field);
        if (field == MetadataSpecification.LB_DURATION) {
          try {
            // last character is the quality
            quality = value.substring(value.length() - 1);
            int durationInSeconds = Integer.parseInt(value.substring(0, 2)) * 60
                + Integer.parseInt(value.substring(3, 5));
            value = Integer.toString(durationInSeconds);
          } catch (Exception ignored) {
            LOG.warning(String.format(
                "Exception parsing time & quality from '%s'. Substituting 0 l.",
                audioItem.getUuid()));
            quality = "l";
            value = "0";
          }
        }
        values[i] = value;
      }
      values[numColumns - 3] = UIUtils.getCategoryListAsString(audioItem);
      values[numColumns - 2] = quality;
      values[numColumns - 1] = project;
      writer.writeNext(values);
    }

    writer.close();
  }

  private static <T> String getStringValue(Metadata metadata,
      MetadataField<T> field) {
    MetadataValue<T> value = metadata.getMetadataValue(field);
    if (value == null) {
      return "";
    } else {
      return value.getValue().toString();
    }
  }
}
