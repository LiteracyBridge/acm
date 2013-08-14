package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.utils.IOUtils;

public class FileSystemExporter {
	public static final String FILENAME_SEPARATOR = "___";
	
	public static void export(LocalizedAudioItem[] selectedAudioItems, File targetDir, AudioFormat targetFormat) 
		throws IOException {
		
		try {
			AudioItemRepository repository = Configuration.getRepository();
			
			for (LocalizedAudioItem localizedAudioItem : selectedAudioItems) {				
				// first: check which formats we have
				File sourceFile = repository.convert(localizedAudioItem.getParentAudioItem(), targetFormat);
				
				if (sourceFile != null) {
					String title = localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue()
							+ FILENAME_SEPARATOR + localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.DC_IDENTIFIER).get(0).getValue();
					
					// replace invalid file name characters (windows) with an underscore ('_')
					title = title.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
					File targetFile;
					int counter = 0;
					do {
						if (counter == 0) {
							targetFile = new File(targetDir, title + "." + targetFormat.getFileExtension());
						} else {
							targetFile = new File(targetDir, title + "-" + counter + "." + targetFormat.getFileExtension());
						}
						counter++;
					} while (targetFile.exists());
					if (targetFormat == AudioFormat.A18) {
						repository.exportA18WithMetadataToFile(localizedAudioItem.getParentAudioItem(), targetFile);
					} else {
						IOUtils.copy(sourceFile, targetFile);
					}
				}
			}
		} catch (ConversionException e) {
			throw new IOException(e);
		}
	}
}
