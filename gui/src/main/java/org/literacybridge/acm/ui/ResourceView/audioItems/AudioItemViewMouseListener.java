package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ListSelectionModel;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemTableModel.LocalizedAudioItemNode;
import org.literacybridge.acm.ui.dialogs.AudioItemContextMenuDialog;
import org.literacybridge.acm.util.UIUtils;
import org.literacybridge.acm.util.language.LanguageUtil;

public class AudioItemViewMouseListener extends MouseAdapter {
	public AudioItemView adaptee;
	public IDataRequestResult currentResult;
	
	public AudioItemViewMouseListener(AudioItemView adaptee, IDataRequestResult currentResult) {
		this.adaptee = adaptee;
		this.currentResult = currentResult;
	}
	
	public void mouseReleased(MouseEvent e) {
		int row = adaptee.audioItemTable.rowAtPoint(e.getPoint());
	
		if (e.getClickCount() == 2) {
			AudioItem audioItem =  adaptee.getValueAt(row, 0);
			if (audioItem != null) {				
				LocalizedAudioItem lai = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());
				Application.getMessageService().pumpMessage(lai);
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		showAudioItemDlg(e);
	}

	private void showAudioItemDlg(MouseEvent e) {
		int row = adaptee.audioItemTable.rowAtPoint(e.getPoint());
		int col = adaptee.audioItemTable.columnAtPoint(e.getPoint());
		
		// trigger if right button was clicked, or if the settings icon
		// was clicked with the left mouse button
		if (col == AudioItemTableModel.INFO_ICON || e.getButton() != MouseEvent.BUTTON1) {
			AudioItem clickedAudioItem = adaptee.getValueAt(row, 0);

			int[] selectedRows = adaptee.audioItemTable.getSelectedRows();
			AudioItem[] selectedAudioItems = new AudioItem[selectedRows.length];
			for (int i = 0; i < selectedRows.length; i++) {
				selectedAudioItems[i] = adaptee.getValueAt(selectedRows[i], 0);
			}
			
			UIUtils.showDialog(new AudioItemContextMenuDialog(Application.getApplication(), 
					clickedAudioItem, selectedAudioItems, adaptee, currentResult), e.getXOnScreen() + 2, e.getYOnScreen());
		}
	}
	

	
}