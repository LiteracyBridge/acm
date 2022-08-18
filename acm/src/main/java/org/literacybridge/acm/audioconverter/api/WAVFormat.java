package org.literacybridge.acm.audioconverter.api;

public class WAVFormat extends AudioConversionFormat {

    public WAVFormat(float sampleRate, int sampleSizeInBits, int channels) {
        super("wav", sampleRate, sampleSizeInBits, channels);
    }
}
