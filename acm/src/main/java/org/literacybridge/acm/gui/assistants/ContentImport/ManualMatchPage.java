package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.Assistant.ImageLabel;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ContentSpec.MessageSpec;
import org.literacybridge.core.spec.ContentSpec.PlaylistSpec;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManualMatchPage extends AssistantPage<ContentImportContext> {
    private final ContentImportContext context;

    private final JLabel deployment;
    private final JLabel language;
    private DefaultMutableTreeNode playlistRootNode;
    private JTree playlistTree;
    private DefaultTreeModel playlistTreeModel;
    private DefaultListModel<String> filesListModel;
    private JList<String> filesList;
    private JButton makeMatchButton;
    private Map<MessageSpec, MatchableImportableAudio> audioItemsMap;
    private Map<String, MatchableImportableAudio> filesMap;

    ManualMatchPage(PageHelper listener) {
        super(listener);
        context = getContext();
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        Insets insets = new Insets(0, 0, 15, 0);
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
            "<html>" + "<span style='font-size:2em'>Manually Match Files.</span>"
                + "<br/><br/><p>Select the matching Audio Items and Files.</p>" + "</html>");
        add(welcome, gbc);

        Border greyBorder = new LineBorder(Color.green); //new LineBorder(new Color(0xf0f0f0));
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing message content for deployment "));
        deployment = parameterText();
        hbox.add(deployment);
        hbox.add(new JLabel(" and language "));
        language = parameterText();
        hbox.add(language);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets.bottom = 0;
        add(makeMatchPanels(), gbc);

//        buildTree(playlistRootNode);
//        playlistTreeModel.reload();
//        for (int i=0; i<playlistTree.getRowCount(); i++)
//            playlistTree.expandRow(i);
//
//        List<File> testingFiles = Collections.singletonList(new File("/Users/bill/A-test1"));
//        List<File> expandedFiles = expandDirectories(testingFiles);
//        expandedFiles
//            .stream()
//            .map(File::getName)
//            .forEach(filesListModel::addElement);
    }

    private JComponent makeMatchPanels() {
        String[] headings = { "Audio Items", "", "Files" };
        String r = "----+----1----+----2----+----3----+----4----+----5----+----6----+----7----+----8----+----9----+----0";
        String[] rulers = { r, "", r };

        // Collator panel is multiple columns.
        JPanel collator = new JPanel();
        collator.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        Arrays
            .asList(headings)
            .forEach(s -> collator.add(new JLabel(s), gbc));
//        gbc.gridy++;
//        Arrays.asList(rulers).forEach(s->collator.add(new JLabel(s), gbc));
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy++;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;

        // The tree.
        playlistRootNode = new DefaultMutableTreeNode();
        playlistTree = new JTree(playlistRootNode);
        playlistTreeModel = (DefaultTreeModel) playlistTree.getModel();
        playlistTree.setRootVisible(false);
        playlistTree.addTreeSelectionListener(ev -> treeListener());
        playlistTree.setDragEnabled(true);
        playlistTree.setDropMode(DropMode.ON);
        playlistTree.setTransferHandler(playlistTreeTransferHandler);
        playlistTree.addTreeSelectionListener(this::treeSelectionListener);
        JScrollPane playlistScrollPane = new JScrollPane(playlistTree);

        collator.add(playlistScrollPane, gbc);

        // The buttons.
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridx = 0;
        buttonGbc.gridy = GridBagConstraints.RELATIVE;
        buttonGbc.anchor = GridBagConstraints.CENTER;
        buttonGbc.fill = GridBagConstraints.NONE;

        makeMatchButton = new JButton("Match!");
        makeMatchButton.setEnabled(false);
        makeMatchButton.addActionListener(this::onMakeMatchAction);
        buttonPanel.add(makeMatchButton, buttonGbc);
        JLabel matchPrompt = new JLabel("<html>" + "Select an Audio Item on the left "
            + "and a File on the right, then click the <b>Match!</b> button."
            + "<br/><br/>You can click to select, or drag items to their match in the other panel."
            + "</html>");
        Font cf = ImageLabel.fontResource(ImageLabel.AVENIR);
        matchPrompt.setFont(cf.deriveFont(11f));
        matchPrompt.setBorder(new EmptyBorder(5, 5, 0, 5));
        buttonGbc.weightx = 1.0;
        buttonGbc.fill = GridBagConstraints.HORIZONTAL;
        buttonPanel.add(matchPrompt, buttonGbc);

        buttonGbc.weighty = 1.0;
        buttonGbc.weightx = 1.0;
        buttonPanel.add(new JLabel(""), buttonGbc);

        gbc.weightx = 0.10;
        collator.add(buttonPanel, gbc);
        gbc.weightx = 0.6;

        filesListModel = new DefaultListModel<>();
        filesList = new JList<>(filesListModel);
        filesList.setDragEnabled(true);
        filesList.setTransferHandler(fileListTransferHandler);
        filesList.addListSelectionListener(this::fileSelectionListener);
        JScrollPane filesPreviewScroller = new JScrollPane(filesList);
        collator.add(filesPreviewScroller, gbc);

        return collator;
    }

    private void onMakeMatchAction(ActionEvent actionEvent) {
        // Remove node from playlist tree. If that empties the playlist, remove the playlist.
        TreePath treeSelection = playlistTree.getSelectionPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeSelection.getLastPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        playlistTreeModel.removeNodeFromParent(node);
        if (parent != null && parent.getChildCount() == 0) {
            playlistTreeModel.removeNodeFromParent(parent);
        }

        // Remove file from the files list.
        String filename = filesList.getSelectedValue();
        int[] listSelection = filesList.getSelectedIndices();
        filesListModel.removeElementAt(listSelection[0]);
        playlistTree.clearSelection();
        filesList.clearSelection();

        if (node instanceof AudioItemNode) {
            MessageSpec message = ((AudioItemNode) node).matchableItem.getLeft().getMessage();
            if (audioItemsMap.containsKey(message) && filesMap.containsKey(filename)) {
                MatchableImportableAudio a = audioItemsMap.get(message);
                MatchableImportableAudio b = filesMap.get(filename);
                context.matcher.setMatch(a, b);
            }
        }

    }

    private void fileSelectionListener(ListSelectionEvent listSelectionEvent) {
        enableMatchButton();
    }

    private void treeSelectionListener(TreeSelectionEvent event) {
        enableMatchButton();
    }

    private void enableMatchButton() {
        boolean canMatch = false;
        // Both an audio item and a file selected?
        TreePath treeSelection = playlistTree.getSelectionPath();
        if (treeSelection != null) {
            canMatch = (treeSelection.getLastPathComponent() instanceof AudioItemNode) && (
                filesList.getSelectedIndices().length > 0);
        }
        makeMatchButton.setEnabled(canMatch);
    }

    private TransferHandler playlistTreeTransferHandler = new TransferHandler() {
        @Override
        public int getSourceActions(JComponent c) {
            return COPY | LINK | MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection("tree");
        }

        @Override
        public boolean canImport(TransferSupport support) {
            boolean can = false;
            try {
                String source = support.getTransferable().getTransferData(DataFlavor.stringFlavor).toString();
                if (source.equals("files")) {
                    // If this is coming from the "files" side, clear our selection, in preparation for the drop.
                    playlistTree.clearSelection();
                } else if (source.equals("tree")) {
                    // If coming from the "playlist" side, do not accept the drop.
                    support.setShowDropLocation(false);
                    return false;
                }
            } catch (Exception ignored) {}
            if (support.getDropLocation() instanceof JTree.DropLocation) {
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dropLocation.getPath();
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    can = node instanceof AudioItemNode;
                }
            }
            support.setShowDropLocation(can);
            return can;
        }

        @Override
        public boolean importData(TransferSupport support) {
            boolean imported = false;
            if (support.getDropLocation() instanceof JTree.DropLocation) {
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dropLocation.getPath();
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if (node instanceof AudioItemNode) {
                        imported = true;
                        playlistTree.setSelectionPath(path);
                    }
                }
            }
            support.setShowDropLocation(imported);
            return imported;
        }
    };

    private TransferHandler fileListTransferHandler = new TransferHandler() {
        @Override
        public int getSourceActions(JComponent c) {
            return COPY | LINK | MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection("files");
        }

        @Override
        public boolean canImport(TransferSupport support) {
            boolean can = false;
            try {
                String source = support.getTransferable().getTransferData(DataFlavor.stringFlavor).toString();
                if (source.equals("tree")) {
                    // If an item from the other list is being dropped, clear our selection.
                    filesList.clearSelection();
                } else if (source.equals("files")) {
                    // If from this side, do not accept the drop.
                    support.setShowDropLocation(false);
                    return false;
                }
            } catch (Exception ignored) {}
            if (support.getDropLocation() instanceof JList.DropLocation) {
                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
                int index = dropLocation.getIndex();
                if (index >= 0) {
                    can = true;
                }
            }
            support.setShowDropLocation(can);
            return can;
        }

        @Override
        public boolean importData(TransferSupport support) {
            boolean imported = false;
            if (support.getDropLocation() instanceof JList.DropLocation) {
                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
                int index = dropLocation.getIndex();
                if (index >= 0) {
                    filesList.setSelectedIndex(index);
                    imported = true;
                }
            }
            support.setShowDropLocation(imported);
            return imported;
        }

    };

    private void treeListener() {

    }

    @Override
    protected void onPageEntered(boolean progressing) {
        // Fill deployment and language
        deployment.setText(Integer.toString(context.deploymentNo));
        language.setText(context.languagecode);

        // Unmatched files.
        filesListModel.clear();
        // Get the unmatched files as a stream.
        List<MatchableImportableAudio> unmatchedFileItems = context.matcher.matchableItems
            .stream()
            .filter(item -> item.getMatch() == MATCH.RIGHT_ONLY)
            .collect(Collectors.toList());
        // Build a map of { filename : MatchableImportableAudio }
        filesMap = unmatchedFileItems
            .stream()
            .collect(Collectors.toMap(item -> item
                .getRight()
                .getFile()
                .getName(), item -> item));

        // Add the unmatched filenames to the list of filenames.
        unmatchedFileItems
            .stream()
            .map(item -> item
                .getRight()
                .getFile()
                .getName())
            .forEach(filesListModel::addElement);

        // Unmatched messages.
        playlistRootNode.removeAllChildren();
        // Get the unmatched Audio Items as a stream.
        List<MatchableImportableAudio> unmatchedAudioItems = context.matcher.matchableItems
            .stream()
            .filter(item -> item.getMatch() == MATCH.LEFT_ONLY)
            .collect(Collectors.toList());
        // Build a map of { audio : MatchableImportableAudio }
        audioItemsMap = new HashMap<>();
        unmatchedAudioItems
            .forEach(item -> {
                audioItemsMap.put(item.getLeft().getMessage(), item);});

        // Helper object to keep track of which playlist nodes have already been built.
        Map<String, PlaylistNode> playlistNodes = new HashMap<>();
        // Build a tree of the unmatched audio items.
        unmatchedAudioItems.forEach(item -> {
            MessageSpec message = item
                .getLeft()
                .getMessage();
            PlaylistNode playlistNode = playlistNodes.get(message.getPlaylistTitle());
            if (playlistNode == null) {
                playlistNode = new PlaylistNode(message.getPlaylistTitle());
                playlistRootNode.add(playlistNode);
                playlistNodes.put(message.getPlaylistTitle(), playlistNode);
            }
            AudioItemNode messageNode = new AudioItemNode(item);
            playlistNode.add(messageNode);
        });
        playlistTreeModel.reload();
        for (int i = 0; i < playlistTree.getRowCount(); i++)
            playlistTree.expandRow(i);

        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Manually Match Audio Items to Files";
    }

    /**
     * Node class for a Language. One or more languages in a Deployment. These can't be
     * re-arranged (because it makes no difference on a TB).
     */
    public static class LanguageNode extends DefaultMutableTreeNode {
        final String languagecode;

        LanguageNode(String languagecode) {
            this.languagecode = languagecode;
        }

        public String toString() {
            return languagecode;
        }
    }

    /**
     * Node class for a Playlist. One or more playlists in a language. These can be
     * re-arranged within their language.
     */
    public static class PlaylistNode extends DefaultMutableTreeNode {
        final String playlist;

        PlaylistNode(String playlist) {
            this.playlist = playlist;
        }

        public String toString() {
            return playlist;
        }
    }

    /**
     * Node class for an Audio Item. One or more Audio Items in a playlist. These can
     * be re-arranged within their playlist, and can also be moved to a different playlist
     * within the language.
     */
    public static class AudioItemNode extends DefaultMutableTreeNode {
        final MatchableImportableAudio matchableItem;
        final String title;

        AudioItemNode(MatchableImportableAudio matchableItem) {
            this.matchableItem = matchableItem;
            this.title = matchableItem
                .getLeft()
                .getMessage()
                .getTitle();
        }

        public String toString() {
            return title;
        }
    }

}
