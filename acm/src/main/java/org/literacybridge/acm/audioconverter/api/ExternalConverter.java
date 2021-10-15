package org.literacybridge.acm.audioconverter.api;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.audioconverter.converters.*;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;

public class ExternalConverter {

    /**
     * Gets a File named like the source but with the correct extension for the audio format, and in the target
     * directory.
     * @param sourceFile A file that provides the base name.
     * @param targetDir The desired directory for the file.
     * @param targetFormat The format for which the file will be named (.mp3, .a18, etc.)
     * @return The file.
     */
    public static File targetFile(File sourceFile, File targetDir, AudioConversionFormat targetFormat) {
        return BaseAudioConverter.targetFile(sourceFile, targetDir, targetFormat.getFileExtension());
    }

    public void convert(File sourceFile, File destFile, AudioConversionFormat targetFormat, boolean overwrite)
            throws ConversionException {
        File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
        convert(sourceFile, destFile, TMP_DIR, targetFormat, overwrite);
    }

    public String convert(File sourceFile, File targetFile, File tmpDir,
                AudioConversionFormat targetFormat, boolean overwrite)
            throws ConversionException {
        String result = null;
        Map<String, String> parameters = getParameters(targetFormat);
        BaseAudioConverter converter = null;

        if (targetFormat.getFileEnding().equalsIgnoreCase("A18")) {
             converter = new AnyToA18Converter();
        } else {
            if (FilenameUtils.getExtension(sourceFile.getName()).equalsIgnoreCase("a18")) {
                if (targetFormat.getFileEnding().equalsIgnoreCase("WAV")) {
                     converter = new A18ToWavConverter();
                } else {
                     converter = new A18ToAnyConverter(targetFormat.getFileExtension());
                }
            } else {
                 converter = new FFMpegConverter();
            }
        }
        result = converter.convertFile(sourceFile, targetFile, tmpDir, overwrite, parameters);

        return result;
    }

    private static Map<String, String> getParameters(AudioConversionFormat targetFormat) {

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
