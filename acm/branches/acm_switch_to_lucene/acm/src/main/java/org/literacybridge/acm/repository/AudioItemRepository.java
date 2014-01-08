package org.literacybridge.acm.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
import com.google.common.collect.Sets;

/**
 * This repository manages all audio files associated with the audio items
 * that the ACM stores. Metadata of the audio items is stored separately,
 * not in this repository.
 */
// TODO: We could improve multi-threading performance here if we lock
// per audioitem, instead of using one lock across all items. 
public abstract class AudioItemRepository {
	private final static File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
	
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
	public synchronized boolean hasAudioItem(AudioItem audioItem) {
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
	public synchronized boolean hasAudioItemFormat(AudioItem audioItem, AudioFormat format) {
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
	public synchronized File storeAudioFile(AudioItem audioItem, File externalFile)
				throws DuplicateItemException, UnsupportedFormatException, IOException {

//		Commenting out these four lines below so that an existing cached .wav file doesn't stop an .a18 import
//		if (hasAudioItem(audioItem)) {
//			throw new DuplicateItemException(
//					"Audio item with uid=" + audioItem.getUuid() + " already exists in this repository.");
//		}
		
		AudioFormat format = determineFormat(externalFile);
		if (format == null) {
			throw new UnsupportedFormatException(
					"Unsupported or unrecognized audio format for file: " + externalFile); 
		}
		
		File toFile = resolveFile(audioItem, format, true);
		
		if (format == AudioFormat.A18) {
			// we only store the audio itself in the repo, as we keep the metadata separately in the database;
			// therefore strip metadata section herea
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(externalFile)));
			int numBytes = IOUtils.readLittleEndian32(in);
			in.close();			
			IOUtils.copy(externalFile, toFile, numBytes+4);
		} else {
			IOUtils.copy(externalFile, toFile);
			try {
				// convert file to A18 format right away
				convert(audioItem, AudioFormat.A18);
			} catch (ConversionException e) {
				throw new IOException(e);
			}
		}
		
		// update the duration in the audioitem's metadata section
		A18DurationUtil.updateDuration(audioItem);
		
		// optional garbage collection
		gc();
		
		return toFile;
	}

	/**
	 * Returns a handle to the audio file for the specified audioItem.
	 * Returns null, if the audio item is not stored with the given format
	 * in this repository;
	 */
	public synchronized File getAudioFile(AudioItem audioItem, AudioFormat format) {
		checkFilesUpToDate(audioItem);
		
		File file = resolveFile(audioItem, format, false);
		return file.exists() ? file : null;
	}
	
	/**
	 * Converts the audio item into the specified targetFormat and returns 
	 * a handle to the newly created file. 
	 */
	public synchronized File convert(AudioItem audioItem, AudioFormat targetFormat) throws ConversionException, IOException {
		checkFilesUpToDate(audioItem);
		
		File audioFile = resolveFile(audioItem, targetFormat, true);
		if (audioFile.exists()) {
			// conversion not necessary
			return audioFile;
		}
		
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
			audioConverter.convert(sourceFile, audioFile.getParentFile(), TMP_DIR, 
					targetFormat.getAudioConversionFormat(), false);
		}
		
		// optional garbage collection
		gc();

		return audioFile;
	}
	
	/**
	 * Updates all stores audio files associated with the given AudioItem.
	 * 
	 * First the file in the given format is imported and the old version of that format is, if it exists, overwritten.
	 * Then this new version is converted into all formats that were previously stored for that audio item.
	 */
	public synchronized void updateAudioItem(AudioItem audioItem, File externalFile) throws ConversionException, IOException, 
	                                                                           UnsupportedFormatException {
		if (determineFormat(externalFile) == null) {
			throw new UnsupportedFormatException(
					"Unsupported or unrecognized audio format for file: " + externalFile); 
		}
		
		// first determine in which formats the item is currently stored
		Set<AudioFormat> existingFormats = Sets.newHashSet();
		
		for (AudioFormat format : AudioFormat.values()) {
			if (hasAudioItemFormat(audioItem, format)) {
				existingFormats.add(format);
			}
		}
		
		// now delete the old files
		delete(audioItem);
		
		// store the new sourceFile
		try {
			storeAudioFile(audioItem, externalFile);
		} catch (UnsupportedFormatException e) {
			// can't happen - we determined already that we can handle the format
		} catch (DuplicateItemException e) {
			// can't happen - we deleted all files for this audio item
		}
		
		// convert to all previously stored formats
		for (AudioFormat format : existingFormats) {
			convert(audioItem, format);
		}
	}
	
	/**
	 * Deletes all files associated with an audioitem from the repository.
	 */
	public synchronized void delete(AudioItem audioItem) {
		// we need to loop over all formats, because the different formats
		// could be stored in different directories (e.g. local cache)
		for (AudioFormat format : AudioFormat.values()) {
			File file = resolveFile(audioItem, format, true);
			if (file != null) {
				IOUtils.deleteRecursive(file.getParentFile());
			}
		}
	}
	
	public synchronized void exportA18WithMetadata(AudioItem audioItem, File targetDirectory) throws ConversionException, IOException {
		File fromFile = convert(audioItem, AudioFormat.A18);
		if (fromFile == null) {
			throw new IOException("AudioItem " + audioItem.getUuid() + " not found in repository.");
		}
		
		exportA18WithMetadataToFile(audioItem, new File(targetDirectory, fromFile.getName()));
	}

	public synchronized void exportA18WithMetadataToFile(AudioItem audioItem, File targetFile) throws ConversionException, IOException {
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
	protected abstract File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess);

	/**
	 * Can optionally be overwritten by subclasses to performs a garbage collection 
	 * in the repository to free up disk space.
	 */
	protected void gc() throws IOException {
		// nothing to do by default
	}
	
	/**
	 * Checks if no file belonging to an audio item is older than its corresponding a18 file
	 */
	private void checkFilesUpToDate(AudioItem audioItem) {
		File a18 = resolveFile(audioItem, AudioFormat.A18, false);
		if (a18.exists()) {
			for (AudioFormat format : AudioFormat.values()) {
				if (format == AudioFormat.A18) {
					continue;
				}
				File file = resolveFile(audioItem, format, true);
				if (file.exists() && file.lastModified() < a18.lastModified()) {
					// a18 file is newer - delete it, it will get recreated from the a18 in convert()
					file.delete();
				}
			}

		}
	}

	private static void appendMetadataToA18(AudioItem audioItem, File a18File) throws IOException {
		// remove locale hack once we get rid of localized audio items
		Metadata metadata = audioItem.getMetadata();
		
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
