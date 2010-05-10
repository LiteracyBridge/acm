package org.literacybridge.acm.importexport;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.importexport.FileImporter.Importer;
import org.literacybridge.acm.metadata.LBMetadataSerializer;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.Repository;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.OSChecker;
import org.literacybridge.audioconverter.api.ExternalConverter;
import org.literacybridge.audioconverter.api.WAVFormat;
import org.literacybridge.audioconverter.converters.BaseAudioConverter.ConversionException;

public class A18Importer extends Importer {

	private static final Map<String, String> LegacyCategoryStrings = new HashMap<String, String>();
	static {
		LegacyCategoryStrings.put("OTHER", "0");
		LegacyCategoryStrings.put("AGRIC", "1");
		LegacyCategoryStrings.put("HEALTH", "2");
		LegacyCategoryStrings.put("EDU", "3");
		LegacyCategoryStrings.put("STORY", "4");
	}
	
	@Override
	protected void importSingleFile(Category category, File file)
			throws IOException {
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			int bytesToSkip = IOUtils.readLittleEndian32(in);
			
			LocalizedAudioItem localizedAudioItem = new LocalizedAudioItem("", Locale.ENGLISH);
			Metadata metadata = localizedAudioItem.getMetadata();
			
			Set<Category> categories = new HashSet<Category>();
			if (bytesToSkip + 4 < file.length()) {
				try {
					in.skipBytes(bytesToSkip);
					LBMetadataSerializer serializer = new LBMetadataSerializer();
					metadata = serializer.deserialize(categories, in);
				} catch (IOException e) {
					// do nothing
				}
			} 
			
			AudioItem audioItem = null;
			
			if (metadata.getNumberOfFields() == 0) {
				// legacy mode
				audioItem = new AudioItem(Repository.getNewUUID());
				String fileName = file.getName();
				metadata.addMetadataField(MetadataSpecification.DTB_REVISION, new MetadataValue<String>("1"));
				metadata.addMetadataField(MetadataSpecification.DC_IDENTIFIER, new MetadataValue<String>(audioItem.getUuid()));
				metadata.addMetadataField(MetadataSpecification.DC_LANGUAGE, 
						new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode("en")));

				int index = fileName.indexOf('#');
				if (index >= 0) {
					metadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(fileName.substring(0, index)));
					// only parse categories from filename if no particular target category is selected
					if (category == null) {
						String catString = fileName.substring(index + 1, fileName.length() - 4);
						String catID = LegacyCategoryStrings.get(catString);
						if (catID != null) {
							categories.add(new Category(PersistentCategory.getFromDatabase(catID)));
						}
					}
				} else {
					metadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(fileName.substring(0, fileName.length() - 4)));
				}
				
			} else {
				audioItem = new AudioItem(metadata.getMetadataValues(MetadataSpecification.DC_IDENTIFIER).get(0).getValue());
			}
						
			// add the category that was selected during drag&drop
			audioItem.addCategory(category);
	
			// add categories the file had already, if any
			for (Category cat : categories) {
				audioItem.addCategory(cat);
			}
			
			localizedAudioItem.setUuid(audioItem.getUuid() + "-en");
			audioItem.addLocalizedAudioItem(localizedAudioItem);
			
			audioItem.commit();
			
			Repository repository = Repository.getRepository();
			repository.store(file, file.getName(), localizedAudioItem);
			
			File newFile = new File(repository.resolveName(localizedAudioItem), file.getName());
			FileOutputStream fos = new FileOutputStream(newFile, true);
			DataOutputStream out = new DataOutputStream(fos);
			LBMetadataSerializer serializer = new LBMetadataSerializer();
			serializer.serialize(categories, metadata, out);
			out.close();
			
			if (OSChecker.WINDOWS) {
				ExternalConverter audioConverter = new ExternalConverter();
				File itemDir = repository.resolveName(localizedAudioItem);
				File sourceFile = new File(itemDir, file.getName());
				audioConverter.convert(sourceFile, new File(sourceFile.getParent()), new WAVFormat(128, 16000, 1));
			}
	
			System.out.println(metadata.toString());
			in.close();
		} catch (ConversionException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected String[] getSupportedFileExtensions() {
		return new String[] {".a18"};
	}
}
