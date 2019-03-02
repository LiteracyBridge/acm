package org.literacybridge.acm.gui.MainWindow;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel;
import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.core.MessageBus.Message;
import org.literacybridge.acm.device.DeviceConnectEvent;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.PlaylistListModel.PlaylistLabel;
import org.literacybridge.acm.gui.dialogs.audioItemImportDialog.AudioItemImportDialog;
import org.literacybridge.acm.gui.messages.AudioItemTableSortOrderMessage;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.ACMContainer;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.gui.util.language.UILanguageChanged;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.acm.store.Taxonomy;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SidebarView extends ACMContainer implements Observer {
    private static final Logger LOG = Logger
        .getLogger(SidebarView.class.getName());

    private static final long serialVersionUID = 5551716856269051991L;

    // model
    private SearchResult result;
    // categories
    private CheckboxTree categoryTree = null;
    // playlists
    private JList<PlaylistLabel> playlistList = null;
    // Languages
    private CheckboxTree languageTree = null;

    private JTree deviceTree = null;
    // root nodes
    private final DefaultMutableTreeNode categoryRootNode;
    private final DefaultMutableTreeNode languageRootNode;
    private final DefaultMutableTreeNode deviceRootNode;
    private final DefaultTreeModel deviceTreeModel;

    private JXTaskPaneContainer taskPaneContainer;

    private enum TaskPaneProps {
        CATEGORY(300, false),
        PLAYLIST(120, true),
        LANGUAGE(90, false),
        DEVICE(50, true);
        TaskPaneProps(int preferredHeight, boolean initiallyCollapsed) {
            this.preferredSize = new Dimension(150, preferredHeight);
            this.initiallyCollapsed = initiallyCollapsed;
        }
        Dimension preferredSize;
        boolean initiallyCollapsed;
    }

    private Locale currLocale = null;

    private Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    private Cursor resizingCursor = new Cursor(Cursor.S_RESIZE_CURSOR);
    private Border resizingBorder = null;

    // list of available devices
    private Map<String, DefaultMutableTreeNode> deviceUidtoTreeNodeMap = new HashMap<>();
    // list of available languages
    private List<LanguageLabel> languageList = new ArrayList<>();

    SidebarView(SearchResult result) {
        this.result = result;
        categoryRootNode = new DefaultMutableTreeNode();
        deviceRootNode = new DefaultMutableTreeNode(LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL));
        languageRootNode = new DefaultMutableTreeNode();
        deviceTreeModel = new DefaultTreeModel(deviceRootNode);
        createControls();
    }

    private void createControls() {
        setLayout(new BorderLayout());

        // parent
        taskPaneContainer = new JXTaskPaneContainer();

        createTasks();
        populateLanguageList();
        addOptionList();
        addDragAndDrop();
        JScrollPane taskPane = new JScrollPane(taskPaneContainer);
        add(BorderLayout.CENTER, taskPane);

        // init controls with default language
        updateTreeNodes();
        updatePlaylistTable();
        Application.getMessageService().addObserver(this);
    }

    private void createTasks() {
        taskPaneContainer.add(createCategoryTaskPane());
        taskPaneContainer.add(createLanguageTaskPane());
        taskPaneContainer.add(createPlaylistTaskPane());
        taskPaneContainer.add(createDeviceTaskPane());

        addListeners(); // at last
    }

    private JXTaskPane createCategoryTaskPane() {
        categoryTree = new CheckboxTree(categoryRootNode);
        categoryTree.setRootVisible(false);
        categoryTree.setCellRenderer(new FacetCountCellRenderer());
        categoryTree.expandPath(new TreePath(categoryRootNode.getPath()));
        categoryTree.setSelectionModel(NO_SELECTION_MODEL);
        categoryTree.getCheckingModel().setCheckingMode(TreeCheckingModel.CheckingMode.PROPAGATE_PRESERVING_CHECK);

        JXTaskPane categoryTaskPane = new JXTaskPane();
        categoryTaskPane.setTitle(LabelProvider.getLabel(LabelProvider.CATEGORY_ROOT_LABEL));
        JScrollPane categoryScrollPane = new JScrollPane(categoryTree);
        categoryTaskPane.add(categoryScrollPane);

        fillCategories();

        attachResizers(categoryTaskPane, categoryScrollPane);

        categoryScrollPane.setPreferredSize(TaskPaneProps.CATEGORY.preferredSize);
        categoryTaskPane.setCollapsed(TaskPaneProps.CATEGORY.initiallyCollapsed);

        return categoryTaskPane;
    }

    private JXTaskPane createLanguageTaskPane() {
        JXTaskPane languageTaskPane = new JXTaskPane();
        languageTaskPane.setTitle(LabelProvider.getLabel(LabelProvider.LANGUAGES_ROOT_LABEL));
        languageTree = new CheckboxTree(languageRootNode);
        languageTree.setSelectionModel(NO_SELECTION_MODEL);
        languageTree.setCellRenderer(new LanguageCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(languageTree);
        JScrollPane languageScrollPane = new JScrollPane(languageTree);
        languageTaskPane.add(languageScrollPane);

        attachResizers(languageTaskPane, languageScrollPane);

        languageScrollPane.setPreferredSize(TaskPaneProps.LANGUAGE.preferredSize);
        languageTaskPane.setCollapsed(TaskPaneProps.LANGUAGE.initiallyCollapsed);

        return languageTaskPane;
    }

    private JXTaskPane createPlaylistTaskPane() {
        JXTaskPane playlistTaskPane = new JXTaskPane();
        playlistTaskPane.setTitle(LabelProvider.getLabel(LabelProvider.PLAYLIST_ROOT_LABEL));

        playlistList = new JList<>();
        playlistList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                    Application.getFilterState().setSelectedPlaylist(null);
                } else {
                    super.setSelectionInterval(index0, index1);
                    Application.getFilterState().setSelectedPlaylist(
                        playlistList.getModel().getElementAt(index0).getPlaylist());
                }
            }
        });

        playlistList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean rightButtonClicked = e.getButton() == MouseEvent.BUTTON3;

                if (rightButtonClicked) {
                    int index = playlistList.locationToIndex(e.getPoint());
                    if (playlistList.getSelectedIndex() != index) {
                        playlistList.setSelectedIndex(index);
                    }
                    PlaylistLabel label = playlistList.getSelectedValue();
                    if (label != null) {
                        PlaylistPopupMenu menu = new PlaylistPopupMenu(label);
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

            }
        });
        playlistList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

                Component component = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
                PlaylistLabel label = (PlaylistLabel) value;

                Font f = component.getFont();
                if (label.getFacetCount() > 0) {
                    component.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
                } else {
                    component.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
                }

                return component;
            }
        });
        JScrollPane playlistScrollPane = new JScrollPane(playlistList);
        playlistTaskPane.add(playlistScrollPane);
        JButton addPlaylistButton = new JButton(LabelProvider.getLabel(LabelProvider.NEW_PLAYLIST_LABEL));
        addPlaylistButton.addActionListener(action -> addNewPlaylist());
        playlistTaskPane.add(addPlaylistButton);

        attachResizers(playlistTaskPane, playlistScrollPane);

        playlistScrollPane.setPreferredSize(TaskPaneProps.PLAYLIST.preferredSize);
        playlistTaskPane.setCollapsed(TaskPaneProps.PLAYLIST.initiallyCollapsed);

        return playlistTaskPane;
    }

    private JXTaskPane createDeviceTaskPane() {
        deviceTree = new JTree(deviceTreeModel);
        deviceTree.setRootVisible(false);
        final DeviceTreeCellRenderer renderer = new DeviceTreeCellRenderer();
        deviceTree.setCellRenderer(renderer);

        deviceTree.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseMoved(MouseEvent e) {
                TreePath path = deviceTree.getPathForLocation(e.getX(), e.getY());
                renderer.highlight = (path == null) ? null
                                                    : path.getLastPathComponent();

                deviceTree.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        });

        deviceTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                renderer.highlight = null;
                deviceTree.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = deviceTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
                        .getLastPathComponent();
                    Object deviceInfo = node.getUserObject();
                    if (deviceInfo instanceof DeviceInfo) {
                        UIUtils.showDialog(Application.getApplication(),
                            new AudioItemImportDialog(Application.getApplication(),
                                (DeviceInfo) deviceInfo));
                    }
                }
            }
        });

        JXTaskPane deviceTaskPane = new JXTaskPane();
        deviceTaskPane.setTitle(LabelProvider.getLabel(LabelProvider.DEVICES_ROOT_LABEL));
        JScrollPane deviceScrollPane = new JScrollPane(deviceTree);
        deviceTaskPane.add(deviceScrollPane);
        deviceTaskPane
            .setIcon(new ImageIcon(getClass().getResource("/315_three-shot.png")));

        attachResizers(deviceTaskPane, deviceScrollPane);

        deviceScrollPane.setPreferredSize(TaskPaneProps.DEVICE.preferredSize);
        deviceTaskPane.setCollapsed(TaskPaneProps.DEVICE.initiallyCollapsed);

        return deviceTaskPane;
    }

    /**
     * Implements a simple resizing handle (visual only).
     */
    class ResizingHandle extends JPanel {
        final int width = 40;
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(0xc0, 0xc0, 0xc0));

            int x1 = this.getWidth()/2 - width/2;
            int x2 = x1 + width;
            for (int y=2; y<this.getHeight(); y+=2) {
                g2d.drawLine(x1, y, x2, y);
            }
        }
    }

    /**
     * Given a JXTaskPane and a contained JScrollPane, make the task pane resizable, by resizing
     * the scroll pane. Adjusts the border to make space to paint a resizing handle. Attaches
     * various mouse listeners to manage the resizing.
     *
     * Resizing is vertical only.
     *
     * @param taskPane The JXTaskPane to make resizable.
     * @param scrollPane The JScrollPane that will change size to let the task pane resize.
     */
    private void attachResizers(JXTaskPane taskPane, JScrollPane scrollPane) {
        // Debugging code commented out...

        // The default border has insets all around, but we want the space at the bottom to draw
        // our resizing handle. If we know what the existing border is (compound border, etc.)
        // replace the existing one with our own, with no inset on the bottom.
        if (resizingBorder == null) {
            Border defaultBorder = ((JComponent) taskPane.getContentPane()).getBorder();
            if (defaultBorder instanceof CompoundBorder) {
                Border outsideBorder = ((CompoundBorder) defaultBorder).getOutsideBorder();
                Insets insets = ((EmptyBorder)((CompoundBorder)defaultBorder).getInsideBorder()).getBorderInsets();
                Border insideBorder = new EmptyBorder(insets.top, insets.left, 0, insets.right);
                resizingBorder = new CompoundBorder(outsideBorder, insideBorder);
            } else {
                resizingBorder = defaultBorder;
            }
        }
        ((JComponent) taskPane.getContentPane()).setBorder(resizingBorder);

        taskPane.setAnimated(false);

        ResizingHandle resizingHandle = new ResizingHandle();
        taskPane.add(resizingHandle);

        Container contentPane = taskPane.getContentPane();

        MouseAdapter adp = new MouseAdapter() {
            boolean resizeUI = false;
            boolean resizing = false;
            int resizeYZone;  // scroll handle is this coordinate and higher
            int resizeDelta;  // mouse.y to scrollPane.height adjustment.

            private boolean inResizeZone(MouseEvent e) {
                return e.getY() >= resizeYZone;
            }

            private void setResizeCursor(boolean resizing) {
                resizeUI = resizing;
                contentPane.setCursor(resizing ? resizingCursor : defaultCursor);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                // If isShowingUI != shouldShowUI, adjust cursor
                if (!resizing && resizeUI != inResizeZone(e)) {
                    setResizeCursor(!resizeUI);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                //System.out.printf("Mouse dragged @ (%d,%d)\n", e.getX(), e.getY());
                if (resizing) {
                    int newHeight = e.getY() + resizeDelta;
                    if (newHeight > 25) {
                        //System.out.printf("Resized %d => %d\n", scrollPane.getBounds().height, newHeight);
                        scrollPane.setPreferredSize(new Dimension(scrollPane.getBounds().width, newHeight));
                        // There is no apparent way to get the task pane to just honor the new size.
                        // Collapsing and expanding does, however. That's why we turned off animation
                        // in the beginning.
                        taskPane.setCollapsed(true);
                        taskPane.setCollapsed(false);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                //System.out.printf("Mouse pressed @ (%d,%d)\n", e.getX(), e.getY());
                // Turn on resizing flag only if UI shows that we will.
                resizing = resizeUI && inResizeZone(e);
                if (resizing) {
                    //System.out.printf("Now resizing\n");
                    resizeDelta = scrollPane.getBounds().height - e.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                //System.out.printf("Mouse released @ (%d,%d)\n", e.getX(), e.getY());
                if (resizing) {
                    //System.out.printf("No longer resizing\n");
                    resizeYZone = contentPane.getBounds().height - scrollPane.getY();
                    setResizeCursor(inResizeZone(e));
                }
                resizing = false;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                //System.out.printf("Mouse exited @ (%d,%d)\n", e.getX(), e.getY());
                setResizeCursor(false);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                //System.out.printf("Mouse entered @ (%d,%d)\n", e.getX(), e.getY());
                resizeYZone = contentPane.getBounds().height - scrollPane.getY();
                setResizeCursor(inResizeZone(e));
            }
        };

        contentPane.addMouseMotionListener(adp);
        contentPane.addMouseListener(adp);
    }

    private class LanguageLabel implements FacetCountProvider {
        private Locale locale;

        LanguageLabel(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String toString() {
            String displayLabel = LanguageUtil.getLocalizedLanguageName(locale);
            int count = result.getLanguageFacetCount(locale.getLanguage());
            if (count > 0) {
                displayLabel += " [" + count + "]";
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

        @Override
        public String toString() {
            return LanguageUtil.getLocalizedLanguageName(locale);
        }
    }

    private void addOptionList() {
        final int NUM_OPTIONS = 2;
        JXTaskPane optionsPane = new JXTaskPane();
        optionsPane.setTitle(LabelProvider.getLabel(LabelProvider.OPTIONS_ROOT_LABEL));
        JPanel optionComponent = new JPanel();

        // user language
        optionComponent.setLayout(new GridLayout(NUM_OPTIONS, 2));
        JLabel uiLangugeLb = new JLabel();
        uiLangugeLb.setText(LabelProvider.getLabel(LabelProvider.OPTIONS_USER_LANGUAGE));
        optionComponent.add(uiLangugeLb);
        UILanguageLabel[] langs = { new UILanguageLabel(Locale.ENGLISH),
            new UILanguageLabel(Locale.GERMAN),
            new UILanguageLabel(Locale.FRENCH) };

        JComboBox<UILanguageLabel> userLanguages = new JComboBox<>(langs);

        userLanguages.addActionListener(e -> {
            JComboBox cb = (JComboBox) e.getSource();
            Locale newLocale;
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

            LanguageUtil.setUILanguage(newLocale); // set for controls that will be
            // created on the fly, like ex.
            // popup menus
            Application.getMessageService()
                .pumpMessage(new UILanguageChanged(newLocale, currLocale));
            currLocale = newLocale;
        });
        optionComponent.add(userLanguages);

        JScrollPane optionsScrollPane = new JScrollPane(optionComponent);
        optionsPane.add(optionsScrollPane);
        // taskPaneContainer.add(optionsPane);
        optionsPane.setCollapsed(true);
    }

    private void populateLanguageList() {
        List<Locale> audioLanguages = ACMConfiguration.getInstance().getCurrentDB()
            .getAudioLanguages();
        for (Locale locale : audioLanguages) {
            languageList.add(new LanguageLabel(locale));
        }

        SwingUtilities.invokeLater(() -> {

            for (LanguageLabel curLabel : languageList) {
                languageRootNode.add(new DefaultMutableTreeNode(curLabel));
            }

            languageTree.setRootVisible(false);
            languageTree.expandPath(new TreePath(languageRootNode.getPath()));

            languageTree.addTreeCheckingListener(e -> {
                clearPlaylistSelection();
                pumpLanguageFilter();
            });
        });

    }

    private void pumpLanguageFilter() {
        // map languages on 'Locales'
        TreePath[] tp = languageTree.getCheckingPaths();
        List<Locale> filterLocales = new ArrayList<>(tp.length);
        for (TreePath aTp : tp) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) aTp
                .getLastPathComponent();
            LanguageLabel obj = (LanguageLabel) node.getUserObject();

            filterLocales.add(obj.getLocale());
        }

        Application.getFilterState().setFilterLanguages(filterLocales);
    }

    private void addListeners() {
        categoryTree.addTreeCheckingListener(e -> {
            clearPlaylistSelection();

            TreePath[] tp = categoryTree.getCheckingPaths();
            List<Category> filterCategories = new ArrayList<>(tp.length);
            for (TreePath aTp : tp) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) aTp
                    .getLastPathComponent();
                CategoryTreeNodeObject obj = (CategoryTreeNodeObject) node
                    .getUserObject();
                Category cat = obj.getCategory();
                if (!cat.hasChildren())
                    filterCategories.add(obj.getCategory());
            }

            Application.getFilterState().setFilterCategories(filterCategories);
        });

        playlistList.addListSelectionListener(e -> {
            clearTreeSelections();
            Application
                .getMessageService().pumpMessage(new AudioItemTableSortOrderMessage(
                LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_PLAYLIST_ORDER),
                SortOrder.ASCENDING));
        });
        addAudioDeviceListener();
    }

    private void fillCategories() {
        Taxonomy taxonomy = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getTaxonomy();

        // Save the ids of the rows that are expanded and checked. We'll re-apply those properties later.
        Set<String> expanded = IntStream.range(0, categoryTree.getRowCount())
            .filter(row -> categoryTree.isExpanded(row))
            .mapToObj(row -> ((CategoryTreeNodeObject) ((DefaultMutableTreeNode) categoryTree.getPathForRow(
                row).getLastPathComponent()).getUserObject()).getCategory().getId())
            .collect(Collectors.toSet());

        Set<String> checked = Application.getFilterState().getFilterCategories().stream()
            .map(Category::getId).collect(Collectors.toSet());

        // Clear the old tree.
        categoryRootNode.removeAllChildren();
        DefaultTreeModel model = (DefaultTreeModel) categoryTree.getModel();
        model.reload();

        // (re-)fill with fresh data.
        Category rootCategory = taxonomy.getRootCategory();
        for (Category c : rootCategory.getSortedChildren()) {
            // Only show categories that can be assigned to.
            if ((c.isVisible() || c.hasVisibleChildren()) && !c.isNonAssignable()) {
                fillChildCategories(categoryRootNode, c);
            }
        }

        // Re-expand the rows that were expanded before. The root is always expanded, and
        // as it is hidden, it can't be collapsed.
        // Re-check the rows that were checked before.
        categoryTree.expandPath(new TreePath(categoryRootNode.getPath()));
        categoryTree.clearChecking();

        Enumeration en = categoryRootNode.depthFirstEnumeration();
        while (en.hasMoreElements()) {

            // Unfortunately the enumeration isn't genericised so we need to downcast
            // when calling nextElement():
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
            CategoryTreeNodeObject obj = (CategoryTreeNodeObject) node.getUserObject();
            if (obj != null) {
                String id = obj.getCategory().getId();
                if (expanded.contains(id)) {
                    categoryTree.expandPath(new TreePath(node.getPath()));
                }
                if (checked.contains(id)) {
                    categoryTree.addCheckingPath(new TreePath(node.getPath()));
                }
            }
        }

    }

    public Collection<String> getExpandedCategories() {
        // Get the ids of the rows that are expanded.
        Set<String> expanded = IntStream.range(0, categoryTree.getRowCount())
            .filter(row -> categoryTree.isExpanded(row))
            .mapToObj(row -> ((CategoryTreeNodeObject) ((DefaultMutableTreeNode) categoryTree.getPathForRow(
                row).getLastPathComponent()).getUserObject()).getCategory().getId())
            .collect(Collectors.toSet());
        return expanded;
    }

    private void fillChildCategories(DefaultMutableTreeNode parent, Category category) {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new CategoryTreeNodeObject(category));
        parent.add(child);
        if (category.hasChildren()) {
            for (Category c : category.getSortedChildren()) {
                // Only show categories that are are user definable. We never let the
                // "nonassignable" categories be visible, and the autogenerated ones always
                // inherit from their parents.
                if ((c.isVisible() || c.hasVisibleChildren()) && !c.isNonAssignable()) {
                    fillChildCategories(child, c);
                }
            }
        }
    }

    private void addDeviceNode(
        DefaultMutableTreeNode parent,
        final DeviceInfo deviceInfo) {
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
                deviceTreeModel.insertNodeInto(child, parent, parent.getChildCount());
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
                    Runnable addDeviceToList = () -> addDeviceNode(deviceRootNode,
                        dce.getDeviceInfo());

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

        playlistList.setDropMode(DropMode.ON);
        playlistList.setTransferHandler(new PlaylistTransferHandler());
    }

    public interface FacetCountProvider {
        int getFacetCount();
    }

    // Helper class for tree nodes
    public class CategoryTreeNodeObject implements FacetCountProvider {
        private Category category;

        CategoryTreeNodeObject(Category category) {
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
            String displayLabel;
            if (category != null) {
                displayLabel = category.getCategoryName();
                int count = result.getFacetCount(category);
                if (count > 0) {
                    displayLabel += " [" + count + "]";
                }
            } else {
                displayLabel = LabelProvider.getLabel("ERROR");
            }

            return displayLabel;
        }
    }

    private static class FacetCountCellRenderer
        extends DefaultCheckboxTreeCellRenderer {
        /**
         * This hack prevents the D&D cursor flickering when hovering over a drop target.
         * @return false, always.
         */
        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree, Object object,
            boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            DefaultCheckboxTreeCellRenderer cell = (DefaultCheckboxTreeCellRenderer) super.getTreeCellRendererComponent(
                tree, object, selected, expanded, leaf, row, hasFocus);

            if (node.getUserObject() != null
                && node.getUserObject() instanceof FacetCountProvider) {
                FacetCountProvider countProvider = (FacetCountProvider) node.getUserObject();
                // make label bold
                Font f = super.label.getFont();
                int count = countProvider.getFacetCount();
                // System.out.println("Node: " + node.toString() + " - Count= " +
                // count);
                if (count > 0) {
                    super.label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
                } else {
                    super.label.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
                }

            }

            return cell;
        }
    }

    /**
     * Renderer to add tooltips for the language panel. Tooltips contain ISO-639 codes.
     */
    private static class LanguageCellRenderer extends FacetCountCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object object,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            Component component = super.getTreeCellRendererComponent(tree,
                object,
                selected,
                expanded,
                leaf,
                row,
                hasFocus);
            if (component != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
                if (node.getUserObject() != null
                    && node.getUserObject() instanceof LanguageLabel) {
                    Locale locale = ((LanguageLabel)node.getUserObject()).getLocale();
                    String tip = LanguageUtil.getLanguageNameWithCode(locale);
                    setToolTipText(tip);
                }
            }
            return component;
        }
    }

    private void updatePlaylistTable() {
        if (!clearingSelections) {
            MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
                .getMetadataStore();
            ListModel<PlaylistLabel> model = new PlaylistListModel(store.getPlaylists(), result);
            playlistList.setModel(model);
        }
    }

    private void addNewPlaylist() {
        String playlistName = (String) JOptionPane.showInputDialog(this,
            "Enter playlist name:", "Add new playlist", JOptionPane.PLAIN_MESSAGE,
            null, null, "");
        playlistName = (playlistName != null) ? playlistName.trim(): playlistName;
        if (!StringUtils.isEmpty(playlistName)) {
            MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
                .getMetadataStore();
            Playlist playlist = store.newPlaylist(playlistName);
            try {
                store.commit(playlist);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Unable to create playlist with name " + playlistName,
                    e);
            }

            Application.getMessageService().pumpMessage(new PlaylistsChanged());
        }
    }

    private void updateTreeNodes() {
        for (Enumeration e = categoryRootNode.breadthFirstEnumeration(); e
            .hasMoreElements(); ) {
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
            CategoryTreeNodeObject obj = (CategoryTreeNodeObject) current
                .getUserObject();
            categoryTree.getModel()
                .valueForPathChanged(new TreePath(current.getPath()), obj);
        }
        for (Enumeration e = languageRootNode.breadthFirstEnumeration(); e
            .hasMoreElements(); ) {
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
            LanguageLabel obj = (LanguageLabel) current.getUserObject();
            languageTree.getModel()
                .valueForPathChanged(new TreePath(current.getPath()), obj);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof UILanguageChanged) {
            updateTreeNodes();
        }

        if (arg instanceof SearchResult) {
            result = (SearchResult) arg;
            updateTreeNodes();
            updatePlaylistTable();
        }

        if (arg instanceof PlaylistsChanged) {
            updatePlaylistTable();
        }

        if (arg instanceof Taxonomy.CategoryVisibilitiesUpdated) {
            fillCategories();
        }
    }

    private boolean clearingSelections = false;

    private void clearPlaylistSelection() {
        if (!clearingSelections) {
            clearingSelections = true;
            try {
                Application.getFilterState().setSelectedPlaylist(null);
                UIUtils.invokeAndWait(() -> playlistList.clearSelection());
            } finally {
                clearingSelections = false;
            }
        }
    }

    private void clearTreeSelections() {
        if (!clearingSelections) {
            clearingSelections = true;

            try {
                UIUtils.invokeAndWait(() -> {
                    categoryTree.clearChecking();
                    languageTree.clearChecking();
                });
            } finally {
                clearingSelections = false;
            }
        }
    }

    private static class DeviceTreeCellRenderer extends DefaultTreeCellRenderer {
        Object highlight;
        ImageIcon icon = new ImageIcon(
            getClass().getResource("/sync-green-16.png"));

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
            JLabel cell = (JLabel) super.getTreeCellRendererComponent(tree, value,
                sel, expanded, leaf, row, hasFocus);
            if (highlight != null && value.equals(highlight)) {
                cell.setIcon(icon);
            }
            cell.setHorizontalTextPosition(SwingConstants.LEFT);
            Dimension d = cell.getPreferredSize();
            cell.setPreferredSize(new Dimension(
                (int) d.getWidth() + icon.getIconWidth() + 15, (int) d.getHeight()));
            return cell;
        }
    }

    private static final TreeSelectionModel NO_SELECTION_MODEL = new DefaultTreeSelectionModel() {
        @Override
        public void addSelectionPath(TreePath path) {
        }

        @Override
        public void addSelectionPaths(TreePath[] paths) {
        }

        @Override
        public void setSelectionPath(TreePath path) {
        }

        @Override
        public void setSelectionPaths(TreePath[] paths) {
        }
    };

    static class PlaylistsChanged {
    }
}
