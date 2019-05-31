package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;

public class PromptTarget extends Target {
    private String promptId;
    private String promptDefinition;

    // Is there already an audio greeting for the Recipient in the ACM?
    private boolean hasPrompt;

    PromptTarget(String promptId, String promptDefinition) {
        this.promptId = promptId;
        this.promptDefinition = promptDefinition;
    }

    String getPromptId() {
        return promptId;
    }
    String getPromptDefinition() {
        return promptDefinition;
    }
    private boolean hasPrompt() {
        return hasPrompt;
    }
    void setHasPrompt(boolean hasPrompt) {
        this.hasPrompt = hasPrompt;
    }

    @Override
    public boolean targetExists() {
        return hasPrompt();
    }

    @Override
    public String toString() {
        return promptId + ": " + promptDefinition;
    }

    
}
