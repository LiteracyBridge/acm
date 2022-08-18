package org.literacybridge.acm.audioconverter.api;

public class OggFormat extends AudioConversionFormat {

    public OggFormat(float sampleRate, int sampleSizeInBits, int channels) {
        super("ogg", sampleRate, sampleSizeInBits, channels);
    }

}
