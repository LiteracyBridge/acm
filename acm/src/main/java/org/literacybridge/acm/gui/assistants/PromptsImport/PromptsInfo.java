package org.literacybridge.acm.gui.assistants.PromptsImport;

import com.opencsv.CSVReader;
import org.apache.commons.io.input.BOMInputStream;
import org.literacybridge.acm.Constants;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromptsInfo {
    private static Pattern tutorialPattern = Pattern.compile("^(i)?(\\"+ Constants.CATEGORY_TUTORIAL+")$");

    @SuppressWarnings("unused")
    public static class PromptInfo {
        private String id;
        private String filename;
        private String text;
        private String explanation;
        private boolean tutorialPrompt = false;
        private boolean tutorialInvitation = false;

        public PromptInfo(String id, String filename, String text, String explanation) {
            this.id = id;
            this.filename = filename;
            this.text = text;
            this.explanation = explanation;
            Matcher matcher = tutorialPattern.matcher(id);
            if (matcher.matches()) {
                tutorialPrompt = true;
                tutorialInvitation = matcher.groupCount() > 1;
            }
        }
        public PromptInfo(String[] info) {
            this(info[0], info[1], info[2], info[3]);
        }

        public String getId() {
            return id;
        }

        public String getFilename() {
            return filename;
        }

        public String getText() {
            return text;
        }

        public String getExplanation() {
            return explanation;
        }

        public boolean isTutorialPrompt() {
            return tutorialPrompt;
        }
        public boolean isTutorialInvitation() {
            return tutorialInvitation;
        }
    }

    Map<String, PromptInfo> promptsMap = new LinkedHashMap<>();
    List<String> ids;
    public PromptsInfo() {
        loadPromptsInfo();
    }

    public PromptInfo getPrompt(String id) {
        return promptsMap.get(id);
    }

    public List<PromptInfo> getPrompts() {
        return new ArrayList<>(promptsMap.values());
    }

    public List<String> getIds() {
        if (ids == null) {
            ids = new ArrayList<>(promptsMap.keySet());
        }
        return ids;
    }

    /**
     * Reads a .csv file of "id,filename,text,explanation" tuples from a resource.
\     */
    private void loadPromptsInfo() {
        String promptsFileName = "prompts_ex.csv";
        InputStream csvStream = PromptImportAssistant.class.getClassLoader().getResourceAsStream(promptsFileName);
        try (BOMInputStream bis = new BOMInputStream(csvStream);
            Reader ir = new InputStreamReader(bis);
            CSVReader reader = new CSVReader(ir)) {
            Collection<PromptInfo> prompts = new LinkedList<>();
            for (String[] line : reader.readAll()) {
                prompts.add(new PromptInfo(line));
            }
            prompts.forEach(i->promptsMap.put(i.getId(), i));
        } catch (Exception ignored) {
            // Ignore
        }
    }

}
