package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.ProblemReviewDialog;
import org.literacybridge.acm.gui.assistants.ContentImport.AudioMessageTarget;
import org.literacybridge.acm.gui.assistants.ContentImport.AudioTarget;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.*;
import org.literacybridge.acm.utils.EmailHelper;
import org.literacybridge.acm.utils.EmailHelper.TD;
import org.literacybridge.acm.utils.Version;

import javax.swing.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.literacybridge.acm.Constants.*;
import static org.literacybridge.acm.utils.EmailHelper.pinkZebra;

public class PromptImportedPage extends AcmAssistantPage<PromptImportContext> {

    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JLabel importedMessagesLabel;
    private final JLabel updatedMessagesLabel;
    private final JLabel standardFileMessagesLabel;
    private final JLabel errorMessagesLabel;
    private final JButton viewErrorsButton;
    private final JLabel currentMessage;

    private int importCount;
    private int updateCount;
    private int standardFileCount;
    private Box standardLabelBox;
    private int errorCount;
    private int progressCount;
    private StringBuilder summaryMessage;
    private EmailHelper.HtmlTable summaryTable;
    private List<Exception> errors = new ArrayList<>();
    private DBConfiguration dbConfig;

    PromptImportedPage(Assistant.PageHelper<PromptImportContext> listener) {
        super(listener);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Finished Importing System Prompts</span>"
                + "</html>");
        add(welcome, gbc);

        // The status line. Updated as items are imported.
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Imported "));
        importedMessagesLabel = makeBoxedLabel("no");
        hbox.add(importedMessagesLabel);
        hbox.add(new JLabel(" new prompt(s); updated "));
        updatedMessagesLabel = makeBoxedLabel("no");
        hbox.add(updatedMessagesLabel);
        hbox.add(new JLabel(" prompt(s). "));
        standardLabelBox = Box.createHorizontalBox();
        standardLabelBox.add(new JLabel("Installed "));
        standardFileMessagesLabel = makeBoxedLabel("no");
        standardLabelBox.add(standardFileMessagesLabel);
        standardLabelBox.add(new JLabel(" standard files. "));
        standardLabelBox.setVisible(false);
        hbox.add(standardLabelBox);

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
        List<PromptMatchable> matches = context.matcher.matchableItems.stream()
            .filter((item)->item.getMatch().isMatch())
            .filter((item) -> item.getLeft().isImportable())
            .collect(Collectors.toList());

        AudioImporter importer = AudioImporter.getInstance();
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

        // Debugging
        for (PromptMatchable matchable : matches) {
            // Get data of all previous items
            PromptTarget left = matchable.getLeft();
            String id = left.getPromptId();

            // Get next data
            File importableFile = matchable.getRight().getFile();

            try {
                Category systemCategory = store.getTaxonomy().getCategory(CATEGORY_TB_SYSTEM);
                ImportHandler handler = new ImportHandler(systemCategory, matchable);

                AudioItem audioItem = importer.importAudioItemFromFile(importableFile, handler);
                matchable.getLeft().setItem(audioItem);
                store.newAudioItem(audioItem);
            } catch (Exception e) {
                //System.out.println(e.getMessage());
            }
        }

