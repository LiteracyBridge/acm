package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.Assistant.Assistant;

public class GreetingsImportAssistant {

    public static Assistant<GreetingsImportContext> create() {

        // Development debugging
        GreetingsImportContext context = new GreetingsImportContext();
        // Debugging & development:
//        if (ACMConfiguration.isTestData()) {
//        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        Assistant<GreetingsImportContext> assistant = new Assistant.Factory<GreetingsImportContext>()
            .withContext(context)
            .withPageFactories(WelcomePage::new,
                GreetingsFilesPage::new,
                GreetingsMatchPage::new,
                GreetingsImportedPage::new)
            .withTitle("Greetings Import Assistant")
            .create();

        return assistant;
    }

}
