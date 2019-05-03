package org.literacybridge.acm.gui.assistants.SystemPromptsImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;

public class PromptTarget extends Target {
    private String promptId;
    private String promptDefinition;

    // Is there already an audio greeting for the Recipient in the ACM?
    private boolean hasPrompt;

    public PromptTarget(String promptId, String promptDefinition) {
        this.promptId = promptId;
        this.promptDefinition = promptDefinition;
    }

    public String getPromptId() {
        return promptId;
    }
    public String getPromptDefinition() {
        return promptDefinition;
    }
    public boolean hasPrompt() {
        return hasPrompt;
    }
    public void setHasPrompt(boolean hasPrompt) {
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
