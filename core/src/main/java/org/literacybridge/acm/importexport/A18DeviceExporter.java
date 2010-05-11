package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.repository.Repository;
import org.literacybridge.acm.utils.OSChecker;
import org.literacybridge.audioconverter.api.A18Format;
import org.literacybridge.audioconverter.api.ExternalConverter;
import org.literacybridge.audioconverter.api.A18Format.AlgorithmList;
import org.literacybridge.audioconverter.api.A18Format.useHeaderChoice;
import org.literacybridge.audioconverter.converters.BaseAudioConverter.ConversionException;

public class A18DeviceExporter {
	private static final String INBOX_SUB_DIR = "inbox/new-pkgs";
	
	public static boolean exportToDevice(LocalizedAudioItem item, DeviceInfo device) throws IOException {
		File deviceLocation = device.getPathToDevice();
		deviceLocation = new File(deviceLocation, INBOX_SUB_DIR);
		
		if (!deviceLocation.exists() || !deviceLocation.isDirectory()) {
			return false;
		}

		File[] files = Repository.getRepository().resolveName(item).listFiles();

		File audioFile = null;
		for (File f : files) {
			if (FileImporter.getFileExtension(f).equals(".a18")) {
				// prefer a18 files
				audioFile = f;
				break;
			} else if (FileImporter.getFileExtension(f).equals(".wav")) {
				audioFile = f;
			} else {
				if (audioFile == null) {
					audioFile = f;
				}
			}
		}
		
			// TODO: check if this file already exists on device, and update if revision higher
			
		if (FileImporter.getFileExtension(audioFile).equalsIgnoreCase(".a18")) {
			// already a18 file - just copy
			File target = new File(deviceLocation, audioFile.getName());
			Repository.copy(audioFile, target);
		} else {
			// convert first
			if (OSChecker.WINDOWS) {
				try {
					ExternalConverter audioConverter = new ExternalConverter();
					audioConverter.convert(audioFile, new File(audioFile.getParent()), new A18Format(128, 16000, 1, AlgorithmList.A1800, useHeaderChoice.No));
					String fileName = audioFile.getName();
					
					File fileToCopy = new File(audioFile.getParent(), fileName.substring(0, fileName.length() - 4)+ ".a18");
					// TODO: generate old-style a18 file name (with 

					File target = new File(deviceLocation, fileName.substring(0, fileName.length() - 4)+ "#OTHER.a18");
					Repository.copy(fileToCopy, target);
				} catch (ConversionException e) {
					throw new IOException(e);
				}
			}
		}
			
			
		
		// success
		return true;
	}
}
