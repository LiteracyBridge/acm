package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class SummaryPage extends AssistantPage<ContentImportContext> {

    private ContentImportContext context;

    public SummaryPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Finished Content Import!</span>"
                + "<br/><br/><p>Contratulations, you have imported N messages for language ABC.</p>"
                + "<br/><br/><p>M new playlists were created.</p>"
                + "<br/>Click \"Finish\" to close. "

                + "</html>");
        add(welcome);
    }

    @Override
    protected void onPageEntered(boolean progressing) {

        Timer t = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setComplete();
            }
        });
        t.setRepeats(false);
        t.start();

        performUpdate();
    }

    @Override
    protected String getTitle() {
        return "Summary";
    }

    private void performUpdate() {
        int deploymentNo = context.deploymentNo;
        String languagecode = context.languagecode;
        ProgramSpec programSpec = context.programSpec;
        Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher = context.matcher;

        // General Other category.
        Category category = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore()
            .getTaxonomy()
            .getCategory("0-0");

        AudioImporter importer = AudioImporter.getInstance();

        // Look at all of the matches.
        for (MatchableImportableAudio matchableItem : matcher.matchableItems) {
            if (matchableItem.getMatch().isMatch()) {
                ImportableAudioItem importableAudio = matchableItem.getLeft();
                // If not already imported, do so, and add to the playlist.
                if (!importableAudio.hasAudioItem()) {
                    try {
                        System.out.println(String.format("Import: %s from %s",
                            importableAudio,
                            matchableItem.getRight().getFile().getCanonicalPath()));
                        AudioItem audioItem = importer.importFile(matchableItem.getRight().getFile(),
                            (i) -> {
                                // There really should be an item, but don't NPE if not.
                                if (i != null) {
                                    // If there is no category, add to "General Other"
                                    if (i.getCategoryList().size() == 0)
                                        i.addCategory(category);
                                    // If the item didn't know what language it was, add to the selected language.
                                    // TODO: Warn user if unexpected language.
                                    if (!i.getMetadata().containsField(MetadataSpecification.DC_LANGUAGE)) {
                                        i.getMetadata()
                                            .put(MetadataSpecification.DC_LANGUAGE, languagecode);
                                    }
                                    // Force the title.
                                    i.getMetadata().put(MetadataSpecification.DC_TITLE, importableAudio.getTitle());
                                }
                            });
                        // If the item isn't already in the playlist, add it.
                        // TODO: that would be an error.
                        Playlist playlist = importableAudio.getPlaylist();
                        if (!audioItem.hasPlaylist(playlist)) {
                            try {
                                audioItem.addPlaylist(playlist);
                                playlist.addAudioItem(audioItem);
                                ACMConfiguration.getInstance()
                                    .getCurrentDB()
                                    .getMetadataStore()
                                    .commit(audioItem, playlist);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Application.getFilterState().updateResult(true);

    }
}
