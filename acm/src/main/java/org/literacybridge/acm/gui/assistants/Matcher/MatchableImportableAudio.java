package org.literacybridge.acm.gui.assistants.Matcher;

public class MatchableImportableAudio extends MatchableItem<ImportableAudioItem, ImportableFile> {

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
