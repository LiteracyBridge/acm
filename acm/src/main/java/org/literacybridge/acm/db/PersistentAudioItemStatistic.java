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

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.types.MetadataStatisticsField;

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
    
    @Column(name="boot_cycle_number")
    private int boot_cycle_number;
    
	@Column(name="lb_copy_count")
    private int lb_copy_count = 0;
    
    @Column(name="lb_open_count")
    private int lb_open_count = 0;    

    @Column(name="lb_completion_count")
    private int lb_completion_count = 0;    

    @Column(name="lb_survey1_count")
    private int lb_survey1_count = 0;    

    @Column(name="lb_apply_count")
    private int lb_apply_count = 0;    

    @Column(name="lb_useless_count")
    private int lb_useless_count = 0;    
    
    @ManyToOne
    @JoinColumn(name = "metadata")
    private PersistentMetadata persistentMetadata;
    
    public PersistentAudioItemStatistic() { }
    
    protected PersistentAudioItemStatistic(String deviceId, int bootCycleNumber) {
    	this();
    	this.setDeviceID(deviceId);
    	this.setBootCycleNumber(bootCycleNumber);    	
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
	
	public void setBootCycleNumber(int bootCycleNumber) {
		this.boot_cycle_number = bootCycleNumber;
	}
	
	public int getBootCycleNumber() {
		return boot_cycle_number;
	}

	public int getStatistic(MetadataStatisticsField statisticsField) {
		if (statisticsField.equals(MetadataSpecification.LB_APPLY_COUNT)) {
			return lb_apply_count;
		} else if (statisticsField.equals(MetadataSpecification.LB_COMPLETION_COUNT)) {
			return lb_completion_count;
		} else if (statisticsField.equals(MetadataSpecification.LB_COPY_COUNT)) {
			return lb_copy_count;
		} else if (statisticsField.equals(MetadataSpecification.LB_OPEN_COUNT)) {
			return lb_open_count;
		} else if (statisticsField.equals(MetadataSpecification.LB_SURVEY1_COUNT)) {
			return lb_survey1_count;
		} else if (statisticsField.equals(MetadataSpecification.LB_NOHELP_COUNT)) {
			return lb_useless_count;
		}		
		
		throw new IllegalArgumentException("Unknown statistics field: " + statisticsField.getName());
	}

	public void setStatistic(MetadataStatisticsField statisticsField, int count) {
		if (statisticsField.equals(MetadataSpecification.LB_APPLY_COUNT)) {
			lb_apply_count = count;
		} else if (statisticsField.equals(MetadataSpecification.LB_COMPLETION_COUNT)) {
			lb_completion_count = count;
		} else if (statisticsField.equals(MetadataSpecification.LB_COPY_COUNT)) {
			lb_copy_count = count;
		} else if (statisticsField.equals(MetadataSpecification.LB_OPEN_COUNT)) {
			lb_open_count = count;
		} else if (statisticsField.equals(MetadataSpecification.LB_SURVEY1_COUNT)) {
			lb_survey1_count = count;
		} else if (statisticsField.equals(MetadataSpecification.LB_NOHELP_COUNT)) {
			lb_useless_count = count;
		} else {
			throw new IllegalArgumentException("Unknown statistics field: " + statisticsField.getName());
		}
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
        EntityManager em = ACMConfiguration.getCurrentDB().getEntityManager();
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
