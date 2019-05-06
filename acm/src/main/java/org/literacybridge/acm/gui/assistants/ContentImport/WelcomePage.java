package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.SidebarView;
import org.literacybridge.acm.gui.assistants.util.PSContent;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class WelcomePage extends ContentImportBase<ContentImportContext> {
    private static final Logger LOG = Logger.getLogger(WelcomePage.class.getName());

    private final JComboBox<Object> deploymentChooser;
    private final JComboBox<String> languageChooser;
    private final Box titlePreviewBox;
    private final JScrollPane titlePreviewScroller;

    private final DefaultMutableTreeNode progSpecRootNode;
    private final DefaultTreeModel progSpecTreeModel;
    private final JTree progSpecTree;

    WelcomePage(PageHelper<ContentImportContext> listener) {
        super(listener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel("<html>"
            + "<span style='font-size:2.5em'>Welcome to the Content Import Assistant.</span>"
            + "<br/><br/><p>This assistant will guide you through importing audio content into your project. Steps to import audio:</p>"
            + "<ol>"
            + "<li> You choose the Deployment #, and the Language of the content you want to import.</li>"
            + "<li> You choose the files and folders containing the content.</li>"
            + "<li> The assistant will automatically match as many imported files as it can. You will "
            + "have an opportunity to match remaining files, or to \"unmatch\" files as needed.</li>"
            + "<li> You review and approve the final message-to-file matches.</li>"
            + "<li> The audio items are imported into the ACM, and placed in appropriate playlists.</li>"
            + "</ol>"
            + "</html>");
        add(welcome, gbc);

        // Deployment # and language chooser, in a HorizontalBox.
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Choose the Deployment: "));
        deploymentChooser = new JComboBox<>();
        deploymentChooser.addActionListener(this::onSelection);
        setComboWidth(deploymentChooser, "Choose...");
        deploymentChooser.setMaximumSize(deploymentChooser.getPreferredSize());
        hbox.add(deploymentChooser);
        hbox.add(Box.createHorizontalStrut(10));

        hbox.add(new JLabel("and the Language: "));
        languageChooser = new LanguageChooser();
        languageChooser.addActionListener(this::onSelection);
        setComboWidth(languageChooser, "Choose...");
        languageChooser.setMaximumSize(languageChooser.getPreferredSize());
        hbox.add(languageChooser);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        add(new JLabel("Click 'Next' when you are ready to continue."), gbc);

        // Title preview.
        titlePreviewBox = Box.createHorizontalBox();
        titlePreviewBox.add(new JLabel("Playlists and Message Titles in the Deployment. (Titles with "));
        titlePreviewBox.add(new JLabel(soundImage));
        titlePreviewBox.add(new JLabel(" have content in the ACM, with "));
        titlePreviewBox.add(new JLabel(noSoundImage));
        titlePreviewBox.add(new JLabel("do not.):"));

        titlePreviewBox.add(Box.createHorizontalGlue());

        gbc.insets = new Insets(0,0,0,0);
        add(titlePreviewBox, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));

        progSpecRootNode = new DefaultMutableTreeNode();
        progSpecTree = new JTree(progSpecRootNode);
        progSpecTreeModel = (DefaultTreeModel) progSpecTree.getModel();
        progSpecTree.setRootVisible(false);
        progSpecTree.setCellRenderer(treeCellRenderer);

        titlePreviewScroller = new JScrollPane(progSpecTree);
        panel.add(titlePreviewScroller, BorderLayout.CENTER);
        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.bottom = 0;
        add(panel, gbc);

        titlePreviewBox.setVisible(false);
        titlePreviewScroller.setVisible(false);

        getProjectInfo();
    }

    /**
     * Called when a selection changes. Inspect the deployment and language
     * selections, and if both have a selection, enable the "Next" button.
     *
     * @param actionEvent is unused. (Examine entire state when any part changes.
     */
    private void onSelection(ActionEvent actionEvent) {
        int deploymentNo = getSelectedDeployment();
        if (actionEvent != null && actionEvent.getSource() == deploymentChooser) {
            deploymentChooser.setBorder(deploymentNo < 0 ? redBorder : blankBorder);
            // If no deployment chosen, no language, either.
            if (deploymentNo < 0) {
                languageChooser.removeAllItems();
            } else {
                fillLanguagesForDeployment(deploymentNo);
            }
        }
        deploymentChooser.setBorder(getSelectedDeployment()<=0 ? redBorder : blankBorder);
        String languagecode = getSelectedLanguage();
        languageChooser.setBorder(languagecode == null ? redBorder : blankBorder);

        fillTitleList();

        setComplete(deploymentNo >= 0 && languagecode != null);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        // Fill deployments
        deploymentChooser.removeAllItems();
        deploymentChooser.insertItemAt("Choose...", 0);
        List<String> deployments = context.programSpec.getDeployments()
            .stream()
            .map(d -> Integer.toString(d.deploymentnumber))
            .collect(Collectors.toList());
        deployments
            .forEach(deploymentChooser::addItem);

        // Empty the list until we have a deployment.
        languageChooser.removeAllItems();

        // If only one deployment, or previously selected, auto-select.
        if (deployments.size() == 1) {
            deploymentChooser.setSelectedIndex(1); // only item after "choose..."
        } else if (context.deploymentNo >= 0) {
            deploymentChooser.setSelectedItem(Integer.toString(context.deploymentNo));
        }
        if (deploymentChooser.getSelectedIndex() > 0) {
            fillLanguagesForDeployment(getSelectedDeployment());
        }
        if (context.languagecode != null) {
            languageChooser.setSelectedItem(context.languagecode);
        }
        onSelection(null);
    }

    private void fillLanguagesForDeployment(int deploymentNo) {
        languageChooser.removeAllItems();
        languageChooser.insertItemAt("Choose...", 0);
        Set<String> languages = context.programSpec.getLanguagesForDeployment(deploymentNo);
        languages.forEach(languageChooser::addItem);
        if (languages.size() == 1) {
            languageChooser.setSelectedIndex(1);
        }
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        // Since this is the welcome page, there must be something selected in order to move on.
        context.deploymentNo = getSelectedDeployment();
        context.languagecode = getSelectedLanguage();

        if (progressing) {
            ensurePlaylists(context.deploymentNo, context.languagecode);
        }
    }

    @Override
    protected String getTitle() {
        return "Introduction & Choose Deployment, Language.";
    }

    /**
     * Reads the Program Specification to get information about the project and its Deployments.
     */
    private void getProjectInfo() {
        String project = ACMConfiguration.cannonicalProjectName(ACMConfiguration.getInstance()
            .getCurrentDB()
            .getSharedACMname());
        File programSpecDir = ACMConfiguration.getInstance().getProgramSpecDirFor(project);

        context.programSpec = new ProgramSpec(programSpecDir);
    }

    private int getSelectedDeployment() {
        int deploymentNo = -1;
        Object deploymentStr = deploymentChooser.getSelectedItem();
        if (deploymentStr != null) {
            try {
                deploymentNo = Integer.parseInt(deploymentStr.toString());
            } catch (NumberFormatException ignored) {
                // ignored
            }
        }
        return deploymentNo;
    }

    private String getSelectedLanguage() {
        int langIx = languageChooser.getSelectedIndex();
        if (langIx <= 0) return null;
        return languageChooser.getItemAt(langIx);
    }

    /**
     * Based on the selected Deployment fill the title preview.
     */
    private void fillTitleList() {
        progSpecRootNode.removeAllChildren();

        int deploymentNo = getSelectedDeployment();
        String languagecode = getSelectedLanguage();
        if (deploymentNo >= 0 && languagecode != null) {
            PSContent.fillTreeForDeployment(progSpecRootNode,
                context.programSpec,
                deploymentNo,
                languagecode);
            progSpecTreeModel.reload();
            for (int i = 0; i < progSpecTree.getRowCount(); i++) {
                progSpecTree.expandRow(i);
            }
        }
        
        boolean hasContent = hasContent(progSpecRootNode);
        titlePreviewScroller.setVisible(hasContent);
        titlePreviewBox.setVisible(hasContent);
    }

    private boolean hasContent(DefaultMutableTreeNode node) {
        for (Enumeration e = node.breadthFirstEnumeration(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
            if (current instanceof PSContent.MessageNode)
                return true;
        }
        return false;
    }

    @SuppressWarnings("FieldCanBeLocal")
    private TreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            ImageIcon icon = null;
            if (value instanceof PSContent.MessageNode) {
                String title = ((PSContent.MessageNode)value).getItem().getTitle();
                AudioItem item = findAudioItemForTitle(title, getSelectedLanguage());
                icon = item==null ? noSoundImage : soundImage;
            }
            JLabel comp = (JLabel) super.getTreeCellRendererComponent(tree, value,
                selected, expanded, leaf, row, hasFocus);
            comp.setIcon(icon);
            return comp;
        }
    };
    /**
     * Create the given playlist, if it doesn't already exist. The playlist will be
     * named like "1-Malaria_Prevention-dga", with deployment #, playlist name, and
     * language code. Note that there are no spaces.
     *
     * @param deploymentNo The deployment in which the playlist is distributed.
     * @param languagecode The language of content in the specific playlist.
     */
    private void ensurePlaylists(int deploymentNo, String languagecode) {
        boolean anyAdded = false;
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Map<String, Playlist> acmPlaylists = store.getPlaylists()
            .stream()
            .collect(Collectors.toMap(Playlist::getName, pl -> pl));

        List<ContentSpec.PlaylistSpec> contentPlaylistSpecs = context.programSpec.getContentSpec()
                                                                                 .getDeployment(deploymentNo)
                                                                                 .getPlaylistSpecs(languagecode);
        for (ContentSpec.PlaylistSpec contentPlaylistSpec : contentPlaylistSpecs) {
            String plName = decoratedPlaylistName(contentPlaylistSpec.getPlaylistTitle(), deploymentNo, languagecode);
            if (!acmPlaylists.containsKey(plName)) {
                Playlist playlist = store.newPlaylist(plName);
                try {
                    context.createdPlaylists.add(plName);
                    store.commit(playlist);
                    anyAdded = true;
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Unable to create playlist with name " + plName, e);
                }
            }
        }
        if (anyAdded) {
            Application.getMessageService().pumpMessage(new SidebarView.PlaylistsChanged());
        }
    }


}
