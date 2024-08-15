package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.audioconverter.converters.FFMpegConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
//import org.literacybridge.acm.gui.util.Toast;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.RFC3066LanguageCode;

import com.google.common.collect.Maps;

import static org.literacybridge.acm.store.MetadataSpecification.*;

public class AudioItemPropertiesModel extends AbstractTableModel {
    private static final Logger LOG = Logger
            .getLogger(AudioItemPropertiesModel.class.getName());

    public static final String NO_LONGER_USED = "NO LONGER USED";
    static final String STATUS_NAME = "Status";
    public static final String[] STATUS_VALUES = {"Current", NO_LONGER_USED};
    static final Map<String, Integer> STATUS_VALUES_MAP = Maps.newHashMap();

    static {
        for (int i = 0; i < STATUS_VALUES.length; i++) {
            STATUS_VALUES_MAP.put(STATUS_VALUES[i], i);
        }
    }

    private String[] columnNames = {
            LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_PROPERTY",
                    LanguageUtil.getUILanguage()),
            LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_VALUE",
                    LanguageUtil.getUILanguage()),
            "" // edit column doesn't have a title
    };

    static final int TITLE_COL = 0;
    static final int VALUE_COL = 1;
    static final int EDIT_COL = 2;

    private AudioItem audioItem = null;
    private List<AudioItemProperty> audioItemPropertiesObject = new ArrayList<>();

    public AudioItemPropertiesModel(AudioItem audioItem) {
        this.audioItem = audioItem;

        // TODO: make this list configurable
        audioItemPropertiesObject.add(new AudioItemProperty(true) {
            @Override
            public String getName() {
                return STATUS_NAME;
            }

            @Override
            public String getValue(AudioItem audioItem) {
                MetadataValue<Integer> value = audioItem.getMetadata()
                        .getMetadataValue(LB_STATUS);
                if (value == null) {
                    return "Current";
                }

                return STATUS_VALUES[value.getValue()];
            }

            @Override
            public void setValue(AudioItem audioItem, Object newValue) {
                audioItem.getMetadata().putMetadataField(
                        MetadataSpecification.LB_STATUS, new MetadataValue<Integer>(
                                STATUS_VALUES_MAP.get(newValue.toString())));
            }
        });
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(DC_TITLE, true));
        // TODO: calculate duration of audio item
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_DURATION, false));


        audioItemPropertiesObject.add(new AudioItemProperty(true) {
            @Override
            public String getName() {
                return "Volume";
            }

            @Override
            public String getValue(AudioItem audioItem) {
                MetadataValue<String> value = audioItem.getMetadata()
                        .getMetadataValue(LB_VOLUME);
                if (value == null) {
                    return "100"; // Default volume is 100%
                }
                return value.getValue();
            }

            @Override
            public void setValue(AudioItem audioItem, Object newValue) {
                new Thread(() -> {
                    try {
                        FFMpegConverter converter = new FFMpegConverter();
                        converter.normalizeVolume(audioItem, newValue.toString());

                        // Update volume field if conversion completed successfully
                        audioItem.getMetadata().putMetadataField(
                                LB_VOLUME, new MetadataValue<>(newValue.toString()));
                    } catch (Exception e) {
                        // TODO: show a dialog of some sort.
                        e.printStackTrace();
                    }
                }).start();
            }
        });

        audioItemPropertiesObject.add(new AudioItemProperty(false, true) {
            @Override
            public String getName() {
                return "Categories";
            }

            @Override
            public String getValue(AudioItem audioItem) {
                return UIUtils.getCategoryNamesAsString(audioItem);
            }

            @Override
            public void setValue(AudioItem audioItem, Object newValue) {
                // not supported
            }
        });
        audioItemPropertiesObject.add(new AudioItemProperty(false, false) {
            @Override
            public String getName() {
                return "Playlists";
            }

            @Override
            public String getValue(AudioItem audioItem) {
                return UIUtils.getPlaylistAsString(audioItem);
            }

            @Override
            public void setValue(AudioItem audioItem, Object newValue) {
                // not supported
            }
        });

        audioItemPropertiesObject
                .add(new AudioItemProperty.LanguageProperty(DC_LANGUAGE, true));

        // TODO: add combo boxes
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_MESSAGE_FORMAT, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_TARGET_AUDIENCE, true));
        // TODO: add calendar picker
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_DATE_RECORDED, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_KEYWORDS, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_TIMING, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(DC_SOURCE, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_PRIMARY_SPEAKER, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_GOAL, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(DC_PUBLISHER, false));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(DC_IDENTIFIER, false));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(DC_RELATION, true));
        audioItemPropertiesObject.add(new AudioItemProperty(false) {
            @Override
            public String getName() {
                return "Related Message Title";
            }

            @Override
            public String getValue(AudioItem audioItem) {
                MetadataValue<String> value = audioItem.getMetadata()
                        .getMetadataValue(DC_RELATION);
                if (value != null) {
                    String id = value.getValue();
                    if (!StringUtils.isEmpty(id)) {
                        AudioItem item = ACMConfiguration.getInstance().getCurrentDB()
                                .getMetadataStore().getAudioItem(id);
                        MetadataValue<String> values1 = item == null ? null : item.getMetadata()
                                .getMetadataValue(DC_TITLE);
                        if (values1 != null) {
                            return values1.getValue();
                        }
                    }
                }

                return null;
            }

            @Override
            public void setValue(AudioItem audioItem, Object newValue) {
                // not supported
            }
        });

        audioItemPropertiesObject.add(new AudioItemProperty(false) {
            @Override
            public String getName() {
                return "File name";
            }

            @Override
            public String getValue(AudioItem audioItem) {
                AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
                if (repository.findAudioFileWithFormat(audioItem, AudioFormat.A18) != null) {
                    return repository.getAudioFilename(audioItem, AudioFormat.A18);
                } else {
                    return null;
                }
            }

            @Override
            public void setValue(AudioItem audioItem, Object newValue) {
                // not supported
            }
        });

        // TOOD: long text fields
        audioItemPropertiesObject.add(
                new AudioItemProperty.MetadataProperty(LB_ENGLISH_TRANSCRIPTION, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_NOTES, true));
        audioItemPropertiesObject
                .add(new AudioItemProperty.MetadataProperty(LB_BENEFICIARY, true));

    }

    @Override
    public String getColumnName(int arg0) {
        return columnNames[arg0];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return audioItemPropertiesObject.size();
    }

    public AudioItem getSelectedAudioItem() {
        return audioItem;
    }

    public boolean showEditIcon(int row) {
        return audioItemPropertiesObject.get(row).showEditIcon();
    }

    public Locale getMetadataLocale() {
        return getLanguage(audioItem, DC_LANGUAGE);
    }

    public static Locale getLanguage(AudioItem audioItem,
                                     MetadataField<RFC3066LanguageCode> language) {
        MetadataValue<RFC3066LanguageCode> mv = audioItem.getMetadata()
                .getMetadataValue(language);
        if (mv != null) {
            return mv.getValue().getLocale();
        }
        return null;
    }

    public AudioItemProperty getAudioItemProperty(int row) {
        return audioItemPropertiesObject.get(row);
    }

    boolean highlightRow(int row) {
        if (row == 0) {
            AudioItemProperty obj = audioItemPropertiesObject.get(row);
            String value = obj.getValue(audioItem);
            return !value.equals(STATUS_VALUES[0]);
        }

        return false;
    }

    boolean isLanguageRow(int row) {
        AudioItemProperty obj = audioItemPropertiesObject.get(row);
        if (obj instanceof AudioItemProperty.LanguageProperty) {
            return true;
        }

        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        AudioItemProperty obj = audioItemPropertiesObject.get(row);

        switch (col) {
            case TITLE_COL:
                return obj.getName();
            case VALUE_COL:
                return obj.getValue(audioItem);
            case EDIT_COL:
                return "";
            default:
                break;
        }

        return LabelProvider.getLabel("ERROR", LanguageUtil.getUILanguage());
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != VALUE_COL) {
            return false;
        }

        AudioItemProperty obj = audioItemPropertiesObject.get(rowIndex);
        return obj.isCellEditable();
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        if (aValue == null) {
            return;
        }

        String newValue = aValue.toString();
        AudioItemProperty<?> obj = audioItemPropertiesObject.get(row);

        if (obj instanceof AudioItemProperty.LanguageProperty) {
            Locale newLocale = (Locale) aValue;
            ((AudioItemProperty.LanguageProperty) obj).setValue(audioItem, newLocale);
        } else if (obj instanceof AudioItemProperty) {
            ((AudioItemProperty) obj).setValue(audioItem, newValue);
        }

        Metadata metadata = audioItem.getMetadata();
        incrementRevision(metadata);
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
                .getMetadataStore();
        try {
            store.commit(audioItem);
        } catch (IOException e) {
            LOG.log(Level.SEVERE,
                    "Unable to commit changes to AudioItem " + audioItem.getId(), e);
        }
    }

    protected static void setStringValue(MetadataField<String> field,
                                         Metadata metadata, String value) {
        metadata.putMetadataField(field, new MetadataValue<String>(value));
    }

    protected static void setLocaleValue(MetadataField<RFC3066LanguageCode> field,
                                         AudioItem audioItem, Locale newLocale) {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
                .getMetadataStore();
        Metadata metadata = audioItem.getMetadata();
        metadata.putMetadataField(field, new MetadataValue<RFC3066LanguageCode>(
                new RFC3066LanguageCode(newLocale.getLanguage())));
        try {
            store.commit(audioItem);
        } catch (IOException e) {
            LOG.log(Level.SEVERE,
                    "Unable to commit changes to AudioItem " + audioItem.getId(), e);
        }
    }

    private static void incrementRevision(Metadata metadata) {
        MetadataValue<String> revision = metadata
                .getMetadataValue(MetadataSpecification.DTB_REVISION);
        if (revision != null) {
            long rev = 0;
            if (!revision.getValue().isEmpty()) {
                try {
                    rev = Long.parseLong(revision.getValue());
                } catch (NumberFormatException e) {
                    // use 0
                }
            }

            rev++;
            setStringValue(MetadataSpecification.DTB_REVISION, metadata,
                    Long.toString(rev));
        }
    }

    // TODO Ask Michael: What is that csv list for a single metadata field all
    // about?
    // private void setValues(MetadataField<String> field, Metadata metadata,
    // String commaSeparatedListValue) {
    // StringTokenizer t = new StringTokenizer(commaSeparatedListValue, ",");
    //
    // List<MetadataValue<String>> existingValues = metadata
    // .getMetadataValues(field);
    // Iterator<MetadataValue<String>> it = null;
    //
    // if (existingValues != null) {
    // it = existingValues.iterator();
    // }
    //
    // while (t.hasMoreTokens()) {
    // String value = t.nextToken().trim();
    // if (it != null && it.hasNext()) {
    // it.next().setValue(value);
    // } else {
    // metadata.addMetadataField(field, new MetadataValue<String>(
    // value));
    // }
    // }
    // }
}
