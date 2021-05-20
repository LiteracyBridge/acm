package org.literacybridge.acm.gui.settings;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.SidebarView;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Taxonomy;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class VisibleCategoriesPanel extends AbstractSettingsBase {
    private Taxonomy originalTaxonomy;
    private Collection<String> originalExpanded;

    private Taxonomy taxonomy;
    private final DefaultMutableTreeNode categoryRootNode;
    private final CheckboxTree categoryTree;

    VisibleCategoriesPanel(AbstractSettingsDialog.SettingsHelper helper) {
        super(helper);
        preserveState();
        setLayout(new BorderLayout());

        add(new JLabel(LabelProvider.getLabel("Visible categories, applies to the entire program.")), BorderLayout.NORTH);

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        add(dialogPanel, BorderLayout.CENTER);

        // The tree. Note that the root does not hold the taxonomy root. (TODO: why not?)
        categoryRootNode = new DefaultMutableTreeNode();
        categoryTree = new CheckboxTree(categoryRootNode);
        categoryTree.setRootVisible(false);
        categoryTree.setSelectionModel(SidebarView.NO_SELECTION_MODEL);
        categoryTree.getCheckingModel().setCheckingMode(TreeCheckingModel.CheckingMode.PROPAGATE_PRESERVING_CHECK);
        categoryTree.addTreeCheckingListener(this::categoryCheckedHandler);
        JScrollPane categoryScrollPane = new JScrollPane(categoryTree);
        // Set the size large, so it will take up most of the Vertical Box.
        categoryScrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        fillCategories();

        dialogPanel.add(categoryScrollPane);
    }

    private void preserveState() {
        originalTaxonomy = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore()
            .getTaxonomy()
            .clone();

        originalExpanded = Application.getApplication()
            .getMainView()
            .getSidebarView()
            .getExpandedCategories();
    }

    /**
     * Fill the category tree. If already filled, empty it first, giving the
     * effect of resetting the tree.
     * <p>
     * If the tree already had content, and some of the nodes were expanded, those
     * expansions are preserved.
     */
    private void fillCategories() {
        // Clear the old tree.
        categoryRootNode.removeAllChildren();
        DefaultTreeModel model = (DefaultTreeModel) categoryTree.getModel();
        model.reload();

        taxonomy = originalTaxonomy.clone();
        Category rootCategory = taxonomy.getRootCategory();
        for (Category c : rootCategory.getSortedChildren()) {
            // Only show categories that can be assigned to.
            if (!c.isNonAssignable()) {
                fillChildCategories(categoryRootNode, c);
            }
        }

        // Re-expand the rows that were expanded before. The root is always expanded, and
        // as it is hidden, it can't be collapsed.
        categoryTree.expandPath(new TreePath(categoryRootNode.getPath()));

        Enumeration<?> en = categoryRootNode.depthFirstEnumeration();
        while (en.hasMoreElements()) {

            // Unfortunately the enumeration isn't genericised so we need to downcast
            // when calling nextElement():
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
            Object obj = node.getUserObject();
            if (obj instanceof Category) {
                String id = ((Category) obj).getId();
                if (originalExpanded.contains(id)) {
                    categoryTree.expandPath(new TreePath(node.getPath()));
                }
            }
        }
    }

    /**
     * Helper to fill the tree with a category and its children.
     *
     * @param parent   The parent tree node to get the new child the Category.
     * @param category The child Category to be placed into the tree.
     */
    private void fillChildCategories(DefaultMutableTreeNode parent, Category category) {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(category);
        parent.add(child);
        // If the category is visible, check the box in the tree.
        if (category.isVisible()) {
            categoryTree.addCheckingPath(new TreePath(child.getPath()));
        }
        if (category.hasChildren()) {
            for (Category c : category.getSortedChildren()) {
                // Only show categories that are are user definable. We never let the
                // "nonassignable" categories be visible, and the autogenerated ones always
                // inherit from their parents.
                if (!c.isNonAssignable() && !c.isAutogenerated()) {
                    fillChildCategories(child, c);
                }
            }
        }
    }

    /**
     * Called when the user checks or unchecks an item. We only get notification of the item
     * itself, and must perform any desired propagation ourselves.
     *
     * @param ev event describing the node checked or un-checked.
     */
    private void categoryCheckedHandler(TreeCheckingEvent ev) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) ev.getPath().getLastPathComponent();
        Category cat = (Category) node.getUserObject();
        if (cat != null) {
            fillVisibility(cat, ev.isCheckedPath());
        }
    }

    /**
     * Helper to apply visibility to the given category and its children.
     *
     * @param cat     to be made visible or not.
     * @param visible whether should be visible.
     */
    private static void fillVisibility(Category cat, boolean visible) {
        cat.updateVisibility(visible);
        for (Category child : cat.getChildren()) {
            fillVisibility(child, visible);
        }
    }

    @Override
    public void onCancel() {
        // Nothing to do; just abandon any changes.
    }

    @Override
    public void onOk() {
        if (!taxonomy.equals(originalTaxonomy)) {
            // Build a map of id:isVisible from our copy of the taxonomy, and update the global taxonomy.
            Map<String, Boolean> visiblityStates = StreamSupport.stream(taxonomy.breadthFirstIterator()
                .spliterator(), false).collect(Collectors.toMap(Category::getId, Category::isVisible));
            ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getTaxonomy().updateCategoryVisibility(visiblityStates);

            // Persist the visibility in the "category.includelist" file.
            ACMConfiguration.getInstance().getCurrentDB().writeCategoryFilter(taxonomy);
        }
    }

    @Override
    public String getTitle() {
        return LabelProvider.getLabel("VISIBLE_CATEGORIES");
    }

}
