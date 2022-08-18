package org.literacybridge.acm.audioconverter.converters;

import org.literacybridge.acm.utils.OsUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.literacybridge.acm.Constants.JAVA_TMP_DIR;

public class A18ToAnyConverter extends BaseAudioConverter {
    private final FFMpegConverter ffmpegConverter = new FFMpegConverter();
    private final A18ToWavConverter a18ToWavConverter = new A18ToWavConverter();

    public A18ToAnyConverter(String extension) {
        super(extension);
    }

    @Override
    public ConversionResult doConvertFile(File inputFile, File targetDir,
                                          File targetFile, Map<String, String> parameters)
            throws ConversionException {

        if (!OsUtils.WINDOWS) {
            throw new ConversionException("A18 conversion is only supported on Windows.");
        }

        File tmp = null;
        try {
            ConversionResult r1 = a18ToWavConverter.doConvertFile(inputFile, JAVA_TMP_DIR,
                    targetFile, parameters);
            tmp = r1.outputFile;
            if (!targetDir.exists()) {
                if (!targetDir.mkdirs())
                    throw new ConversionException("Could not create output directory: "+targetDir.getAbsolutePath());
            }
            ConversionResult r2 = ffmpegConverter.doConvertFile(r1.outputFile,
                    targetDir, targetFile, parameters);

            r2.response = r1.response + "\n" + r2.response;
            return r2;
        } finally {
            if (tmp != null) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }

    @Override
    public String getShortDescription() {
        return "Convert *.a18 audio file to any ffmpeg supported format.";
    }

    private static final Set<String> EXTENSIONS = new HashSet<>();

    static {
        EXTENSIONS.add("a18");
    }

    @Override
    public Set<String> getSourceFileExtensions() {
        return EXTENSIONS;
    }

    @Override
    public void validateConverter() throws AudioConverterInitializationException {
        ffmpegConverter.validateConverter();
        a18ToWavConverter.validateConverter();
    }

}
