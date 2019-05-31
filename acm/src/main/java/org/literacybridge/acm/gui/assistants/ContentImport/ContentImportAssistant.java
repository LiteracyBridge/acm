package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant;

/**
 * Helper class to create a new Content Import Assistant.
 */
public class ContentImportAssistant {

    private static Assistant<ContentImportContext> create(boolean promptsOnly) {
        // Create the context and populate from project configuration
        ContentImportContext context = new ContentImportContext();
        context.fuzzyThreshold = ACMConfiguration.getInstance().getCurrentDB().getFuzzyThreshold();
        context.notifyList = ACMConfiguration.getInstance().getCurrentDB().getNotifyList();
        context.promptsOnly = promptsOnly;

        // Debugging & development:
        if (ACMConfiguration.isTestData()) {
            context.deploymentNo = 1;
            context.languagecode = "en";
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        Assistant<ContentImportContext> assistant = new Assistant.Factory<ContentImportContext>().withContext(
            context)
            .withPageFactories(WelcomePage::new, ContentFilesPage::new,
//                ContentMatchPageOld::new,
                ContentMatchPage::new, ReviewPage::new, ImportedPage::new)
            .withTitle("Content Import Assistant")
            .create();

        return assistant;
    }

    public static Assistant<ContentImportContext> create() {
        return create(false);
    }

    public static Assistant<ContentImportContext> createPromptImporter() {
        return create(true);
    }

}
