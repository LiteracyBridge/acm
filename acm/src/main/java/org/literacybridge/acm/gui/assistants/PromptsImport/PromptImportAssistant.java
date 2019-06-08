package org.literacybridge.acm.gui.assistants.PromptsImport;

import com.opencsv.CSVReader;
import org.apache.commons.io.input.BOMInputStream;
import org.literacybridge.acm.gui.Assistant.Assistant;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class PromptImportAssistant {

    public static Assistant<PromptImportContext> create() {

        // Development debugging
        PromptImportContext context = new PromptImportContext();
        // Debugging & development:
//        if (ACMConfiguration.isTestData()) {
//        }

        context.promptDefinitions = loadSystemPrompts();
        context.promptIds = new ArrayList<>(context.promptDefinitions.keySet());
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
     * Reads a .csv file of "id,definition" pairs from a resource.
     * @return A map of {id : definition}.
     */
    private static Map<String, String> loadSystemPrompts() {
        Map<String, String> result = null;
        String promptsFileName = "prompts.csv";
        InputStream csvStream = PromptImportAssistant.class.getClassLoader().getResourceAsStream(promptsFileName);
        try (BOMInputStream bis = new BOMInputStream(csvStream);
            Reader ir = new InputStreamReader(bis);
            CSVReader reader = new CSVReader(ir)) {

            Map<String,String> prompts = new LinkedHashMap<>();

            String[] nextLine;
            nextLine = reader.readNext();

            if (nextLine.length == 2 && nextLine[0].equals("id")
                && nextLine[1].equals("definition")) {
                for (String[] line : reader.readAll()) {
                    prompts.put(line[0], line[1]);
                }
            }
            result = prompts;
        } catch (Exception ignored) {
            // Ignore; return null
        }
        return result;
    }

}
