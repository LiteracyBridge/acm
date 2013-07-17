package org.literacybridge.acm.gui.ResourceView;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingListener;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultListSelectionModel;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.config.ControlAccess;
import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.core.MessageBus.Message;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.device.DeviceConnectEvent;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ResourceView.TagsListModel.TagLabel;
import org.literacybridge.acm.gui.dialogs.audioItemImportDialog.AudioItemImportDialog;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.ACMContainer;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.gui.util.language.UILanguageChanged;

public class CategoryView extends ACMContainer implements Observer {

	private static final long serialVersionUID = 5551716856269051991L;

	// model
	private IDataRequestResult result = null;
	// categories
	private CheckboxTree categoryTree = null;
	// tags
	private JList tagsList = null;
	private JButton addTagButton = null;
	// Languages
	private CheckboxTree languageTree = null;
	
	private JTree deviceTree = null;
	// root nodes
	private final DefaultMutableTreeNode categoryRootNode;
	private final DefaultMutableTreeNode languageRootNode;
	private final DefaultMutableTreeNode deviceRootNode;
	private final DefaultTreeModel deviceTreeModel;

	
	private JXTaskPaneContainer taskPaneContainer;
	private JXTaskPane categoryPane;
	private JXTaskPane tagsPane;
	private JXTaskPane languagePane;
	private JXTaskPane devicePane;
	private JXTaskPane optionsPane;
	
	private JLabel uiLangugeLb;
	private Locale currLocale = null;
	
	// list of available devices
	private Map<String, DefaultMutableTreeNode> deviceUidtoTreeNodeMap = new HashMap<String, DefaultMutableTreeNode>();
	// list of available languages
	private List<LanguageLabel> languagesList = new ArrayList<LanguageLabel>();
	
