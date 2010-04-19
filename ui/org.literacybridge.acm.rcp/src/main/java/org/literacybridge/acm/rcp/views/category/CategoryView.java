package org.literacybridge.acm.rcp.views.category;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.demo.DemoRepository;
import org.literacybridge.acm.rcp.core.Activator;
import org.literacybridge.acm.rcp.core.Application;
import org.literacybridge.acm.rcp.views.adapters.ICategoryResultContainer;
import org.literacybridge.acm.rcp.views.category.helpers.CheckedTreeNodeProvider;
import org.literacybridge.acm.rcp.views.category.helpers.CheckedTreeNodeSelection;

public class CategoryView extends ViewPart {

	private CheckboxTreeViewer checkboxTreeViewer = null;
	// wrapper for the current checked tree nodes; as the eclipse
	// selection service does not supported "checked selection", which is
	// obvious, as this is no real selection, we must use this hack to use 
	// the selection service to inform other about the current checked state...
	private CheckedTreeNodeProvider checkedTreeNodeProvider = null;
	
	public CategoryView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		createCheckboxTreeViewer(parent);

		// init database
		try {
			Persistence.initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		DemoRepository demo = new DemoRepository();
		IDataRequestResult result = demo.getDataRequestResult();
		
		checkboxTreeViewer.setInput(result);
		
		getSite().setSelectionProvider(checkboxTreeViewer);	// provide selection
		checkedTreeNodeProvider = new CheckedTreeNodeProvider();
		getSite().setSelectionProvider(checkedTreeNodeProvider);
	}
	
	private void createCheckboxTreeViewer(Composite parent) {
		checkboxTreeViewer = new CheckboxTreeViewer(parent, SWT.MULTI);
		checkboxTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		checkboxTreeViewer.setContentProvider(new CategoryTreeContentProvider());
		checkboxTreeViewer.setLabelProvider(new CategoryTreeLabelProvider());
		
		checkboxTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {

				if (event.getChecked()) {
					checkboxTreeViewer.setSubtreeChecked(event.getElement(), true);
		        } else {
		        	checkboxTreeViewer.setSubtreeChecked(event.getElement(), false);
		        }			

				// pump our checked-selection through selection service
				checkedTreeNodeProvider.setSelection(new CheckedTreeNodeSelection(getTreeLeafs()));
			}
		});
	}
	
	
	private List<ICategoryResultContainer> getTreeLeafs() {
		Vector<ICategoryResultContainer> nodes = new Vector<ICategoryResultContainer>();
		Object[] checkedCategories = checkboxTreeViewer.getCheckedElements();
		// we are only interested in the leafs, because currently only theses
		// can have auto items
		for (int i=0; i<checkedCategories.length; ++i) {
			Object o = checkedCategories[i];
			if (o instanceof ICategoryResultContainer) {
				ICategoryResultContainer tna = (ICategoryResultContainer) o;
				if (! tna.getCategory().hasChildren()) {
					nodes.add(tna);
				}
			}
		}
		
		return nodes;
	}
	
	@Override
	public void setFocus() {
	}
}
