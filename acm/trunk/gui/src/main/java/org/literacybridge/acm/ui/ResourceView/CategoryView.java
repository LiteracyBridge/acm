package org.literacybridge.acm.ui.ResourceView;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingListener;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;

import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.util.LanguageUtil;

public class CategoryView extends Container {

	private static final long serialVersionUID = 5551716856269051991L;

	// model
	private IDataRequestResult result = null;
	// tree
	private CheckboxTree categoryTree = null;

	public CategoryView(IDataRequestResult result) {
		this.result = result;
		createControls();
	}

	private void createControls() {
		setLayout(new BorderLayout());

		createTree();	
		JScrollPane sp = new JScrollPane(categoryTree);
		add(BorderLayout.CENTER, sp);
	}

	private void createTree() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root Node");
		addChildNodes(root, result.getRootCategory());
		categoryTree = new CheckboxTree(root);	
		addListeners();
	}

	private void addListeners() {
		categoryTree.addTreeCheckingListener(new TreeCheckingListener() {
			public void valueChanged(TreeCheckingEvent e) {
				TreePath[] tp = categoryTree.getCheckingPaths();
				for (int i = 0; i < tp.length; i++) {
					System.out.println("Checked paths changed: user clicked on " + tp[i]);					
				}

				/*
				 * ADD CODE THAT GETS AUDIO ITEMS FROM BACKEND FOR SELECTED CATEGORIES ....
				 */
				
				
				// testing only...
				Application.getMessageService().pumpMessage(result);	
			}
		});
	}

	private void addChildNodes(DefaultMutableTreeNode parent, Category category) {
		String categoryName = category.getCategoryName(LanguageUtil.getUserChoosenLanguage()).getLabel();
		DefaultMutableTreeNode child = new DefaultMutableTreeNode(categoryName);
		parent.add(child);
		if (category.hasChildren()) {
			for (Category c : category.getChildren()) {
				addChildNodes(child, c);
			}
		}
	}
}
