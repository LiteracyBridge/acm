package org.literacybridge.acm.audioconverter.api;

// import javax.sound.sampled.AudioFormat;

import org.literacybridge.acm.audioconverter.converters.AnyToA18Converter;

import java.util.Map;

public class A18Format extends AudioConversionFormat {

    public final String usedAlgo;
    public final String usedHeader;

    public enum AlgorithmList {
        A1600, A1800, A3600
    }

    public enum useHeaderChoice {
        Yes, No
    }

    public A18Format(float sampleRate, int sampleSizeInBits, int channels) {
        super("a18", sampleRate, sampleSizeInBits, channels);

        this.usedAlgo = String.valueOf(A18Format.AlgorithmList.A1800);
        this.usedHeader = String.valueOf(A18Format.useHeaderChoice.No);
    }

    public Map<String, String> getParameters() {
        Map<String, String> parameters = super.getParameters();

        parameters.put(AnyToA18Converter.USE_HEADER, usedHeader);
        parameters.put(AnyToA18Converter.ALGORITHM, usedAlgo);
        return parameters;
    }

}
