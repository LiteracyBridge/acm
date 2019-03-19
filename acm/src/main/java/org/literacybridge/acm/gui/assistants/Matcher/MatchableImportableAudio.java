package org.literacybridge.acm.gui.assistants.Matcher;

import javafx.beans.property.SimpleBooleanProperty;

public class MatchableImportableAudio extends MatchableItem<ImportableAudioItem, ImportableFile> {

    SimpleBooleanProperty doUpdate = new SimpleBooleanProperty(true);
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
            if (getLeft().hasAudioItem()) {
                status = "Imported";
            } else {
                status = "Missing";
            }
        } else {
            status = "Extra File";
        }
        return status;
    }

    /**
     * If there's a match, and there's an existing AudioItem, and user is OK with it, then the
     * import operation is an "update". Otherwise, it is not an update.
     * @return true if the import operation is an update.
     */
    public boolean isUpdate() {
        return getMatch().isMatch() && getLeft().hasAudioItem() && getDoUpdate();
    }

    protected MatchableImportableAudio(ImportableAudioItem left, ImportableFile right, MATCH match) {
        super(left, right, match);
    }

    public MatchableImportableAudio(ImportableAudioItem left, ImportableFile right) {
        super(left, right);
    }
    protected MatchableImportableAudio(ImportableAudioItem left,
        ImportableFile right,
        MATCH match,
        int score)
    {
        super(left, right, match, score);
    }


    public MatchableImportableAudio[] disassociate() {
        MatchableImportableAudio[] result = new MatchableImportableAudio[2];
        result[0] = new MatchableImportableAudio(getLeft(), null);
        result[0].setDoUpdate(getDoUpdate());
        result[1] = new MatchableImportableAudio(null, getRight());
        return result;
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
