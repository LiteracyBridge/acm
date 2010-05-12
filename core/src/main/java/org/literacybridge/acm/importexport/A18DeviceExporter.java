package org.literacybridge.acm.importexport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.metadata.LBMetadataSerializer;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.repository.Repository;
import org.literacybridge.acm.utils.IOUtils;
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
					appendMetadataToA18(item, fileToCopy);
					
					List<File> targetFiles = appendCategoryStringToFileName(fileToCopy, item);
					
					for (File target : targetFiles) {
						Repository.copy(fileToCopy, new File(deviceLocation, target.getName()));
					}
				} catch (ConversionException e) {
					throw new IOException(e);
				}
			}
		}
		
		// success
		return true;
	}
	
	private static void appendMetadataToA18(LocalizedAudioItem item, File a18File) throws IOException {
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(a18File)));
		int bytesToSkip = IOUtils.readLittleEndian32(in);
		in.close();
		
		Metadata metadata = item.getMetadata();
		
		Set<Category> categories = new HashSet<Category>();
		if (bytesToSkip + 4 == a18File.length()) {
			// append
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(a18File, true)));
			try {
				LBMetadataSerializer serializer = new LBMetadataSerializer();
				serializer.serialize(categories, metadata, out);
			} finally {
				out.close();
			}
		}
	}

	private final static Map<String, String> oldStyleCatStrings = new HashMap<String, String>();
	static {
		oldStyleCatStrings.put("0", "OTHER");
		oldStyleCatStrings.put("1", "AGRIC");
		oldStyleCatStrings.put("2", "HEALTH");	
		oldStyleCatStrings.put("3", "EDU");
		oldStyleCatStrings.put("4", "STORY");
	}
	
	private static List<File> appendCategoryStringToFileName(File a18File, LocalizedAudioItem item) {
		List<Category> cats = item.getParentAudioItem().getCategoryList();
		List<File> result = new LinkedList<File>();

		// while the device only supports single categories
		for (Category cat : cats) {
			String catString = oldStyleCatStrings.get(cat.getUuid());
			if (catString != null) {
				String path = a18File.getAbsolutePath();
				path = path.substring(0, path.length() - 4) + "#" + catString + ".a18";
				File target = new File(path);
				result.add(target);
			}
		}
		
		if (result.isEmpty()) {
			result.add(a18File);
		}
		
		return result;
	}
}
