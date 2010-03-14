package org.literacybridge.acm.rcp.views.devices;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.literacybridge.acm.core.MessageBus.Message;

public class DeviceContentProvider implements IStructuredContentProvider {

	private Message message = null;
	
	@Override
	public Object[] getElements(Object inputElement) {
		return new Object[] { message };
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof Message) {
			Message newMessage = (Message) newInput;	
			message = newMessage;
		}
	}
}
