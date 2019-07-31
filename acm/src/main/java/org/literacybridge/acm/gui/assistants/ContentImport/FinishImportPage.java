package org.literacybridge.acm.gui.assistants.ContentImport;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.ProblemReviewDialog;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.Transaction;
import org.literacybridge.acm.utils.EmailHelper;
import org.literacybridge.acm.utils.Version;
import org.literacybridge.core.spec.ContentSpec;

import javax.swing.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
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
import static org.literacybridge.acm.Constants.CATEGORY_TB_CATEGORIES;
import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import static org.literacybridge.acm.utils.EmailHelper.HtmlTable;
import static org.literacybridge.acm.utils.EmailHelper.TR;
import static org.literacybridge.acm.utils.EmailHelper.pinkZebra;

public class FinishImportPage extends ContentImportBase<ContentImportContext> {
    private static final Logger LOG = Logger.getLogger(FinishImportPage.class.getName());

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
    private Category categoriesCategory = ACMConfiguration.getInstance()
        .getCurrentDB()
        .getMetadataStore()
        .getTaxonomy()
        .getCategory(CATEGORY_TB_CATEGORIES);

    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JButton viewErrorsButton;
    private int progressCount;

    FinishImportPage(PageHelper<ContentImportContext> listener) {
        super(listener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Finished Content Import</span>" + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Import message content for deployment "));
        hbox.add(makeBoxedLabel(Integer.toString(context.deploymentNo)));
        hbox.add(new JLabel(" in language "));
        hbox.add(makeBoxedLabel(AcmAssistantPage.getLanguageAndName(context.languagecode)));
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        // The status line. Updated as items are imported.
        hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Imported "));
        importedMessagesLabel = makeBoxedLabel("no");
        hbox.add(importedMessagesLabel);
        hbox.add(new JLabel(" new message(s); updated "));
        updatedMessagesLabel = makeBoxedLabel("no");
        hbox.add(updatedMessagesLabel);
        hbox.add(new JLabel(" message(s). Created "));
        String message = context.createdPlaylists.size() == 0 ?
                         "no" :
                         Integer.toString(context.createdPlaylists.size());
        hbox.add(makeBoxedLabel(message));
        hbox.add(new JLabel(" new playlist(s). "));
        errorMessagesLabel = makeBoxedLabel("No");
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
                UIUtils.setLabelText(currentMessage, "Click \"Close\" to return to the ACM.");
                setCursor(Cursor.getDefaultCursor());
                summaryMessage.append(summaryTable.toString());
                summaryMessage.append("</html>");
                EmailHelper.sendNotificationEmail("Content Imported", summaryMessage.toString());
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

