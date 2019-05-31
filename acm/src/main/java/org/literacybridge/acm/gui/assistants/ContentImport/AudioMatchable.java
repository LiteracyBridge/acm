package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;

public class AudioMatchable extends MatchableItem<AudioTarget, ImportableFile> {

    private AudioMatchable(AudioTarget left, ImportableFile right, MATCH match) {
        super(left, right, match);
    }

    public AudioMatchable(AudioTarget left, ImportableFile right) {
        super(left, right);
    }

    @Override
    public AudioMatchable disassociate() {
        AudioMatchable disassociated = new AudioMatchable(null, getRight(), MATCH.RIGHT_ONLY);
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
            sb.append("Title: ");
            break;
        case RIGHT_ONLY:
            sb.append("Audio File: ");
            break;
        }
        return sb.append(this.toString()).toString();
    }

}
