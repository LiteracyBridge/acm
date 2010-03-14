package org.literacybridge.acm.rcp.views.category.helpers;

import java.util.HashSet;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class CheckedTreeNodeProvider implements IPostSelectionProvider {

	private HashSet<ISelectionChangedListener> listeners = new HashSet<ISelectionChangedListener>();
	
	private String id = "CheckedTreeNodeProvider";
	
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {


	}

	@Override
	public ISelection getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {

	}

	@Override
	public void setSelection(ISelection selection) {
		fireSelectionChanged(selection);
	}

	@Override
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removePostSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.remove(listener);	
	}
	
	private void fireSelectionChanged(ISelection selection) {
		SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
		for (ISelectionChangedListener listener : listeners) {
			listener.selectionChanged(event);
		}
	}
}
