package org.literacybridge.acm.repository;

import org.literacybridge.acm.audioconverter.api.A18Format;
import org.literacybridge.acm.audioconverter.api.AudioConversionFormat;
import org.literacybridge.acm.audioconverter.api.MP3Format;
import org.literacybridge.acm.audioconverter.api.OggFormat;
import org.literacybridge.acm.audioconverter.api.WAVFormat;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.io.IOException;

public interface AudioItemRepository {
    void addNewAudioItemFromFile(AudioItem audioItem, File externalFile)
            throws UnsupportedFormatException, IOException, DuplicateItemException, BaseAudioConverter.ConversionException;

    void updateExistingAudioItemFromFile(AudioItem audioItem, File externalFile)
                    throws BaseAudioConverter.ConversionException, IOException, UnsupportedFormatException;

    void deleteAudioItem(AudioItem audioItem);

    /**
     * Finds the audio file with the given format.
     * @param audioItem for which the file is desired.
     * @param format audio format for which the file is desired.
     * @return the File or null if no file.
     */
    File findAudioFileWithFormat(AudioItem audioItem, AudioFormat format);

    File getAudioFile(AudioItem audioItem, AudioFormat format) throws
                                                               IOException,
                                                               BaseAudioConverter.ConversionException,
                                                               UnsupportedFormatException;

    String getAudioFilename(AudioItem audioItem, AudioFormat audioFormat);

    /**
     * Exports the audio item in the given format. Creates the format if necessary. The file will contain metadata.
     * @param audioItem The audio item to be exported.
     * @param targetFile The file to which to export. NOTE: Regardless of the extension of this file, the native
     *                   extension of the format is used.
     * @param targetFormat The format in which to export.
     * @return Returns the actual file exported.
     * @throws IOException If a file can't be read or written.
     * @throws BaseAudioConverter.ConversionException If an existing file can't be converted to the desired format.
     */
    File exportAudioFileWithFormat(AudioItem audioItem, File targetFile, AudioFormat targetFormat) throws
                                                                                                   IOException,
                                                                                                   BaseAudioConverter.ConversionException,
                                                                                                   UnsupportedFormatException;

    void exportFileWithFormat(File sourceFile, File targetFile, AudioFormat targetFormat) throws
                                                                                                 BaseAudioConverter.ConversionException,
                                                                                                 IOException;
    /**
     * Exports the system prompt for the given language, in the given format. Creates the format if necessary. The
     * file may or may not contain metadata.
     * @param promptId The system prompt file name, like "0" or "21".
     * @param targetFile The file to which to export. NOTE: Regardless of any extension of this file, the native
     *                   extension of the format is actually used.
     * @param language The language for which the prompt is to be exported.
     * @param targetFormat The format in which to export.
     * @throws IOException If a file can't be read or written.
     * @throws BaseAudioConverter.ConversionException If an existing file can't be converted to the desired format.
     */
    void exportSystemPromptFileWithFormat(String promptId, File targetFile, String language, AudioFormat targetFormat) throws IOException, BaseAudioConverter.ConversionException;

    /**
     * Exports the pair of prompts for the given playlist category or name.
     * @param packageName This is needed to find a "prompts.txt" file for ACM database located prompts.
     * @param prompt The prompt id, like "2-0" or "LB-2_uzz71upxwm_11e"
     * @param targetFile The basic name of the file to be exported, like .../2-0.a18 (or .mp3, etc). Will also
     *                   implicitly create the .../i2-0.a18 file.
     * @param language The language being exported. Used to find "2-0" style files in the TB_Options.
     * @param targetFormat The desired audio format, like A18 or MP3.
     * @throws IOException If a file can't be read or written.
     * @throws BaseAudioConverter.ConversionException If an existing file can't be converted to the desired format.
     */
    void exportCategoryPromptPairWithFormat(String packageName, String prompt, File targetFile, String language, AudioFormat targetFormat) throws
                                                                                                                                           IOException,
                                                                                                                                           BaseAudioConverter.ConversionException,
                                                                                                                                           UnsupportedFormatException;

    /**
     * Exports the greeting for the given recipient.
     * @param sourceDir Where the greeting should be found.
     * @param targetFile The basic name of the file to be exported, like .../1d3d00aa4cdf2368.a18 (or .mp3, etc).
     *                   NOTE: Regardless of the extension of this file, the native extension of the format is used.
     * @param targetFormat The desired audio format, like A18 or MP3.
     */
    void exportGreetingWithFormat(File sourceDir, File targetFile, AudioFormat targetFormat) throws
                                                                                                                          BaseAudioConverter.ConversionException,
                                                                                                                          IOException;

    /**
     * An enum of all supported audio formats.
     */
    enum AudioFormat {
        A18(new A18Format(16000, 128, 1)),
        WAV(new WAVFormat(16000, 128, 1)),
        MP3(new MP3Format(16000, 128, 1)),
        OGG(new OggFormat(16000, 128, 1)),
        WMA(new AudioConversionFormat("wma", 16000, 128, 1) {}),
        AAC(new AudioConversionFormat("aac", 1600, 128, 1) {}),
        M4A(new AudioConversionFormat("m4a", 1600, 128, 1) {});

        private static final AudioFormat[] EXPORTABLES = {A18, WAV, MP3, OGG};

        private final AudioConversionFormat audioConversionFormat;

        AudioFormat(AudioConversionFormat audioConversionFormat) {
            this.audioConversionFormat = audioConversionFormat;
        }

        public String getFileExtension() {
            return audioConversionFormat.getFileExtension();
        }

        public AudioConversionFormat getAudioConversionFormat() {
            return audioConversionFormat;
        }

        public static AudioFormat[] exportables() {
            return EXPORTABLES;
        }
    }

    interface ImportFunc {
        void accept(AudioItem item, File file)
            throws UnsupportedFormatException, IOException, DuplicateItemException, BaseAudioConverter.ConversionException;
    }

    class AudioItemRepositoryException extends Exception {
        public AudioItemRepositoryException(String msg) {
            super(msg);
        }
    }

    final class MissingItemException extends AudioItemRepositoryException {
        public MissingItemException(String msg) {
            super(msg);
        }
    }

    final class DuplicateItemException extends AudioItemRepositoryException {
        public DuplicateItemException(String msg) {
            super(msg);
        }
    }

    final class UnsupportedFormatException extends AudioItemRepositoryException {
        UnsupportedFormatException(String msg) {
            super(msg);
        }
    }
}
