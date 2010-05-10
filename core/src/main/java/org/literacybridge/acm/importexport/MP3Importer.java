package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.cmc.music.common.ID3ReadException;
import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.importexport.FileImporter.Importer;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.Repository;
import org.literacybridge.acm.utils.OSChecker;
import org.literacybridge.audioconverter.api.ExternalConverter;
import org.literacybridge.audioconverter.api.WAVFormat;
import org.literacybridge.audioconverter.converters.BaseAudioConverter.ConversionException;

public class MP3Importer extends Importer {

	@Override
	protected void importSingleFile(Category category, File file) throws IOException {
		try {
			MusicMetadataSet musicMetadataSet = new MyID3().read(file);
			IMusicMetadata musicMetadata = musicMetadataSet.getSimplified();
			
			AudioItem audioItem = new AudioItem(Repository.getNewUUID());
			audioItem.addCategory(category);

			LocalizedAudioItem localizedAudioItem = new LocalizedAudioItem(audioItem.getUuid() + "-en", Locale.ENGLISH);
			audioItem.addLocalizedAudioItem(localizedAudioItem);
			
			Metadata metadata = localizedAudioItem.getMetadata();
			metadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>(musicMetadata.getSongTitle()));
			metadata.addMetadataField(MetadataSpecification.DC_CREATOR, new MetadataValue<String>(musicMetadata.getArtist()));
			metadata.addMetadataField(MetadataSpecification.DTB_REVISION, new MetadataValue<String>("1"));
			Number year = musicMetadata.getYear();
			if (year != null) {
				metadata.addMetadataField(MetadataSpecification.DTB_REVISION_DATE, new MetadataValue<String>(year.toString()));
			}
			metadata.addMetadataField(MetadataSpecification.DC_LANGUAGE, 
					new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode("en")));
			audioItem.commit();
			
			Repository repository = Repository.getRepository();
			repository.store(file, file.getName(), localizedAudioItem);
			
			if (OSChecker.WINDOWS) {
				ExternalConverter audioConverter = new ExternalConverter();
				File itemDir = repository.resolveName(localizedAudioItem);
				File sourceFile = new File(itemDir, file.getName());
				File targetFile = new File(itemDir, file.getName().replace(".mp3", ".wav"));
				audioConverter.convert(sourceFile, new File(sourceFile.getParent()), new WAVFormat(16000, 128, 1));
			}
		} catch (ID3ReadException e) {
			throw new IOException(e);
		} catch (ConversionException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected String[] getSupportedFileExtensions() {
		return new String[] {".mp3"};
	}
}
