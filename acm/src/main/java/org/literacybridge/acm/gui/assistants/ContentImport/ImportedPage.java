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

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import static org.literacybridge.acm.Constants.CATEGORY_GENERAL_OTHER;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class ImportedPage extends AssistantPage<ContentImportContext> {

    private final JLabel importedMessagesLabel;
    private final JLabel currentMessage;
    private final JTextPane importedMessagesLog;
    private final JLabel updatedMessagesLabel;
    private ContentImportContext context;
    private int importCount;
    private int updateCount;

    Category generalOtherCategory = ACMConfiguration
        .getInstance()
        .getCurrentDB()
        .getMetadataStore()
        .getTaxonomy()
        .getCategory(CATEGORY_GENERAL_OTHER);

    public ImportedPage(PageHelper listener) {
        super(listener);
        context = getContext();
        context = getContext();
        setLayout(new GridBagLayout());

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
            "<html>" + "<span style='font-size:2.5em'>Finished Content Import</span>" + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Import message content for deployment "));
        hbox.add(parameterText(Integer.toString(context.deploymentNo)));
        hbox.add(new JLabel(" in language "));
        hbox.add(parameterText(context.languagecode));
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox, gbc);

        hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Imported "));
        importedMessagesLabel = parameterText("no");
        hbox.add(importedMessagesLabel);
        hbox.add(new JLabel(" new and updated "));
        updatedMessagesLabel = parameterText("no");
        hbox.add(new JLabel("messages. Created "));
        hbox.add(parameterText(Integer.toString(context.createdPlaylists.size())));
        hbox.add(new JLabel(" new playlists."));
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox, gbc);

        currentMessage = new JLabel("...");
        add(currentMessage, gbc);

        // Absorb any vertical space.
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        importedMessagesLog = new JTextPane();
        importedMessagesLog.setText("");
        importedMessagesLog.setEditable(false);
        JScrollPane logScroller = new JScrollPane(importedMessagesLog);
        add(logScroller, gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                performImports();
                return 0;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
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

    private void performImports() {
        Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher = context.matcher;

        // Look at all of the matches.
        importCount = 0;
        updateCount = 0;
        for (MatchableImportableAudio matchableItem : matcher.matchableItems) {
            if (matchableItem.getMatch().isMatch()) {
                // If not already in the ACM DB, or the update switch is on, do the import.
                boolean okToImport =
                    !matchableItem.getLeft().hasAudioItem() || matchableItem.getDoUpdate();
                if (okToImport) {
                    importOneItem(matchableItem);
                }
            }
        }
        Application.getFilterState().updateResult(true);
        UIUtils.setLabelText(currentMessage,"Click \"Close\" to return to the ACM.");
    }

    private void importOneItem(MatchableImportableAudio matchableItem)
    {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        AudioImporter importer = AudioImporter.getInstance();
        ImportableAudioItem importableAudio = matchableItem.getLeft();

        try {
            String msg = String.format("%s: '%s' from '%s'",
                matchableItem.getOperation(),
                importableAudio,
                matchableItem.getRight().getFile().getCanonicalPath());
            UIUtils.setLabelText(currentMessage, msg);

            ImportHandler handler = new ImportHandler(generalOtherCategory, matchableItem);
            File importableFile = matchableItem.getRight().getFile();

            // Add or update the audio file to the ACM database.
            AudioItem audioItem = importer.importFile(importableFile, handler);
            UIUtils.appendLabelText(importedMessagesLog, msg, false);

            // If the item isn't already in the playlist, add it.
            // TODO: that would be an error.
            Playlist playlist = importableAudio.getPlaylist();
            if (!audioItem.hasPlaylist(playlist)) {
                try {
                    audioItem.addPlaylist(playlist);
                    playlist.addAudioItem(audioItem);
                    store.commit(audioItem, playlist);
                    UIUtils.appendLabelText(importedMessagesLog,
                        String.format("    and add to playlist '%s'.",
                            audioItem.getTitle(),
                            playlist.getName()),
                        false);
                } catch (Exception e) {
                    UIUtils.appendLabelText(importedMessagesLog,
                        String.format("  Exception adding '%s'\n    to playlist '%s'\n  %s",
                            importableAudio,
                            playlist.getName(),
                            e.getMessage()),
                        false);
                    e.printStackTrace();
                }
            }
            if (matchableItem.isUpdate()) {
                updateCount++;
                UIUtils.setLabelText(updatedMessagesLabel, Integer.toString(importCount));
            } else {
                importCount++;
                UIUtils.setLabelText(importedMessagesLabel, Integer.toString(importCount));
            }
        } catch (IOException e) {
            UIUtils.appendLabelText(importedMessagesLog,
                String.format("Exception importing '%s'\n  %s", importableAudio, e.getMessage()),
                false);
            e.printStackTrace();
        }
    }

    /**
     * ImportHandler for file imports. Sets category, language, and/or title in
     * the callback handler.
     */
    private class ImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final MatchableImportableAudio matchableItem;

        ImportHandler(Category category, MatchableImportableAudio matchableItem) {
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
                    item
                        .getMetadata()
                        .put(MetadataSpecification.DC_TITLE, importableAudio.getTitle());
                }

            }
        }
    }

}
