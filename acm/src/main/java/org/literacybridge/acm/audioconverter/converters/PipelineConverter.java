package org.literacybridge.acm.audioconverter.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.literacybridge.acm.Constants.JAVA_TMP_DIR;

public class PipelineConverter extends BaseAudioConverter {
    private final List<BaseAudioConverter> pipeline;

    public PipelineConverter(BaseAudioConverter... pipeline) {
        this(Arrays.asList(pipeline));
    }
    public PipelineConverter(List<BaseAudioConverter> pipeline) {
        // Ultimate output type is the output type of the final segment of the pipeline.
        super(pipeline.get(pipeline.size()-1).targetFormatExtension);
        this.pipeline = pipeline;
    }

    @Override
    public ConversionResult doConvertFile(File audioFile, File targetDir, File targetFile, Map<String, String> parameters)
        throws ConversionException {

        List<File> tempFiles = new ArrayList<>();
        File input = audioFile;
        File output;
        ConversionResult result = null;
        try {
            for (int i = 0; i < pipeline.size(); i++) {
                if (i>0) {
                    // Any input files after the first are ones we created here.
                    tempFiles.add(input);
                }
                File pipelineDir = (i == pipeline.size() - 1) ? targetDir : JAVA_TMP_DIR;
                BaseAudioConverter converter = pipeline.get(i);
                output = converter.targetFile(audioFile, pipelineDir);
                ConversionResult r = converter.doConvertFile(input, pipelineDir, output, parameters);
                if (result == null) {
                    result = r;
                } else {
                    result.response = result.response + "\n" + r.response;
                }
                if (i==pipeline.size()-1) {
                    result.outputFile = r.outputFile; // Result of last stage is final output.
                } else {
                    input = r.outputFile; // for next stage, if any
                }
            }
            return result;
        } catch(Exception e) {
            throw new ConversionException(("Converter: could not convert "+input.getName()), e);
        } finally {
            tempFiles.forEach(File::delete);
        }
    }

    @Override
    public String getShortDescription() {
        return "Convert any ffmpeg supported audio file to .a18";
    }

    @Override
    public Set<String> getSourceFileExtensions() {
        return this.pipeline.get(0).getSourceFileExtensions();
    }

    @Override
    public void validateConverter() throws AudioConverterInitializationException {
        for (BaseAudioConverter converter : this.pipeline) {
            converter.validateConverter();
        }
    }

}
