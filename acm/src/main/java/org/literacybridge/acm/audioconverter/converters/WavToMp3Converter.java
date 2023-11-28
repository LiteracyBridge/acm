package org.literacybridge.acm.audioconverter.converters;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.ExternalCommandRunner;
import org.literacybridge.acm.utils.ExternalCommandRunner.CommandWrapper;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static org.literacybridge.acm.store.MetadataSpecification.LB_VOLUME;
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
            LameWrapper lw = new LameWrapper(sourceFile, targetFile, parameters);
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
        private final Map<String, String> parameters;

        LameWrapper(File sourceFile, File targetFile, Map<String, String> paramters) {
            this.sourceFile = sourceFile;
            this.targetFile = targetFile;
            this.parameters = paramters;
        }

        @Override
        protected File getRunDirectory() {
            return new File(".");
        }

        protected String[] getCommand() {
            // -m m: Force mono, -S: quiet output, -b 16: 16000 BPS, -q 0: best encoding
            List<String> cmds = new ArrayList<String>(Arrays.asList( getConverterEXEPath(), "-m", "m", "-S", "-b", "16", "-q", "0"));

            if( this.parameters != null && this.parameters.containsKey("volume")){
                cmds.add("--gain");
                cmds.add(this.getVolumeDecibels(parameters.get("volume")));
            }

            cmds.add(sourceFile.getAbsolutePath());
            cmds.add( targetFile.getAbsolutePath());
            return cmds.toArray(new String[0]);

//            return new String[] {
//                    getConverterEXEPath(), "-m", "m", "-S", "-b", "16", "-q", "0", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()
//            };
        }

        protected String getVolumeDecibels(String value) {
            float volume = (float) (Float.parseFloat(value) * 0.01);
            float minVolume = 0.0f, maxVolume = 100.0f;

            // Ensure volume is within the valid range
            volume = (int) Math.max(minVolume, Math.min(volume, maxVolume));

            // Normalize volume to a value between 0 and 1
            double normalizedVolume = (double) (volume - minVolume) / (maxVolume - minVolume);

            // Calculate decibels within the specified range
            float minDecibels = -20.0f, maxDecibels = 12.0f;
            double decibels = minDecibels + normalizedVolume * (maxDecibels - minDecibels);

            System.out.printf("Volume level '%f' in decibels '%f'", volume, decibels);
            return String.valueOf(decibels);
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
