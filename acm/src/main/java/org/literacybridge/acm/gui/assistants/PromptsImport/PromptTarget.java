package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.acm.store.AudioItem;

public class PromptTarget extends Target {
    private final PromptsInfo.PromptInfo promptInfo;

    // Is there already an audio file for the System Prompt in the ACM?
    private boolean hasPrompt;

    protected AudioItem item;

    PromptTarget(PromptsInfo.PromptInfo promptInfo) {
        this.promptInfo = promptInfo;
    }

    public PromptsInfo.PromptInfo getPromptInfo() {
        return promptInfo;
    }

    /**
     * The TB id of the prompt. The pure system prompts are numeric, like "1", "2", ... There
     * are also required playlist prompts with names "9-0" and "$0-1".
     * @return the prompt's id.
     */
    String getPromptId() {
        return promptInfo.getId();
    }
    String getPromptFilename() { return promptInfo.getFilename(); }
    String getPromptText() {
        return promptInfo.getText();
    }
    private boolean hasPrompt() {
        return hasPrompt;
    }
    void setHasPrompt(boolean hasPrompt) {
        this.hasPrompt = hasPrompt;
    }

    public void setItem(AudioItem item) {
        this.item = item;
    }

    public AudioItem getItem() {
        return item;
    }

    @Override
    public boolean targetExists() {
        return hasPrompt();
    }

    @Override
    public String toString() {
        return getPromptId() + ": " + promptInfo.getFilename() + ": " + getPromptText();
    }

}
