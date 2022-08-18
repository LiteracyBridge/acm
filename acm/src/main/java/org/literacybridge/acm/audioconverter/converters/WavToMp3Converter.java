package org.literacybridge.acm.audioconverter.converters;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.ExternalCommandRunner;
import org.literacybridge.acm.utils.ExternalCommandRunner.CommandWrapper;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static org.literacybridge.acm.utils.ExternalCommandRunner.LineProcessorResult.HANDLED;

public class WavToMp3Converter extends BaseAudioConverter {
    public WavToMp3Converter() {
        super(".mp3");

    }

    public static String getConverterEXEPath() {
        return ACMConfiguration.getInstance().getSoftwareDir().getPath() + "/converters/lame/lame.exe";
    }

    @Override
    public ConversionResult doConvertFile(File sourceFile, File targetDir, File targetFile, Map<String, String> parameters) {
        boolean OK = false;
        try {
            LameWrapper lw = new LameWrapper(sourceFile, targetFile);
            lw.go();
            ConversionResult result = new ConversionResult();
            if (lw.didEncode() && targetFile.exists()) {
                result.outputFile = targetFile;
                result.response = lw.outputLine();
            }
            OK = true;
            return result;
        } finally {
            System.out.printf("Conversion %s.\n", OK?"successful":"failed");
        }
    }

    @Override
    public void validateConverter() throws AudioConverterInitializationException {
        BaseAudioConverter.validateConverterExecutable(getConverterEXEPath(), false, "LAME");
    }

    private static final Set<String> EXTENSIONS = new HashSet<>();
    static {
        EXTENSIONS.add("wav");
    }
    @Override
    public Set<String> getSourceFileExtensions() {
        return EXTENSIONS;
    }

    @Override
    public String getShortDescription() {
        return "Convert .wav file to .mp3 file.";
    }

    private static class LameWrapper extends CommandWrapper {
        private final Pattern ENCODING = Pattern.compile("^(Encoding as).*");

        private final File sourceFile;
        private final File targetFile;

        LameWrapper(File sourceFile, File targetFile) {
            this.sourceFile = sourceFile;
            this.targetFile = targetFile;
        }

        @Override
        protected File getRunDirectory() {
            return new File(".");
        }

        protected String[] getCommand() {
            return new String[] {
                    // -m m: Force mono, -S: quiet output, -b 16: 16000 BPS, -q 0: best encoding
                    getConverterEXEPath(), "-m", "m", "-S", "-b", "16", "-q", "0", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()
            };
        }

        @Override
        protected List<ExternalCommandRunner.LineHandler> getLineHandlers() {
            return Collections.singletonList(
                    new ExternalCommandRunner.LineHandler(ENCODING, this::gotLine)
            );
        }

        private String outputLine;
        private boolean didEncode = false;
        public boolean didEncode() { return didEncode; }
        public String outputLine() { return outputLine; }
        private ExternalCommandRunner.LineProcessorResult gotLine(java.io.Writer writer, java.util.regex.Matcher matcher) {
            outputLine = matcher.group(0);
            didEncode = true;
            return HANDLED;
        }

    }

}
