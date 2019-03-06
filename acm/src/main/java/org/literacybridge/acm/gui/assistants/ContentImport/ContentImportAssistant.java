package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.Assistant.Assistant;

import java.util.HashMap;
import java.util.Map;

public class ContentImportAssistant {

    public static Assistant<ContentImportContext> create() {
        // create the AssistantContainer:

        ContentImportContext context = new ContentImportContext();
        // Debugging & development:
        context.deploymentNo = 1;
        context.languagecode = "en";

        Map<String, Object> props = new HashMap<>();
        Assistant<ContentImportContext> assistant = new Assistant.Factory<ContentImportContext>()
            .withContext(context)
            .withPageCtors(WelcomePage::new, FilesPage::new, MatchPage::new, ReviewPage::new, ResultsPage::new)
            .withTitle("Content Import Assistant")
            .withLastPageSummary()
            .create();

        return assistant;
    }

}
