package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.repository.AudioItemRepository;

public class A18DeviceExporter {
	private static final Logger LOG = Logger.getLogger(A18DeviceExporter.class.getName());
	
	private static final String INBOX_SUB_DIR = "inbox/messages";
	
	public static boolean exportToDevice(LocalizedAudioItem item, DeviceInfo device) throws IOException {
		File deviceLocation = device.getPathToDevice();
		deviceLocation = new File(deviceLocation, INBOX_SUB_DIR);
		
		if (!deviceLocation.exists() || !deviceLocation.isDirectory()) {
			return false;
		}

		AudioItemRepository repository = ACMConfiguration.getCurrentDB().getRepository();
		try {
			repository.exportA18WithMetadata(item.getParentAudioItem(), deviceLocation);
		} catch (ConversionException e) {
			LOG.log(Level.WARNING, "Error converting audio file.", e);
			return false;
		}
		
		// success
		return true;
	}
}
