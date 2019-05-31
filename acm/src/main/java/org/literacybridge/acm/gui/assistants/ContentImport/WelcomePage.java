package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.MainWindow.SidebarView;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
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
import java.awt.Font;
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

    private final JComboBox<String> deploymentChooser;
    private final JComboBox<String> languageChooser;
    private final Box titlePreviewBox;
    private final JScrollPane titlePreviewScroller;

    private final DefaultMutableTreeNode progSpecRootNode;
    private final DefaultTreeModel progSpecTreeModel;
    private final JTree progSpecTree;

    WelcomePage(PageHelper<ContentImportContext> listener) {
        super(listener);
        getProjectInfo();

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(getWelcomeIntro());
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
        Set<String> languageStrings = context.programSpec.getLanguages()
            .stream()
            .map(AcmAssistantPage::getLanguageAndName)
            .collect(Collectors.toSet());
        setComboWidth(languageChooser, languageStrings, "Choose...");
        languageChooser.setMaximumSize(languageChooser.getPreferredSize());
        hbox.add(languageChooser);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        // Title preview.
        titlePreviewBox = Box.createHorizontalBox();
        titlePreviewBox.add(new JLabel(getTitlePreviewProlog()));
        titlePreviewBox.add(new JLabel(soundImage));
        titlePreviewBox.add(new JLabel(" already have content in the ACM, with "));
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
        javax.swing.ToolTipManager.sharedInstance().registerComponent(progSpecTree);

        titlePreviewScroller = new JScrollPane(progSpecTree);
        panel.add(titlePreviewScroller, BorderLayout.CENTER);
        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.bottom = 0;
        add(panel, gbc);

        titlePreviewBox.setVisible(false);
        titlePreviewScroller.setVisible(false);
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
                fillLanguageChooser(languageChooser, deploymentNo, context.programSpec, context.languagecode);
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
        fillDeploymentChooser(deploymentChooser, context.programSpec, context.deploymentNo);

        // Empty the list until we have a deployment.
        languageChooser.removeAllItems();
        if (getSelectedDeployment() > 0) {
            fillLanguageChooser(languageChooser, getSelectedDeployment(), context.programSpec, context.languagecode);
        }
        onSelection(null);
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

    private String getWelcomeIntro() {
        String content = "<html>"
            + "<span style='font-size:2.5em'>Welcome to the Content Import Assistant.</span>"
            + "<br/><br/><p>This assistant will guide you through importing audio content "
            + "into your project. Steps to import:</p>"
            + "<ol>"
            + "<li> Choose the Deployment # and the Language of the content you wish to import.</li>"
            + "<li> Choose the files and folders containing the audio.</li>"
            + "<li> The assistant will automatically match as many imported files as it can. You will "
            + "have an opportunity to match remaining files, or to \"unmatch\" files as needed.</li>"
            + "<li> Review and approve the final message-to-file matches.</li>"
            + "<li> The audio files are imported into the ACM, and placed in appropriate playlists.</li>"
            + "</ol>"
            + "</html>";

        String playlists = "<html>"
            + "<span style='font-size:2.5em'>Welcome to the Playlist Prompt Assistant.</span>"
            + "<br/><br/><p>This assistant will guide you through importing playlist prompts "
            + "into your project. Steps to import:</p>"
            + "<ol>"
            + "<li> Choose the Deployment # and the Language of the prompts you wish to import.</li>"
            + "<li> Choose the files and folders containing the prompts.</li>"
            + "<li> The assistant will automatically match as many imported files as it can. You will "
            + "have an opportunity to match remaining files, or to \"unmatch\" files as needed.</li>"
            + "<li> Review and approve the final prompt-to-file matches.</li>"
            + "<li> The audio files are imported into the ACM.</li>"
            + "</ol>"
            + "</html>";

        return context.promptsOnly ? playlists : content;
    }

    private String getTitlePreviewProlog() {
        if (context.promptsOnly) {
            return "Playlist prompts in the Deployment. (Prompts with ";
        } else {
            return "Prompts and message titles in the Deployment. (Titles with ";
        }
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
            // Find the playlist recordings so that we can paint the speaker to indicate existing prompts.
            findPlaylistRecordings(deploymentNo);
            if (context.promptsOnly) {
                PSContent.fillTreeWithPlaylistPromptsForDeployment(progSpecRootNode,
                    context.programSpec,
                    deploymentNo,
                    languagecode);
            } else {
                PSContent.fillTreeForDeployment(progSpecRootNode,
                    context.programSpec,
                    deploymentNo,
                    languagecode);
            }
            progSpecTreeModel.reload();
            for (int i = 0; i < progSpecTree.getRowCount(); i++) {
                progSpecTree.expandRow(i);
            }
        }
        
        boolean hasContent = hasContent(progSpecRootNode);
        titlePreviewScroller.setVisible(hasContent);
        titlePreviewBox.setVisible(hasContent);
    }

    private void findPlaylistRecordings(int deploymentNo) {
        String languagecode = getSelectedLanguage();
        context.playlistPromptsMap.clear();

        ContentSpec contentSpec = context.programSpec.getContentSpec();
        ContentSpec.DeploymentSpec deploymentSpec = contentSpec.getDeployment(deploymentNo);
        if (deploymentSpec == null) return;
        List<ContentSpec.PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecs(getSelectedLanguage());

        for (ContentSpec.PlaylistSpec contentPlaylistSpec : playlistSpecs) {
            PlaylistPrompts prompts = new PlaylistPrompts(contentPlaylistSpec.getPlaylistTitle(), languagecode);
            prompts.findPrompts();
            context.playlistPromptsMap.put(contentPlaylistSpec.getPlaylistTitle(), prompts);
        }
    }

    private boolean hasContent(DefaultMutableTreeNode node) {
        for (Enumeration e = node.breadthFirstEnumeration(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
            if (current instanceof PSContent.MessageNode || current instanceof PSContent.PromptNode)
                return true;
        }
        return false;
    }

    @SuppressWarnings("FieldCanBeLocal")
    private TreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
        Font normalFont = getFont();
//        Font italicFont = new Font(normalFont.getName(),
//                normalFont.getStyle()|Font.ITALIC,
//            normalFont.getSize());
        Font italicFont = LabelButton.fontResource(LabelButton.AVENIR).deriveFont((float)normalFont.getSize()).deriveFont(Font.ITALIC);


        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            Font font = normalFont;
            ImageIcon icon = null;
            String tooltip = null;
            if (value instanceof PSContent.MessageNode) {
                String title = ((PSContent.MessageNode)value).getItem().getTitle();
                AudioItem item = findAudioItemForTitle(title, getSelectedLanguage());
                icon = item!=null ? soundImage : noSoundImage;
            } else if (value instanceof PSContent.PromptNode) {
                PSContent.PromptNode promptNode = (PSContent.PromptNode)value;
                String title = promptNode.getPlaylist().getPlaylistTitle();
                PlaylistPrompts prompts = context.playlistPromptsMap.get(title);
                boolean hasPrompt = promptNode.isLongPrompt() ? prompts.hasLongPrompt() : prompts.hasShortPrompt();
                icon = hasPrompt ? soundImage : noSoundImage;
                tooltip = String.format("%s playlist prompt for %s", promptNode.isLongPrompt() ? "Long":"Short", title);
                font = italicFont;
            }
            JLabel comp = (JLabel) super.getTreeCellRendererComponent(tree, value,
                selected, expanded, leaf, row, hasFocus);
            comp.setIcon(icon);
            comp.setToolTipText(tooltip);
            comp.setFont(font);
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
