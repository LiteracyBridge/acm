package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.core.spec.Content;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class MatchPage extends AssistantPage<ContentImportContext> {

    private final JLabel deployment;
    private final JLabel language;
    private final MatcherPanel matcherPanel;

    private ContentImportContext context;
    private MetadataStore store = ACMConfiguration.getInstance()
        .getCurrentDB()
        .getMetadataStore();

    public MatchPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Match Files with Content.</span>"
                + "<br/><br/><p>The Assistant has automatically matched files as possible with content. "
                + "Only high-confidence matches are performed, so manual matching may be required. "
                + "Perform any additional matching (or un-matching) as required, then click \"Next\" to continue.</p>"
                + "</html>");
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(welcome);

        add(Box.createVerticalStrut(20));

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing message content for deployment "));
        deployment = parameterText();
        hbox.add(deployment);
        hbox.add(new JLabel(" and language "));
        language = parameterText();
        hbox.add(language);
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox);

        add(Box.createVerticalStrut(20));

        hbox = Box.createHorizontalBox();
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        matcherPanel = new MatcherPanel(context);
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

        // List of titles (left side list)
        List<ImportableAudioItem> titles = new ArrayList<>();
        List<Content.Playlist> contentPlaylists = context.programSpec.getContent()
            .getDeployment(deploymentNo)
            .getPlaylists();
        for (Content.Playlist contentPlaylist : contentPlaylists) {
            for (Content.Message message : contentPlaylist.getMessages()) {
                String playlistName = WelcomePage.qualifiedPlaylistName(message.playlistTitle, deploymentNo, languagecode);
                Playlist playlist = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().findPlaylistByName(playlistName);
                ImportableAudioItem importableAudio = new ImportableAudioItem(message.title, playlist);
                // See if we already have this title.
                AudioItem audioItem = findAudioItemForTitle(message.title, languagecode);
                importableAudio.setItem(audioItem);
                titles.add(importableAudio);
            }
        }
        // List of files (right side list)
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

    /**
     * Given a message title (ie, from the Program Spec), see if we already have such an
     * audio item in the desired language.
     * @param title The title to search for.
     * @param languagecode The language in which we want the audio item.
     * @return the AudioItem if it exists, otherwise null.
     */
    private AudioItem findAudioItemForTitle(String title, String languagecode) {
        List<Category> categoryList = new ArrayList<>();
        List<Locale> localeList = Arrays.asList(new RFC3066LanguageCode(languagecode).getLocale());

        SearchResult searchResult = store.search(title, categoryList, localeList);
        // Filter because search will return near matches.
        AudioItem item = searchResult.getAudioItems()
            .stream()
            .map(store::getAudioItem)
            .filter(i->i.getTitle().equals(title))
            .findAny()
            .orElse(null);
        return item;
    }

    @Override
    protected String getTitle() {
        return "Auto-match Files with Content";
    }
}
