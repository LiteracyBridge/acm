package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.util.AcmContent.AudioItemNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.LanguageNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.PlaylistNode;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class TweaksPage extends AssistantPage<DeploymentContext> {
    private final JLabel deployment;
    private final JCheckBox includeUfCategory;
    private final JCheckBox includeTbCategory;
    private final JCheckBox noPublish;
    private final JTree playlistTree;
    private final JButton remove;

    private DeploymentContext context;

    private final JButton moveUp;
    private final JButton moveDown;
    private final DefaultTreeModel playlistTreeModel;

    static TweaksPage Factory(PageHelper listener) {
        return new TweaksPage(listener);
    }

    private TweaksPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0, 0, 15, 0);
        Insets tight = new Insets(0, 0, 5, 0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Adjustments</span>" + "</ul>"
                + "<br/>Changes made here affect only the created Deployment. No changes will be made "
                + "to the ACM or to the Program Specification."
                + "<br/>Make any final adjustments, and click \"Next\" to create the Deployment. "

                + "</html>");
        add(welcome, gbc);

        gbc.insets = tight;
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        deployment = parameterText();
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        includeUfCategory = new JCheckBox("Include User Feedback Category.");
        includeUfCategory.setSelected(true);
        add(includeUfCategory, gbc);
        includeUfCategory.addActionListener(this::onSelection);
        includeTbCategory = new JCheckBox("Include Talking Book category ('The Talking Book is an audio computer, "
            +"that shares knowledge...')");
        add(includeTbCategory, gbc);
        includeTbCategory.addActionListener(this::onSelection);
        noPublish = new JCheckBox("Do not publish the Deployment; create only.");
        // Always allow publish from a test database.
        // Do not allow publish from production if running sandboxed.
        noPublish.setSelected(!ACMConfiguration.isTestAcm() || ACMConfiguration.isSandbox());
        noPublish.addActionListener(this::onSelection);
        add(noPublish, gbc);

        // The tree. 
        context.playlistRootNode = new DefaultMutableTreeNode();
        playlistTree = new JTree(context.playlistRootNode);
        playlistTreeModel = (DefaultTreeModel) playlistTree.getModel();
        playlistTree.setRootVisible(false);
        playlistTree.addTreeSelectionListener(ev -> enableButtons());
        JScrollPane playlistScrollPane = new JScrollPane(playlistTree);
        // Set the size large, so it will take up most of the Vertical Box.
        playlistScrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(playlistScrollPane, gbc);

        hbox = Box.createHorizontalBox();
        moveUp = new JButton("Move Up");
        moveUp.addActionListener(this::onMoveUp);
        moveUp.setToolTipText("Move the Playlist or Audio Item up");
        hbox.add(moveUp);
        hbox.add(Box.createHorizontalStrut(5));
        moveDown = new JButton("Move Down");
        moveDown.addActionListener(this::onMoveDown);
        moveDown.setToolTipText("Move the Playlist or Audio Item down");
        hbox.add(moveDown);
        hbox.add(Box.createHorizontalStrut(5));
        remove = new JButton("Remove");
        remove.addActionListener(this::onRemove);
        remove.setToolTipText("Remove the Playlist or Audio Item from the Deployment");
        hbox.add(remove);

        hbox.add(Box.createHorizontalGlue());
        gbc.weighty = 0;
        add(hbox, gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        deployment.setText(Integer.toString(context.deploymentNo));

        if (progressing) {
            fillPlaylists();
            expandTree();
        }
        onSelection(null);
        enableButtons();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        context.includeUfCategory = includeUfCategory.isSelected();
        context.includeTbCategory = includeTbCategory.isSelected();
        // For time being, "no publish" if not a test database.
        context.noPublish = noPublish.isSelected() || !ACMConfiguration.isTestAcm() || ACMConfiguration.isSandbox();
    }

    @Override
    protected String getTitle() {
        return "Final Adjustments";
    }

    /**
     * Enable the edit buttons based on the selection, if any.
     */
    private void enableButtons() {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
        moveUp.setEnabled(canMoveUp(selected));
        moveDown.setEnabled(canMoveDown(selected));
        remove.setEnabled(canRemove(selected));
    }

    /**
     * Determines the target to which a node should be moved for "down".
     * @param actionEvent is ignored.
     */
    private void onMoveDown(ActionEvent actionEvent) {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();

        int selectedIx = parent.getIndex(selected);
        // Move down amongst siblings?
        if (selectedIx < parent.getChildCount()-1) {
            // Yes.
            selectedIx++;
            System.out.println(String.format("Moving %s to position %d", selected, selectedIx));
        } else {
            // No. If the selected node is an audio item, move it to next playlist. Get the auntie node.
            parent = parent.getNextSibling();
            selectedIx = 0;
            System.out.println(String.format("Moving %s to %s", selected, parent));
        }

        // Perform the move and restore the tree state.
        moveNode(selected, parent, selectedIx);
    }

    /**
     * Determines the target to which a node should be moved for "up".
     * @param actionEvent is ignored.
     */
    private void onMoveUp(ActionEvent actionEvent) {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();

        int selectedIx = parent.getIndex(selected);
        // Move up amongst siblings?
        if (selectedIx > 0) {
            // Yes.
            selectedIx--;
            System.out.println(String.format("Moving %s to position %d", selected, selectedIx));
        } else {
            // No. If the selected node is an audio item, move it to previous playlist. Get the auntie node.
            parent = parent.getPreviousSibling();
            selectedIx = parent.getChildCount();
            System.out.println(String.format("Moving %s to %s", selected, parent));
        }

        // Perform the move and restore the tree state.
        moveNode(selected, parent, selectedIx);
    }

    /**
     * Moves a node from one place to another. May move to a new parent or stay in current
     * parent.
     * @param selected node to be moved.
     * @param newParent of the node (may be existing parent).
     * @param ix to which the node is to be moved.
     */
    private void moveNode(DefaultMutableTreeNode selected, DefaultMutableTreeNode newParent, int ix) {
        // Remember the expansion state, because the reloads will clobber it.
        Enumeration<TreePath> expanded = playlistTree.getExpandedDescendants(new TreePath(context.playlistRootNode.getPath()));
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selected.getParent();

        // Perform the actual move.
        playlistTreeModel.insertNodeInto(selected, newParent, ix);

        // Reload both affected trees.
        playlistTreeModel.reload(parent);
        playlistTreeModel.reload(newParent);

        // Restore the expansion state.
        while (expanded.hasMoreElements()) {
            TreePath path = expanded.nextElement();
            playlistTree.expandPath(path);
        }

        // Just for good measure, ensure the "to" tree is expanded.
        playlistTree.expandPath(new TreePath(newParent.getPath()));


        // And finally, re-select the moved item.
        TreePath path = new TreePath(selected.getPath());
        playlistTree.setSelectionPath(path);

    }

    private void onRemove(ActionEvent actionEvent) {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();
        // Select the next sibling if there is one, else the previous sibling, else the parent.
        DefaultMutableTreeNode newSelection = selected.getNextSibling();
        if (newSelection == null) newSelection = selected.getPreviousSibling();
        if (newSelection == null) newSelection = parent;
        // Do the actual removal.
        playlistTreeModel.removeNodeFromParent(selected);
        // Restore the tree state.
        playlistTreeModel.reload(parent);
        playlistTree.expandPath(new TreePath(parent.getPath()));
        // Make the new selection.
        TreePath path = new TreePath(newSelection.getPath());
        playlistTree.setSelectionPath(path);

    }

    /**
     * Determines if the currently selected node, if any, can be moved up.
     * @param selected node.
     * @return true if OK to move the node up, false otherwise.
     */
    private boolean canMoveUp(DefaultMutableTreeNode selected) {
        if (selected == null) return false;
        if (selected instanceof LanguageNode) return false;
        DefaultMutableTreeNode sibling = selected.getPreviousSibling();
        if (sibling != null) return true;
        if (selected instanceof AudioItemNode) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();
            DefaultMutableTreeNode parentSibling = parent.getPreviousSibling();
            return parentSibling != null;
        }
        return false;
    }

    /**
     * Determines if the currently selected node, if any, can be moved down.
     * @param selected node.
     * @return true if OK to move the node down, false otherwise.
     */
    private boolean canMoveDown(DefaultMutableTreeNode selected) {
        if (selected == null) return false;
        if (selected instanceof LanguageNode) return false;
        DefaultMutableTreeNode sibling = selected.getNextSibling();
        if (sibling != null) return true;
        if (selected instanceof AudioItemNode) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();
            DefaultMutableTreeNode parentSibling = parent.getNextSibling();
            return parentSibling != null;
        }
        return false;
    }

    /**
     * Determines if the currently selected node, if any, is valid to be removed.
     * @param selected node.
     * @return true if OK to remove the node, false otherwise.
     */
    private boolean canRemove(DefaultMutableTreeNode selected) {
        if (selected == null) return false;
        return (selected instanceof AudioItemNode || selected instanceof PlaylistNode);
    }
    /**
     * Called when a selection changes.
     *
     * @param actionEvent is ignored.
     */
    private void onSelection(ActionEvent actionEvent) {
        boolean okToPublish = ACMConfiguration.isTestAcm() || !ACMConfiguration.isSandbox();
        if (!okToPublish) {
            noPublish.setSelected(true);
        }
        setComplete();
    }

    /**
     * Expands all nodes in the tree.
     */
    private void expandTree() {
        Enumeration e = context.playlistRootNode.breadthFirstEnumeration();
        while(e.hasMoreElements()) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)e.nextElement();
            if(node.isLeaf()) continue;
            int row = playlistTree.getRowForPath(new TreePath(node.getPath()));
            playlistTree.expandRow(row);
        }
    }

    /**
     * Fills the playlist tree.
     */
    private void fillPlaylists() {
        context.playlistRootNode.removeAllChildren();

        context.allAcmPlaylists.entrySet()
            .forEach(this::fillPlaylistsForLanguage);

        playlistTreeModel.reload();

        playlistTree.expandRow(0);
    }

    /**
     * Fills the tree for a single language.
     * @param stringListEntry a { language : [playlist] }.
     */
    private void fillPlaylistsForLanguage(Map.Entry<String, List<Playlist>> stringListEntry) {
        LanguageNode node = new LanguageNode(stringListEntry.getKey());
        context.playlistRootNode.add(node);
        stringListEntry.getValue().forEach(pl->fillPlaylist(node, pl));
    }

    /**
     * Creates a Playlist node and adds it to its parent language node. Then populates the
     * playlist with its audio items.
     * @param languageNode that contains this playlist.
     * @param playlist to be added.
     */
    private void fillPlaylist(LanguageNode languageNode, Playlist playlist) {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        PlaylistNode playlistNode = new PlaylistNode(playlist);
        languageNode.add(playlistNode);

        playlist
            .getAudioItemList()
            .stream()
            .map(store::getAudioItem)
            .forEach(item -> {
                AudioItemNode messageNode = new AudioItemNode(item);
                playlistNode.add(messageNode);
            });
    }


}
