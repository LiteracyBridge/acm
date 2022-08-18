package org.literacybridge.acm.audioconverter.api;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.audioconverter.converters.A18ToAnyConverter;
import org.literacybridge.acm.audioconverter.converters.A18ToWavConverter;
import org.literacybridge.acm.audioconverter.converters.AnyToA18Converter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.audioconverter.converters.FFMpegConverter;
import org.literacybridge.acm.audioconverter.converters.PipelineConverter;
import org.literacybridge.acm.audioconverter.converters.WavToMp3Converter;

import java.io.File;
import java.util.Map;

public class ExternalConverter {
    private final File sourceFile;
    private final AudioConversionFormat targetFormat;
    private File targetFile;
    private File targetDirectory;
    private Boolean overwriteExisting;

    /**
     * Creates an object to manage the conversion of an audio file from one format to another.
     *
     * @param sourceFile   to be converted.
     * @param targetFormat of the converted file.
     */
    public ExternalConverter(File sourceFile, AudioConversionFormat targetFormat) {
        this.sourceFile = sourceFile;
        this.targetFormat = targetFormat;
    }

    /**
     * Provides an explicit file name, and possibly an explicit directory, for the output file. If no
     * file is provided, the name will be the source file's name with the correct extension. Note that
     * an explicit directory will override any directory of this target file.
     *
     * @param targetFile to be created.
     * @return this, for chaining.
     */
    public ExternalConverter toFile(File targetFile) {
        this.targetFile = targetFile;
        return this;
    }

    /**
     * Provides the directory in which the file should be created. If not provided the directory of
     * the target file will be used; if no such directory, the directory of the source file will
     * be used.
     *
     * @param targetDirectory to receive the converted file.
     * @return this, for chaining.
     */
    public ExternalConverter inDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
        return this;
    }

    /**
     * Instructs the conversion whether to overwrite any existing file.
     *
     * @param overwriteExisting True if any existing file should be overwritten.
     * @return this, for chaining.
     */
    public ExternalConverter overwritingExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
        return this;
    }

    /**
     * Instructs the conversion to not overwrite any existing output file.
     *
     * @return this, for chaining.
     */
    public ExternalConverter noOverwrite() {
        this.overwriteExisting = false;
        return this;
    }

    /**
     * Performs the conversion based on the provided parameters.
     *
     * @return A string describing the results of the conversion attempt.
     * @throws ConversionException if the conversion fails.
     */
    public String go() throws ConversionException {
        if (sourceFile == null || targetFormat == null) {
            throw new IllegalStateException("Source file and target format are required for conversion");
        }
        if (targetDirectory == null) {
            // No target directory was specified. If a given target file has an explicit parent, use that, otherwise use
            // the source file's directory.
            targetDirectory = (targetFile == null || targetFile.getParentFile() == null)
                              ? sourceFile.getParentFile()
                              : targetFile.getParentFile();
        }
        if (targetFile == null) {
            // No target file given. Base name on the source file. Explicit output directory, or implicitly input file's directory (from above).
            targetFile = BaseAudioConverter.targetFile(sourceFile, targetDirectory, targetFormat.getFileExtension());
        } else {
            // Target file given; enforce the extension. Explicit output dir, else target file's dir, else source file's dir (from above).
            targetFile = BaseAudioConverter.targetFile(targetFile, targetDirectory, targetFormat.getFileExtension());
        }
        if (overwriteExisting == null) {
            overwriteExisting = true;
        }

        Map<String, String> parameters = targetFormat.getParameters();
        BaseAudioConverter converter = getConverter();

        return converter.convertFile(sourceFile, targetFile, overwriteExisting, parameters);
    }


    /**
     * Gets a File named like the source but with the correct extension for the audio format, and in the target
     * directory.
     *
     * @param sourceFile   A file that provides the base name.
     * @param targetDir    The desired directory for the file.
     * @param targetFormat The format for which the file will be named (.mp3, .a18, etc.)
     * @return The file.
     */
    public static File targetFile(File sourceFile, File targetDir, AudioConversionFormat targetFormat) {
        return BaseAudioConverter.targetFile(sourceFile, targetDir, targetFormat.getFileExtension());
    }

    /**
     * Gets the appropriate converter based on the source file and target format.
     *
     * @return an audio converter. Extends BaseAudioConverter.
     */
    private BaseAudioConverter getConverter() {
        BaseAudioConverter converter;
        if (targetFormat.getFileExtension().equalsIgnoreCase("A18")) {
            converter = new AnyToA18Converter();
        } else if (targetFormat.getFileExtension().equalsIgnoreCase("mp3")) {
            if (FilenameUtils.getExtension(sourceFile.getName()).equalsIgnoreCase("a18")) {
                converter = new PipelineConverter(new A18ToWavConverter(), new WavToMp3Converter());
            } else if (FilenameUtils.getExtension(sourceFile.getName()).equalsIgnoreCase("wav")) {
                converter = new WavToMp3Converter();
            } else {
                converter = new PipelineConverter(new FFMpegConverter(), new WavToMp3Converter());
            }
        } else {
            if (FilenameUtils.getExtension(sourceFile.getName()).equalsIgnoreCase("a18")) {
                if (targetFormat.getFileExtension().equalsIgnoreCase("WAV")) {
                    converter = new A18ToWavConverter();
                } else {
                    converter = new A18ToAnyConverter(targetFormat.getFileExtension());
                }
            } else {
                converter = new FFMpegConverter();
            }
        }
        return converter;
    }
}
