package org.literacybridge.acm.db;

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
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentAudioItemStatistic.findAll", query = "select o from PersistentAudioItemStatistic o")
})
@Table(name = "t_audioitem_statistic")
public class PersistentAudioItemStatistic extends PersistentObject {

	private static final long serialVersionUID = -976609359839768497L;

	private static final String COLUMN_VALUE = "gen_audioitem_statistic";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;

    @Column(name="device_id")
    private String device_id;    
    
	@Column(name="lb_copy_count")
    private int lb_copy_count = 0;
    
    @Column(name="lb_play_count")
    private int lb_play_count = 0;    
    
    @ManyToOne
    @JoinColumn(name = "metadata")
    private PersistentMetadata persistentMetadata;
    
    public PersistentAudioItemStatistic() { }
    
    protected PersistentAudioItemStatistic(String deviceId, int copyCount, int playCount) {
    	this();
    	this.setDeviceID(deviceId);
    	this.setCopyCount(copyCount);
    	this.setPlayCount(playCount);
    }

    public Integer getId() {
        return id;
    }  

    public String getDeviceID() {
		return device_id;
	}

	public void setDeviceID(String deviceId) {
		device_id = deviceId;
	}

	public int getCopyCount() {
		return lb_copy_count;
	}

	public void setCopyCount(int lbCopyCount) {
		lb_copy_count = lbCopyCount;
	}

	public int getPlayCount() {
		return lb_play_count;
	}

	public void setPlayCount(int lbPlayCount) {
		lb_play_count = lbPlayCount;
	}    
    
    public PersistentMetadata getPersistentMetadata() {
        return persistentMetadata;
    }

    public void setPersistentMetadata(PersistentMetadata persistentMetadata) {
        this.persistentMetadata = persistentMetadata;
    }
    
    public static PersistentAudioItemStatistic getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentAudioItemStatistic.class, id);
    }
    
    public static PersistentAudioItemStatistic getFromDatabase(String deviceId) {
        EntityManager em = Persistence.getEntityManager();
        PersistentAudioItemStatistic result = null;
        try {
            Query findObject = em.createQuery("SELECT o FROM PersistentAudioItemStatistic o WHERE o.device_id = '" + deviceId + "'");
            result = (PersistentAudioItemStatistic) findObject.getSingleResult();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return result;
    }     
    
}