        progressBar.setMaximum(matches.size()+1);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                summaryMessage = new StringBuilder("<html>");
                summaryTable = new EmailHelper.HtmlTable().withStyle("border-collapse:collapse;border:2px lightgray");
                performImports(matches);
                return 0;
            }

            @Override
            protected void done() {
                UIUtils.setLabelText(currentMessage, "Click \"Close\" to return to the ACM.");
                setCursor(Cursor.getDefaultCursor());
                summaryMessage.append(summaryTable.toString());
                summaryMessage.append("</html>");
                EmailHelper.sendNotificationEmail(
                    String.format("%s System Prompts Imported for language %s",
                        dbConfig.getProjectName(), getLanguageAndName(context.languagecode)),
                    summaryMessage.toString());
                UIUtils.setProgressBarValue(progressBar, ++progressCount);
                setComplete();
                progressBar.setVisible(false);
                if (errors.size() > 0) {
                    statusLabel.setForeground(Color.red);
                    setStatus("Finished, but with errors.");
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
    protected boolean isSummaryPage() {
        return true;
    }

    private void setStatus(String status) {
        UIUtils.setLabelText(statusLabel, "<html>" + "<span style='font-size:2.5em'>"+status+"</span>" + "</html>");
    }

    /**
     * Shows the errors to the user, and gives them an opportunity to send an error report to Amplio.
     * @param actionEvent is unused.
     */
    private void onViewErrors(ActionEvent actionEvent) {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        String computerName;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }

        String message = "<html>The following error(s) occurred when attempting to import system prompts. " +
            "Please double check that the files are good audio. If possible, try listening to the " +
            "messages outside of the ACM application. If the problem persists, contact Amplio technical " +
            "support. The button below will send this report to Amplio."+
            "</html>";
        String reportHeading = String.format("Error report from System Prompts Import Assistant\n\n" +
                "Program %s, User %s, Computer %s, Language %s\nSystem Prompts Import at %s\n" +
                "ACM Version %s, built %s\n",
            dbConfig.getProjectName(),
            ACMConfiguration.getInstance().getUserContact(),
            computerName, getLanguageAndName(context.languagecode),
            localDateTimeFormatter.format(LocalDateTime.now()),
            Constants.ACM_VERSION, Version.buildTimestamp);

        ProblemReviewDialog dialog = new ProblemReviewDialog(Application.getApplication(),
            "Errors While Importing",
            "Error report from Prompt Assistant");
        dialog.showProblems(message, reportHeading, null, errors);
    }

    private void performImports(List<PromptMatchable> matches) {
        String languagecode = context.languagecode;
        dbConfig = ACMConfiguration.getInstance().getCurrentDB();

        summaryMessage.append(String.format("<h2>Program %s</h2>", dbConfig.getProjectName()));
        summaryMessage.append(String.format("<h3>%s</h3>", localDateTimeFormatter.format(LocalDateTime.now())));
        summaryMessage.append(String.format("<p>Importing System Prompts for language %s.</p>", getLanguageAndName(languagecode)));

        importCount = 0;
        updateCount = 0;
        standardFileCount = 0;
        errorCount = 0;
        progressCount = 0;

        // Iterate over the matched items.
        matches
            .forEach((item) -> {
                try {
                    // Make sure the directories exist. Create the .grp file if it doesn't exist.
                    PromptsInfo.PromptInfo promptInfo = item.getLeft().getPromptInfo();
                    String promptId = promptInfo.getId(); // 0, 1, etc.
                    UIUtils.setLabelText(currentMessage, item.getLeft().toString()); // 0: bell,...
                    File destDir = promptInfo.isPlaylistPrompt() ? context.promptsDir : context.languageDir;
                    File promptFile = new File(destDir, promptId+".a18");
                    boolean isReplace = promptFile.exists();
                    summaryTable.append(new EmailHelper.TR(isReplace?"Replace":"Import", item.getLeft().toString(), item.getRight().getFile().toString()));
                    if (!promptFile.exists()) {
                        if (!destDir.exists()) {
                            if (!destDir.mkdirs()) throw new Exception(String.format("Unable to create directory '%s'", destDir.getAbsolutePath()));
                            summaryTable.append(new EmailHelper.TR(new TD(), new TD("Created language directory")));
                        }
                    }
                    // Import the audio.
                    File sourceFile = item.getRight().getFile();
                    AudioUtils.copyOrConvert(item.getLeft().toString(), languagecode, sourceFile, promptFile);

                    if (isReplace) {
                        UIUtils.setLabelText(updatedMessagesLabel, Integer.toString(++updateCount));
                    } else {
                        UIUtils.setLabelText(importedMessagesLabel, Integer.toString(++importCount));
                    }

                } catch (Exception e) {
                    errors.add(e);
                    errorCount++;
                    UIUtils.setLabelText(errorMessagesLabel, Integer.toString(errorCount));
                    summaryTable.append(new EmailHelper.TR("Exception importing", item.getRight().getTitle(), e.toString())
                        .withStyler(pinkZebra));
                }
                UIUtils.setProgressBarValue(progressBar, ++progressCount);
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
            });

        importBoilerplateFiles(TUTORIAL_LIST);
        importBoilerplateFiles(BELL_SOUND);
        importBoilerplateFiles(SILENCE);
    }

    /**
     * Files that don't depend on program or language, the bell, and the .txt file for the tutorial.
     * @param name of the boilerplate file.
     */
    private void importBoilerplateFiles(String name) {
        File destFile = new File(context.languageDir, name);
        if (!destFile.exists()) {
            InputStream srcStream = PromptImportedPage.class.getClassLoader().getResourceAsStream(name);
            try {
                // srcStream may be null, in which case we'll catch and report the exception.
                //noinspection ConstantConditions
                FileUtils.copyInputStreamToFile(srcStream, destFile);
                UIUtils.setVisible(standardLabelBox, true);
                UIUtils.setLabelText(standardFileMessagesLabel, Integer.toString(++standardFileCount));
            } catch (Exception e) {
                errors.add(e);
                errorCount++;
                UIUtils.setLabelText(errorMessagesLabel, Integer.toString(errorCount));
                summaryTable.append(new EmailHelper.TR("Exception saving standard file", name)
                    .withStyler(pinkZebra));
            }
        }
    }

    @Override
    protected String getTitle() {
        return "Import Results";
    }

    private class ImportHandler implements AudioImporter.AudioItemProcessor {

        private final Category category;
        private final PromptMatchable promptMatchable;

        ImportHandler(Category category, PromptMatchable matchable) {
            this.category = category;
            this.promptMatchable = matchable;
        }

        @Override
        public void process(AudioItem item) {
            if (item != null) {
                item.addCategory(category);
            }

            String existingLanguage = item.getLanguageCode();
            if (!context.languagecode.equals(existingLanguage)) {
                System.out.printf("Forcing language to '%s' was '%s'.%n",
                        context.languagecode,
                        existingLanguage);
                item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, context.languagecode);
            }

            String existingTitle = item.getTitle();
            // Debugging
            String _debug = promptMatchable.getLeft().getPromptId();
            if (!promptMatchable.getLeft().getPromptId().equals(existingTitle)) {
                System.out.printf("Renaming '%s' to '%s'.%n",
                        existingTitle,
                        promptMatchable.getLeft().getPromptId());
                item.getMetadata()
                        .put(MetadataSpecification.DC_TITLE, promptMatchable.getLeft().getPromptId());
            }


        }
    }
}
