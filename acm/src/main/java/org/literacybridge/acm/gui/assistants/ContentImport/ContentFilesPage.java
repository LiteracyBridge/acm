package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.assistants.common.AbstractFilesPage;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContentFilesPage extends AbstractFilesPage<ContentImportContext> {
    private ContentImportBase.ImportReminderLine importReminderLine;

    ContentFilesPage(PageHelper<ContentImportContext> listener) {
        super(listener);
    }

    protected List<JComponent> getPageIntro() {
        List<JComponent> components = new ArrayList<>();
        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Choose Audio Files to Import.</span>"
                + "<br/><br/><p>Choose the audio files that you wish to import.</p>" + "</html>");
        components.add(welcome);

        importReminderLine = new ContentImportBase.ImportReminderLine();
        components.add(importReminderLine.getLine());

        return components;
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        super.onPageEntered(progressing);

        if (ACMConfiguration.isTestData() && progressing && context.importableRoots.size()==0) {
            context.importableRoots.addAll(Collections.singletonList(new File("/Users/bill/A-test1")));
        }

        // Fill deployment and language
        importReminderLine.getDeployment().setText(Integer.toString(context.deploymentNo));
        importReminderLine.getLanguage().setText(AcmAssistantPage.getLanguageAndName(context.languagecode));
    }

    @Override
    protected String getTitle() {
        return "Audio Files to Import";
    }

}
