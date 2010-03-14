package org.literacybridge.acm.rcp.views.requestResult;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.literacybridge.acm.rcp.views.category.helpers.CheckedTreeNodeSelection;

public class RequestResultView extends ViewPart {

	private ISelectionListener selectionListener = null;
	private TableViewer tableViewer = null;
	
	public RequestResultView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		createTableViewer(parent);		
		createSelectionListener();
	}
	
	private TableViewer createTableViewer(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		createColumn(tableViewer);
		tableViewer.setContentProvider(new AudioItemTableContentProvider());
		tableViewer.setLabelProvider(new AudioItemTableLabelProvider());
		return tableViewer;
	}
	
	private void createColumn(TableViewer viewer) {
		String[] titles = { "Title", "Category", "Creator", "Language" };
		int[] bounds = { 100, 100, 100, 100 };

		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(titles[i]);
			column.getColumn().setWidth(bounds[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
		}
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
	
	public void createSelectionListener() {
		selectionListener = new ISelectionListener() {			
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part != RequestResultView.this && !selection.isEmpty() && selection instanceof CheckedTreeNodeSelection) {
					CheckedTreeNodeSelection checkTreeNodeSel = (CheckedTreeNodeSelection) selection;
					tableViewer.getTable().removeAll();
					tableViewer.setInput(new AudioTableInput(checkTreeNodeSel.getCheckTreeLeafs()));
				} else {
					tableViewer.getTable().clearAll();
				}
			}
		};	
		
		ISelectionService selectionService = getSite().getWorkbenchWindow().getSelectionService();		
		selectionService.addPostSelectionListener(selectionListener);
	}

//	private void dumpCheckCategories(List<ICategoryResultContainer> checkTreeNodes) {
//		System.out.println("++++++++++++++++++++++++++++");
//		for(ICategoryResultContainer tna : checkTreeNodes) {
//			System.out.println("Checked :" + tna.getCategory().getCategoryName(Locale.GERMAN).getLabel());
//		}
//		System.out.println("++++++++++++++++++++++++++++");
//	}
	
	@Override
	public void setFocus() {
	}
	
	@Override
	public void dispose() {
		if (selectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(selectionListener);			
		}
	}
}
