package org.literacybridge.acm.gui.messages;

import org.literacybridge.acm.store.AudioItem;

public class PlayAudioItemMessage extends Message {

    private AudioItem audioItem;

    public PlayAudioItemMessage(AudioItem audioItem) {
        super();
        this.audioItem = audioItem;
    }

    public AudioItem getAudioItem() {
        return audioItem;
    }
}
