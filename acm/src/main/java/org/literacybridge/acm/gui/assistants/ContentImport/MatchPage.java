package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherPanel;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.Content;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class MatchPage extends AssistantPage<ContentImportContext> {

    private final JLabel deployment;
    private final JLabel language;
    private final MatcherPanel matcherPanel;

    private ContentImportContext context;

    public MatchPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Match Files with Content, part 1.</span>"
                + "<br/><br/><p>This shows the result of automatically matching files with content.</p>"

                + "<br/><p>Click \"Next\" to continue.</p>"

                + "</html>");
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(welcome);

        add(Box.createVerticalStrut(20));

        Border greyBorder = new LineBorder(Color.green); //new LineBorder(new Color(0xf0f0f0));
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing message content for deployment "));
        deployment = new JLabel();
        deployment.setOpaque(true);
        deployment.setBackground(Color.white);
        deployment.setBorder(greyBorder);
        hbox.add(deployment);
        hbox.add(new JLabel(" and language "));
        language = new JLabel();
        language.setOpaque(true);
        language.setBackground(Color.white);
        language.setBorder(greyBorder);
        hbox.add(language);
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox);

        add(Box.createVerticalStrut(20));

        hbox = Box.createHorizontalBox();
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        matcherPanel = new MatcherPanel();
        matcherPanel.setBorder(new LineBorder(Color.lightGray, 1));
        hbox.add(matcherPanel);
        add(hbox);


    }

    @Override
    protected void onPageEntered(boolean progressing) {
        String languagecode = context.languagecode;

        // Fill deployment and language
        deployment.setText(Integer.toString(context.deploymentNo));
        language.setText(languagecode);

        int deploymentNo = Integer.parseInt(deployment.getText());

        List<ImportableAudioItem> titles = new ArrayList<>();
        List<Content.Playlist> contentPlaylists = context.programSpec.getContent()
            .getDeployment(deploymentNo)
            .getPlaylists();
        for (Content.Playlist contentPlaylist : contentPlaylists) {
            for (Content.Message message : contentPlaylist.getMessages()) {
                String playlistName = WelcomePage.qualifiedPlaylistName(message.playlistTitle, deploymentNo, languagecode);
                Playlist playlist = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().findPlaylistByName(playlistName);
                titles.add(new ImportableAudioItem(message.title, playlist));
            }
        }
        List<ImportableFile> files = context.importableFiles.stream()
            .map(ImportableFile::new)
            .collect(Collectors.toList());

        if (progressing) {
            context.matcher.setData(titles, files, MatchableImportableAudio::new);
        }
        matcherPanel.setData(context.matcher, titles, files);
        if (progressing) {
            matcherPanel.reset();
            matcherPanel.autoMatch();
        }

        setComplete();
    }

    @Override
    protected String getTitle() {
        return "Auto-match Files with Content";
    }
}
