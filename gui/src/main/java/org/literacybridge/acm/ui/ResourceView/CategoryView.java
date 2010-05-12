package org.literacybridge.acm.ui.ResourceView;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingListener;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.core.MessageBus.Message;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.device.DeviceConnectEvent;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.util.language.LanguageUtil;
import org.literacybridge.acm.util.language.UILanguageChanged;

public class CategoryView extends Container implements Observer {

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
	private JXTaskPane categoryPane;
	private JXTaskPane devicePane;
	private JXTaskPane optionsPane;
	
	private JLabel uiLangugeLb;
	private Locale currLocale = null;
	
	// list of available devices
	private Map<String, DefaultMutableTreeNode> deviceUidtoTreeNodeMap = new HashMap<String, DefaultMutableTreeNode>();
	
	
	public CategoryView(IDataRequestResult result) {
		this.result = result;
		categoryRootNode = new DefaultMutableTreeNode();
		deviceRootNode = new DefaultMutableTreeNode(
				LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, LanguageUtil.getUILanguage()));

		deviceTreeModel = new DefaultTreeModel(deviceRootNode);
		createControls();
	}
	
	private void createControls() {
		setLayout(new BorderLayout());

		// parent
	    taskPaneContainer = new JXTaskPaneContainer();

		createTree();	
		addOptionList();
		addDragAndDropForTree();
		add(BorderLayout.CENTER, taskPaneContainer);
		
		// init controls with default language
		updateControlLanguage(LanguageUtil.getUILanguage());
		Application.getMessageService().addObserver(this);
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
		categoryTree.setCellRenderer(new FacetCountCellRenderer());
		categoryTree.expandPath(new TreePath(categoryRootNode.getPath()));

		deviceTree = new JTree(deviceTreeModel);
		deviceTree.setRootVisible(false);
		final DeviceTreeCellRenderer renderer = new DeviceTreeCellRenderer();
		deviceTree.setCellRenderer(renderer);
		
		deviceTree.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				TreePath path = deviceTree.getPathForLocation(e.getX(), e.getY());
				renderer.highlight = (path == null) ? null : path.getLastPathComponent();
	           
				deviceTree.repaint();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		
		deviceTree.addMouseListener(new MouseListener() {
			@Override
			public void mouseExited(MouseEvent e) {
	           renderer.highlight = null;
	           deviceTree.repaint();
			}
			
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e)  {}
			@Override public void mouseEntered(MouseEvent e)  {}
			@Override public void mouseClicked(MouseEvent e)  {}
		});
		

		
		categoryPane = new JXTaskPane();
		JScrollPane categoryScrollPane = new JScrollPane(categoryTree);
		categoryPane.add(categoryScrollPane);		

		devicePane = new JXTaskPane();
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
	
	private void addOptionList() {
		final int NUM_OPTIONS = 2;
		optionsPane = new JXTaskPane();
		JPanel optionComponent = new JPanel();

		// user language
		optionComponent.setLayout(new GridLayout(NUM_OPTIONS, 2));
		uiLangugeLb = new JLabel();
		optionComponent.add(uiLangugeLb);		
		String[] langs = {"English", "German", "Dagaare"};
		JComboBox userLanguages = new JComboBox(langs);
		userLanguages.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				 JComboBox cb = (JComboBox) e.getSource();
				 Locale newLocale = null;
				 int index = cb.getSelectedIndex();
				 switch (index) {
				 case 0:
					 newLocale = Locale.ENGLISH;
					 break;
				 case 1:
					 newLocale = Locale.GERMAN;
					 break;
				 case 2:
					 newLocale = LanguageUtil.DAGAARE;
					 break;
				default:
					newLocale = Locale.ENGLISH;
				 }
				 LanguageUtil.setUserChoosenLanguage(newLocale);
				 Application.getMessageService().pumpMessage(new UILanguageChanged(newLocale, currLocale));
				 currLocale = newLocale;
			}
		});
		optionComponent.add(userLanguages);
		
		JScrollPane optionsScrollPane = new JScrollPane(optionComponent);
		optionsPane.add(optionsScrollPane);
		taskPaneContainer.add(optionsPane);
	}

	private void addListeners() {
		categoryTree.addTreeCheckingListener(new TreeCheckingListener() {
			public void valueChanged(TreeCheckingEvent e) {
				TreePath[] tp = categoryTree.getCheckingPaths();
				List<PersistentCategory> filterCategories = new ArrayList<PersistentCategory>(tp.length);
				for (int i = 0; i < tp.length; i++) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp[i].getLastPathComponent();
					CategoryTreeNodeObject obj = (CategoryTreeNodeObject) node.getUserObject();
					filterCategories.add(obj.getCategory().getPersistentObject());
				}
				
				Application.getFilterState().setFilterCategories(filterCategories);
			}
		});
		
		addAudioDeviceListener();
	}

	private void addChildNodes(DefaultMutableTreeNode parent, Category category) {
		DefaultMutableTreeNode child = new DefaultMutableTreeNode(new CategoryTreeNodeObject(category));
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
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(deviceInfo);
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
		
		deviceTree.setDropMode(DropMode.ON);
		deviceTree.setTransferHandler(new ExportToDeviceTransferHandler());
	}
	
	// Helper class for tree nodes
	public class CategoryTreeNodeObject {
		private Category category;
		
		public CategoryTreeNodeObject(Category category) {
			this.category = category;
		}
	
		public Category getCategory() {
			return category;
		}

		public int getFacetCount() {
			return result.getFacetCount(category);
		}
		
		@Override 
		public String toString() {
			String displayLabel = null;
			if (category != null) {
				displayLabel = category.getCategoryName(LanguageUtil.getUserChoosenLanguage()).getLabel();
				int count = result.getFacetCount(category);
				if (count > 0) {
					displayLabel += " ["+count+"]";
				}
			} else {
				displayLabel = "error";
			}
			
			return displayLabel;
		}
	}
	
	
	private static class FacetCountCellRenderer extends DefaultCheckboxTreeCellRenderer {
	    public Component getTreeCellRendererComponent(JTree tree, Object object, boolean selected, boolean expanded, boolean leaf, int row,
	    	    boolean hasFocus) {
	    	DefaultMutableTreeNode cat = (DefaultMutableTreeNode) object;
	    	DefaultCheckboxTreeCellRenderer cell = (DefaultCheckboxTreeCellRenderer) super.getTreeCellRendererComponent(tree, object, selected, expanded, leaf, row, hasFocus);
	    	
	    	if (cat.getUserObject() != null && cat.getUserObject() instanceof CategoryTreeNodeObject) {
	    		CategoryTreeNodeObject node = (CategoryTreeNodeObject) cat.getUserObject();
		    	// make label bold
		        Font f = super.label.getFont();
		    	int count = node.getFacetCount();
		    	//System.out.println("Node: " + node.toString() + " - Count= " + count);
		        if (count > 0) {
		    		super.label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		    	} else {
		    		super.label.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
		    	}
		    		    		
	    	}

	    	return cell;
	    }
	}
	
	private void updateControlLanguage(Locale locale) {
		categoryPane.setTitle(LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, locale));
		devicePane.setTitle(LabelProvider.getLabel(LabelProvider.DEVICES_ROOT_LABEL, locale));
		optionsPane.setTitle(LabelProvider.getLabel(LabelProvider.OPTIONS_ROOT_LABEL, locale));
		uiLangugeLb.setText(LabelProvider.getLabel(LabelProvider.OPTIONS_USER_LANGUAGE, locale));
		
		updateTreeNodes();
	}

	
	private void updateTreeNodes() {
		for (Enumeration e = categoryRootNode.breadthFirstEnumeration(); e.hasMoreElements(); ) {
	        DefaultMutableTreeNode current = (DefaultMutableTreeNode)e.nextElement();
	        CategoryTreeNodeObject obj = (CategoryTreeNodeObject) current.getUserObject();
	        categoryTree.getModel().valueForPathChanged(new TreePath(current.getPath()), obj);
		}
	}
	
	
	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof UILanguageChanged) {
			UILanguageChanged newLocale = (UILanguageChanged) arg;
			updateControlLanguage(newLocale.getNewLocale());
		}
		
		if (arg instanceof IDataRequestResult) {
			result = (IDataRequestResult) arg;
			updateTreeNodes();
		}
	}	
	
	private static class DeviceTreeCellRenderer extends DefaultTreeCellRenderer {
		Object highlight;
		ImageIcon icon = new ImageIcon(getClass().getResource("/sync-green-16.png"));
		
		@Override
	    public Component getTreeCellRendererComponent(JTree tree, Object value,
				  boolean sel,
				  boolean expanded,
				  boolean leaf, int row,
				  boolean hasFocus) {
			JLabel cell = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if (highlight != null && value.equals(highlight)) {
				cell.setIcon(icon);
			}
			cell.setHorizontalTextPosition(SwingConstants.LEFT);
			Dimension d = cell.getPreferredSize();
			cell.setPreferredSize(new Dimension((int) d.getWidth() + icon.getIconWidth() + 15, (int) d.getHeight()));
			return cell;
		}
	}
}
