package org.literacybridge.acm.ui.ResourceView;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingListener;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.core.MessageBus.Message;
import org.literacybridge.acm.device.DeviceConnectEvent;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.util.LanguageUtil;

public class CategoryView extends Container {

	private static final long serialVersionUID = 5551716856269051991L;

	// model
	private IDataRequestResult result = null;
	// tree
	private CheckboxTree categoryTree = null;
	
	private JTree deviceTree = null;
	// root nodes
	private final DefaultMutableTreeNode categoryRootNode;
	private final DefaultMutableTreeNode deviceRootNode;
	private final DefaultTreeModel deviceTreeModel;

	
	private JXTaskPaneContainer taskPaneContainer;
	
	// list of available devices
	private Map<String, DefaultMutableTreeNode> deviceUidtoTreeNodeMap = new HashMap<String, DefaultMutableTreeNode>();
	
	
	public CategoryView(IDataRequestResult result) {
		this.result = result;
		categoryRootNode = new DefaultMutableTreeNode(
				LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, LanguageUtil.getUserChoosenLanguage()));
		deviceRootNode = new DefaultMutableTreeNode(
				LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, LanguageUtil.getUserChoosenLanguage()));

		deviceTreeModel = new DefaultTreeModel(deviceRootNode);
		createControls();
	}

	private void createControls() {
		setLayout(new BorderLayout());

		createTree();	
		addDragAndDropForTree();
		add(BorderLayout.CENTER, taskPaneContainer);
	}

	private void createTree() {
		// add all categories
		Category rootCategory = result.getRootCategory();
		if (rootCategory.hasChildren()) {
			for (Category c : rootCategory.getChildren()) {
				addChildNodes(categoryRootNode, c);
			}
		}
		
		categoryTree = new CheckboxTree(categoryRootNode);
		categoryTree.setRootVisible(false);
		categoryTree.expandPath(new TreePath(categoryRootNode.getPath()));

		deviceTree = new JTree(deviceTreeModel);
		deviceTree.setRootVisible(false);
		
	    taskPaneContainer = new JXTaskPaneContainer();
		JXTaskPane categoryPane = new JXTaskPane();
		categoryPane.setTitle(LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, LanguageUtil.getUserChoosenLanguage()));
		JScrollPane categoryScrollPane = new JScrollPane(categoryTree);
		categoryPane.add(categoryScrollPane);		

		JXTaskPane devicePane = new JXTaskPane();
		devicePane.setTitle(LabelProvider.getLabel(LabelProvider.DEVICES_ROOT_LABEL, LanguageUtil.getUserChoosenLanguage()));
		JScrollPane deviceScrollPane = new JScrollPane(deviceTree);
		devicePane.add(deviceScrollPane);
		devicePane.setIcon(new ImageIcon(getClass().getResource("/315_three-shot.png")));

		taskPaneContainer.add(categoryPane);
		taskPaneContainer.add(devicePane);
		
		categoryScrollPane.setPreferredSize(new Dimension(150, 280));
		deviceScrollPane.setPreferredSize(new Dimension(150, 90));
		
		categoryTree.expandPath(new TreePath(deviceRootNode.getPath()));
		addListeners(); // at last
	}

	private void addListeners() {
		categoryTree.addTreeCheckingListener(new TreeCheckingListener() {
			public void valueChanged(TreeCheckingEvent e) {
				TreePath[] tp = categoryTree.getCheckingPaths();
//				for (int i = 0; i < tp.length; i++) {
//					System.out.println("Checked paths changed: user clicked on " + tp[i]);					
//				}

				/*
				 * ADD CODE THAT GETS AUDIO ITEMS FROM BACKEND FOR SELECTED CATEGORIES ....
				 */
				
				
				// testing only...
				Application.getMessageService().pumpMessage(result);	
			}
		});
		
		addAudioDeviceListener();
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
	
	private void addDeviceNode(DefaultMutableTreeNode parent, final DeviceInfo deviceInfo) {
		if (parent != null) {
			String deviceID = deviceInfo.getDeviceUID();
			if (deviceUidtoTreeNodeMap.containsKey(deviceID)) {
				// already existing, assume device was unplugged
				DefaultMutableTreeNode node = deviceUidtoTreeNodeMap.get(deviceID);
				deviceTreeModel.removeNodeFromParent(node);
				deviceUidtoTreeNodeMap.remove(deviceID);
			} else {
				// new device found
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(deviceInfo.getPathToDevice());
				deviceTreeModel.insertNodeInto(child, parent, 
						parent.getChildCount());
				deviceTree.expandPath(new TreePath(deviceRootNode.getPath()));
				TreePath path = new TreePath(child.getPath());
				deviceTree.setSelectionPath(path);	
				deviceUidtoTreeNodeMap.put(deviceID, child);
			}
		}
	}
	
	private void addAudioDeviceListener() {
		MessageBus bus = MessageBus.getInstance();
		bus.addListener(DeviceConnectEvent.class, new MessageBus.MessageListener() {
			
			@Override
			public void receiveMessage(final Message message) {
				
				if (message instanceof DeviceConnectEvent) {
					final DeviceConnectEvent dce = (DeviceConnectEvent) message;
					Runnable addDeviceToList = new Runnable() {					
						@Override
						public void run() {
							addDeviceNode(deviceRootNode, dce.getDeviceInfo());						
						}
					};
					
					SwingUtilities.invokeLater(addDeviceToList);	
				}
			}
		});
	}
	
	private void addDragAndDropForTree() {
		categoryTree.setDropMode(DropMode.ON);
		categoryTree.setTransferHandler(new TreeTransferHandler());
	}
	
}
