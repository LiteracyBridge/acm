package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;

public class AudioExporter {
    static final String AUDIOITEM_ID_SEPARATOR = "___";

    private static AudioExporter instance;
    public static synchronized AudioExporter getInstance() {
        if (instance == null) {
            instance = new AudioExporter();
        }
        return instance;
    }

    // Map of file extensions to ctor of the class that exports that extension.
    private Map<String, BiFunction<AudioItem, File, AudioFileExporter>> cmap = new HashMap<>();

    // Initializes the exporter.
    private AudioExporter() {
        registerExporter(AudioFormat.A18, A18Exporter::new);
        registerExporter(AudioFormat.MP3, MP3Exporter::new);
        registerExporter(AudioFormat.WAV, WavExporter::new);
        registerExporter(AudioFormat.OGG, OggExporter::new);
    }
    // Given an audio format and the ctor for a class that exports, build the map of export ctors.
    private void registerExporter(AudioFormat format, BiFunction<AudioItem, File, AudioFileExporter> ctor) {
        cmap.put(format.getFileExtension(), ctor);
    }

    /**
     * Given an audio item, a target file, and target format, return an exporter that can perform
     * the desired export. Null if no such exporter.
     * @param item The audio item to be exported.
     * @param targetFile The target file into which the export should take place.
     * @param targetFormat The target format.
     * @return The proper AudioFileExporter, or null if no such exporter exists.
     */
    private AudioFileExporter getExporter(AudioItem item, File targetFile, AudioFormat targetFormat) {
        BiFunction<AudioItem, File, AudioFileExporter> ctor = cmap.get(targetFormat.getFileExtension());
        if (ctor != null) {
            return ctor.apply(item, targetFile);
        }
        return null;
    }

    public void export(Collection<AudioItem> audioItems,
                       File targetDirectory,
                       AudioFormat targetFormat,
                       boolean titleInFilename,
                       boolean idInFilename) throws IOException {
        export(audioItems, targetDirectory, targetFormat, titleInFilename, idInFilename, null);
    }

    public void export(Collection<AudioItem> audioItems,
        File targetDirectory,
        AudioFormat targetFormat,
        boolean titleInFilename,
        boolean idInFilename,
        BiFunction<Integer,Integer,Boolean> onProgress
    ) throws IOException
    {

        try {
            AudioItemRepository repository = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getRepository();

            int count = 0;
            for (AudioItem audioItem : audioItems) {
                // Build the desired file name.
                Metadata metadata = audioItem.getMetadata();
                StringBuilder basename = new StringBuilder();
                if (titleInFilename) {
                    String t = metadata.get(MetadataSpecification.DC_TITLE);
                    String title = metadata.getMetadataValue(MetadataSpecification.DC_TITLE).getValue();
                    basename.append(title);
                    if (idInFilename) {
                        basename.append(AUDIOITEM_ID_SEPARATOR);
                    }
                }
                if (idInFilename) {
                    String id = metadata.getMetadataValue(MetadataSpecification.DC_IDENTIFIER).getValue();
                    basename.append(id);
                }

                // replace invalid file name characters (windows) with an underscore
                String filename = basename.toString().trim().replaceAll("[\\\\/:*?\"<>|']", "_");
                String extension = "." + targetFormat.getFileExtension();
                File targetFile = new File(targetDirectory,filename + extension);
                int counter = 0;
                while (targetFile.exists()) {
                    targetFile = new File(targetDirectory,filename + "-" + ++counter + extension);
                }

                export(audioItem, targetFile, targetFormat);

                if (onProgress != null) {
                    boolean okToContinue=onProgress.apply(++count, audioItems.size());
                    if (!okToContinue) {
                        break;
                    }
                }
            }
        } catch (ConversionException | AudioItemRepository.UnsupportedFormatException e) {
            throw new IOException(e);
        }
    }

    /**
     * Exports a single audio item to a given file.
     * @param audioItem The audio item to be exported.
     * @param targetFile The file to which it should be exported.
     * @param targetFormat The format in which it should be exported.
     * @throws IOException If the source file can't be read, or target file can't be written.
     * @throws ConversionException If the file can't be converted.
     */
    public void export(AudioItem audioItem, File targetFile, AudioFormat targetFormat)
        throws IOException, ConversionException, AudioItemRepository.UnsupportedFormatException {
        AudioFileExporter exporter = getExporter(audioItem, targetFile, targetFormat);
        if (exporter == null) {
            // Count as a file we can't convert.
            return;
        }

        exporter.export();
    }

}
