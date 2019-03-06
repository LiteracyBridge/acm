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
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static org.literacybridge.acm.Constants.CATEGORY_GENERAL_OTHER;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class ImportPage extends AssistantPage<ContentImportContext> {

    private ContentImportContext context;

    public ImportPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,20,0);
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
            "<html>" + "<span style='font-size:2.5em'>Perform Import!</span>"
                + "<br/>When you are satisfied with these imports, click \"Finish\" to perform the import. "

                + "</html>");
        add(welcome, gbc);

        // Absorb any vertical space.
        gbc.weighty = 1.0;
        add(new JLabel(), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        if (progressing)
            performUpdate();
    }

    @Override
    protected String getTitle() {
        return "Import Files";
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
            .getCategory(CATEGORY_GENERAL_OTHER);

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
