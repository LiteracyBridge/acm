package org.literacybridge.acm.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.audioconverter.api.A18Format;
import org.literacybridge.acm.audioconverter.api.A18Format.AlgorithmList;
import org.literacybridge.acm.audioconverter.api.A18Format.useHeaderChoice;
import org.literacybridge.acm.audioconverter.api.AudioConversionFormat;
import org.literacybridge.acm.audioconverter.api.ExternalConverter;
import org.literacybridge.acm.audioconverter.api.MP3Format;
import org.literacybridge.acm.audioconverter.api.WAVFormat;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.LBMetadataSerializer;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.OSChecker;

import com.google.common.collect.Maps;

/**
 * This repository manages all audio files associated with the audio items
 * that the ACM stores. Metadata of the audio items is stored separately,
 * not in this repository.
 */
public abstract class AudioItemRepository {
	public static final class DuplicateItemException extends Exception {
		public DuplicateItemException(String msg) {
			super(msg);
		}
	}
	
	public static final class UnsupportedFormatException extends Exception {
		public UnsupportedFormatException(String msg) {
			super(msg);
		}
	}
	
	/**
	 * An enum of all supported audio formats. 
	 */
	public static enum AudioFormat {
		// TODO: make settings configurable
		A18("a18", new A18Format(128, 16000, 1, AlgorithmList.A1800, useHeaderChoice.No)),
		WAV("wav", new WAVFormat(128, 16000, 1)),
		MP3("mp3", new MP3Format(128, 16000, 1));
		
		
		private final String fileExtension;
		private final AudioConversionFormat audioConversionFormat;
		
		private AudioFormat(String fileExtension, AudioConversionFormat audioConversionFormat) {
			this.fileExtension = fileExtension;
			this.audioConversionFormat = audioConversionFormat;
		}
		
		public String getFileExtension() {
			return fileExtension;
		}
		
		public AudioConversionFormat getAudioConversionFormat() {
			return audioConversionFormat;
		}
	}
	
	private static final Map<String, AudioFormat> EXTENSION_TO_FORMAT = Maps.newHashMap();
	static {
		for (AudioFormat format : AudioFormat.values()) {
			EXTENSION_TO_FORMAT.put(format.getFileExtension().trim().toLowerCase(), format);
		}
	}
	
	private final ExternalConverter audioConverter = new ExternalConverter();
	
