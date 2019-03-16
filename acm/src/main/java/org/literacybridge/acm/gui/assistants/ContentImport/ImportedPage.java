package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import static org.literacybridge.acm.Constants.CATEGORY_GENERAL_OTHER;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class ImportedPage extends AssistantPage<ContentImportContext> {

    private final JLabel importedMessages;
    private final JLabel currentMessage;
    private ContentImportContext context;
    private int importCount;

    public ImportedPage(PageHelper listener) {
        super(listener);
        context = getContext();
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0, 0, 20, 0);
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
            "<html>" + "<span style='font-size:2.5em'>Finished Content Import</span>" + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Imported message content for deployment "));
        hbox.add(parameterText(Integer.toString(context.deploymentNo)));
        hbox.add(new JLabel(" in language "));
        hbox.add(parameterText(context.languagecode));
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox, gbc);

        hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Imported "));
        importedMessages = parameterText("no");
        hbox.add(importedMessages);
        hbox.add(new JLabel(" new messages. Created "));
        hbox.add(parameterText(Integer.toString(context.createdPlaylists.size())));
        hbox.add(new JLabel(" new playlists."));
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox, gbc);

        currentMessage = parameterText("...");
        add(currentMessage, gbc);

        add(new JLabel("Click \"Close\" to return to the ACM."), gbc);

        // Absorb any vertical space.
        gbc.weighty = 1.0;
        add(new JLabel(), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
        Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        setCursor(waitCursor);
        SwingWorker worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                performUpdate();
                return 0;
            }

            @Override
            protected void done() {
                setCursor(defaultCursor);
                setComplete();
            }
        };
        worker.execute();
        setComplete(false);
    }

    @Override
    protected String getTitle() {
        return "Import Results";
    }

    @Override
    protected boolean isSummaryPage() { return true; }

    private void performUpdate() {
        int deploymentNo = context.deploymentNo;
        String languagecode = context.languagecode;
        ProgramSpec programSpec = context.programSpec;
        Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher = context.matcher;
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

        // General Other category.
        Category category = ACMConfiguration
            .getInstance()
            .getCurrentDB()
            .getMetadataStore()
            .getTaxonomy()
            .getCategory(CATEGORY_GENERAL_OTHER);

        AudioImporter importer = AudioImporter.getInstance();

        // Look at all of the matches.
        importCount = 0;
        for (MatchableImportableAudio matchableItem : matcher.matchableItems) {
            if (matchableItem.getMatch().isMatch()) {
                ImportableAudioItem importableAudio = matchableItem.getLeft();
                boolean okToImport = !importableAudio.hasAudioItem() || matchableItem.getDoUpdate();
                if (okToImport) {
                    try {
                        String msg = String.format("%s: %s from %s",
                            matchableItem.getOperation(),
                            importableAudio,
                            matchableItem.getRight().getFile().getCanonicalPath());
                        UIUtils.setLabelText(currentMessage, msg);

                        ImportHandler handler = new ImportHandler(category, matchableItem);
                        File importableFile = matchableItem.getRight().getFile();

                        // Add or update the audio file to the ACM database.
                        AudioItem audioItem = importer.importFile(importableFile, handler);

                        // If the item isn't already in the playlist, add it.
                        // TODO: that would be an error.
                        Playlist playlist = importableAudio.getPlaylist();
                        if (!audioItem.hasPlaylist(playlist)) {
                            try {
                                audioItem.addPlaylist(playlist);
                                playlist.addAudioItem(audioItem);
                                store.commit(audioItem, playlist);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        importCount++;
                        UIUtils.setLabelText(importedMessages, Integer.toString(importCount));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Application.getFilterState().updateResult(true);
    }

    private class ImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final MatchableImportableAudio matchableItem;

        public ImportHandler(Category category, MatchableImportableAudio matchableItem) {
            this.category = category;
            this.matchableItem = matchableItem;
        }

        @Override
        public void process(AudioItem item) {
            ImportableAudioItem importableAudio = matchableItem.getLeft();

            // There really should be an item, but don't NPE if not.
            if (item != null) {
                // If there is no category, add to "General Other"
                if (item.getCategoryList().size() == 0) item.addCategory(category);
                // If the item didn't know what language it was, add to the selected language.
                // TODO: Warn user if unexpected language.
                String existingLanguage = item.getLanguageCode();
                if (!context.languagecode.equals(existingLanguage)) {
                    System.out.println(String.format("Forcing language to '%s' was '%s'.",
                        context.languagecode,
                        existingLanguage));
                    item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, context.languagecode);
                }
                // Force the title.
                String existingTitle = item.getTitle();
                if (!importableAudio.getTitle().equals(existingTitle)) {
                    System.out.println(String.format("Renaming '%s' to '%s'.",
                        existingTitle,
                        importableAudio.getTitle()));
                    item.getMetadata().put(MetadataSpecification.DC_TITLE, importableAudio.getTitle());
                }

            }
        }

    }
}
