package org.literacybridge.acm.audioconverter.converters;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.repository.AudioItemRepository;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
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

    public ConversionResult normalizeVolume(File audioFile)
            throws ConversionException, IOException {
        // For a18 files, we need to convert to wav first before we can normalize
        // TODO: convert a18 to wav first

        String fileSeparator = System.getProperty("file.separator");
//        String format =  audioFile.getName().("a18") ? "wav" : TARGET_EXTENSION;
        String tempFilePath = AmplioHome.getTempsDir() + System.getProperty("file.separator") + audioFile.getName();

        // TODO: parse loudnorm values as parameters
        // TODO: comment ffmpeg params
        String cmd = String.format("%s -i %s -af loudnorm=I=-12:LRA=7:tp=-2.0:print_format=summary -ar 48k -y %s", getConverterEXEPath(), audioFile.getAbsolutePath(), tempFilePath);
//                "-i input.mp3  output_normalized.wav\n -v 0 -i \"" + sourceFile.getAbsolutePath() + "\"" // input file
//                + " -ab 16k" + " -ar 16000" // 16000 sampling rate
//                + " -ac 1" // 1 channel = mono
//                + " -y" // overwrite output file
//                + " \"" + targetFile.getAbsolutePath() + "\""; // outout file name

        System.out.printf("Convert to 'wav' from file:\n%s\n with command:\n%s%n", audioFile, cmd);

        ConversionResult result = new ConversionResult();
//        result.outputFile = targetFile;
        // important! ffmpeg prints to stderr, not stdout
        result.response = BaseAudioConverter.executeConversionCommand(cmd, true,
                audioFile.getName());

        // Create a temp file to write the output to, then replace the source file with the temp file
        // Replace the source file with the temporary file
//        Files.move()
        Files.move(Paths.get(tempFilePath), Paths.get(audioFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
//        Files.move(tempFilePath, audioFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);


//        Files.write(targetFile.toPath(), result.outputFile.);
//        AudioSystem.write(targetFile, AudioFileFormat.Type.MP, new File(outputFilePath));

        System.out.printf("Conversion to 'wav' result: %s\n%n", result.response);

        return result;
    }

    @Override
    public ConversionResult doConvertFile(File sourceFile, File targetDir,
                                          File targetFile, Map<String, String> parameters)
            throws ConversionException {
        String cmd = getConverterEXEPath() + " -v 0 -i \"" + sourceFile.getAbsolutePath() + "\"" // input file
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
