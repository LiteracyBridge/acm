package org.literacybridge.acm.importexport;

import com.google.common.collect.Sets;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.utils.B26RotatingEncoding;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.logging.Logger;

import static org.literacybridge.acm.store.LBMetadataIDs.FieldToIDMap;
import static org.literacybridge.acm.store.MetadataSpecification.DC_IDENTIFIER;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.store.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.store.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.store.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.store.MetadataSpecification.DTB_REVISION;
import static org.literacybridge.acm.store.MetadataSpecification.LB_BENEFICIARY;
import static org.literacybridge.acm.store.MetadataSpecification.LB_DATE_RECORDED;
import static org.literacybridge.acm.store.MetadataSpecification.LB_DURATION;
import static org.literacybridge.acm.store.MetadataSpecification.LB_ENGLISH_TRANSCRIPTION;
import static org.literacybridge.acm.store.MetadataSpecification.LB_GOAL;
import static org.literacybridge.acm.store.MetadataSpecification.LB_KEYWORDS;
import static org.literacybridge.acm.store.MetadataSpecification.LB_MESSAGE_FORMAT;
import static org.literacybridge.acm.store.MetadataSpecification.LB_NOTES;
import static org.literacybridge.acm.store.MetadataSpecification.LB_PRIMARY_SPEAKER;
import static org.literacybridge.acm.store.MetadataSpecification.LB_SDG_GOALS;
import static org.literacybridge.acm.store.MetadataSpecification.LB_SDG_TARGETS;
import static org.literacybridge.acm.store.MetadataSpecification.LB_STATUS;
import static org.literacybridge.acm.store.MetadataSpecification.LB_TARGET_AUDIENCE;
import static org.literacybridge.acm.store.MetadataSpecification.LB_TIMING;

public class CSVExporter {
    private static final Logger LOG = Logger.getLogger(CSVExporter.class.getName());

    public enum OPTION {
        NONE,
        CATEGORIES_AS_CODES,
        CATEGORY_AS_FULL_NAME,
        NO_HEADER,
    }

    private final static String CATEGORY_COLUMN_NAME = "CATEGORIES";
    private final static String QUALITY_COLUMN_NAME = "QUALITY";
    private final static String PROJECT_COLUMN_NAME = "PROJECT";

    /**
     * This horrible thing defines the order of the metadata columns in the .csv file.
     * Because PostgreSQL can not add new columns in the middle of a table, new columns
     * must be added at the end. But, the CATEGORIES, QUALITY, and PROJECT were
     * unfortunately placed at "the end". Now, we need to keep them in indices
     * 19, 20, and 21, and let new columns flow after that.
     */
    private final static MetadataField<?>[] columns = { DC_TITLE, DC_PUBLISHER, DC_IDENTIFIER,
            DC_SOURCE, DC_LANGUAGE, DC_RELATION, DTB_REVISION, LB_DURATION, LB_MESSAGE_FORMAT,
            LB_TARGET_AUDIENCE, LB_DATE_RECORDED, LB_KEYWORDS, LB_TIMING, LB_PRIMARY_SPEAKER,
            LB_GOAL, LB_ENGLISH_TRANSCRIPTION, LB_NOTES, LB_BENEFICIARY, LB_STATUS, null,
            // categories are stuffed here
            null,  // quality is stuffed here
            null,  // project is stuffed here
            LB_SDG_GOALS, LB_SDG_TARGETS };

    private static final int CATEGORY_COLUMN_INDEX = 19;
    private static final int QUALITY_COLUMN_INDEX = 20;
    private static final int PROJECT_COLUMN_INDEX = 21;

    private static final int NUMBER_OF_COLUMNS = columns.length;

    public static void exportMessages(Iterable<AudioItem> audioItems, Writer targetWriter, OPTION... options) throws IOException {
        Set<OPTION> opts = Sets.newHashSet(options);
        boolean categoriesAsCodes = opts.contains(OPTION.CATEGORIES_AS_CODES);
        boolean categoryFullNames = opts.contains(OPTION.CATEGORY_AS_FULL_NAME);
        boolean noHeader = opts.contains(OPTION.NO_HEADER);
        String project = ACMConfiguration.getInstance().getCurrentDB().getAcmDbDirName();
        if (project.toLowerCase().startsWith("acm-")) {
            project = project.substring(4);
        }

        // Verify that the manual columns are correct.
        if (columns[CATEGORY_COLUMN_INDEX] != null || columns[QUALITY_COLUMN_INDEX] != null
                || columns[PROJECT_COLUMN_INDEX] != null) {
            throw new IllegalStateException("column collision");
        }
        if (columns.length != FieldToIDMap.size() + 3) {
            throw new IllegalStateException("missing columns");
        }

        ICSVWriter writer = new CSVWriterBuilder(targetWriter).build();

        String[] values = new String[NUMBER_OF_COLUMNS];

        if (!noHeader) {
            // first write header (column names)
            for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
                if (columns[i] != null)
                    //noinspection ConstantConditions
                    values[i] = columns[i].getName();
            }
            values[CATEGORY_COLUMN_INDEX] = CATEGORY_COLUMN_NAME;
            values[QUALITY_COLUMN_INDEX] = QUALITY_COLUMN_NAME;
            values[PROJECT_COLUMN_INDEX] = PROJECT_COLUMN_NAME;

            writer.writeNext(values);
        }

