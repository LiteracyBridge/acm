package org.literacybridge.acm.rcp.editors.audioItem;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.literacybridge.acm.content.AudioItem;

public class AudioItemEditor extends EditorPart {

	// model
	private AudioItem audioItem = null;
	// show the properties of the AudioItem
	TreeViewer treeViewer;
	
	public AudioItemEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input instanceof AudioItemInput) {
			AudioItemInput audioItemInput = (AudioItemInput) input;
			audioItem = audioItemInput.getAudioItem();
			setSite(site);
			setInput(input);
			setPartName(audioItemInput.getName());
		} else {
			setPartName("Invalid Format");
		}
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		Tree audioItemPropetiesTree = new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		audioItemPropetiesTree.setLinesVisible(true);
		audioItemPropetiesTree.setHeaderVisible(true);
		// create columns
		createColumns(audioItemPropetiesTree);
		treeViewer = new TreeViewer(audioItemPropetiesTree);
		
	}

	private void createColumns(Tree tree) {
	      TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
	      column1.setAlignment(SWT.LEFT);
	      column1.setText("Land/Stadt");
	      column1.setWidth(160);
	      TreeColumn column2 = new TreeColumn(tree, SWT.RIGHT);
	      column2.setAlignment(SWT.LEFT);
	      column2.setText("Person");
	      column2.setWidth(100);
	      TreeColumn column3 = new TreeColumn(tree, SWT.RIGHT);
	      column3.setAlignment(SWT.LEFT);
	      column3.setText("m/w");
	      column3.setWidth(35);		
	}
	
	@Override
	public void setFocus() {
	}

}
