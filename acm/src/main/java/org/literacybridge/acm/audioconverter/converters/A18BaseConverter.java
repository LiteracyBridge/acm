package org.literacybridge.acm.audioconverter.converters;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.IOUtils;

import static org.literacybridge.acm.Constants.JAVA_TMP_DIR;

public abstract class A18BaseConverter extends BaseAudioConverter {

    // extracted conversion result
    private final Pattern pattern = getPattern();

    public A18BaseConverter(String targetFormatExtension) {
        super(targetFormatExtension);
    }

    protected abstract Pattern getPattern();

    public String getConverterEXEPath() {
        return ACMConfiguration.getInstance().getSoftwareDir().getPath() + "/converters/a18/AudioBatchConverter.exe";
    }

    @Override
    public void validateConverter() throws AudioConverterInitializationException {
        BaseAudioConverter.validateConverterExecutable(getConverterEXEPath(), false,
            "Generalplus Audio Batch Converter Tool");
    }

    protected abstract String getCommand(File audioFile, File targetFile, Map<String, String> parameters);

    protected void postConversion(File outputFile, Map<String, String> parameters) {
        // Default is to do nothing.
    }

    @Override
    public ConversionResult doConvertFile(File sourceFile, File targetDir, File targetFile, Map<String, String> parameters)
        throws ConversionException {
        File tmpSourceFile = null;
        File ultimateDestDir = null;
        boolean OK = false;
        String originalFile = sourceFile.getAbsolutePath();

        try {
            if (!JAVA_TMP_DIR.equals(sourceFile.getParentFile())) {
                tmpSourceFile = new File(JAVA_TMP_DIR, sourceFile.getName());
                try {
                    IOUtils.copy(sourceFile, tmpSourceFile);
                    // the a18 converter stores intermediate files in the source
                    // directory, which is often undesired (e.g. when using dropbox)
                    // copy the source file to a temp directory first
                    sourceFile = tmpSourceFile;
                } catch (IOException e) {
                    // ignore and use original source file
                }
            }

            if (!JAVA_TMP_DIR.equals(targetDir) &&
                targetDir.getAbsolutePath().startsWith(ACMConfiguration.getInstance().getCurrentDB().getProgramHomeDir().getAbsolutePath())) {
                ultimateDestDir = targetDir;
                targetDir = JAVA_TMP_DIR;
            }

            String cmd = getCommand(sourceFile, targetDir, parameters);
            System.out.printf("Convert to '%s' from file:\n%s\n with command:\n%s%n", targetFormatExtension, originalFile, cmd);
            ConversionResult result = new ConversionResult();
            result.outputFile = new File(targetDir, sourceFile.getName() + targetFormatExtension);
            if (ultimateDestDir != null && result.outputFile.exists()) {
                result.outputFile.delete();
            }
            // important! a18 converter prints to stdout, not stderr
            result.response = BaseAudioConverter.executeConversionCommand(cmd, false, sourceFile.getName());

            if (ultimateDestDir != null) {
                if (result.outputFile.renameTo(targetFile)) {
                    result.outputFile = targetFile;
                }
            }
            Matcher matcher = pattern.matcher(result.response);
            if (matcher.matches()) {
                if (!result.response.endsWith("Succeeded")) {
                    throw new ConversionException("Converter: Internal error while converting file: '" + sourceFile.getName() + "'");
                }
            }
            postConversion(result.outputFile, parameters);
            OK = true;
            return result;
        } finally {
            System.out.printf("Conversion %s.%n", OK ? "successful" : "failed");
            if (tmpSourceFile != null && tmpSourceFile.exists()) {
                tmpSourceFile.delete();
            }
            if (ultimateDestDir != null) {
                File tmpDest = new File(JAVA_TMP_DIR, sourceFile.getName() + targetFormatExtension);
                if (tmpDest.exists()) {
                    tmpDest.delete();
                }
            }
        }
    }

    @Override
    public String getShortDescription() {
        return "Convert *.a18 to WAV audio files";
    }

    private static final Set<String> EXTENSIONS = new HashSet<>();

    static {
        EXTENSIONS.add("a18");
    }

    @Override
    public Set<String> getSourceFileExtensions() {
        return EXTENSIONS;
    }
}