	/**
	 * Returns true, if this audio item is stored in any supported
	 * format in this repository. 
	 */
	public boolean hasAudioItem(AudioItem audioItem) {
		for (AudioFormat format : AudioFormat.values()) {
			if (hasAudioItemFormat(audioItem, format)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Returns true, if this audio item is stored in the given format
	 * in the repository.
	 */
	public boolean hasAudioItemFormat(AudioItem audioItem, AudioFormat format) {
		File file = getAudioFile(audioItem, format);
		return file != null && file.exists();
	}

	/**
	 * Store a new audioItem in the repository. 
	 * 
	 * Throws {@link DuplicateItemException} if the item already exists in this repository.
	 * For storing different audio formats of the same audio item in this repository
	 * call convert() instead of calling this method multiple times.
	 */
	public File storeAudioFile(AudioItem audioItem, File externalFile)
				throws DuplicateItemException, UnsupportedFormatException, IOException {
		if (hasAudioItem(audioItem)) {
			throw new DuplicateItemException(
					"Audio item with uid=" + audioItem.getUuid() + " already exists in this repository.");
		}
		
		AudioFormat format = determineFormat(externalFile);
		if (format == null) {
			throw new UnsupportedFormatException(
					"Unsupported or unrecognized audio format for file: " + externalFile); 
		}
		
		File toFile = resolveFile(audioItem, format);
		
		if (format == AudioFormat.A18) {
			// we only store the audio itself in the repo, as we keep the metadata separately in the database;
			// therefore strip metadata section here
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(externalFile)));
			int numBytes = IOUtils.readLittleEndian32(in);
			in.close();			
			IOUtils.copy(externalFile, toFile, numBytes+4);
		} else {
			IOUtils.copy(externalFile, toFile);
		}
		
		// optional garbage collection
		gc();
		
		return toFile;
	}

	/**
	 * Returns a handle to the audio file for the specified audioItem.
	 * Returns null, if the audio item is not stored with the given format
	 * in this repository;
	 */
	public File getAudioFile(AudioItem audioItem, AudioFormat format) {
		File file = resolveFile(audioItem, format);
		return file.exists() ? file : null;
	}
	
	/**
	 * Converts the audio item into the specified targetFormat and returns 
	 * a handle to the newly created file. 
	 */
	public File convert(AudioItem audioItem, AudioFormat targetFormat) throws ConversionException, IOException {
		File audioFile = getAudioFile(audioItem, targetFormat);
		if (audioFile != null) {
			// conversion not necessary
			return audioFile;
		}
		
		audioFile = resolveFile(audioItem, targetFormat);
		
		// we prefer to convert from WAV if possible
		File sourceFile = getAudioFile(audioItem, AudioFormat.WAV);
		if (sourceFile == null) {
			// no WAV, try any other format
			for (AudioFormat sourceFormat : AudioFormat.values()) {
				sourceFile = getAudioFile(audioItem, sourceFormat);
				break;
			}
		}
		
		if (OSChecker.WINDOWS) {
			audioConverter.convert(sourceFile, null, targetFormat.getAudioConversionFormat());
		}
		
		// optional garbage collection
		gc();

		return audioFile;
	}
	
	/**
	 * Deletes all files associated with an audioitem from the repository.
	 */
	public void delete(AudioItem audioItem) {
		// we need to loop over all formats, because the different formats
		// could we stored in different directories (e.g. local cache)
		for (AudioFormat format : AudioFormat.values()) {
			File file = getAudioFile(audioItem, format);
			if (file != null) {
				IOUtils.deleteRecursive(file.getParentFile());
			}
		}
	}
	
	public void exportA18WithMetadata(AudioItem audioItem, File targetDirectory) throws ConversionException, IOException {
		File fromFile = convert(audioItem, AudioFormat.A18);
		if (fromFile == null) {
			throw new IOException("AudioItem " + audioItem.getUuid() + " not found in repository.");
		}
		
		exportA18WithMetadataToFile(audioItem, new File(targetDirectory, fromFile.getName()));
	}

	public void exportA18WithMetadataToFile(AudioItem audioItem, File targetFile) throws ConversionException, IOException {
		File fromFile = convert(audioItem, AudioFormat.A18);
		if (fromFile == null) {
			throw new IOException("AudioItem " + audioItem.getUuid() + " not found in repository.");
		}
		
		IOUtils.copy(fromFile, targetFile);
		appendMetadataToA18(audioItem, targetFile);
	}
	
	/**
	 * Returns a handle to the audio file in the given format. Does not guarantee that the file exists. 
	 */
	protected abstract File resolveFile(AudioItem audioItem, AudioFormat format);

	/**
	 * Can optionally be overwritten by subclasses to performs a garbage collection 
	 * in the repository to free up disk space.
	 */
	protected void gc() throws IOException {
		// nothing to do by default
	}

	private static void appendMetadataToA18(AudioItem audioItem, File a18File) throws IOException {
		// remove locale hack once we get rid of localized audio items
		Metadata metadata = audioItem.getLocalizedAudioItem(Locale.ENGLISH).getMetadata();
		
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(a18File, true)));
		try {
			LBMetadataSerializer serializer = new LBMetadataSerializer();
			serializer.serialize(audioItem.getCategoryList(), metadata, out);
		} finally {
			out.close();
		}
	}
	
	/**
	 * Determines the format of the given file. Returns null, if the format was not recognized. 
	 */
	private static AudioFormat determineFormat(File file) {
		// TODO: we could try to detect the format without relying on the file extension
		String extension = IOUtils.getFileExtension(file).trim().toLowerCase();
		return EXTENSION_TO_FORMAT.get(extension);
	}
}
