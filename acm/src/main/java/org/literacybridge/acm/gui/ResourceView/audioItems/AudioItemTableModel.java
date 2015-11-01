package org.literacybridge.acm.gui.ResourceView.audioItems;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.AudioItemCache;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesModel;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;

public class AudioItemTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -2998511081572936717L;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // positions of the table columns
    public static final int NUM_COLUMNS 	   = 8; // keep in sync
    public static final int INFO_ICON 		   = 0;
    public static final int TITLE 			   = 1;
    public static final int DURATION 		   = 2;
    public static final int CATEGORIES 		   = 3;
    public static final int SOURCE			   = 4;
    public static final int LANGUAGES          = 5;
    public static final int DATE_FILE_MODIFIED = 6;
    public static final int PLAYLIST_ORDER	   = 7;
    private static String[] columns = null;

    protected IDataRequestResult result = null;

    private final AudioItemCache cache;

    public static void initializeTableColumns( String[] initalColumnNames) {
        columns = initalColumnNames;
    }

    public AudioItemTableModel(IDataRequestResult result, AudioItemCache cache) {
        this.result = result;
        this.cache = cache;
        if (result != null) {
            result.getAudioItems();
        }
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }



    @Override
    public int getRowCount() {
        if (result != null) {
            return result.getAudioItems().size();
        }

        return 0;
    }

    private AudioItem getFromCache(int rowIndex) {
        // we cache more items to make scrolling smooth
        ensureCached(Math.max(0, rowIndex - 20), Math.min(result.getAudioItems().size(), rowIndex + 100));
        synchronized (cache) {
            return cache.get(result.getAudioItems().get(rowIndex));
        }
    }

    private void ensureCached(int fromRow, int toRow) {
        synchronized (cache) {
            for (int row = fromRow; row < toRow; row++) {
                String uuid = result.getAudioItems().get(row);
                cache.get(uuid);
            }
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        AudioItem audioItem = getFromCache(rowIndex);

        String cellText = "";
        try {
            switch (columnIndex) {
            case INFO_ICON: {
                cellText = "";
                break;
            }
            case TITLE: {
                List<MetadataValue<String>> values = audioItem.getMetadata().getMetadataValues(
                        MetadataSpecification.DC_TITLE);
                if (values != null) {
                    cellText = values.get(0).getValue();
                }
                break;
            }
            case DURATION: {
                List<MetadataValue<String>> values = audioItem.getMetadata().getMetadataValues(
                        MetadataSpecification.LB_DURATION);
                if (values != null && !StringUtils.isEmpty(values.get(0).getValue())) {
                    cellText = values.get(0).getValue();
                }
                break;
            }
            case CATEGORIES: {
                cellText = UIUtils.getCategoryListAsString(audioItem);
                break;
            }
            case SOURCE: {
                List<MetadataValue<String>> values = audioItem.getMetadata().getMetadataValues(
                        MetadataSpecification.DC_SOURCE);
                if (values != null) {
                    cellText = values.get(0).getValue();
                }
                break;
            }

            case LANGUAGES: {
                cellText = LanguageUtil.getLocalizedLanguageName(AudioItemPropertiesModel
                        .getLanguage(audioItem, MetadataSpecification.DC_LANGUAGE));
                break;
            }

            /*				case MESSAGE_FORMAT: {
					List<MetadataValue<String>> values = localizedAudioItem.getMetadata().getMetadataValues(
							MetadataSpecification.LB_MESSAGE_FORMAT);
					if (values != null) {
						cellText = values.get(0).getValue();
					}
					break;
				}
             */
            case DATE_FILE_MODIFIED: {
                File file = ACMConfiguration.getCurrentDB().getRepository().getAudioFile(audioItem, AudioFormat.A18);
                if (file != null) {
                    Date date = new Date(file.lastModified());
                    cellText = dateFormat.format(date);
                }
                break;
            }
            case PLAYLIST_ORDER: {
                Playlist tag = Application.getFilterState().getSelectedPlaylist();
                if (tag == null) {
                    cellText = "";
                } else {
                    int position = tag.getPosition(audioItem);
                    cellText = Integer.toString(position);
                }
                break;
            }
            //				case COPY_COUNT: {
            //					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
            //							MetadataSpecification.LB_COPY_COUNT);
            //					cellText = values != null ? "" + values.get(0).getValue() : "0";
            //					break;
            //				}
            //				case OPEN_COUNT: {
            //					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
            //							MetadataSpecification.LB_OPEN_COUNT);
            //					cellText = values != null ? "" + values.get(0).getValue() : "0";
            //					break;
            //				}
            //				case COMPLETION_COUNT: {
            //					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
            //							MetadataSpecification.LB_COMPLETION_COUNT);
            //					cellText = values != null ? "" + values.get(0).getValue() : "0";
            //					break;
            //				}
            //				case SURVEY1_COUNT: {
            //					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
            //							MetadataSpecification.LB_SURVEY1_COUNT);
            //					cellText = values != null ? "" + values.get(0).getValue() : "0";
            //					break;
            //				}
            //				case APPLY_COUNT: {
            //					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
            //							MetadataSpecification.LB_APPLY_COUNT);
            //					cellText = values != null ? "" + values.get(0).getValue() : "0";
            //					break;
            //				}
            //				case NOHELP_COUNT: {
            //					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
            //							MetadataSpecification.LB_NOHELP_COUNT);
            //					cellText = values != null ? "" + values.get(0).getValue() : "0";
            //					break;
            //				}


            default: {
                cellText = "";
                break;
            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new AudioItemNode(audioItem, cellText);
    }





}
