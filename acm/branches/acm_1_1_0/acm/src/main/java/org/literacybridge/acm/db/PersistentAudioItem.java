package org.literacybridge.acm.db;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentAudioItem.findAll", query = "select o from PersistentAudioItem o")
})
@Table(name = "t_audioitem")
public class PersistentAudioItem extends PersistentObject {
    
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
    private List<PersistentCategory> persistentCategoryList = new LinkedList<PersistentCategory>();    

    @ManyToMany
    @JoinTable(
        name = "t_audioitem_has_tag",
        joinColumns =
            @JoinColumn(name = "audioitem", referencedColumnName = "id"),
        inverseJoinColumns =
            @JoinColumn(name = "tag", referencedColumnName = "id")
    )
    private List<PersistentTag> persistentTagList = new LinkedList<PersistentTag>();    

    
    @OneToMany(mappedBy = "persistentAudioItem", cascade = {CascadeType.ALL})
    private List<PersistentLocalizedAudioItem> persistentLocalizedAudioItemList = new LinkedList<PersistentLocalizedAudioItem>();

    public PersistentAudioItem() {
    }

    public Integer getId() {
        return id;
    }    

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<PersistentCategory> getPersistentCategoryList() {
        return persistentCategoryList;
    }

    public void setPersistentCategoryList(List<PersistentCategory> persistentCategoryList) {
        this.persistentCategoryList = persistentCategoryList;
    }

    public PersistentCategory addPersistentAudioItemCategory(PersistentCategory persistentCategory) {
        getPersistentCategoryList().add(persistentCategory);
        persistentCategory.getPersistentAudioItemList().add(this);
        return persistentCategory;
    }

    public PersistentCategory removePersistentCategory(PersistentCategory persistentCategory) {
        getPersistentCategoryList().remove(persistentCategory);
        persistentCategory.getPersistentAudioItemList().remove(this);
        return persistentCategory;
    }

    public void removeAllPersistentCategories() {
    	for (PersistentCategory category : getPersistentCategoryList()) {
    		category.getPersistentAudioItemList().remove(this);
    	}
        getPersistentCategoryList().clear();
    }

    public List<PersistentTag> getPersistentTagList() {
        return persistentTagList;
    }

    public void setPersistentTagList(List<PersistentTag> persistentTagList) {
        this.persistentTagList = persistentTagList;
    }

    public PersistentTag addPersistentAudioItemTag(PersistentTag persistentTag) {
    	if (!getPersistentTagList().contains(persistentTag)) {
	        getPersistentTagList().add(persistentTag);
	        persistentTag.getPersistentAudioItemList().add(this);
    	}
        return persistentTag;
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
    
    public List<PersistentLocalizedAudioItem> getPersistentLocalizedAudioItems() {
        return persistentLocalizedAudioItemList;
    }

    public PersistentLocalizedAudioItem addPersistentLocalizedAudioItem(PersistentLocalizedAudioItem persistentLocalizedAudioItem) {
        getPersistentLocalizedAudioItems().add(persistentLocalizedAudioItem);
        persistentLocalizedAudioItem.setPersistentAudioItem(this);
        return persistentLocalizedAudioItem;
    }

    public PersistentLocalizedAudioItem removePersistentLocalizedAudioItem(PersistentLocalizedAudioItem persistentLocalizedAudioItem) {
        getPersistentLocalizedAudioItems().remove(persistentLocalizedAudioItem);
        persistentLocalizedAudioItem.setPersistentAudioItem(null);
        return persistentLocalizedAudioItem;
    }

    public static Collection<PersistentAudioItem> getFromDatabase() {
        return PersistentQueries.getPersistentObjects(PersistentAudioItem.class);
    }
    
    public static PersistentAudioItem getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentAudioItem.class, id);
    }
    
    public static PersistentAudioItem getFromDatabase(String uuid) {
        EntityManager em = Persistence.getEntityManager();
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
    
    public static List<PersistentAudioItem> getFromDatabaseBySearch(String searchFilter, 
    		List<PersistentCategory> categories, List<PersistentLocale> locales) {
    	return PersistentQueries.searchForAudioItems(searchFilter, categories, locales);
    }
    
    public static List<PersistentAudioItem> getFromDatabaseBySearch(String searchFilter, 
    		PersistentTag selectedTag) {
    	return PersistentQueries.searchForAudioItems(searchFilter, selectedTag);
    }  

}
