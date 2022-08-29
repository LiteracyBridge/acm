package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.util.AcmContent;
import org.literacybridge.acm.gui.assistants.util.AcmContent.AudioItemNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.LanguageNode;
import org.literacybridge.acm.gui.assistants.util.AcmContent.PlaylistNode;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdjustmentsPage extends AssistantPage<DeploymentContext> {
    private final JLabel deployment;
    private final JCheckBox includeUfCategory;
    private final JCheckBox includeTbTutorial;
    private final JCheckBox deployTb2AsMp3;
    private final JCheckBox noPublish;
    private final JTree playlistTree;
    private final JButton remove;

    private final DeploymentContext context;

    private final JButton moveUp;
    private final JButton moveDown;
    private final DefaultTreeModel playlistTreeModel;
    private final String introMessageCategoryName;
    private final Set<Object> introMessageCategories = new HashSet<>();

    AdjustmentsPage(PageHelper<DeploymentContext> listener) {
        super(listener);
        context = getContext();
        introMessageCategoryName = store.getCategory(Constants.CATEGORY_INTRO_MESSAGE).getCategoryName();

        setLayout(new GridBagLayout());

        Insets tight = new Insets(0, 0, 5, 0);
        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Adjustments</span>" + "</ul>"
                + "<br/>Changes made here affect only the created deployment. No changes will be made "
                + "to the ACM or Program Specification."
                + "<br/>Make any final adjustments, and then click \"Next\" to create the deployment. "

                + "</html>");
        add(welcome, gbc);

        gbc.insets = tight;
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        deployment = makeBoxedLabel();
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        includeUfCategory = new JCheckBox("Make User Feedback visible.");
        if (ACMConfiguration.getInstance().getCurrentDB().isUserFeedbackHidden()) {
            includeUfCategory.setSelected(false);
            JLabel label = new JLabel("User Feedback is hidden for this program.");
            Font normalFont = label.getFont();
            Font italicFont = new Font(normalFont.getName(),normalFont.getStyle()|Font.ITALIC, normalFont.getSize());
            label.setFont(italicFont);
            add(label, gbc);
        } else {
            includeUfCategory.setSelected(true);
            add(includeUfCategory, gbc);
            includeUfCategory.addActionListener(this::onSelection);
        }


        includeTbTutorial = new JCheckBox("Include Talking Book tutorial ('Talking Book. To learn about this device, press the tree ...')");
        includeTbTutorial.setSelected(true);
        add(includeTbTutorial, gbc);
        includeTbTutorial.addActionListener(this::onSelection);
        deployTb2AsMp3 = new JCheckBox("Create TB-2 deployment with .MP3 audio files.");
        deployTb2AsMp3.setSelected(true);
        if (ACMConfiguration.getInstance().getCurrentDB().hasTbV2Devices()) {
            add(deployTb2AsMp3, gbc);
        }
        noPublish = new JCheckBox("Do not publish the deployment. Create only.");
        // Default to no-publish if running sandboxed.
        noPublish.setSelected(ACMConfiguration.isSandbox());
        noPublish.addActionListener(this::onSelection);
        add(noPublish, gbc);

        // The tree. 
        context.playlistRootNode = new AcmContent.AcmRootNode();
        playlistTree = new JTree(context.playlistRootNode);
        playlistTreeModel = (DefaultTreeModel) playlistTree.getModel();
        playlistTree.setRootVisible(false);
        playlistTree.setShowsRootHandles(true);
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
        remove.setToolTipText("Remove the playlist or audio item from the deployment");
        hbox.add(remove);

        hbox.add(Box.createHorizontalGlue());
        gbc.weighty = 0;
        add(hbox, gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        deployment.setText(Integer.toString(context.deploymentNo));

        includeUfCategory.setSelected(context.includeUfCategory);
        includeTbTutorial.setSelected(context.includeTbTutorial);

        if (progressing) {
            collectDeploymentInformation(context.deploymentNo);
            fillPlaylists();
            expandTree();
        }
        onSelection(null);
        enableButtons();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        context.includeUfCategory = includeUfCategory.isSelected();
        context.includeTbTutorial = includeTbTutorial.isSelected();
        context.deployTb2AsMp3 = deployTb2AsMp3.isSelected();
        // Don't publish if user chose "no publish" option. Also don't publish if running in sandbox.
        context.setNoPublish(noPublish.isSelected());
    }

    @Override
    protected String getTitle() {
        return "Final Adjustments";
    }

    /**
     * Get the languages codes and playlists for the given deployment.
     * @param deploymentNo that we are creating.
     */
    private void collectDeploymentInformation(int deploymentNo) {
        RecipientList recipients = context.getProgramSpec().getRecipientsForDeployment(deploymentNo);
        context.languageCodes = recipients.stream().map(r -> r.languagecode).collect(Collectors.toSet());

        context.allProgramSpecPlaylists = getProgramSpecPlaylists(deploymentNo, context.languageCodes);
        context.allAcmPlaylists = getAcmPlaylists(deploymentNo, context.languageCodes);
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
    private void onMoveDown(@SuppressWarnings("unused") ActionEvent actionEvent) {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();

        int selectedIx = parent.getIndex(selected);
        // Move down amongst siblings?
        if (selectedIx < parent.getChildCount()-1) {
            // Yes.
            selectedIx++;
            System.out.printf("Moving %s to position %d%n", selected, selectedIx);
        } else {
            // No. If the selected node is an audio item, move it to next playlist. Get the auntie node.
            parent = parent.getNextSibling();
            selectedIx = 0;
            System.out.printf("Moving %s to %s%n", selected, parent);
        }

        // Perform the move and restore the tree state.
        moveNode(selected, parent, selectedIx);
    }

    /**
     * Determines the target to which a node should be moved for "up".
     * @param actionEvent is ignored.
     */
    private void onMoveUp(@SuppressWarnings("unused") ActionEvent actionEvent) {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();

        int selectedIx = parent.getIndex(selected);
        // Move up amongst siblings?
        if (selectedIx > 0) {
            // Yes.
            selectedIx--;
            System.out.printf("Moving %s to position %d%n", selected, selectedIx);
        } else {
            // No. If the selected node is an audio item, move it to previous playlist. Get the auntie node.
            parent = parent.getPreviousSibling();
            selectedIx = parent.getChildCount();
            System.out.printf("Moving %s to %s%n", selected, parent);
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


        // And finally, re-select the moved item, and make sure it is visible.
        TreePath path = new TreePath(selected.getPath());
        playlistTree.setSelectionPath(path);
        playlistTree.scrollPathToVisible(path);
    }

    private void onRemove(@SuppressWarnings("unused") ActionEvent actionEvent) {
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
        // The intro message category is first and can't be moved. And the category after any Intro Message
        // playlist can't be moved up.
        if (selected instanceof PlaylistNode) {
            if (introMessageCategories.contains(selected.getUserObject())) return false;
            if (sibling != null && introMessageCategories.contains(sibling.getUserObject())) return false;
        }
        if (sibling != null) return true;
        if (selected instanceof AudioItemNode) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();
            DefaultMutableTreeNode parentSibling = parent.getPreviousSibling();
            // Message node can't move into or out of "Intro Message" category.
            if (introMessageCategories.contains(parent.getUserObject())) return false;
            if (parentSibling != null && introMessageCategories.contains(parentSibling.getUserObject())) return false;
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
        // The intro message category is first and can't be moved.
        if (selected instanceof PlaylistNode && introMessageCategories.contains(selected.getUserObject())) return false;
        DefaultMutableTreeNode sibling = selected.getNextSibling();
        if (sibling != null) return true;
        if (selected instanceof AudioItemNode) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selected.getParent();
            DefaultMutableTreeNode parentSibling = parent.getNextSibling();
            // Message node can't move into or out of "Intro Message" category.
            if (introMessageCategories.contains(parent.getUserObject())) return false;
            if (parentSibling != null && introMessageCategories.contains(parentSibling.getUserObject())) return false;
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
    private void onSelection(@SuppressWarnings("unused") ActionEvent actionEvent) {
        setComplete();
    }

    /**
     * Expands all nodes in the tree.
     */
    private void expandTree() {
        //noinspection rawtypes
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
     * @param playlistsForLanguage a { language : [playlist] }.
     */
    private void fillPlaylistsForLanguage(Map.Entry<String, List<Playlist>> playlistsForLanguage) {
        if (playlistsForLanguage.getValue().size() > 0) {
            LanguageNode node = new LanguageNode(playlistsForLanguage.getKey());
            context.playlistRootNode.add(node);
            playlistsForLanguage.getValue().forEach(pl -> fillPlaylist(node, pl));
        }
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

    /**
     * Gets the playlists defined in the Program Spec for a given Deployment. Note that playlists
     * may be different between languages, due to missing content in some languages.
     *
     * @param deploymentNo of the Deployment.
     * @param languageCodes    of all the Recipients in the Deployment.
     * @return a map of { language : [ContentSpec.PlaylistSpec ] }
     */
    private Map<String, List<ContentSpec.PlaylistSpec>> getProgramSpecPlaylists(int deploymentNo,
        Set<String> languageCodes)
    {
        ContentSpec contentSpec = context.getProgramSpec().getContentSpec();
        ContentSpec.DeploymentSpec deploymentSpec = contentSpec.getDeployment(deploymentNo);
        Map<String, List<ContentSpec.PlaylistSpec>> programSpecPlaylists = new HashMap<>();
        for (String languageCode : languageCodes) {
            programSpecPlaylists.put(languageCode, deploymentSpec.getPlaylistSpecsForLanguage(languageCode));
        }
        return programSpecPlaylists;
    }

    /**
     * Gets the playlists defined in the ACM for a given Deployment. If all content was imported,
     * and playlists were not manually edited, these will completely match the programSpec playlists.
     * Additional playlists may be present, if there were any created with the pattern #-pl-lang.
     *
     * @param deploymentNo of the Deployment.
     * @param languages    of all the Recipients in the Deployment.
     * @return a map of { language : [ Playlist ] }
     */
    private Map<String, List<Playlist>> getAcmPlaylists(int deploymentNo, Set<String> languages) {
        Map<String, List<Playlist>> acmPlaylists = new LinkedHashMap<>();
        Collection<Playlist> playlists = store.getPlaylists();

        introMessageCategories.clear();

        for (String language : languages) {
            List<Playlist> langPlaylists = new ArrayList<>();
            // Look for anything matching the pattern, whether from the Program Spec or not.
            Pattern pattern = Pattern.compile(String.format("%d-.*-%s", deploymentNo, language));
            for (Playlist pl : playlists) {
                Matcher plMatcher = pattern.matcher(pl.getName());
                if (plMatcher.matches()) {
                    langPlaylists.add(pl);
                    // Remember which playlists were "Intro Message" categories.
                    if (introMessageCategoryName.equals(AudioUtils.undecoratedPlaylistName(pl.getName()))) {
                        introMessageCategories.add(pl);
                    }
                }
            }
            Map<String, ContentSpec.PlaylistSpec> specPlaylists =
                context.allProgramSpecPlaylists.get(language)
                    .stream()
                    .collect(Collectors.toMap(ContentSpec.PlaylistSpec::getPlaylistTitle,
                        Function.identity()));
            langPlaylists.sort((a,b)->{
                // If this is the "Intro Message" category, it sorts first.
                if (introMessageCategories.contains(a)) return -1;
                if (introMessageCategories.contains(b)) return 1;
                // Get the names, to look up in the programspec.
                String nameA = AudioUtils.undecoratedPlaylistName(a.getName());
                String nameB = AudioUtils.undecoratedPlaylistName(b.getName());

                ContentSpec.PlaylistSpec psA = specPlaylists.get(nameA);
                ContentSpec.PlaylistSpec psB = specPlaylists.get(nameB);
                if (psA != null && psB != null) {
                    return psA.getOrdinal() - psB.getOrdinal();
                } else if (psA == null && psB == null) {
                    return nameA.compareToIgnoreCase(nameB);
                } else if (psA == null) {
                    return 1;
                }
                return -1;
            });
            acmPlaylists.put(language, langPlaylists);
        }
        return acmPlaylists;
    }

}
