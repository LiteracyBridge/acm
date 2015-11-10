package org.literacybridge.acm.db;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.MetadataStore.Transaction;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
final class DBAudioItem extends AudioItem {
    private PersistentAudioItem mItem;

    public DBAudioItem(PersistentAudioItem item) {
        super(item.getUuid());
        mItem = item;
        loadFromDB();
    }

    public DBAudioItem(String uuid) {
        super(uuid);
        mItem = new PersistentAudioItem();
        mItem.setUuid(uuid);
        ACMConfiguration.getCurrentDB().getMetadataStore().commit(mItem);
    }

    public PersistentAudioItem getPersistentAudioItem() {
        return mItem;
    }

    @Override
    public Metadata getMetadata() {
        Metadata metadata = new Metadata();
        PersistentMetadata mMetadata = mItem.getPersistentLocalizedAudioItem().getPersistentMetadata();

        metadata.setMetadataField(MetadataSpecification.DC_IDENTIFIER,
                new MetadataValue<String>(mMetadata.getDc_identifier()));
        metadata.setMetadataField(MetadataSpecification.DC_PUBLISHER,
                new MetadataValue<String>(mMetadata.getDc_publisher()));
        metadata.setMetadataField(MetadataSpecification.DC_RELATION,
                new MetadataValue<String>(mMetadata.getDc_relation()));
        metadata.setMetadataField(MetadataSpecification.DC_SOURCE,
                new MetadataValue<String>(mMetadata.getDc_source()));
        metadata.setMetadataField(MetadataSpecification.DC_TITLE,
                new MetadataValue<String>(mMetadata.getDc_title()));
        metadata.setMetadataField(MetadataSpecification.DTB_REVISION,
                new MetadataValue<String>(mMetadata.getDtb_revision()));
        metadata.setMetadataField(MetadataSpecification.LB_DATE_RECORDED,
                new MetadataValue<String>(mMetadata.getDate_recorded()));
        metadata.setMetadataField(MetadataSpecification.DC_LANGUAGE,
                new MetadataValue<RFC3066LanguageCode>(
                        (mMetadata.getPersistentLocale() == null || mMetadata.getPersistentLocale().getLanguage() == null)
                        ? null : new RFC3066LanguageCode(mMetadata.getPersistentLocale().getLanguage())));
        metadata.setMetadataField(MetadataSpecification.LB_DURATION,
                new MetadataValue<String>(mMetadata.getDuration()));
        metadata.setMetadataField(MetadataSpecification.LB_MESSAGE_FORMAT,
                new MetadataValue<String>(mMetadata.getMessage_format()));
        metadata.setMetadataField(MetadataSpecification.LB_TARGET_AUDIENCE,
                new MetadataValue<String>(mMetadata.getTarget_audience()));
        metadata.setMetadataField(MetadataSpecification.LB_KEYWORDS,
                new MetadataValue<String>(mMetadata.getKeywords()));
        metadata.setMetadataField(MetadataSpecification.LB_TIMING,
                new MetadataValue<String>(mMetadata.getTiming()));
        metadata.setMetadataField(MetadataSpecification.LB_PRIMARY_SPEAKER,
                new MetadataValue<String>(mMetadata.getPrimary_speaker()));
        metadata.setMetadataField(MetadataSpecification.LB_GOAL,
                new MetadataValue<String>(mMetadata.getGoal()));
        metadata.setMetadataField(MetadataSpecification.LB_ENGLISH_TRANSCRIPTION,
                new MetadataValue<String>(mMetadata.getEnglish_transcription()));
        metadata.setMetadataField(MetadataSpecification.LB_NOTES,
                new MetadataValue<String>(mMetadata.getNotes()));
        metadata.setMetadataField(MetadataSpecification.LB_BENEFICIARY,
                new MetadataValue<String>(mMetadata.getBeneficiary()));
        metadata.setMetadataField(MetadataSpecification.LB_STATUS,
                new MetadataValue<Integer>(mMetadata.getNoLongerUsed()));

        return metadata;
    }

    @Override
    public void commitTransaction(Transaction t) {
        throw new UnsupportedOperationException("Writing to Derby DB is not supported anymore.");
    }

    public void loadFromDB() {
        // add all categories from DB to in-memory list
        removeAllCategories();
        for (PersistentCategory cat : mItem.getPersistentCategoryList()) {
            this.categories.put(cat.getUuid(), ACMConfiguration.getCurrentDB().getMetadataStore().getCategory(cat.getUuid()));
        }

        // add all playlists from DB to in-memory list
        for (PersistentTag playlist : mItem.getPersistentTagList()) {
            this.playlists.put(playlist.getUuid(), new DBPlaylist(playlist));
        }
    }
}
