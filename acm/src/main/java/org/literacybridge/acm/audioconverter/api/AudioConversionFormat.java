package org.literacybridge.acm.audioconverter.api;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;

import javax.sound.sampled.AudioFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AudioConversionFormat extends AudioFormat {
    private final String fileExtension;

    public AudioConversionFormat(String fileExtension, float sampleRate, int sampleSizeInBits, int channels) {
        super(sampleRate, sampleSizeInBits, channels, false, false);
        if (fileExtension.charAt(0) == '.') {
            throw new IllegalArgumentException("Extensions should not include leading dot.");
        }
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getSampleRateString() {
        return Integer.toString((int) sampleRate);
    }

    public Map<String, String> getParameters() {
        Map<String, String> parameters = new LinkedHashMap<>();

        parameters.put(BaseAudioConverter.SAMPLE_RATE, getSampleRateString());
        return parameters;
    }

}
