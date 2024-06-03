package org.literacybridge.acm.audioconverter.converters;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.config.PathsProvider;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFMpegConverter extends BaseAudioConverter {
    final static String TARGET_EXTENSION = ".wav";

    public FFMpegConverter() {
        super(TARGET_EXTENSION);
    }

    /**
     * Normalises the volume of the given audio item using ffpemg's loudnorm filter. The normalised audio is converted
     * to all supported formats and replaces the original audio item (cached audio file is used in sandbox mode)
     *
     * @param audioItem   AudioItem to be normalized
     * @param volumeLevel Desired volume level, in the range of 0-100. Value is translated to a LUFS value in the range of -23 to -1.
     * @throws ConversionException
     * @throws IOException
     * @throws AudioItemRepository.UnsupportedFormatException
     */
    public void normalizeVolume(AudioItem audioItem, String volumeLevel)
            throws ConversionException, IOException, AudioItemRepository.UnsupportedFormatException {
        File sourceFile = null;

        // If in sandbox mode, use the cached file, otherwise use the uncached file
        if (ACMConfiguration.getInstance().isForceSandbox()) {
            sourceFile = ACMConfiguration.getInstance().getCurrentDB().getRepository().getAudioFile(audioItem, AudioItemRepository.AudioFormat.WAV);
        } else {
            sourceFile = ACMConfiguration.getInstance().getCurrentDB().getRepository().getUncachedAudioFile(audioItem, AudioItemRepository.AudioFormat.WAV);
        }

        // Create a temp file to write the output to, then replace the source file with the temp file
        String tempFilePath = AmplioHome.getTempsDir() + File.separator + sourceFile.getName();
        File lock = PathsProvider.getLocalLockFile(audioItem.getId(), true); // Lock the audio item to prevent concurrent access

        double lufsValue = convertVolumeToLUFS(Double.parseDouble(volumeLevel));
        String cmd = getConverterEXEPath() // ffmpeg.exe
                + " -i " + sourceFile.getAbsolutePath() // input file
                + " -af loudnorm=I=" + lufsValue + ":LRA=7:tp=-2.0:print_format=summary" // loudnorm filter, Loudness Range target of 7 LU, True Peak target of -2.0 dBTP
                + " -ar 16k" // 48000 sampling rate
                + " -ac 1" // 1 channel = mono
                + " -y" // overwrite output file
                + " " + tempFilePath; // outout file name

        System.out.printf("Convert to 'wav' from file:\n%s\n with command:\n%s%n", sourceFile, cmd);

        ConversionResult result = new ConversionResult();
        // important! ffmpeg prints to stderr, not stdout
        result.response = BaseAudioConverter.executeConversionCommand(cmd, true,
                sourceFile.getName());

        // Replace the source file with the temporary file
        Files.move(Paths.get(tempFilePath), Paths.get(sourceFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);

        // An audio item can be in multiple formats, we have to convert it to all supported formats, starting with WAV.
        // Since getUncachedAudioFile/getAudioFile do implicit conversion, so we simply call getAudioFile with the format we want.
        if (ACMConfiguration.getInstance().isForceSandbox()) {
            ACMConfiguration.getInstance().getCurrentDB().getRepository().getAudioFile(audioItem, AudioItemRepository.AudioFormat.MP3);
            ACMConfiguration.getInstance().getCurrentDB().getRepository().getAudioFile(audioItem, AudioItemRepository.AudioFormat.A18);
        } else {
            ACMConfiguration.getInstance().getCurrentDB().getRepository().getUncachedAudioFile(audioItem, AudioItemRepository.AudioFormat.MP3);
            ACMConfiguration.getInstance().getCurrentDB().getRepository().getUncachedAudioFile(audioItem, AudioItemRepository.AudioFormat.A18);

        }
        System.out.printf("Conversion to 'wav' result: %s\n%n", result.response);

        lock.delete(); // No need to keep the lock file after the conversion is done
    }

    /**
     * Converts a volume level to Loudness Units Full Scale (LUFS) using a linear mapping.
     * <p>
     * The function assumes a linear mapping from a volume level in the range of 0-100 to LUFS values
     * in the range of -23 to -1. The conversion is performed using the formula: LUFS = slope * volume + intercept.
     *
     * @param volumeLevel The input volume level to be converted to LUFS. Should be in the range of 0 to 100 (clamped to this range if not).
     * @return The corresponding LUFS value after the linear conversion. The result is clamped to the range of -23 to -1.
     */
    private double convertVolumeToLUFS(double volumeLevel) {
        // Assuming a linear mapping from 0-100 to LUFS values in the range of -23 to -1
        double slope = 0.23;    // 0.21 LUFS per volume unit
        double intercept = -23.0; // -24.0 LUFS is the lowest possible value

        // Perform the linear conversion
        volumeLevel = Math.min(Math.max(volumeLevel, 0.0), 100.0); // Ensure that the volume level is within the desired range (0-100)
        double lufsValue = slope * volumeLevel + intercept;

        // Ensure that the result is within the desired LUFS range (-23 to -1)
        return Math.min(Math.max(lufsValue, -23.0), -1.0);
    }


    @Override
    public ConversionResult doConvertFile(File sourceFile, File targetDir,
                                          File targetFile, Map<String, String> parameters)
            throws ConversionException {
        String cmd = getConverterEXEPath() + " -i \"" + sourceFile.getAbsolutePath() + "\"" // input file
                + " -ab 16k" + " -ar 16000" // 16000 sampling rate
                + " -ac 1" // 1 channel = mono
                + " -y" // overwrite output file
                + " \"" + targetFile.getAbsolutePath() + "\""; // outout file name

        System.out.printf("Convert to 'wav' from file:\n%s\n with command:\n%s%n", sourceFile, cmd);

        ConversionResult result = new ConversionResult();
        result.outputFile = targetFile;
        // important! ffmpeg prints to stderr, not stdout
        result.response = BaseAudioConverter.executeConversionCommand(cmd, true,
                sourceFile.getName());

        System.out.printf("Conversion to 'wav' result: %s\n%n", result.response);

        return result;
    }

    @Override
    public void validateConverter() throws AudioConverterInitializationException {
        // Already succeeded?
        if (EXTENSIONS != null) return;
        // important! ffmpeg prints to stderr, not stdout
        BaseAudioConverter.validateConverterExecutable(getConverterEXEPath(), true, "FFmpeg version");
        Set<String> extensions = new HashSet<>();
        boolean success = false;
        try {
            File formatsFile = new File(ACMConfiguration.getInstance().getSoftwareDir(),
                    "/converters/ffmpeg/formats.txt");

            FileInputStream fis = new FileInputStream(formatsFile);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            // parse 'ffmpeg -formats output', it starts like this (data starts in first column):
            //File formats:
            //  E 3g2 3GP2 format
            //  E 3gp 3GP format
            // D 4xm 4X Technologies format

            String line = br.readLine();
            // first line should be: "File Formats:"
            if (!line.trim().equals("File formats:")) {
                return;
            }

            Pattern pattern = Pattern.compile("^(.)(.)(.)\\s*(\\S*).*$");

            // pick every file extensions that has a 'D', which indicates decoding ability
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    break;
                }
                if (matcher.group(2).equals("D")) {
                    String extension = matcher.group(4).toLowerCase();
                    StringTokenizer tokenizer = new StringTokenizer(extension, ",");
                    while (tokenizer.hasMoreTokens()) {
                        extensions.add(tokenizer.nextToken());
                    }
                }
            }

            success = true;
            EXTENSIONS = extensions;
        } catch (Exception e) {
            // nothing to do - finally block will throw exception
        } finally {
            if (!success) {
                throw new AudioConverterInitializationException("Converter executable could not be executed.");
            }
        }

    }

    public String getConverterEXEPath() {
        return ACMConfiguration.getInstance().getSoftwareDir().getPath() + "/converters/ffmpeg/ffmpeg.exe";
    }

    @Override
    public String getShortDescription() {
        return "Convert any audio file to .wav";
    }

    private static Set<String> EXTENSIONS = null;

    @Override
    public Set<String> getSourceFileExtensions() {
        return EXTENSIONS;
    }
}
