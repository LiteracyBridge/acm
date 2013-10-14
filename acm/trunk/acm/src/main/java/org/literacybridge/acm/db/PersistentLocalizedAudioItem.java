package org.literacybridge.acm.db;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.literacybridge.acm.config.ACMConfiguration;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentLocalizedAudioItem.findAll", query = "select o from PersistentLocalizedAudioItem o")
})
@Table(name = "t_localized_audioitem")
public class PersistentLocalizedAudioItem extends PersistentObject {

	private static final long serialVersionUID = -976609359839768497L;

	private static final String COLUMN_VALUE = "gen_localized_audioitem";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;

    @Column(name="location")
    private String location;
    
    @Column(name="uuid")
    private String uuid;
    
    @ManyToOne
    @JoinColumn(name = "audioitem")
    private PersistentAudioItem persistentAudioItem;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "metadata")
    private PersistentMetadata persistentMetadata;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "language")
    private PersistentLocale persistentLocale;

    public PersistentLocalizedAudioItem() {
    }

    public Integer getId() {
        return id;
    }  

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public PersistentAudioItem getPersistentAudioItem() {
        return persistentAudioItem;
    }

    void setPersistentAudioItem(PersistentAudioItem persistentAudioItem) {
        this.persistentAudioItem = persistentAudioItem;
    }

    public PersistentMetadata getPersistentMetadata() {
        return persistentMetadata;
    }

    public void setPersistentMetadata(PersistentMetadata persistentMetadata) {
        this.persistentMetadata = persistentMetadata;
    }

    public PersistentLocale getPersistentLocale() {
        return persistentLocale;
    }

    public void setPersistentLocale(PersistentLocale persistentLocale) {
        this.persistentLocale = persistentLocale;
    }
    
    
    public static PersistentLocalizedAudioItem getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentLocalizedAudioItem.class, id);
    }
    
    public static PersistentLocalizedAudioItem getFromDatabase(String uuid) {
        EntityManager em = ACMConfiguration.getCurrentDB().getEntityManager();
        PersistentLocalizedAudioItem result = null;
        try {
            Query findObject = em.createQuery("SELECT o FROM PersistentLocalizedAudioItem o WHERE o.uuid = '" + uuid + "'");
            result = (PersistentLocalizedAudioItem) findObject.getSingleResult();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return result;
    }     
    
}