        for (AudioItem audioItem : audioItems) {
            Metadata metadata = audioItem.getMetadata();
            String quality = "l";
            for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
                if (columns[i] != null) {
                    MetadataField<?> field = columns[i];
                    String value = getStringValue(metadata, field);
                    if (field == MetadataSpecification.LB_DURATION) {
                        DurationAndQuality dq = durationAndQualityFromValue(value);
                        value = dq.duration;
                        quality = dq.quality;
                    }
                    values[i] = value;
                }
            }
            values[CATEGORY_COLUMN_INDEX] = categoriesAsCodes ? UIUtils.getCategoryCodesAsString(
                    audioItem) : UIUtils.getCategoryNamesAsString(audioItem, categoryFullNames);
            values[QUALITY_COLUMN_INDEX] = quality;
            values[PROJECT_COLUMN_INDEX] = project;
            writer.writeNext(values);
        }

        writer.close();
    }

    /**
     * Create a .csv file of category ids and names.
     * @param targetWriter Where to write the .csv
     * @param options Options from OPTION.
     * @throws IOException if the data can't be written
     */
    public static void exportCategoryCodes(Writer targetWriter, OPTION... options) throws IOException {
        CategoryExporter exporter = new CategoryExporter(targetWriter, options);
        exporter.export();
    }

    /**
     * Given a string mm:ssq, parse the mm:ss into seconds.
     *
     * @param value as a string
     * @return Value in seconds.
     */
    private static DurationAndQuality durationAndQualityFromValue(String value) {
        DurationAndQuality result = new DurationAndQuality();
        try {
            // last character is the quality
            int durationInSeconds = Integer.parseInt(value.substring(0, 2)) * 60 + Integer.parseInt(
                    value.substring(3, 5));
            result.duration = Integer.toString(durationInSeconds);
            // last character is the quality
            result.quality = value.substring(value.length() - 1);

        } catch (Exception ignored) {
            LOG.warning(
                    String.format("Exception parsing time & quality from '%s'. Substituting 0 l.",
                                  value));
            result.duration = "0";
            result.quality = "l";
        }
        return result;
    }

    /**
     * Given an integer valued string, return the B26RotatingEncoding encoding of
     * the integer. If not integer valued, return an empty string.
     *
     * @param value An integer valued string.
     * @return The encoded string, or an empty string.
     */
    @SuppressWarnings("unused")
    private static String correlationIdFromValue(String value) {
        String id = "";
        if (value.length() > 0) {
            try {
                int intValue = Integer.parseInt(value);
                id = B26RotatingEncoding.encode(intValue);
            } catch (Exception ignored) {
                // ignore, return empty string.
            }
        }
        return id;
    }

    private static <T> String getStringValue(Metadata metadata, MetadataField<T> field) {
        MetadataValue<T> value = metadata.getMetadataValue(field);
        if (value == null) {
            return "";
        } else {
            return value.getValue().toString();
        }
    }

    private static class DurationAndQuality {
        public String duration;
        public String quality;
    }

    /**
     * Helper class to export category ids and names.
     */
    private static class CategoryExporter {
        ICSVWriter writer;
        String [] values;
        boolean listFullCategories;

        CategoryExporter(Writer targetWriter, OPTION... options) {
            Set<OPTION> opts = Sets.newHashSet(options);
            boolean noheader = opts.contains(OPTION.NO_HEADER);

            writer = new CSVWriterBuilder(targetWriter).build();
            this.listFullCategories = opts.contains(OPTION.CATEGORY_AS_FULL_NAME);

            // If listFullCategories, we have 3 columns; otherwise only 2
            if (listFullCategories) {
                values = new String[3];
                values[2] = "FULLNAME";
            } else {
                values = new String[2];
            }
            values[0] = "ID";
            values[1] = "NAME";
            if (!noheader) {
                writer.writeNext(values);
            }
        }

        /**
         * Starts the export process.
         * @throws IOException if the data can't be written
         */
        void export() throws IOException {
            Category root = ACMConfiguration.getInstance()
                    .getCurrentDB()
                    .getMetadataStore()
                    .getTaxonomy()
                    .getRootCategory();
            for (Category child : root.getSortedChildren()) {
                exportCategoryCodes(writer, child);
            }

            writer.close();
        }

        /**
         * Writes one node to the .csv file, then recurses on any children.
         * @param writer Where to write
         * @param node   What to write
         */
        private void exportCategoryCodes(ICSVWriter writer, Category node) {
            values[0] = node.getId();
            values[1] = node.getCategoryName();
            if (listFullCategories) {
                values[2] = node.getFullName();
            }
            writer.writeNext(values);
            for (Category child : node.getSortedChildren()) {
                exportCategoryCodes(writer, child);
            }
        }
    }


}
