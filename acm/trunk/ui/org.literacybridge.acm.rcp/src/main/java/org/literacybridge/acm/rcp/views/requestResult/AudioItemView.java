package org.literacybridge.acm.rcp.views.requestResult;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class AudioItemView extends ViewPart {

	private ISelectionListener selectionListener = null;
	private TreeViewer treeViewer = null;
	
	public enum TableColumn {
		TITLE,
		CREATOR,
		LANGUAGE
	}
	
	public AudioItemView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		createTreeViewer(parent);		
		createSelectionListener();
	}
	
	private TreeViewer createTreeViewer(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		createColumns(treeViewer);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLinesVisible(true);	
		treeViewer.setContentProvider(new AudioItemTableContentProvider());
		treeViewer.setLabelProvider(new AudioItemTableLabelProvider());
		return treeViewer;
	}
	
	private void createColumns(TreeViewer viewer) {
		int columnWidth = 200;
		
		for (TableColumn i : TableColumn.values()) {
			TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(getColumnString(i));
			column.getColumn().setWidth(columnWidth);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);			
		}
	}
	
	private String getColumnString(TableColumn id) {
		switch (id) {
		case TITLE:
			return "Title";
		case CREATOR:
			return "Creator";
		case LANGUAGE:
			return "Language";
		default:
			return "<Error>";
		}
	}
	
	public void createSelectionListener() {
		selectionListener = new ISelectionListener() {			
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				treeViewer.setInput(new Object());
//				if (part != AudioItemView.this && !selection.isEmpty() && selection instanceof CheckedTreeNodeSelection) {
//					CheckedTreeNodeSelection checkTreeNodeSel = (CheckedTreeNodeSelection) selection;
//					treeViewer.getTree().removeAll();
//					treeViewer.setInput(new AudioTableInput(checkTreeNodeSel.getCheckTreeLeafs()));
//				} else {
//					treeViewer.getTree().removeAll();
//				}
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
