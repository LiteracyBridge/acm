package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.importexport.FileImporter.Importer;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.DuplicateItemException;
import org.literacybridge.acm.repository.AudioItemRepository.UnsupportedFormatException;

public class WAVImporter extends Importer {

	@Override
	protected void importSingleFile(Category category, File file) throws IOException {
		try {
			AudioItem audioItem = new AudioItem(ACMConfiguration.getCurrentDB().getNewAudioItemUID());
			audioItem.addCategory(category);

			LocalizedAudioItem localizedAudioItem = new LocalizedAudioItem(audioItem.getUuid() + "-en", Locale.ENGLISH);
			audioItem.addLocalizedAudioItem(localizedAudioItem);
			
			Metadata metadata = localizedAudioItem.getMetadata();
			String title = file.getName().substring(0, file.getName().length() - 4);
			metadata.setMetadataField(MetadataSpecification.DC_IDENTIFIER, new MetadataValue<String>(audioItem.getUuid()));
			metadata.setMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(title));
			metadata.setMetadataField(MetadataSpecification.DTB_REVISION, new MetadataValue<String>("1"));
			metadata.setMetadataField(MetadataSpecification.DC_LANGUAGE, 
					new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(Locale.ENGLISH.getLanguage())));
			
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
		return new String[] {".wav"};
	}
}
