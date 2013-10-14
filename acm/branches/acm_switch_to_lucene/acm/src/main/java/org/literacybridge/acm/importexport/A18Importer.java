package org.literacybridge.acm.importexport;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.importexport.FileImporter.Importer;
import org.literacybridge.acm.metadata.LBMetadataSerializer;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.DuplicateItemException;
import org.literacybridge.acm.repository.AudioItemRepository.UnsupportedFormatException;
import org.literacybridge.acm.utils.IOUtils;

public class A18Importer extends Importer {
	public static LocalizedAudioItem loadMetadata(File file) throws IOException {
		return loadMetadata(null, file);
	}
	
	public static LocalizedAudioItem loadMetadata(Category importCategory, File file) throws IOException {
		DataInputStream in = null;
		
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			int bytesToSkip = IOUtils.readLittleEndian32(in);
			
			LocalizedAudioItem localizedAudioItem = new LocalizedAudioItem("", Locale.ENGLISH);
			Metadata metadata = localizedAudioItem.getMetadata();
			
			Set<Category> categories = new HashSet<Category>();
			if (bytesToSkip + 4 < file.length()) {
				try {
					in.skipBytes(bytesToSkip);
					LBMetadataSerializer serializer = new LBMetadataSerializer();
					serializer.deserialize(metadata, categories, in);
				} catch (IOException e) {
					// do nothing
				}
			} 
			
			AudioItem audioItem = null;
			
			if (metadata.getNumberOfFields() == 0) {
				// legacy mode
				audioItem = new AudioItem(ACMConfiguration.getCurrentDB().getNewAudioItemUID());
				String fileName = file.getName();
				metadata.setMetadataField(MetadataSpecification.DTB_REVISION, new MetadataValue<String>("1"));
				metadata.setMetadataField(MetadataSpecification.DC_IDENTIFIER, new MetadataValue<String>(audioItem.getUuid()));
				metadata.setMetadataField(MetadataSpecification.DC_LANGUAGE, 
						new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(Locale.ENGLISH.getLanguage())));
	
				metadata.setMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(fileName.substring(0, fileName.length() - 4)));
				// add the category that was selected during drag&drop
				if (importCategory != null) {
					categories.add(importCategory);
				}
				
			} else {
				localizedAudioItem.setLocale(metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE).get(0).getValue().getLocale());
				// set metadata language to localizedAudioItem language
				localizedAudioItem.getMetadata().setMetadataField(MetadataSpecification.DC_LANGUAGE
							, new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(localizedAudioItem.getLocale().getLanguage())));
				
				
				audioItem = new AudioItem(metadata.getMetadataValues(MetadataSpecification.DC_IDENTIFIER).get(0).getValue());
				if (metadata.getMetadataValues(MetadataSpecification.DTB_REVISION) == null) {
					metadata.setMetadataField(MetadataSpecification.DTB_REVISION, new MetadataValue<String>("0"));
				}
			}
						
			// add categories the file had already, if any
			for (Category cat : categories) {
				audioItem.addCategory(cat);
			}
			
			localizedAudioItem.setUuid(audioItem.getUuid() + "-en");
			audioItem.addLocalizedAudioItem(localizedAudioItem);
			return localizedAudioItem;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	@Override
	protected void importSingleFile(Category category, File file)
			throws IOException {
		try {
			LocalizedAudioItem localizedAudioItem = loadMetadata(category, file);
			AudioItem audioItem = localizedAudioItem.getParentAudioItem();
			
			// TODO: handle updating the file by making use of revisions
			if (AudioItem.getFromDatabase(audioItem.getUuid()) != null) {
				// just skip for now if we have an item with the same id already
				System.out.println("  *already in database; skipping*");
				return;
			}
			
			AudioItemRepository repository = ACMConfiguration.getCurrentDB().getRepository();
			repository.storeAudioFile(audioItem, file);
			
			audioItem.commit();

		} catch (UnsupportedFormatException e) {
			throw new IOException(e);
		} catch (DuplicateItemException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected String[] getSupportedFileExtensions() {
		return new String[] {".a18"};
	}
}
