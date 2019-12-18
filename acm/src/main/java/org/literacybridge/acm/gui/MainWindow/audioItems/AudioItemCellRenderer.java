package org.literacybridge.acm.gui.MainWindow.audioItems;

import static org.literacybridge.acm.store.MetadataSpecification.LB_STATUS;

import java.awt.*;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesModel;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;

@SuppressWarnings("serial")
public class AudioItemCellRenderer extends DefaultTableCellRenderer {

  private static ImageIcon settingsImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_SETTINGS_16_PX));

  public int highlightedRow = -1;

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {

    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
        isSelected, hasFocus, row, column);

    int modelColumn = table.convertColumnIndexToModel(column);
    // If this is the "info icon column", and the row is highlighted, set the icon to be the gear icon.
    if (modelColumn == AudioItemTableModel.infoIconColumn.getColumnIndex() && row == highlightedRow) {
      label.setIcon(settingsImageIcon);
    } else {
      label.setIcon(null);
    }

    if (value != null) {
      AudioItemNode<?> audioItemNode = (AudioItemNode<?>) value;
        String labelText = audioItemNode.toString();
        label.setText(labelText);

        // Set a tooltip for language column (with iso639-3 code) and for wide values (with the
        // full text).
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        Graphics g = table.getGraphics();
        FontMetrics met = g.getFontMetrics();
        int textWidth = met.stringWidth(label.getText()) + 10;
        boolean isLanguage = modelColumn == AudioItemTableModel.languagesColumn.getColumnIndex();

        if (StringUtils.isNotEmpty(audioItemNode.toString()) &&
                (isLanguage || textWidth > columnWidth)) {
            StringBuilder tip = new StringBuilder();
            // For language column, add the iso639 code.
            if (isLanguage) {
                AudioItem audioItem = audioItemNode.getAudioItem();
                Locale locale = AudioItemPropertiesModel.getLanguage(audioItem,
                    MetadataSpecification.DC_LANGUAGE);
                tip.append(LanguageUtil.getLanguageNameWithCode(locale));
            } else {
                tip.append(audioItemNode.toString());
            }
            // This heinous hack is because the tooltip location is only updated if the text
            // changes. So adjacent lines with the same value (language, say), will reuse the
            // same tooltip. This behaviour is visually distracting. By appending some zero-width
            // spaces, the strings are strictly different, but appear the same.
            tip.append(StringUtils.repeat("\ufeff", row % 5));
            label.setToolTipText(tip.toString());
        } else {
            label.setToolTipText(null);
        }

        // The field "LB_STATUS" is an integer field. If STATUS_VALUES [ LB_STATUS ] .equals ("NO LONGER USED"),
        // turn the field to green italic.
      MetadataValue<Integer> statusValue = audioItemNode.getAudioItem().getMetadata().getMetadataValue(LB_STATUS);
      if (statusValue != null &&
              AudioItemPropertiesModel.STATUS_VALUES[statusValue.getValue()].equals(AudioItemPropertiesModel.NO_LONGER_USED)) {
        Font italicsLabel = new Font(label.getFont().getName(), Font.ITALIC, label.getFont().getSize());
        label.setFont(italicsLabel);
      }
    } else {
      label.setText("<null>");
    }
    return label;
  }
}
