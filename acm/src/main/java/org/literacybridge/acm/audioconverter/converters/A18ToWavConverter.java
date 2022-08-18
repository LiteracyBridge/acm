package org.literacybridge.acm.audioconverter.converters;

import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.regex.Pattern;

public class A18ToWavConverter extends A18BaseConverter {
    public A18ToWavConverter() {
        super(".wav");
    }

    @Override
    protected Pattern getPattern() {
        return Pattern.compile("^Decode\\s'([^']*)'.*");
    }

    /**
     * Some computers are converting to .wav files with a bit rate of 8000, despite the
     * -s 16000 argument, making the audio play at half speed. This function detects and
     * corrects that. Yes, it would be better to fix the conversion, but there's no clue
     * to why the converter, with the correct arguments, is producing the wrong results.
     * And no source to debug.
     */
    @Override
    protected void postConversion(File outputFile, Map<String, String> parameters) {
        // See .wav file documentation:
        final int SAMPLE_RATE_OFFSET = 24;
        int intendedRate = 0;
        int filesRate = 0;
        try {
            // If we know what the rate should be...
            if (parameters.containsKey(BaseAudioConverter.SAMPLE_RATE)) {
                intendedRate = Integer.parseInt(parameters.get(BaseAudioConverter.SAMPLE_RATE));
                // ...read the rate from the converted file...
                try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                    raf.seek(SAMPLE_RATE_OFFSET);
                    filesRate = IOUtils.readLittleEndian32(raf);
                    // ...and if they don't match, update the file.
                    if (filesRate != intendedRate) {
                        raf.seek(SAMPLE_RATE_OFFSET);
                        IOUtils.writeLittleEndian32(raf, intendedRate);
                        System.out.printf("Corrected sample rate from %d to %d%n", filesRate,
                                      intendedRate);
                    }
                }
            }
        } catch (Exception e) {
            // Nothing we can really do.
            System.out.printf(
                "Exception validating sample rate: %s\n  Intended rate %d, file's rate %d%n",
                    e.getMessage(), intendedRate, filesRate);
        }
    }

    @Override
    protected String getCommand(File audioFile, File targetFile, Map<String, String> parameters) {
        StringBuilder command = new StringBuilder();
        command.append(getConverterEXEPath());
        command.append(" -d ");

        for (String key : parameters.keySet()) {
            if (key.equals(BaseAudioConverter.SAMPLE_RATE)) {
                command.append(" -s ").append(parameters.get(key));
            }
        }
        command.append(" -o \"").append(targetFile.getAbsolutePath()).append("\"");
        command.append(" \"").append(audioFile.getAbsolutePath()).append("\"");

        return command.toString();
    }
}
