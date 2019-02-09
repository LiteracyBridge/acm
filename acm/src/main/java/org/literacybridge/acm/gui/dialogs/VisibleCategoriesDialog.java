package org.literacybridge.acm.gui.dialogs;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Taxonomy;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.literacybridge.acm.utils.SwingUtils.getApplicationRelativeLocation;

public class VisibleCategoriesDialog extends JDialog {
    private Taxonomy originalTaxonomy;
    private Collection<String> originalExpanded;

    private Taxonomy taxonomy;
    private final DefaultMutableTreeNode categoryRootNode;
    private CheckboxTree categoryTree;

    private VisibleCategoriesDialog(JFrame owner) {
        super(owner,
            LabelProvider.getLabel("CHOOSE_VISIBLE_CATEGORIES"),
            ModalityType.APPLICATION_MODAL);
        preserveState();

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        add(dialogPanel);

        // The tree. Note that the root does not hold the taxonomy root. (TODO: why not?)
        categoryRootNode = new DefaultMutableTreeNode();
        categoryTree = new CheckboxTree(categoryRootNode);
        categoryTree.setRootVisible(false);
        categoryTree.addTreeCheckingListener(this::categoryCheckedHandler);
        JScrollPane categoryScrollPane = new JScrollPane(categoryTree);
        // Set the size large, so it will take up most of the Vertical Box.
        categoryScrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        fillCategories();

        dialogPanel.add(categoryScrollPane);
        dialogPanel.add(Box.createVerticalStrut(5));

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        JButton cancelButton = new JButton(LabelProvider.getLabel("CANCEL"));
        buttonBox.add(cancelButton);
        buttonBox.add(Box.createHorizontalStrut(6));
        JButton okButton = new JButton(LabelProvider.getLabel("OK"));
        buttonBox.add(okButton);
        dialogPanel.add(buttonBox);

        cancelButton.addActionListener(this::cancelButtonHandler);
        okButton.addActionListener(this::okButtonHandler);

        setSize(400, 600);
    }

    private void preserveState() {
        originalTaxonomy = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore()
            .getTaxonomy()
            .clone();

        originalExpanded = Application.getApplication()
            .getResourceView()
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

        Enumeration en = categoryRootNode.depthFirstEnumeration();
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
        cat.setVisible(visible);
        for (Category child : cat.getChildren()) {
            fillVisibility(child, visible);
        }
    }

    private void cancelButtonHandler(ActionEvent actionEvent) {
        this.setVisible(false);
    }

    private void okButtonHandler(ActionEvent actionEvent) {
        // Build a map of id:isVisible from our copy of the taxonomy, and update the global taxonomy.
        Map<String, Boolean> visiblityStates = StreamSupport.stream(taxonomy.breadthFirstIterator()
            .spliterator(), false).collect(Collectors.toMap(Category::getId, Category::isVisible));
        ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore()
            .getTaxonomy()
            .updateCategoryVisibility(visiblityStates);

        // Persist the visibility in the "category.whitelist" file.
        boolean ok = ACMConfiguration.getInstance().getCurrentDB().writeCategoryFilter(taxonomy);

        this.setVisible(false);
    }

    public static void showDialog(ActionEvent e) {
        VisibleCategoriesDialog dialog = new VisibleCategoriesDialog(Application.getApplication());
        // Place the new dialog within the application frame. This is hacky, but if it fails, the dialog
        // simply winds up in a funny place. Unfortunately, Swing only lets us get the location of a
        // component relative to its parent.
        Point pAudio = getApplicationRelativeLocation(Application.getApplication()
            .getResourceView()
            .getAudioItemView());
        dialog.setLocation(pAudio);
        dialog.setVisible(true);
    }
}
