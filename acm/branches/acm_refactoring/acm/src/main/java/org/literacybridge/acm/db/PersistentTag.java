package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;

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
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.literacybridge.acm.config.ACMConfiguration;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentTag.findAll", query = "select o from PersistentTag o")
})
@Table(name = "t_tag")
public class PersistentTag extends PersistentObject {
	private static final String COLUMN_VALUE = "gen_tag";

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

    @Column(name="name")
    private String name;    
    
    @ManyToMany
    @JoinTable(
        name = "t_audioitem_has_tag",
        inverseJoinColumns = {
            @JoinColumn(name = "audioitem", referencedColumnName = "id"),
        },
        joinColumns =
            @JoinColumn(name = "tag", referencedColumnName = "id")
    )
    private List<PersistentAudioItem> persistentAudioItemList = new LinkedList<PersistentAudioItem>();
    
    public PersistentTag() {
    }

    public PersistentTag(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
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

    public String getName() {
        return name;
    }

    public void setTitle(String name) {
        this.name = name;
    }

    public List<PersistentAudioItem> getPersistentAudioItemList() {
    	return persistentAudioItemList;
    }
    
    public static PersistentTag getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentTag.class, id);
    }
    
    public static PersistentTag getFromDatabase(String uuid) {
        EntityManager em = ACMConfiguration.getCurrentDB().getEntityManager();
        PersistentTag result = null;
        try {
            Query findObject = em.createQuery("SELECT o FROM PersistentTag o WHERE o.uuid = '" + uuid + "'");
            result = (PersistentTag) findObject.getSingleResult();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return result;
    }
    
    public static List<PersistentTag> getFromDatabase() {
    	return PersistentQueries.getPersistentObjects(PersistentTag.class);
    }
    
    public String toString() {
    	return uuid;
    }
}