    /**
     * Shows the errors to the user, and gives them an opportunity to send an error report to Amplio.
     * @param actionEvent is unused.
     */
    private void onViewErrors(@SuppressWarnings("unused") ActionEvent actionEvent) {
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
            "The Audio File may be damaged. If possible, try listening to the " +
            "messages outside of the ACM application. If the problem persists, contact Amplio technical " +
            "support. The button below will send this report to Amplio."+
            "</html>";
        String reportHeading = String.format("Error report from Content Import Assistant%n%n" +
                "Project %s, User %s (%s), Computer %s%nContent Import at %s%n" +
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

        ProblemReviewDialog dialog = new ProblemReviewDialog(Application.getApplication(), "Errors While Importing");
        dialog.showProblems(message, reportHeading, null, errors);
    }

    /**
     * Actually perform the imports. Called on a background thread to keep the UI responsive.
     */
    private void performImports() {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        Matcher<AudioTarget, ImportableFile, AudioMatchable> matcher = context.matcher;
        summaryMessage.append(String.format("<h2>Project %s</h2>", dbConfig.getProjectName()));
        summaryMessage.append(String.format("<h3>%s</h3>", localDateTimeFormatter.format(LocalDateTime.now())));
        summaryMessage.append(String.format(
            "<p>Importing content for Deployment %d, in language %s</p>",
            context.deploymentNo,
            dbConfig.getLanguageLabel(new Locale(context.languagecode))));

        importCount = 0;
        updateCount = 0;
        errorCount = 0;
        progressCount = 0;
        // Look at all of the matches.
        for (AudioMatchable matchableItem : matcher.matchableItems) {
            if (matchableItem.getMatch().isMatch()) {
                // If not already in the ACM DB, or the "update item" checkbox is on, do the import.
                boolean okToImport = matchableItem.getLeft().isImportable();
                if (okToImport) {
                    importOneItem(matchableItem);
                } else if (matchableItem.getLeft().hasAudioItem()
                        && !matchableItem.getLeft().isPlaylist()) {
                    ensureAudioInPlaylist(matchableItem.getLeft());
                }
            } else if (matchableItem.getLeft() != null && matchableItem.getLeft().hasAudioItem()
                    && !matchableItem.getLeft().isPlaylist()) {
                ensureAudioInPlaylist(matchableItem.getLeft());
            }
            UIUtils.setProgressBarValue(progressBar, ++progressCount);
        }

        // Refresh the content and playlist views.
        Application.getFilterState().updateResult(true);
    }

    /**
     * Import a single audio file.
     * @param matchableItem to be imported.
     */
    private void importOneItem(AudioMatchable matchableItem)
    {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        AudioImporter importer = AudioImporter.getInstance();
        AudioTarget importableAudio = matchableItem.getLeft();

        try {
            summaryTable.append(new TR(matchableItem.getOperation(),
                importableAudio.toString(),
                matchableItem.getRight().getFile().getCanonicalPath()));
            String msg = String.format("%s: '%s' from '%s'",
                matchableItem.getOperation(),
                importableAudio.toString(),
                matchableItem.getRight().getFile().getCanonicalPath());
            UIUtils.setLabelText(currentMessage, msg);

            Category cat = null;
            if (matchableItem.getLeft().isPlaylist()) {
                cat = categoriesCategory;
            } else {
                ContentSpec.MessageSpec messageSpec = matchableItem.getLeft().getMessageSpec();
                assert(messageSpec != null);
                String defaultCat = messageSpec.default_category;
                if (StringUtils.isNoneEmpty(defaultCat)) {
                    cat = store.getTaxonomy().getRootCategory().findChildWithName(defaultCat);
                }
                if (cat == null) {
                    cat = generalOtherCategory;
                }
            }
            ImportHandler handler = new ImportHandler(cat, matchableItem);
            File importableFile = matchableItem.getRight().getFile();

            // Add or update the audio file to the ACM database.
            boolean isUpdate = matchableItem.getLeft().hasAudioItem(); // to update proper counter after success
            if (isUpdate) {
                AudioItem existingItem = matchableItem.getLeft().getItem();
                importer.updateAudioItem(existingItem, importableFile, handler);
            } else {
                AudioItem audioItem = importer.importFile(importableFile, handler);
                matchableItem.getLeft().setItem(audioItem);
            }

            // If the item isn't already in the playlist, add it. Do not, however, add playlist prompts
            // to a playlist.
            if (!matchableItem.getLeft().isPlaylist()) {
                ensureAudioInPlaylist(matchableItem.getLeft());
            }
            if (isUpdate) {
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
     * Add an audio item to a playlist, if it is not already there.
     * @param audioTarget to be put into a playlist, if needed.
     */
    private void ensureAudioInPlaylist(AudioTarget audioTarget) {
        // Only put content messages (not playlist announement messages) into playlists.
        if (!(audioTarget instanceof AudioMessageTarget)) return;

        AudioMessageTarget audioMessageTarget = (AudioMessageTarget) audioTarget;

        Playlist playlist = audioMessageTarget.getPlaylist();
        AudioItem audioItem = audioMessageTarget.getItem();

        if (!audioItem.hasPlaylist(playlist)) {
            try {
                Transaction transaction = ACMConfiguration.getInstance()
                    .getCurrentDB()
                    .getMetadataStore()
                    .newTransaction();

                audioItem.addPlaylist(playlist);
                transaction.add(audioItem);

                int index = findIndexInPlaylist(audioMessageTarget);
                playlist.addAudioItem(index, audioItem);
                transaction.add(playlist);

                transaction.commit();
                summaryTable.append(new TR("Add to Playlist", audioItem.getTitle(), playlist.getName()));
            } catch (Exception e) {
                reportPlaylistException(e, audioMessageTarget, playlist);
            }
        }

    }

    /**
     * Find the index at which a message should be placed in the ACM playlist.
     *
     * Given a message title (from the program spec) and an ACM playlist, find the target
     * index as follows:
     * - Get a list of the titles already in the ACM playlist.
     * - Get a list of the titles in the program spec playlist. Find the given message in that
     *   list of titles.
     * - Take the immediately preceding title from the program spec playlist, and see if that
     *   title is in the ACM playlist. If it is, put the new message immediately after that
     *   existing message.
     * - If the immediately preceding title is not found, take the immediately following title
     *   from the program spec playlist, and look for that in the ACM playlist. If it is found,
     *   put the new message immediately before that existing message.
     * - If neither of the immediate neighbors was found, try the next closest previous title,
     *   and if it is not found, try the next closest following title.
     * - Continue until a neighbor (however distant) is found in the ACM playlist, or until there
     *   are no further program spec titles for which to look. If no neighbor is found, put the
     *   new title at index 0, the first item in the playlist.
     * @param audioMessageTarget describing the message to be inserted, and the playlist into
     *                           which it should be inserted.
     * @return the index at which the message should be inserted into the playlist.
     */
    private int findIndexInPlaylist(AudioMessageTarget audioMessageTarget) {
        return AudioUtils.findIndexForMessageInPlaylist(audioMessageTarget.getMessageSpec(), audioMessageTarget.getPlaylist(), context.languagecode);
    }

    /**
     * Common code to report an exception encountered while putting an audio item into a playlist.
     * @param e the exception.
     * @param importableAudio the importable audio item (from the Program Spec)
     * @param playlist the ACM Playlist into which an attempt was made to put the audio item.
     */
    private void reportPlaylistException(Exception e,
        AudioTarget importableAudio,
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
        private final AudioMatchable matchableItem;

        ImportHandler(Category category, AudioMatchable matchableItem) {
            this.category = category;
            this.matchableItem = matchableItem;
        }

        @Override
        public void process(AudioItem item) {
            AudioTarget importableAudio = matchableItem.getLeft();

            // There really should be an item, but don't NPE if not.
            if (item != null) {
                // If there is no category, add to the specified category.
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
                // Set the SDG Goals and SDG Targets for content messages, if any are in the spec.
                ContentSpec.MessageSpec messageSpec = importableAudio.getMessageSpec();
                if (messageSpec != null) {
                    // If there are SDG Goals, and the message's SDG Goals are different, set the SDG Goals.
                    String specSdgGoals = messageSpec.sdg_goals;
                    String acmSdgGoals = item.getMetadata().get(MetadataSpecification.LB_SDG_GOALS);
                    if (StringUtils.isNotEmpty(specSdgGoals) && !StringUtils.equals(specSdgGoals, acmSdgGoals)) {
                        item.getMetadata().putMetadataField(MetadataSpecification.LB_SDG_GOALS, new MetadataValue<>(specSdgGoals));
                        summaryTable.append(new TR("Setting SDG Goals", specSdgGoals));
                    }
                    // Same with SDG Targets.
                    String specSdgTargets = messageSpec.sdg_targets;
                    String acmSdgTargets = item.getMetadata().get(MetadataSpecification.LB_SDG_TARGETS);
                    if (StringUtils.isNotEmpty(specSdgTargets) && !StringUtils.equals(specSdgTargets, acmSdgTargets)) {
                        item.getMetadata().putMetadataField(MetadataSpecification.LB_SDG_TARGETS, new MetadataValue<>(specSdgTargets));
                        summaryTable.append(new TR("Setting SDG Targets", specSdgTargets));
                    }
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
