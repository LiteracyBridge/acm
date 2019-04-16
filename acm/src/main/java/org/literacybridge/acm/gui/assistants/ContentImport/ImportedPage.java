package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.ViewExceptionsDialog;
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
import org.literacybridge.acm.utils.Version;

import javax.swing.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.acm.Constants.CATEGORY_GENERAL_OTHER;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import static org.literacybridge.acm.utils.EmailHelper.HtmlTable;
import static org.literacybridge.acm.utils.EmailHelper.TR;
import static org.literacybridge.acm.utils.EmailHelper.pinkZebra;
import static org.literacybridge.acm.utils.EmailHelper.sendEmail;

public class ImportedPage extends ContentImportPage<ContentImportContext> {
    private static final Logger LOG = Logger.getLogger(ImportedPage.class.getName());

    private final JLabel importedMessagesLabel;
    private final JLabel updatedMessagesLabel;
    private final JLabel errorMessagesLabel;
    private final JLabel currentMessage;

    private int importCount;
    private int updateCount;
    private int errorCount;

    private StringBuilder summaryMessage;
    private HtmlTable summaryTable;

    private List<Exception> errors = new ArrayList<>();

    private Category generalOtherCategory = ACMConfiguration.getInstance()
        .getCurrentDB()
        .getMetadataStore()
        .getTaxonomy()
        .getCategory(CATEGORY_GENERAL_OTHER);
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JButton viewErrorsButton;
    private int progressCount;

