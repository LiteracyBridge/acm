package org.literacybridge.acm.gui.assistants.Matcher;

import javafx.beans.property.SimpleBooleanProperty;

public class MatchableImportableAudio extends MatchableItem<ImportableAudioItem, ImportableFile> {

    SimpleBooleanProperty doUpdate = new SimpleBooleanProperty(false);
    public boolean getDoUpdate() {
        return doUpdate.get();
    }
    public SimpleBooleanProperty doUpdateProperty() {
        return doUpdate;
    }
    void setDoUpdate(boolean doUpdate) {
        this.doUpdate.set(doUpdate);
    }

    public boolean isDoReplaceEditable() {
        return getLeft() != null && getLeft().hasAudioItem()
            && getMatch() != null && getMatch().isMatch();
    }

    public boolean isImportable() {
        return getMatch().isMatch() && (!getLeft().hasAudioItem() || getDoUpdate());
    }

    public String getOperation() {
        String status = "";
        if (getMatch().isMatch()) {
            if (!getLeft().hasAudioItem()) {
                status = "Import";
            } else if (getDoUpdate()) {
                status = "Update";
            } else {
                status = "Keep";
            }
        } else if (getLeft() != null) {
            status = "Missing Audio";
        } else {
            status = "Audio File";
        }
        return status;
    }

    protected MatchableImportableAudio(ImportableAudioItem left, ImportableFile right, MATCH match) {
        super(left, right, match);
    }

    public MatchableImportableAudio(ImportableAudioItem left, ImportableFile right) {
        super(left, right, left==null ? MATCH.RIGHT_ONLY : MATCH.LEFT_ONLY);
    }
    protected MatchableImportableAudio(ImportableAudioItem left,
        ImportableFile right,
        MATCH match,
        int score)
    {
        super(left, right, match, score);
    }

    @Override
    public String description() {
        StringBuilder sb = new StringBuilder();
        switch (this.getMatch()) {
        case LEFT_ONLY:
            sb.append("Audio: ");
            break;
        case RIGHT_ONLY:
            sb.append("File: ");
            break;
        }
        return sb.append(this.toString()).toString();
    }

}
