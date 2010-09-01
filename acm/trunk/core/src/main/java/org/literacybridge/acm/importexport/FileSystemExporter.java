package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;

import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.repository.Repository;
import org.literacybridge.acm.utils.OSChecker;
import org.literacybridge.audioconverter.api.AudioConversionFormat;
import org.literacybridge.audioconverter.api.ExternalConverter;
import org.literacybridge.audioconverter.converters.BaseAudioConverter.ConversionException;

public class FileSystemExporter {
	public static void export(LocalizedAudioItem[] selectedAudioItems, File targetDir, AudioConversionFormat targetFormat) 
		throws IOException {
		String targetFormatExtension = "." + targetFormat.getFileEnding();
		
		try {
			ExternalConverter audioConverter = new ExternalConverter();
			Repository repository = Repository.getRepository();
			
			for (LocalizedAudioItem localizedAudioItem : selectedAudioItems) {
				File itemDir = repository.resolveName(localizedAudioItem);
				String[] files = itemDir.list();
				
				// first: check which formats we have
				String fileToCopy = null;
				boolean needToConvert = true;
				
				for (String file : files) {
					if (FileImporter.getFileExtension(file).equalsIgnoreCase(targetFormatExtension)) {
						fileToCopy = file;
						needToConvert = false;
						break;
					}
	
					if (fileToCopy == null) {
						fileToCopy = file;
					} else {
						if (FileImporter.getFileExtension(file).equalsIgnoreCase(".wav")) {
							// prefer wav over mp3 and a18
							fileToCopy = file;
						} else if (FileImporter.getFileExtension(file).equalsIgnoreCase(".mp3")
								&& !FileImporter.getFileExtension(fileToCopy).equalsIgnoreCase(".wav")) {
							// prefer mp3 over a18
							fileToCopy = file;
						}
					}
				}
				
				// second: convert if necessary
				if (needToConvert && fileToCopy != null) {
					// convert first
					if (OSChecker.WINDOWS) {
						File sourceFile = new File(itemDir, fileToCopy);
						audioConverter.convert(sourceFile, new File(sourceFile.getParent()), targetFormat);
						fileToCopy = fileToCopy.substring(0, fileToCopy.length() - 4) + targetFormatExtension;
					} else {
						fileToCopy = null;
					}
				}
				
				// third: copy to external folder
				if (fileToCopy != null) {
					File sourceFile = new File(itemDir, fileToCopy);
					File targetFile = new File(targetDir, localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue()
														  + targetFormatExtension);
					Repository.copy(sourceFile, targetFile);
				}
			}
		} catch (ConversionException e) {
			throw new IOException(e);
		}
	}
}
