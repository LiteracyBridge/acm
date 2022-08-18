package org.literacybridge.acm.audioconverter.api;

public class MP3Format extends AudioConversionFormat {

    public MP3Format(float sampleRate, int sampleSizeInBits, int channels) {
        super("mp3", sampleRate, sampleSizeInBits, channels);
    }

}