	public CategoryView(IDataRequestResult result) {
		this.result = result;
		categoryRootNode = new DefaultMutableTreeNode();
		deviceRootNode = new DefaultMutableTreeNode(
				LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, LanguageUtil.getUILanguage()));
		languageRootNode = new DefaultMutableTreeNode();
		deviceTreeModel = new DefaultTreeModel(deviceRootNode);
		createControls();
	}
	
	private void createControls() {
		setLayout(new BorderLayout());

		// parent
	    taskPaneContainer = new JXTaskPaneContainer();

		createTasks();	
		createLanguageList();
		addOptionList();
		if (!ControlAccess.isACMReadOnly()) {
			addDragAndDrop();
		}
		JScrollPane taskPane = new JScrollPane(taskPaneContainer);
		add(BorderLayout.CENTER, taskPane);
		
		// init controls with default language
		updateControlLanguage(LanguageUtil.getUILanguage());
		updateTagsTable(PersistentTag.getFromDatabase());
		Application.getMessageService().addObserver(this);
	}
	
	private void createTasks() {
		// add all categories
		Category rootCategory = result.getRootCategory();
		if (rootCategory.hasChildren()) {
			for (Category c : getSortedChildren(rootCategory)) {
				addChildNodes(categoryRootNode, c);
			}
		}
		
		categoryTree = new CheckboxTree(categoryRootNode);
		categoryTree.setRootVisible(false);
		categoryTree.setCellRenderer(new FacetCountCellRenderer());
		categoryTree.expandPath(new TreePath(categoryRootNode.getPath()));
		categoryTree.setSelectionModel(NO_SELECTION_MODEL);

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
			@Override public void mouseClicked(MouseEvent e)  {
				TreePath path = deviceTree.getPathForLocation(e.getX(), e.getY());
				if (path != null) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
					Object deviceInfo = node.getUserObject();
					if (deviceInfo instanceof DeviceInfo) {
						UIUtils.showDialog(Application.getApplication(), new AudioItemImportDialog(Application.getApplication(), (DeviceInfo) deviceInfo));
					}
				}
			}
		});
		
		categoryPane = new JXTaskPane();
		JScrollPane categoryScrollPane = new JScrollPane(categoryTree);
		categoryPane.add(categoryScrollPane);	
		
		languagePane = new JXTaskPane();
		languageTree = new CheckboxTree(languageRootNode);
		languageTree.setSelectionModel(NO_SELECTION_MODEL);
		languageTree.setCellRenderer(new FacetCountCellRenderer());
		JScrollPane languageScrollPane = new JScrollPane(languageTree);
		languagePane.add(languageScrollPane);

		tagsPane = new JXTaskPane();
		tagsList = new JList();
		tagsList.setSelectionModel(new DefaultListSelectionModel() {
			@Override public void setSelectionInterval(int index0, int index1) {
				if (isSelectedIndex(index0)) {
				    super.removeSelectionInterval(index0, index1);
				    Application.getFilterState().setSelectedTag(null);
				}
				else {
				    super.setSelectionInterval(index0, index1);
				    Application.getFilterState().setSelectedTag(((TagLabel) tagsList.getModel().getElementAt(index0)).getTag());
				}
			}
		});
		tagsList.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {}
			
			@Override
			public void mousePressed(MouseEvent e) {}
			
			@Override
			public void mouseExited(MouseEvent e) {}
			
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				boolean rightButtonClicked = e.getButton() == MouseEvent.BUTTON3;
				
				if (rightButtonClicked) {
					int index = tagsList.locationToIndex(e.getPoint());
					if (tagsList.getSelectedIndex() != index) {
						tagsList.setSelectedIndex(index);
					}
					TagLabel label = (TagLabel) tagsList.getSelectedValue();
					if (label != null) {
						TagsListPopupMenu menu = new TagsListPopupMenu(label);
				        menu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
				
			}
		});
		JScrollPane tagsScrollPane = new JScrollPane(tagsList);
		tagsPane.add(tagsScrollPane);
		addTagButton = new JButton(LabelProvider.getLabel(LabelProvider.NEW_TAG_LABEL, LanguageUtil.getUILanguage()));
		addTagButton.addActionListener(new ActionListener() {			
			@Override public void actionPerformed(ActionEvent action) {
				addNewTag();
			}
		});
		if (ControlAccess.isACMReadOnly()) {
		    addTagButton.setEnabled(false);
		}
		tagsPane.add(addTagButton);

		devicePane = new JXTaskPane();
		JScrollPane deviceScrollPane = new JScrollPane(deviceTree);
		devicePane.add(deviceScrollPane);
		devicePane.setIcon(new ImageIcon(getClass().getResource("/315_three-shot.png")));

		taskPaneContainer.add(categoryPane);
		taskPaneContainer.add(languagePane);
		taskPaneContainer.add(tagsPane);
		taskPaneContainer.add(devicePane);
		
		categoryScrollPane.setPreferredSize(new Dimension(150, 250));
		languageScrollPane.setPreferredSize(new Dimension(150, 90));
		deviceScrollPane.setPreferredSize(new Dimension(150, 50));
		tagsScrollPane.setPreferredSize(new Dimension(150, 90));
		
		categoryTree.expandPath(new TreePath(deviceRootNode.getPath()));
		addListeners(); // at last
	}
	
	private class LanguageLabel implements FacetCountProvider {
		private Locale locale;
		public LanguageLabel(Locale locale) {
			this.locale = locale;
		}
		
		@Override public String toString() {
			String displayLabel = LanguageUtil.getLocalizedLanguageName(locale);
			int count = result.getLanguageFacetCount(locale.getLanguage());
			if (count > 0) {
				displayLabel += " ["+count+"]";
			}
			return displayLabel;
		}

		public Locale getLocale() {
			return locale;
		}

		@Override
		public int getFacetCount() {
			return result.getLanguageFacetCount(locale.getLanguage());
		}
	}
	
	private static final class UILanguageLabel {
		private final Locale locale;
		
		UILanguageLabel(Locale locale) {
			this.locale = locale;
		}
		
		@Override public String toString() {
			return LanguageUtil.getLocalizedLanguageName(locale);
		}
	}
	
	private void addOptionList() {
		final int NUM_OPTIONS = 2;
		optionsPane = new JXTaskPane();
		JPanel optionComponent = new JPanel();

		// user language
		optionComponent.setLayout(new GridLayout(NUM_OPTIONS, 2));
		uiLangugeLb = new JLabel();
		optionComponent.add(uiLangugeLb);		
		UILanguageLabel[] langs = {new UILanguageLabel(Locale.ENGLISH),
								 new UILanguageLabel(Locale.GERMAN),
								 new UILanguageLabel(Locale.FRENCH)};
		
		
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
					 newLocale = Locale.FRENCH;
					 break;
				default:
					newLocale = Locale.ENGLISH;
				 }

				LanguageUtil.setUILanguage(newLocale); // set for controls that will be created on the fly, like ex. popup menus
				Application.getMessageService().pumpMessage(new UILanguageChanged(newLocale, currLocale));
				currLocale = newLocale;
			}
		});
		optionComponent.add(userLanguages);
		
		JScrollPane optionsScrollPane = new JScrollPane(optionComponent);
		optionsPane.add(optionsScrollPane);
		taskPaneContainer.add(optionsPane);
	}

	private void createLanguageList() {
		List<Locale> audioLanguages = Configuration.getAudioLanguages();
		for (Locale locale : audioLanguages) {
			languagesList.add(new LanguageLabel(locale));			
		}

		for (LanguageLabel currLable : languagesList) {
			languageRootNode.add(new DefaultMutableTreeNode(currLable));	
		}		
		
		languageTree.setRootVisible(false);
		languageTree.expandPath(new TreePath(languageRootNode.getPath()));
		
		languageTree.addTreeCheckingListener(new TreeCheckingListener() {
			@Override public void valueChanged(TreeCheckingEvent e) {
				clearTagSelection();
				pumpLanguageFilter();
			};
		});
	}
	
	private void pumpLanguageFilter() {
		// map languages on 'Locales'
		TreePath[] tp = languageTree.getCheckingPaths();
		List<PersistentLocale> filterLocales = new ArrayList<PersistentLocale>(tp.length);
		for (int i = 0; i < tp.length; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp[i].getLastPathComponent();
			LanguageLabel obj = (LanguageLabel) node.getUserObject();
			
			//new PersistentLocale
			PersistentLocale locale = new PersistentLocale();
			locale.setCountry(obj.getLocale().getCountry());
			locale.setLanguage(obj.getLocale().getLanguage());
			filterLocales.add(locale);
		}
		
		Application.getFilterState().setFilterLanguages(filterLocales);	
	}
	
	private void addListeners() {
		categoryTree.addTreeCheckingListener(new TreeCheckingListener() {
			public void valueChanged(TreeCheckingEvent e) {
				clearTagSelection();
				
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
		
		tagsList.addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				clearTreeSelections();			
			}
		});
		addAudioDeviceListener();
	}

	private void addChildNodes(DefaultMutableTreeNode parent, Category category) {
		DefaultMutableTreeNode child = new DefaultMutableTreeNode(new CategoryTreeNodeObject(category));
		parent.add(child);
		if (category.hasChildren()) {
			for (Category c : getSortedChildren(category)) {
				addChildNodes(child, c);
			}
		}
	}

	private List<Category> getSortedChildren(Category category) {
		List<Category> children = category.getChildren();
		Collections.sort(children, new Comparator<Category>() {
			@Override public int compare(Category c1, Category c2) {
				return c1.getOrder() - c2.getOrder();
			}
		});
		
		return children;
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
	
	private void addDragAndDrop() {
		categoryTree.setDropMode(DropMode.ON);
		categoryTree.setTransferHandler(new TreeTransferHandler());
		
		deviceTree.setDropMode(DropMode.ON);
		deviceTree.setTransferHandler(new ExportToDeviceTransferHandler());
		
		tagsList.setDropMode(DropMode.ON);
		tagsList.setTransferHandler(new TagsTransferHandler());
	}
	
	
	public static interface FacetCountProvider {
		public int getFacetCount();	
	}
	
	// Helper class for tree nodes
	public class CategoryTreeNodeObject implements FacetCountProvider {
		private Category category;
		
		public CategoryTreeNodeObject(Category category) {
			this.category = category;
		}
	
		public Category getCategory() {
			return category;
		}

		@Override
		public int getFacetCount() {
			return result.getFacetCount(category);
		}
		
		@Override 
		public String toString() {
			String displayLabel = null;
			if (category != null) {
				displayLabel = category.getCategoryName(LanguageUtil.getUILanguage()).getLabel();
				int count = result.getFacetCount(category);
				if (count > 0) {
					displayLabel += " ["+count+"]";
				}
			} else {
				displayLabel = LabelProvider.getLabel("ERROR", LanguageUtil.getUILanguage());
			}
			
			return displayLabel;
		}
	}
	
	
	private static class FacetCountCellRenderer extends DefaultCheckboxTreeCellRenderer {
	    public Component getTreeCellRendererComponent(JTree tree, Object object, boolean selected, boolean expanded, boolean leaf, int row,
	    	    boolean hasFocus) {
	    	DefaultMutableTreeNode cat = (DefaultMutableTreeNode) object;
	    	DefaultCheckboxTreeCellRenderer cell = (DefaultCheckboxTreeCellRenderer) super.getTreeCellRendererComponent(tree, object, selected, expanded, leaf, row, hasFocus);
	    	
	    	if (cat.getUserObject() != null && cat.getUserObject() instanceof FacetCountProvider) {
	    		FacetCountProvider node = (FacetCountProvider) cat.getUserObject();
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
	
	public void updateTagsTable(List<PersistentTag> tags) {
		if (!clearingSelections) {
			tagsList.setModel(new TagsListModel(tags));
		}
	}

	public void addNewTag() {
		String tagName = (String)JOptionPane.showInputDialog(
                this,
                "Enter playlist name:",
                "Add new playlist",
                JOptionPane.PLAIN_MESSAGE,
                null, null, "");
		if (!StringUtils.isEmpty(tagName)) {
			PersistentTag tag = new PersistentTag();
			tag.setTitle(tagName);
			tag.commit();
			
			Application.getMessageService().pumpMessage(new TagsListChanged(PersistentTag.getFromDatabase()));
		}
	}
	
	private void updateControlLanguage(Locale locale) {
		categoryPane.setTitle(LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL, locale));
		tagsPane.setTitle(LabelProvider.getLabel(LabelProvider.TAGS_ROOT_LABEL, locale));
		devicePane.setTitle(LabelProvider.getLabel(LabelProvider.DEVICES_ROOT_LABEL, locale));
		optionsPane.setTitle(LabelProvider.getLabel(LabelProvider.OPTIONS_ROOT_LABEL, locale));
		languagePane.setTitle(LabelProvider.getLabel(LabelProvider.LANGUAGES_ROOT_LABEL, locale));
		uiLangugeLb.setText(LabelProvider.getLabel(LabelProvider.OPTIONS_USER_LANGUAGE, locale));
		
		updateTreeNodes();
	}

	
	private void updateTreeNodes() {
		for (Enumeration e = categoryRootNode.breadthFirstEnumeration(); e.hasMoreElements(); ) {
	        DefaultMutableTreeNode current = (DefaultMutableTreeNode)e.nextElement();
	        CategoryTreeNodeObject obj = (CategoryTreeNodeObject) current.getUserObject();
	        categoryTree.getModel().valueForPathChanged(new TreePath(current.getPath()), obj);
		}
		for (Enumeration e = languageRootNode.breadthFirstEnumeration(); e.hasMoreElements(); ) {
	        DefaultMutableTreeNode current = (DefaultMutableTreeNode)e.nextElement();
	        LanguageLabel obj = (LanguageLabel) current.getUserObject();
	        languageTree.getModel().valueForPathChanged(new TreePath(current.getPath()), obj);
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
		
		if (arg instanceof TagsListChanged) {
			updateTagsTable(((TagsListChanged) arg).tags);
		}
	}	
	
	private boolean clearingSelections = false;
	
	private void clearTagSelection() {
		if (!clearingSelections) {
			clearingSelections = true;
			try {
				Application.getFilterState().setSelectedTag(null);
				UIUtils.invokeAndWait(new Runnable() {
					@Override public void run() {
						tagsList.clearSelection();
					}
				});
			} finally {
				clearingSelections = false;
			}
		}
	}
	
	private void clearTreeSelections() {
		if (!clearingSelections) {
			clearingSelections = true;

			try {
				UIUtils.invokeAndWait(new Runnable() {
					@Override public void run() {
						categoryTree.clearChecking();
						languageTree.clearChecking();
					}
				});
			} finally {
				clearingSelections = false;
			}
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
	
	private static final TreeSelectionModel NO_SELECTION_MODEL = new DefaultTreeSelectionModel() {
		@Override public void addSelectionPath(TreePath path) {}
		@Override public void addSelectionPaths(TreePath[] paths) {}
		@Override public void setSelectionPath(TreePath path) {}
		@Override public void setSelectionPaths(TreePath[] paths) {}			
	};
	
	public static class TagsListChanged {
		private final List<PersistentTag> tags;
		
		TagsListChanged(List<PersistentTag> tags) {
			this.tags = tags;
		}
	}
}
