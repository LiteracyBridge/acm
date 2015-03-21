package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.utils.IOUtils;

public class FileSystemExporter {
	public static final String FILENAME_SEPARATOR = "___";
	
	public static void export(LocalizedAudioItem[] selectedAudioItems, File targetDir, AudioFormat targetFormat, boolean titleInFilename, boolean idInFilename) 
		throws IOException {
		
		try {
			AudioItemRepository repository = ACMConfiguration.getCurrentDB().getRepository();
			
			for (LocalizedAudioItem localizedAudioItem : selectedAudioItems) {				
				// first: check which formats we have
				File sourceFile = repository.convert(localizedAudioItem.getParentAudioItem(), targetFormat);
				
				if (sourceFile != null) {
					String title = (titleInFilename?localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue():"")
							+ (idInFilename && titleInFilename?FILENAME_SEPARATOR:"")
							+ (idInFilename?localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.DC_IDENTIFIER).get(0).getValue():"");
					
					// replace invalid file name characters (windows) with an underscore ('_')
					title = title.trim().replaceAll("[\\\\/:*?\"<>|']", "_");
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
