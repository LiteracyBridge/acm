package org.literacybridge.acm.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.RFC3066LanguageCode;

@Entity
@NamedQueries({
    @NamedQuery(name = "PersistentAudioItem.findAll", query = "select o from PersistentAudioItem o")
})
@Table(name = "t_audioitem")
/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
class PersistentAudioItem extends PersistentObject {
    private static final Logger LOG = Logger.getLogger(PersistentAudioItem.class.getName());
    private static final long serialVersionUID = 6523719801839346881L;

    private static final String COLUMN_VALUE = "gen_audioitem";

    @TableGenerator(name = COLUMN_VALUE,
            table = PersistentObject.SEQUENCE_TABLE_NAME,
            pkColumnName = PersistentObject.SEQUENCE_KEY,
            valueColumnName = PersistentObject.SEQUENCE_VALUE,
            pkColumnValue = COLUMN_VALUE,
            allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;

    @Column(name="uuid")
    private String uuid;

    @ManyToMany
    @JoinTable(
            name = "t_audioitem_has_category",
            joinColumns =
            @JoinColumn(name = "audioitem", referencedColumnName = "id"),
            inverseJoinColumns =
            @JoinColumn(name = "category", referencedColumnName = "id")
            )
    private Set<PersistentCategory> persistentCategoryList = new LinkedHashSet<PersistentCategory>();

    @ManyToMany
    @JoinTable(
            name = "t_audioitem_has_tag",
            joinColumns =
            @JoinColumn(name = "audioitem", referencedColumnName = "id"),
            inverseJoinColumns =
            @JoinColumn(name = "tag", referencedColumnName = "id")
            )
    private Set<PersistentTag> persistentTagList = new LinkedHashSet<PersistentTag>();


    @OneToMany(mappedBy = "persistentAudioItem", cascade = {CascadeType.ALL})
    private List<PersistentLocalizedAudioItem> persistentLocalizedAudioItemList = new ArrayList<PersistentLocalizedAudioItem>();

    public PersistentAudioItem() {
        persistentLocalizedAudioItemList.add(new PersistentLocalizedAudioItem());
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
        getPersistentLocalizedAudioItem().setUuid(uuid);
    }

    public Integer getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Collection<PersistentCategory> getPersistentCategoryList() {
        return persistentCategoryList;
    }

    public PersistentCategory addPersistentAudioItemCategory(PersistentCategory persistentCategory) {
        getPersistentCategoryList().add(persistentCategory);
        return persistentCategory;
    }

    public boolean hasPersistentAudioItemCategory(PersistentCategory persistentCategory) {
        return getPersistentCategoryList().contains(persistentCategory);
    }

    public PersistentCategory removePersistentCategory(PersistentCategory persistentCategory) {
        getPersistentCategoryList().remove(persistentCategory);
        return persistentCategory;
    }

    public void removeAllPersistentCategories() {
        getPersistentCategoryList().clear();
    }

    public Collection<PersistentTag> getPersistentTagList() {
        return persistentTagList;
    }

    public PersistentTag addPersistentAudioItemTag(PersistentTag persistentTag) {
        if (!getPersistentTagList().contains(persistentTag)) {
            getPersistentTagList().add(persistentTag);
            persistentTag.getPersistentAudioItemList().add(this);
        }
        return persistentTag;
    }

    public boolean hasPersistentAudioItemTag(PersistentTag persistentTag) {
        return getPersistentTagList().contains(persistentTag);
    }

    public PersistentTag removePersistentTag(PersistentTag persistentTag) {
        getPersistentTagList().remove(persistentTag);
        persistentTag.getPersistentAudioItemList().remove(this);
        return persistentTag;
    }

    public void removeAllPersistentTags() {
        for (PersistentTag tag : getPersistentTagList()) {
            tag.getPersistentAudioItemList().remove(this);
        }
        getPersistentTagList().clear();
    }

    public PersistentLocalizedAudioItem getPersistentLocalizedAudioItem() {
        return persistentLocalizedAudioItemList.get(0);
    }

    public static List<PersistentAudioItem> getFromDatabase() {
        return PersistentQueries.getPersistentObjects(PersistentAudioItem.class);
    }

    public static PersistentAudioItem getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentAudioItem.class, id);
    }

    public static PersistentAudioItem getFromDatabase(String uuid) {
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
        PersistentAudioItem result = null;
        try {
            Query findObject = em.createQuery("SELECT o FROM PersistentAudioItem o WHERE o.uuid = '" + uuid + "'");
            result = (PersistentAudioItem) findObject.getSingleResult();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return result;
    }

    public static Integer uidToId(String uuid) {
        PersistentAudioItem audioItem = PersistentAudioItem.getFromDatabase(uuid);
        if (audioItem == null) {
            return null;
        }
        return audioItem.getId();
    }

    public static AudioItem convert(PersistentAudioItem mItem) {
        AudioItem audioItem = new AudioItem(mItem.getUuid());
        // add all categories from DB to in-memory list
        for (PersistentCategory cat : mItem.getPersistentCategoryList()) {
            Category category = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getCategory(cat.getUuid());
            if (category == null) {
                LOG.warning("DB migration: Category with uuid " + cat.getUuid() + " not found.");
            } else {
                audioItem.addCategory(category);
            }
        }

        // add all playlists from DB to in-memory list
        for (PersistentTag playlist : mItem.getPersistentTagList()) {
            Playlist pl = PersistentTag.convert(playlist);
            audioItem.addPlaylist(pl);
            pl.addAudioItem(audioItem.getUuid());
        }

        loadMetadata(mItem, audioItem.getMetadata());
        return audioItem;
    }

    private static Metadata loadMetadata(PersistentAudioItem mItem, Metadata metadata) {
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
}
