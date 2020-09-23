package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.MainWindow.SidebarView;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
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
    private final JComponent noContentWarningBox;
    private ContentImportBase.ImportReminderLine noContentWarningLine;

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
        hbox.add(new JLabel("Choose the deployment: "));
        deploymentChooser = new JComboBox<>();
        deploymentChooser.addActionListener(this::onSelection);
        setComboWidth(deploymentChooser, "Choose...");
        deploymentChooser.setMaximumSize(deploymentChooser.getPreferredSize());
        hbox.add(deploymentChooser);
        hbox.add(Box.createHorizontalStrut(10));

        hbox.add(new JLabel("and the language: "));
        languageChooser = new LanguageChooser();
        languageChooser.addActionListener(this::onSelection);
        Set<String> languageStrings = context.programSpec.getLanguageCodes()
            .stream()
            .map(AcmAssistantPage::getLanguageAndName)
            .collect(Collectors.toSet());
        setComboWidth(languageChooser, languageStrings, "Choose...");
        languageChooser.setMaximumSize(languageChooser.getPreferredSize());
        hbox.add(languageChooser);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        // "No content" warning, when a deployment and language are selected that have no content defined in the program spec.
        Font defaultFont = new JLabel().getFont();
        Font boldFont = new Font(defaultFont.getName(), defaultFont.getStyle()|Font.BOLD, defaultFont.getSize());
        noContentWarningLine = new ContentImportBase.ImportReminderLine("There are no messages defined for deployment ");
        noContentWarningLine.getPrefix().setFont(boldFont);
        noContentWarningLine.getInfix().setFont(boldFont);
        noContentWarningBox = noContentWarningLine.getLine();
        add(noContentWarningBox, gbc);
        noContentWarningBox.setVisible(false);

        // Title preview. Shown and hidden based on selections.
        titlePreviewBox = getTitlePreviewBox();
        gbc.insets = new Insets(0,0,0,0);
        add(titlePreviewBox, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));
        panel.setBackground(getBackground());

        // Preview of the titles will be built here, after user chooses Depl# & language.
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

        setComplete(deploymentNo >= 0 && languagecode != null  && hasContent(progSpecRootNode));
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
            + "<br/><br/><p>This Assistant will guide you through importing audio content "
            + "for your program. Here are the steps:</p>"
            + "<ol>"
            + "<li> Choose the deployment number and the language of the audio content you wish to import.</li>"
            + "<li> Choose the files and folders containing the audio content.</li>"
            + "<li> The Assistant will automatically match as many imported files as it can from the "
            + "information you provided in the Program Specification. You will "
            + "have an opportunity to match remaining files, or \"unmatch\" files as needed.</li>"
            + "<li> Review and approve the final file matches.</li>"
            + "<li> The audio files are imported into the ACM, and placed into their assigned playlists.</li>"
            + "</ol>"
            + "</html>";

        String playlists = "<html>"
            + "<span style='font-size:2.5em'>Welcome to the Playlist Prompt Assistant.</span>"
            + "<br/><br/><p>This Assistant will guide you through importing playlist prompts "
            + "for your project. Here are the:</p>"
            + "<ol>"
            + "<li> Choose the deployment number and the language of the prompts you wish to import.</li>"
            + "<li> Choose the files and folders containing the prompts.</li>"
            + "<li> The Assistant will automatically match as many imported files as it can. You will "
            + "have an opportunity to match remaining files, or to \"unmatch\" files as needed.</li>"
            + "<li> Review and approve the final file matches.</li>"
            + "<li> The audio files are imported into the ACM.</li>"
            + "</ol>"
            + "</html>";

        return context.promptsOnly ? playlists : content;
    }

    private Box getTitlePreviewBox() {
        String[] labels;
        if (context.promptsOnly) {
            labels = new String[]{"Playlist prompts in the deployment: (Note: Prompts with ",
                    " already have content",
                    "in the ACM. Prompts with ",
                    "do not.)"};
        }
        else {
            labels = new String[]{"Prompts and message titles in the deployment: (Note: Titles with ",
                    " already have content",
                    "in the ACM. Titles with ",
                    "<html>do not. Prompts are listed in <i><span style='font-weight:100'>italics</span></i>.)</html>"};
        }
        int ix=0;
        Box vBox = Box.createVerticalBox();
        Box hBox = Box.createHorizontalBox();
        hBox.add(new JLabel(labels[ix++]));
        hBox.add(new JLabel(soundImage));
        hBox.add(new JLabel(labels[ix++]));
        hBox.add(Box.createHorizontalGlue());
        vBox.add(hBox);
        hBox = Box.createHorizontalBox();
        hBox.add(new JLabel(labels[ix++]));
        hBox.add(new JLabel(noSoundImage));
        hBox.add(new JLabel(labels[ix]));
        hBox.add(Box.createHorizontalGlue());
        vBox.add(hBox);
        return vBox;
    }

    /**
     * Reads the Program Specification to get information about the project and its Deployments.
     */
    private void getProjectInfo() {
        File programSpecDir = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getPathProvider()
                .getProgramSpecDir();

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
        /*
         * The playlist filter tells "fillTree...()" functions whether to:
         * - add a playlist, or ignore it (this disposition is not used here)
         * - add a playlist prompts and content to the tree (ADD_WITH_PROMPTS)
         * - add a playlist's content only to the tree (ADD). (This is used for the intro message,
         *   which looks like a playlist in the program spec, but is treated specially in the
         *   deployment; what matters here is that it has no prompts.)
         */
        PSContent.PlaylistFilter playlistFilter = new PSContent.PlaylistFilter() {
            public PSContent.PlDisposition filter(ContentSpec.PlaylistSpec playlistSpec) {
                String title = playlistSpec.getPlaylistTitle();
                if (context.introMessageCategoryName.equalsIgnoreCase(title)) {
                    return PSContent.PlDisposition.MESSAGES_ONLY;
                }
                return context.promptsOnly?PSContent.PlDisposition.PROMPTS_ONLY:PSContent.PlDisposition.BOTH;
            }
        };

        progSpecRootNode.removeAllChildren();

        int deploymentNo = getSelectedDeployment();
        String languagecode = getSelectedLanguage();
        if (deploymentNo >= 0 && languagecode != null) {
            // Find the playlist recordings so that we can paint the speaker to indicate existing prompts.
            findPlaylistRecordings(deploymentNo);
            PSContent.fillTreeForDeployment(progSpecRootNode,
                context.programSpec,
                deploymentNo,
                languagecode,
                playlistFilter);
            progSpecTreeModel.reload();
            for (int i = 0; i < progSpecTree.getRowCount(); i++) {
                progSpecTree.expandRow(i);
            }
        }
        
        boolean hasContent = hasContent(progSpecRootNode);
        titlePreviewScroller.setVisible(hasContent);
        titlePreviewBox.setVisible(hasContent);
        if (!hasContent && deploymentNo >= 0 && languagecode != null) {
            // Fill deployment and language in warning
            noContentWarningLine.getDeployment().setText(Integer.toString(deploymentNo));
            noContentWarningLine.getLanguage().setText(AcmAssistantPage.getLanguageAndName(languagecode));
            noContentWarningBox.setVisible(true);
        } else {
            noContentWarningBox.setVisible(false);
        }
    }

    private void findPlaylistRecordings(int deploymentNo) {
        String languagecode = getSelectedLanguage();
        context.playlistPromptsMap.clear();

        ContentSpec contentSpec = context.programSpec.getContentSpec();
        ContentSpec.DeploymentSpec deploymentSpec = contentSpec.getDeployment(deploymentNo);
        if (deploymentSpec == null) return;
        List<ContentSpec.PlaylistSpec> playlistSpecs = deploymentSpec.getPlaylistSpecsForLanguage(languagecode);

        for (ContentSpec.PlaylistSpec contentPlaylistSpec : playlistSpecs) {
            PlaylistPrompts prompts = new PlaylistPrompts(contentPlaylistSpec.getPlaylistTitle(), languagecode);
            prompts.findPrompts();
            context.playlistPromptsMap.put(contentPlaylistSpec.getPlaylistTitle(), prompts);
        }
    }

    private boolean hasContent(DefaultMutableTreeNode node) {
        for (
            //noinspection rawtypes
                Enumeration e = node.breadthFirstEnumeration(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
            if (current instanceof PSContent.MessageNode || current instanceof PSContent.PromptNode)
                return true;
        }
        return false;
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final TreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
        private Font normalFont;
        private Font italicFont;
        public Font getItalicFont() {
            if (italicFont == null) {
                italicFont = LabelButton.fontResource(LabelButton.AVENIR).deriveFont((float) getNormalFont()
                    .getSize()).deriveFont(Font.ITALIC);
            }
            return italicFont;
        }
        public Font getNormalFont() {
            if (normalFont == null) {
                normalFont = getFont();
            }
            return normalFont;
        }



        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            Font font = getNormalFont();
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
                if (context.introMessageCategoryName.equalsIgnoreCase(title)) {
                    tooltip = "Special 'Playlist' for Intro Message, which has no prompts.";
                    icon = null;
                } else {
                    boolean hasPrompt = promptNode.isLongPrompt() ?
                                        prompts.hasLongPrompt() :
                                        prompts.hasShortPrompt();
                    icon = hasPrompt ? soundImage : noSoundImage;
                    tooltip = String.format("%s playlist prompt for %s",
                        promptNode.isLongPrompt() ? "Long" : "Short",
                        title);
                    font = getItalicFont();
                }
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
                                                                                 .getPlaylistSpecsForLanguage(languagecode);
        for (ContentSpec.PlaylistSpec contentPlaylistSpec : contentPlaylistSpecs) {
            String plName = AudioUtils.decoratedPlaylistName(contentPlaylistSpec.getPlaylistTitle(), deploymentNo, languagecode);
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
