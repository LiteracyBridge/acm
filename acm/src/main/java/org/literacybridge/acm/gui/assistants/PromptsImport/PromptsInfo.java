package org.literacybridge.acm.gui.assistants.PromptsImport;

import com.opencsv.CSVReader;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromptsInfo {
    private static PromptsInfo instance = null;
    public static synchronized PromptsInfo getInstance() {
        if (instance == null) {
            instance = new PromptsInfo();
        }
        return instance;
    }
    // i?$?\d*(-\d*)+   matches 1-2, i1-2, $0-1, or 1-2-3, ...
    static final Pattern playlistPromptPattern = Pattern.compile("^(i)?(\\$?\\d*(?:-\\d*)+)$");
    public static final String PROMPTS_FILE_NAME = "prompts_ex.csv";

    @SuppressWarnings("unused")
    public static class PromptInfo {
        private final String promptId;
        private final String promptTitle;
        private final String promptText;
        private final String explanation;
        private boolean playlistPrompt = false;
        private boolean playlistInvitation = false;

        public PromptInfo(String promptId, String promptTitle, String promptText, String explanation) {
            this.promptId = promptId;
            this.promptTitle = promptTitle;
            this.promptText = promptText;
            this.explanation = explanation;
            Matcher matcher = playlistPromptPattern.matcher(promptId);
            if (matcher.matches()) {
                playlistPrompt = true;
                playlistInvitation = matcher.groupCount() > 1 && matcher.group(1) != null;
            }
        }
        public PromptInfo(String[] info) {
            this(info[0], info[1], info[2], info[3]);
        }

        public String getPromptId() {
            return promptId;
        }

        public String getPromptTitle() {
            return promptTitle;
        }

        public String getPromptText() {
            return promptText;
        }

        public String getExplanation() {
            return explanation;
        }

        public boolean isPlaylistPrompt() {
            return playlistPrompt;
        }
        public boolean isPlaylistInvitation() {
            return playlistInvitation;
        }

        @Override
        public String toString() { return promptId + ": " + promptTitle; }
    }

    // Map from the id (1, 2, 3, '9-0', '$0-1') to PromptInfo.
    Map<String, PromptInfo> promptsMap = new LinkedHashMap<>();
    List<String> ids;

    private PromptsInfo() {
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
        InputStream csvStream = PromptImportAssistant.class.getClassLoader()
            .getResourceAsStream(PROMPTS_FILE_NAME);
        try (BOMInputStream bis = new BOMInputStream(csvStream);
            Reader ir = new InputStreamReader(bis);
            CSVReader reader = new CSVReader(ir)) {
            Collection<PromptInfo> prompts = new LinkedList<>();
            for (String[] line : reader.readAll()) {
                prompts.add(new PromptInfo(line));
            }
            prompts.forEach(i->promptsMap.put(i.getPromptId(), i));
        } catch (Exception ignored) {
            // Ignore
        }
    }

}
