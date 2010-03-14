package org.literacybridge.acm.rcp.views.devices;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.literacybridge.acm.core.MessageBus.Message;

public class DeviceLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof Message) 
		{
			Message message = (Message) element;
			switch (columnIndex) {
			case 0:
				return message.toString();
			default:
				return "<error>";	
			}
			
		}
		
		return "<error";
	}
}
