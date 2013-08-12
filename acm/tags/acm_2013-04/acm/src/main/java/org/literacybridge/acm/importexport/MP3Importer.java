package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.cmc.music.common.ID3ReadException;
import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.Configuration;
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

public class MP3Importer extends Importer {

	@Override
	protected void importSingleFile(Category category, File file) throws IOException {
		try {
			MusicMetadataSet musicMetadataSet = new MyID3().read(file);
			IMusicMetadata musicMetadata = musicMetadataSet.getSimplified();
			
			AudioItem audioItem = new AudioItem(Configuration.getNewAudioItemUID());
			audioItem.addCategory(category);

			LocalizedAudioItem localizedAudioItem = new LocalizedAudioItem(audioItem.getUuid() + "-en", Locale.ENGLISH);
			audioItem.addLocalizedAudioItem(localizedAudioItem);
			
			Metadata metadata = localizedAudioItem.getMetadata();
			String title = musicMetadata.getSongTitle();
			if (title == null || title.trim().isEmpty()) {
				title = file.getName().substring(0, file.getName().length() - 4);
			}
			metadata.setMetadataField(MetadataSpecification.DC_IDENTIFIER, new MetadataValue<String>(audioItem.getUuid()));
			metadata.setMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(title));
			metadata.setMetadataField(MetadataSpecification.LB_PRIMARY_SPEAKER, new MetadataValue<String>(musicMetadata.getArtist()));
			metadata.setMetadataField(MetadataSpecification.DTB_REVISION, new MetadataValue<String>("1"));
			Number year = musicMetadata.getYear();
			if (year != null) {
				metadata.setMetadataField(MetadataSpecification.LB_DATE_RECORDED, new MetadataValue<String>(year.toString()));
			}
			metadata.setMetadataField(MetadataSpecification.DC_LANGUAGE, 
					new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(Locale.ENGLISH.getLanguage())));
			
			AudioItemRepository repository = Configuration.getRepository();
			repository.storeAudioFile(audioItem, file);			
			
			audioItem.commit();
		} catch (ID3ReadException e) {
			throw new IOException(e);
		} catch (UnsupportedFormatException e) {
			throw new IOException(e);
		} catch (DuplicateItemException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected String[] getSupportedFileExtensions() {
		return new String[] {".mp3"};
	}
}
