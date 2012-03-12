package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.dialogs.AudioItemContextMenuDialog;
import org.literacybridge.acm.ui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.ui.messages.RequestAudioItemToPlayMessage;
import org.literacybridge.acm.util.UIUtils;

public class AudioItemViewMouseListener extends MouseAdapter {
	public AudioItemView adaptee;
	public IDataRequestResult currentResult;
	
	public AudioItemViewMouseListener(AudioItemView adaptee) {
		this.adaptee = adaptee;
	}
	
	public void mouseReleased(MouseEvent e) {
		if (e.getClickCount() == 2) {
			RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(RequestAudioItemMessage.RequestType.Current);
			Application.getMessageService().pumpMessage(msg);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		boolean leftButtonClicked = e.getButton() == MouseEvent.BUTTON1;
		boolean rightButtonClicked = e.getButton() == MouseEvent.BUTTON3;
		boolean shiftKeyPressed = e.isShiftDown();
		
		if (rightButtonClicked && !shiftKeyPressed) {
			selectRowUnderCursor(e);
		} else if (shiftKeyPressed && rightButtonClicked) {
			int startRow = adaptee.audioItemTable.rowAtPoint(e.getPoint());
			int[] selectedRows = adaptee.audioItemTable.getSelectedRows();
			int endRow = startRow; // use as default
			if (selectedRows.length > 0) {
				endRow = selectedRows[selectedRows.length-1]; // value of last item
			}
			selectRange(startRow, endRow);
		} else {		
			selectRowIfNoneIs(e);			
		}
		showAudioItemDlg(e);
	}

	private void selectRange(int rowStart, int rowEnd) {
		adaptee.selectTableRow(rowStart, rowEnd);
	}
	
	private void selectRowIfNoneIs(MouseEvent e) {	
		if (!adaptee.hasSelectedRows()) {
			int row = adaptee.audioItemTable.rowAtPoint(e.getPoint());
			adaptee.selectTableRow(row);
		}
	}
	
	private void selectRowUnderCursor(MouseEvent e) {
		int row = adaptee.audioItemTable.rowAtPoint(e.getPoint());
		adaptee.selectTableRow(row);	
	}
	
	private void showAudioItemDlg(MouseEvent e) {
		int col = adaptee.audioItemTable.columnAtPoint(e.getPoint());
		
		// trigger if right button was clicked, or if the settings icon
		// was clicked with the left mouse button
		if (col == AudioItemTableModel.INFO_ICON || e.getButton() != MouseEvent.BUTTON1) {
			
			AudioItem clickedAudioItem = adaptee.getCurrentAudioItem(); // always the first item of a selection!!
			if (clickedAudioItem != null) {
			
				int[] selectedRows = adaptee.audioItemTable.getSelectedRows();
				AudioItem[] selectedAudioItems = new AudioItem[selectedRows.length];
				for (int i = 0; i < selectedRows.length; i++) {
					selectedAudioItems[i] = adaptee.getAudioItemAtTableRow(selectedRows[i]);
				}
				
				UIUtils.showDialog(	new AudioItemContextMenuDialog(Application.getApplication()
										, clickedAudioItem
										, selectedAudioItems
										, adaptee
										, currentResult)
								, e.getXOnScreen() + 2
								, e.getYOnScreen());
			}
		}
	}

	public void setCurrentResult(IDataRequestResult currentResult) {
		this.currentResult = currentResult;
	}
	

	
}