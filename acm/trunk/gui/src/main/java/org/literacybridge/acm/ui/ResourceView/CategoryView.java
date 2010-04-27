package org.literacybridge.acm.ui.ResourceView;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingListener;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

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
	// root nodes
	private DefaultMutableTreeNode rootNode = null;
	private DefaultMutableTreeNode categoryRootNode = null;
	private DefaultMutableTreeNode deviceRootNode = null;
	
	// list of available devices
	private Map<String, DefaultMutableTreeNode> deviceUidtoTreeNodeMap = new HashMap<String, DefaultMutableTreeNode>();
	
	
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
		rootNode = new DefaultMutableTreeNode("Root node");
		categoryRootNode = new DefaultMutableTreeNode(
				LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, LanguageUtil.getUserChoosenLanguage()));
		rootNode.add(categoryRootNode);
		
		// add all categories
		Category rootCategory = result.getRootCategory();
		if (rootCategory.hasChildren()) {
			for (Category c : rootCategory.getChildren()) {
				addChildNodes(categoryRootNode, c);
			}
		}
		
		deviceRootNode = new DefaultMutableTreeNode("Devices");
		rootNode.add(deviceRootNode);
		
		categoryTree = new CheckboxTree(rootNode);
		categoryTree.setRootVisible(false);
		
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
				DefaultTreeModel model = (DefaultTreeModel) categoryTree.getModel();
				DefaultMutableTreeNode node = deviceUidtoTreeNodeMap.get(deviceID);
				model.removeNodeFromParent(node);
				deviceUidtoTreeNodeMap.remove(deviceID);
			} else {
				// new device found
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(deviceInfo.getPathToDevice());
				parent.add(child);
				categoryTree.setSelectionPath(new TreePath(child.getPath()));	
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
}
