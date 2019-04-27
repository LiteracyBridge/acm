package org.literacybridge.acm.gui.assistants.Matcher;

public class MatchableAudio extends MatchableItem<AudioTarget, ImportableFile> {

    /**
     * If there's a match, and there's an existing AudioItem, and user is OK with it, then the
     * import operation is an "update". Otherwise, it is not an update.
     * @return true if the import operation is an update.
     */
//    public boolean isUpdate() {
//        return getMatch().isMatch() && getLeft().hasAudioItem() && getLeft().isReplaceOk();
//    }
    private MatchableAudio(AudioTarget left, ImportableFile right, MATCH match) {
        super(left, right, match);
    }

    public MatchableAudio(AudioTarget left, ImportableFile right) {
        super(left, right);
    }

    @Override
    public MatchableAudio disassociate() {
        MatchableAudio disassociated = new MatchableAudio(null, getRight(), MATCH.RIGHT_ONLY);
        setRight(null);
        setMatch(MATCH.LEFT_ONLY);
        setScore(0);
        return disassociated;
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
