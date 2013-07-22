package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentCategory.findAll", query = "select o from PersistentCategory o")
})
@Table(name = "t_category")
public class PersistentCategory extends PersistentObject {
   
	private static final long serialVersionUID = -126026515543050565L;

	private static final String COLUMN_VALUE = "gen_category";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;    
    
    @Column(name="uuid", nullable = false)
    private String uuid;    

    @Column(name="revision")
    private Integer revision;    

    @Column(name="ordering")
    private Integer order;    
    
    @ManyToOne
    @JoinColumn(name = "lang_desc")
    private PersistentString persistentDescriptionString;    
    
    @ManyToOne
    @JoinColumn(name = "lang_title")
    private PersistentString persistentTitleString;
    
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "parent")
    private PersistentCategory persistentParentCategory;
    
    @OneToMany(mappedBy = "persistentParentCategory", cascade = CascadeType.ALL)
    private List<PersistentCategory> persistentChildCategoryList = new LinkedList<PersistentCategory>();

    @ManyToMany(mappedBy = "persistentCategoryList")
    private List<PersistentAudioItem> persistentAudioItemList = new LinkedList<PersistentAudioItem>();

    public PersistentCategory() {
    }

    public PersistentCategory(PersistentString title, PersistentString desc, String uuid) {
        this.persistentTitleString = title;
        this.persistentDescriptionString = desc;
        this.uuid = uuid;
    }

    public Integer getId() {
        return id;
    }  

    public Integer getRevision() {
    	return revision;
    }
    
    public void setRevision(Integer revision) {
    	this.revision = revision;
    }

    public Integer getOrder() {
    	return order == null ? 0 : order.intValue();
    }
    
    public void setOrder(Integer order) {
    	this.order = order;
    }
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public PersistentString getDescription() {
        return persistentDescriptionString;
    }

    public void setDescription(PersistentString description) {
        this.persistentDescriptionString = description;
    }

    public PersistentString getTitle() {
        return persistentTitleString;
    }

    public void setTitle(PersistentString title) {
        this.persistentTitleString = title;
    }

    public PersistentCategory getPersistentParentCategory() {
        return persistentParentCategory;
    }

    public void setPersistentParentCategory(PersistentCategory persistentParentCategory) {
        this.persistentParentCategory = persistentParentCategory;
    }

    public List<PersistentCategory> getPersistentChildCategoryList() {
        return persistentChildCategoryList;
    }

    public void setPersistentChildCategoryList(List<PersistentCategory> persistentChildCategoryList) {
        this.persistentChildCategoryList = persistentChildCategoryList;
    }

    public PersistentCategory addPersistentChildCategory(PersistentCategory persistentCategory) {
        getPersistentChildCategoryList().add(persistentCategory);
        persistentCategory.setPersistentParentCategory(this);
        return persistentCategory;
    }

    public PersistentCategory removePersistentChildCategory(PersistentCategory persistentCategory) {
        getPersistentChildCategoryList().remove(persistentCategory);
        persistentCategory.setPersistentParentCategory(null);
        return persistentCategory;
    }
    
    public void clearPersistentChildCategories() {
    	for (PersistentCategory child : getPersistentChildCategoryList()) {
    		child.setPersistentParentCategory(null);
    	}
    	getPersistentChildCategoryList().clear();
    }

    public List<PersistentAudioItem> getPersistentAudioItemList() {
        return persistentAudioItemList;
    }
    
    public static PersistentCategory getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentCategory.class, id);
    }
    
    public static PersistentCategory getFromDatabase(String uuid) {
        EntityManager em = Persistence.getEntityManager();
        PersistentCategory result = null;
        try {
            Query findObject = em.createQuery("SELECT o FROM PersistentCategory o WHERE o.uuid = '" + uuid + "'");
            result = (PersistentCategory) findObject.getSingleResult();
        } catch (NoResultException e) {
            try {
            	// cannot find category id; use the Other Messages Category as backup
                Query findObject = em.createQuery("SELECT o FROM PersistentCategory o WHERE o.uuid = '0-0'");
                result = (PersistentCategory) findObject.getSingleResult();
                System.out.println("  assigning Other category to this audioItem due to nonexistent category id ('" + uuid + "')");
            } catch (NoResultException nre) {
            	nre.printStackTrace();
            // do nothing
            }
        } finally {
            em.close();
        }
        return result;
    }
    
    public String toString() {
    	return uuid;
    }
    
    public static Map<Integer, Integer> getFacetCounts(String filter, List<PersistentCategory> categories, List<PersistentLocale> locales) {
        return PersistentQueries.getCategoryFacetCounts(filter, categories, locales);
    }    
}
