package org.literacybridge.acm.gui.assistants.Matcher;

public class Target {
    private boolean replaceOk = false;

    public void setReplaceOk(boolean replaceOk) {
        this.replaceOk = replaceOk;
    }
    public boolean isReplaceOk() {
        return replaceOk;
    }

    public boolean isImportable() {
        return !targetExists() || isReplaceOk();
    }

    /**
     * Sub-classes override this with appropriate behaviour if the target MAY exist.
     * @return true if there is an extant instance of the target.
     */
    public boolean targetExists() { return false; }

}