    ImportedPage(PageHelper<ContentImportContext> listener) {
        super(listener);
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
        add(hbox, gbc);

        // The status line. Updated as items are imported.
        hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Imported "));
        importedMessagesLabel = parameterText("no");
        hbox.add(importedMessagesLabel);
        hbox.add(new JLabel(" new message(s); updated "));
        updatedMessagesLabel = parameterText("no");
        hbox.add(updatedMessagesLabel);
        hbox.add(new JLabel(" message(s). Created "));
        String message = context.createdPlaylists.size() == 0 ?
                         "no" :
                         Integer.toString(context.createdPlaylists.size());
        hbox.add(parameterText(message));
        hbox.add(new JLabel(" new playlist(s). "));
        errorMessagesLabel = parameterText("No");
        hbox.add(errorMessagesLabel);
        hbox.add(new JLabel(" error(s)."));
        hbox.add(Box.createHorizontalStrut(5));
        viewErrorsButton = new JButton("View Errors");
        viewErrorsButton.setVisible(false);
        viewErrorsButton.addActionListener(this::onViewErrors);
        hbox.add(viewErrorsButton);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        // Current item being imported.
        currentMessage = new JLabel("...");
        add(currentMessage, gbc);

        // Working / Finished
        statusLabel = new JLabel("");
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        add(statusLabel, gbc);
        setStatus("Working...");

        // Add one to the progress max, for the "send email" step.
        progressBar = new JProgressBar(0, context.matcher.matchableItems.size()+1);
        progressBar.setValue(0);
        add(progressBar, gbc);

        // Absorb any vertical space.
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JLabel(""), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                summaryMessage = new StringBuilder("<html>");
                summaryTable = new HtmlTable().withStyle("border-collapse:collapse;border:2px lightgray");
                performImports();
                return 0;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                summaryMessage.append(summaryTable.toString());
                summaryMessage.append("</html>");
                sendSummaryReport();
                UIUtils.setProgressBarValue(progressBar, ++progressCount);
                setComplete();
                progressBar.setVisible(false);
                if (errors.size() > 0) {
                    setStatus("Finished, but with errors.");
                    statusLabel.setForeground(Color.red);
                    viewErrorsButton.setVisible(true);
                } else {
                    setStatus("Finished.");
                }
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

    private void setStatus(String status) {
        UIUtils.setLabelText(statusLabel, "<html>" + "<span style='font-size:2.5em'>"+status+"</span>" + "</html>");
    }

    private void onViewErrors(ActionEvent actionEvent) {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        String computerName;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(
            ZoneId.systemDefault());
        String message = "<html>The following error(s) occurred when attempting to import the content. " +
            "Please double check that the content is good audio. If possible, try listening to the " +
            "messages outside of the ACM application. If the problem persists, contact Amplio technical " +
            "support. The button below will send this report to Amplio."+
            "</html>";
        String reportHeading = String.format("Project %s, User %s (%s), Computer %s%nContent Import at %s%n" +
                "Importing content for Deployment %d, in language %s%n"+
                "ACM Version %s, built %s%n",
            dbConfig.getProjectName(),
            ACMConfiguration.getInstance().getUserName(),
            ACMConfiguration.getInstance().getUserContact(),
            computerName,
            formatter.format(LocalDateTime.now()),
            context.deploymentNo,
            dbConfig.getLanguageLabel(new Locale(context.languagecode)),
            Constants.ACM_VERSION, Version.buildTimestamp);

        ViewExceptionsDialog dialog = new ViewExceptionsDialog(Application.getApplication(), "Errors While Importing");
        dialog.showExceptions(message, reportHeading, errors);
    }

    /**
     * Sends a summary email report to "interested parties".
     */
    private void sendSummaryReport() {
        try {
            sendEmail("ictnotifications@amplio.org",
                "bill@amplio.org",
                "Content Imported",
                summaryMessage.toString(),
                true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Actually perform the imports. Called on a background thread to keep the UI responsive.
     */
    private void performImports() {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher = context.matcher;
        summaryMessage.append(String.format("<h2>Project %s</h2>",
            dbConfig.getProjectName()));
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(
            ZoneId.systemDefault());
        summaryMessage.append(String.format("<h3>%s</h3>", formatter.format(LocalDateTime.now())));
        summaryMessage.append(String.format(
            "<p>Importing content for Deployment %d, in language %s</p>",
            context.deploymentNo,
            dbConfig.getLanguageLabel(new Locale(context.languagecode))));

        importCount = 0;
        updateCount = 0;
        errorCount = 0;
        progressCount = 0;
        // Look at all of the matches.
        for (MatchableImportableAudio matchableItem : matcher.matchableItems) {
            if (matchableItem.getMatch().isMatch()) {
                // If not already in the ACM DB, or the "update item" checkbox is on, do the import.
                boolean okToImport =
                    !matchableItem.getLeft().hasAudioItem() || matchableItem.getLeft()
                        .isReplaceOk();
                if (okToImport) {
                    importOneItem(matchableItem);
                } else if (matchableItem.getLeft().hasAudioItem()) {
                    ensureAudioInPlaylist(matchableItem);
                }
            } else if (matchableItem.getLeft() != null && matchableItem.getLeft().hasAudioItem()) {
                ensureAudioInPlaylist(matchableItem);
            }
            UIUtils.setProgressBarValue(progressBar, ++progressCount);
        }

        Application.getFilterState().updateResult(true);
        UIUtils.setLabelText(currentMessage, "Click \"Close\" to return to the ACM.");
    }

    /**
     * Import a single audio file.
     * @param matchableItem to be imported.
     */
    private void importOneItem(MatchableImportableAudio matchableItem)
    {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        AudioImporter importer = AudioImporter.getInstance();
        ImportableAudioItem importableAudio = matchableItem.getLeft();

        try {
            summaryTable.append(new TR(matchableItem.getOperation(),
                importableAudio.toString(),
                matchableItem.getRight().getFile().getCanonicalPath()));
            String msg = String.format("%s: '%s' from '%s'",
                matchableItem.getOperation(),
                importableAudio.toString(),
                matchableItem.getRight().getFile().getCanonicalPath());
            UIUtils.setLabelText(currentMessage, msg);

            ImportHandler handler = new ImportHandler(generalOtherCategory, matchableItem);
            File importableFile = matchableItem.getRight().getFile();

            // Add or update the audio file to the ACM database.
            AudioItem audioItem = importer.importFile(importableFile, handler);

            // If the item isn't already in the playlist, add it.
            Playlist playlist = importableAudio.getPlaylist();
            if (!audioItem.hasPlaylist(playlist)) {
                try {
                    audioItem.addPlaylist(playlist);
                    playlist.addAudioItem(audioItem);
                    store.commit(audioItem, playlist);
                    summaryTable.append(new TR("Add to Playlist", audioItem.getTitle(), playlist.getName()));
                } catch (Exception e) {
                    reportPlaylistException(e, importableAudio, playlist);
                }
            }
            if (matchableItem.getLeft().hasAudioItem()) {
                updateCount++;
                UIUtils.setLabelText(updatedMessagesLabel, Integer.toString(updateCount));
            } else {
                importCount++;
                UIUtils.setLabelText(importedMessagesLabel, Integer.toString(importCount));
            }
        } catch (Exception e) {
            errorCount++;
            UIUtils.setLabelText(errorMessagesLabel, Integer.toString(errorCount));
            summaryTable.append(new TR("Exception importing", importableAudio.getTitle(), e.toString())
                .withStyler(pinkZebra));

            String logMsg = String.format("Exception importing '%s'", importableAudio.toString());
            LOG.log(Level.SEVERE, logMsg, e);
            errors.add(new ImportException(logMsg, e));

        }
    }

    /**
     * If the audio item didn't need to be imported, this can be used to make sure that it is in
     * the appropriate playlist.
     * @param matchableItem to be put into a playlist, if needed.
     */
    private void ensureAudioInPlaylist(MatchableImportableAudio matchableItem) {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        ImportableAudioItem importableAudio = matchableItem.getLeft();
        Playlist playlist = importableAudio.getPlaylist();
        AudioItem audioItem = matchableItem.getLeft().getItem();
        if (!audioItem.hasPlaylist(playlist)) {
            try {
                audioItem.addPlaylist(playlist);
                playlist.addAudioItem(audioItem);
                store.commit(playlist);
                summaryTable.append(new TR("Add to Playlist", audioItem.getTitle(), playlist.getName()));
            } catch (Exception e) {
                reportPlaylistException(e, importableAudio, playlist);
            }
        }

    }

    /**
     * Common code to report an exception encountered while putting an audio item into a playlist.
     * @param e the exception.
     * @param importableAudio the importable audio item (from the Program Spec)
     * @param playlist the ACM Playlist into which an attempt was made to put the audio item.
     */
    private void reportPlaylistException(Exception e,
        ImportableAudioItem importableAudio,
        Playlist playlist)
    {
        errorCount++;
        UIUtils.setLabelText(errorMessagesLabel, Integer.toString(errorCount));
        summaryTable.append(new TR("Playlist Exception", playlist.getName(), e.toString())
            .withStyler(pinkZebra));

        String logMsg = String.format("Playlist exception '%s'", importableAudio);
        LOG.log(Level.SEVERE, logMsg, e);
        errors.add(new AddToPlaylistException(logMsg, e));
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
                String existingLanguage = item.getLanguageCode();
                if (!context.languagecode.equals(existingLanguage)) {
                    System.out.println(String.format("Forcing language to '%s' was '%s'.",
                        context.languagecode,
                        existingLanguage));
                    item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, context.languagecode);
                    summaryTable.append(new TR("Forcing language", context.languagecode, existingLanguage));
                }
                // Force the title.
                String existingTitle = item.getTitle();
                if (!importableAudio.getTitle().equals(existingTitle)) {
                    System.out.println(String.format("Renaming '%s' to '%s'.",
                        existingTitle,
                        importableAudio.getTitle()));
                    summaryTable.append(new TR("Renaming", existingTitle, importableAudio.getTitle()));
                    item.getMetadata()
                        .put(MetadataSpecification.DC_TITLE, importableAudio.getTitle());
                }

            }
        }
    }

    /**
     * Typed exception wrapper for any audio import exception.
     */
    private static class ImportException extends Exception {
        ImportException(String message, Exception cause) {
            super(message, cause);
        }
    }

    /**
     * Typed exception wrapper for any playlist exception.
     */
    private static class AddToPlaylistException extends Exception {
        AddToPlaylistException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
