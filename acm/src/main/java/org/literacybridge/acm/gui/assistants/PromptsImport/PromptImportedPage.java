package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.ProblemReviewDialog;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.gui.util.UIUtils;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.literacybridge.acm.utils.EmailHelper.pinkZebra;

public class PromptImportedPage extends AcmAssistantPage<PromptImportContext> {

    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JLabel importedMessagesLabel;
    private final JLabel updatedMessagesLabel;
    private final JLabel errorMessagesLabel;
    private final JButton viewErrorsButton;
    private final JLabel currentMessage;

    private int importCount;
    private int updateCount;
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

        progressBar.setMaximum(matches.size()+1);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker worker = new SwingWorker<Integer, Void>() {
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
    private void onViewErrors(@SuppressWarnings("unused") ActionEvent actionEvent) {
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
                "Project %s, User %s (%s), Computer %s, Language %s\nSystem Prompts Import at %s\n" +
                "ACM Version %s, built %s\n",
            dbConfig.getProjectName(),
            ACMConfiguration.getInstance().getUserName(),
            ACMConfiguration.getInstance().getUserContact(),
            computerName, getLanguageAndName(context.languagecode),
            localDateTimeFormatter.format(LocalDateTime.now()),
            Constants.ACM_VERSION, Version.buildTimestamp);

        ProblemReviewDialog dialog = new ProblemReviewDialog(Application.getApplication(), "Errors While Importing");
        dialog.showProblems(message, reportHeading, null, errors);
    }

    private void performImports(List<PromptMatchable> matches) {
        String languagecode = context.languagecode;
        dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        File tbOptionsDir = new File(tbLoadersDir, "TB_Options");
        File languagesDir = new File(tbOptionsDir, "languages");
        File languageDir = new File(languagesDir, languagecode);

        summaryMessage.append(String.format("<h2>Project %s</h2>", dbConfig.getProjectName()));
        summaryMessage.append(String.format("<h3>%s</h3>", localDateTimeFormatter.format(LocalDateTime.now())));
        summaryMessage.append(String.format("<p>Importing System Prompts for language %s.</p>", getLanguageAndName(languagecode)));

        importCount = 0;
        updateCount = 0;
        errorCount = 0;
        progressCount = 0;

        // Iterate over the matched items.
        matches
            .forEach((item) -> {
                try {
                    // Make sure the directories exist. Create the .grp file if it doesn't exist.
                    String promptId = item.getLeft().getPromptId(); // 0, 1, etc.
                    UIUtils.setLabelText(currentMessage, item.getLeft().toString()); // 0: bell,...
                    File promptFile = new File(languageDir, promptId+".a18");
                    boolean isReplace = promptFile.exists();
                    summaryTable.append(new EmailHelper.TR(isReplace?"Replace":"Import", item.getLeft().toString(), item.getRight().getFile().toString()));
                    if (!promptFile.exists()) {
                        if (!languageDir.exists()) {
                            if (!languageDir.mkdirs()) throw new Exception(String.format("Unable to create directory '%s'", languageDir.getAbsolutePath()));
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
                    summaryTable.append(new EmailHelper.TR("Exception importing", item.getRight().getFile().getName(), e.toString())
                        .withStyler(pinkZebra));
                }
                UIUtils.setProgressBarValue(progressBar, ++progressCount);
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
            });
    }

    @Override
    protected String getTitle() {
        return "Import Results";
    }
}