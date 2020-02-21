package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.gui.Assistant.Assistant;

import java.util.Comparator;

public class PromptImportAssistant {

    private static final String PROMPTS_CSV_FILE_NAME = "prompts.csv";

    public static Assistant<PromptImportContext> create() {

        // Development debugging
        PromptImportContext context = new PromptImportContext();
        // Debugging & development:
//        if (ACMConfiguration.isTestData()) {
//        }

        context.promptsInfo = new PromptsInfo();
        //context.promptDefinitions.forEach((k,v)->context.promptHasRecording.put(k,false));

        @SuppressWarnings("UnnecessaryLocalVariable")
        Assistant<PromptImportContext> assistant = new Assistant.Factory<PromptImportContext>().withContext(
            context)
            .withPageFactories(PromptWelcomePage::new,
                PromptFilesPage::new,
                PromptMatchPage::new,
                ReviewPage::new,
                PromptImportedPage::new)
            .withTitle("System Prompts Assistant")
            .create();

        return assistant;
    }

    /**
     * Implements a sorter for prompt ids. The id may be pure numeric, in which case we want to
     * sort by numeric value, or may be a string ($1-0), in which case we want to sort alphanumerially.
     */
    public static class PromptIdSorter implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            Integer i1=null, i2=null;
            try {
                i1 = Integer.parseInt(o1);
            } catch(Exception ignored) { }
            try {
                i2 = Integer.parseInt(o2);
            } catch(Exception ignored) { }
            // Both numeric
            if (i1 != null && i2 != null) {
                return i1-i2;
            }
            // Neither numeric
            if (i1==null && i2==null) {
                int l1 = o1.length();
                int l2 = o2.length();
                if (l1!=0 && l2!=0) {
                    // Two non-empty strings.
                    return o1.compareToIgnoreCase(o2);
                }
                // At least one is empty. Let a non-empty one be smaller.
                if (l1>0) return -1;
                if (l2>0) return 1;
                // Both empty, equal.
                return 0;
            }
            // Only one numeric. Let the numeric one be "smaller".
            if (i1!=null) {
                return -1;
            }
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }
}
