package org.literacybridge.acm.audioconverter.api;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.audioconverter.converters.*;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;

public class ExternalConverter {

    public String convert(File sourceFile, File targetDir, File tmpDir, AudioConversionFormat targetFormat, boolean overwrite)
            throws ConversionException {
        String result = null;
        Map<String, String> parameters = null;

        if (targetFormat.getFileEnding().equalsIgnoreCase("A18")) {
            parameters = getParameters(targetFormat);
            AnyToA18Converter converter = new AnyToA18Converter();
            result = converter.convertFile(sourceFile, targetDir, tmpDir, overwrite, parameters);
        } else {
            if (FilenameUtils.getExtension(sourceFile.getName()).equalsIgnoreCase("a18")) {
                parameters = getParameters(targetFormat);
                if (targetFormat.getFileEnding().equalsIgnoreCase("WAV")) {
                    A18ToWavConverter converter = new A18ToWavConverter();
                    result = converter.convertFile(sourceFile, targetDir, tmpDir, overwrite, parameters);
                } else {
                    A18ToAnyConverter converter = new A18ToAnyConverter(targetFormat.getFileExtension());
                    result = converter.convertFile(sourceFile, targetDir, tmpDir, overwrite, parameters);
                }
            } else {
                parameters = getParameters(targetFormat);
                FFMpegConverter converter = new FFMpegConverter();
                result = converter.convertFile(sourceFile, targetDir, tmpDir, overwrite, parameters, "." + targetFormat.getFileEnding());
            }
        }
        return result;
    }

    private Map<String, String> getParameters(AudioConversionFormat targetFormat) {

        Map<String, String> parameters = new LinkedHashMap<String, String>();

        parameters.put(BaseAudioConverter.BIT_RATE,
                String.valueOf(targetFormat.getBitRateString()));
        parameters.put(BaseAudioConverter.SAMPLE_RATE,
                String.valueOf(targetFormat.getSampleRateString()));

        if (targetFormat.getFileEnding().equals("A18")) {

            parameters.put(AnyToA18Converter.USE_HEADER,
                    ((A18Format) targetFormat).usedHeader);
            parameters.put(AnyToA18Converter.ALGORITHM,
                    ((A18Format) targetFormat).usedAlgo);
        }
        return parameters;
    }

}
