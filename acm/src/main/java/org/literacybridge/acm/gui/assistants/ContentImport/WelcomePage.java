package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.MainWindow.SidebarView;
import org.literacybridge.acm.gui.assistants.util.PSContent;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class WelcomePage extends AssistantPage<ContentImportContext> {
    private static final Logger LOG = Logger.getLogger(WelcomePage.class.getName());

    private final JComboBox<Object> deploymentChooser;
    private final JComboBox<String> languageChooser;
    private final JLabel titlePreviewLabel;
    private final JScrollPane titlePreviewScroller;

    private ContentImportContext context;
    private final DefaultMutableTreeNode progSpecRootNode;
    private final DefaultTreeModel progSpecTreeModel;
    private final JTree progSpecTree;

    WelcomePage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,15,0);
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

        JLabel welcome = new JLabel("<html>"
            + "<span style='font-size:2.5em'>Welcome to the Content Import Assistant.</span>"
            + "<br/><br/><p>This assistant will guide you through importing audio content into your project. Steps to import audio:</p>"
            + "<ol>"
            + "<li> You choose the Deployment #, and the Language of the content you want to import.</li>"
            + "<li> You choose the files and folders containing the content.</li>"
            + "<li> The assistant will automatically match as many imported files as it can.</li>"
            + "<li> You manually match any remaining files to the titles in the Content Calendar.</li>"
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
        languageChooser = new JComboBox<>();
        languageChooser.addActionListener(this::onSelection);
        setComboWidth(languageChooser, "Detect from file path.");
        languageChooser.setMaximumSize(languageChooser.getPreferredSize());
        hbox.add(languageChooser);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        add(new JLabel("Click 'Next' when you are ready to continue."), gbc);

        // Title preview.
        titlePreviewLabel = new JLabel("Playlists and Message Titles in the Deployment:");
        insets = new Insets(0,0,00,0);
        gbc.insets = insets;
        add(titlePreviewLabel, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));

        progSpecRootNode = new DefaultMutableTreeNode();
        progSpecTree = new JTree(progSpecRootNode);
        progSpecTreeModel = (DefaultTreeModel) progSpecTree.getModel();
        progSpecTree.setRootVisible(false);

        titlePreviewScroller = new JScrollPane(progSpecTree);
        panel.add(titlePreviewScroller, BorderLayout.CENTER);
        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        insets = new Insets(0,10,0,30);
        gbc.insets = insets;
        add(panel, gbc);

        titlePreviewLabel.setVisible(false);
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
        int deplIx = deploymentChooser.getSelectedIndex();
        deploymentChooser.setBorder(deplIx == 0 ? redBorder : blankBorder);
        int langIx = languageChooser.getSelectedIndex();
        languageChooser.setBorder(langIx == 0 ? redBorder : blankBorder);

        fillTitleList();

        setComplete(deplIx >= 1 && langIx >= 1);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        // Fill deployments
        deploymentChooser.removeAllItems();
        deploymentChooser.insertItemAt("Choose...", 0);
        // We aspire to be able to import for multiple deployments, but it gets messy with
        // messages repeated across playlists, etc. Initially, let the user specify the
        // exact deployment.
//        deploymentChooser.addItem("All");
        context.programSpec.getDeployments()
            .stream()
            .map(d -> Integer.toString(d.deploymentnumber))
            .forEach(deploymentChooser::addItem);

        // Fill context.programLanguagecodes
        languageChooser.removeAllItems();
        languageChooser.insertItemAt("Choose...", 0);
        // We aspire to be able to make an intelligent guess of the language. Initially, we ask
        // the user to tell us in advance.
//        languageChooser.addItem("Detect from file path.");
        context.programSpec.getRecipients()
            .stream()
            .map(r -> r.language)
            .collect(Collectors.toSet())
            .forEach(languageChooser::addItem);

        // If previously selected, re-select.
        if (context.deploymentNo >= 0) {
            deploymentChooser.setSelectedItem(Integer.toString(context.deploymentNo));
        }
        if (context.languagecode != null) {
            languageChooser.setSelectedItem(context.languagecode);
        }
        onSelection(null);
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        // Since this is the welcome page, there must be something selected in order to move on.
        context.deploymentNo = Integer.parseInt(deploymentChooser.getSelectedItem().toString());
        context.languagecode = languageChooser.getSelectedItem().toString();

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

        context.programLanguagecodes = context.programSpec.getRecipients()
            .stream()
            .map(r -> r.language)
            .collect(Collectors.toSet());
    }

    /**
     * Based on the selected Deployment fill the title preview.
     */
    private void fillTitleList() {
        progSpecRootNode.removeAllChildren();

        Object deploymentStr = deploymentChooser.getSelectedItem();
        Object languageStr = languageChooser.getSelectedItem();
        if (deploymentStr != null && languageStr != null) {
            int deploymentNo = Integer.parseInt(deploymentStr.toString());
            String languagecode = languageStr.toString();

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
        titlePreviewLabel.setVisible(hasContent);
    }

    private boolean hasContent(DefaultMutableTreeNode node) {
        for (Enumeration e = node.breadthFirstEnumeration(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
            if (current instanceof PSContent.MessageNode)
                return true;
        }
        return false;
    }

    /**
     * Determines what Deployment(s), if any, the user has selected. Not that presently this
     * is limited to a single Deployment.
     * @return a possibly empty list of selected Deployments.
     */
    private List<Integer> getSelectedDeployments() {
        List<Integer> result = new ArrayList<>();
        Object selectedObject = deploymentChooser.getSelectedItem();
        String selectedItem = selectedObject != null ? selectedObject.toString() : "";
        if (selectedItem.matches("^\\d+$")) {
            result.add(new Integer(selectedItem));
        } else if (selectedItem.equalsIgnoreCase("all")) {
            context.programSpec.getDeployments()
                .stream()
                .map(d -> Integer.toString(d.deploymentnumber))
                .forEach(s -> result.add(new Integer(s)));
        }
        return result;
    }

    /**
     * Determines what Language(s), if any, the user has selected. Not that presently this
     * is limited to a single Language.
     * @return a possibly empty list of selected context.programLanguagecodes.
     */
    private List<String> getSelectedLanguages() {
        List<String> result = new ArrayList<>();
        Object selectedObject = languageChooser.getSelectedItem();
        String selectedItem = selectedObject != null ? selectedObject.toString() : "";
        if (context.programLanguagecodes.contains(selectedItem)) {
            result.add(selectedItem);
        } else if (selectedItem.equalsIgnoreCase("Detect from file path.")) {
            result.addAll(context.programLanguagecodes);
        }
        return result;
    }

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

        List<Content.Playlist> contentPlaylists = context.programSpec.getContent()
            .getDeployment(deploymentNo)
            .getPlaylists();
        for (Content.Playlist contentPlaylist : contentPlaylists) {
            String plName = qualifiedPlaylistName(contentPlaylist.getPlaylistTitle(), deploymentNo, languagecode);
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

    public static String qualifiedPlaylistName(String title, int deploymentNo, String languagecode) {
        title = normalizePlaylistTitle(title);
        return String.format("%d-%s-%s", deploymentNo, title, languagecode);
    }
    private static Pattern playlistPattern = Pattern.compile("\\d+-(.*)-\\w+");
    public static String basePlaylistName(String qualifiedName) {
        Matcher matcher = playlistPattern.matcher(qualifiedName);
        if (matcher.matches() && matcher.groupCount()==1) {
            return matcher.group(1);
        }
        return null;
    }
    private static String normalizePlaylistTitle(String title) {
        title = title.trim().replaceAll(" ", "_");
        return title;
    }

}
