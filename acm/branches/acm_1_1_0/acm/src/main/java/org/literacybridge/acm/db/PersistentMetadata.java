package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.literacybridge.acm.metadata.types.MetadataStatisticsField;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentMetadata.findAll", query = "select o from PersistentMetadata o")
})
@Table(name = "t_metadata")
public class PersistentMetadata extends PersistentObject {
 
	private static final long serialVersionUID = -4128827355718501175L;

	private static final String COLUMN_VALUE = "gen_metadata";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;    
    
    @OneToOne(mappedBy = "persistentMetadata")
    private PersistentLocalizedAudioItem persistentLocalizedAudioItem;
    
    @OneToMany(mappedBy = "persistentMetadata", cascade = {CascadeType.ALL})
    private List<PersistentAudioItemStatistic> persistentAudioItemStatisticList = new LinkedList<PersistentAudioItemStatistic>();    
    
    @Column(name="dc_title")
    private String dc_title;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "dc_language")
    private PersistentLocale persistentLocale;

    @Column(name="dc_source")
    private String dc_source;

    @Column(name="dc_publisher")
    private String dc_publisher;

    @Column(name="dc_identifier")
    private String dc_identifier;
    
    @Column(name="dc_relation")
    private String dc_relation;

    @Column(name="dtb_revision")
    private String dtb_revision;
    
	@Column(name="duration")
    private String duration;
    
    @Column(name="message_format")
    private String message_format;

    @Column(name="target_audience")
    private String target_audience;

    @Column(name="date_recorded")
    private String date_recorded;

    @Column(name="keywords")
    private String keywords;

    @Column(name="timing")
    private String timing;

    @Column(name="primary_speaker")
    private String primary_speaker;

    @Column(name="goal")
    private String goal;
    
    @Column(name="english_transcription")
    private String english_transcription;

    @Column(name="notes")
    private String notes;

    @Column(name="beneficiary")
    private String beneficiary;
    
    @Column(name="no_longer_used")
    private Integer no_longer_used;
    
    public PersistentMetadata() {
    }

    public Integer getId() {
        return id;
    } 
    
    public PersistentLocalizedAudioItem getPersistentLocalizedAudioItem() {
        return persistentLocalizedAudioItem;
    }   

    public void setPersistentLocalizedAudioItem(PersistentLocalizedAudioItem item) {
        persistentLocalizedAudioItem = item;
    }       
    
    protected List<PersistentAudioItemStatistic> getPersistentAudioItemStatistics() {
        return persistentAudioItemStatisticList;
    }

    protected PersistentAudioItemStatistic addPersistentAudioItemStatistic(PersistentAudioItemStatistic persistentAudioItemStatistic) {
        getPersistentAudioItemStatistics().add(persistentAudioItemStatistic);
        persistentAudioItemStatistic.setPersistentMetadata(this);
        return persistentAudioItemStatistic;
    }

    protected PersistentAudioItemStatistic removePersistentAudioItemStatistic(PersistentAudioItemStatistic persistentAudioItemStatistic) {
        getPersistentAudioItemStatistics().remove(persistentAudioItemStatistic);
        persistentAudioItemStatistic.setPersistentMetadata(null);
        return persistentAudioItemStatistic;
    }    
    
    public String getDc_identifier() {
        return dc_identifier;
    }

    public void setDc_identifier(String dc_identifier) {
        this.dc_identifier = dc_identifier;
    }


    public String getDc_publisher() {
        return dc_publisher;
    }

    public void setDc_publisher(String dc_publisher) {
        this.dc_publisher = dc_publisher;
    }

    public String getDc_relation() {
        return dc_relation;
    }

    public void setDc_relation(String dc_relation) {
        this.dc_relation = dc_relation;
    }

    public String getDc_source() {
        return dc_source;
    }

    public void setDc_source(String dc_source) {
        this.dc_source = dc_source;
    }

    public String getDc_title() {
        return dc_title;
    }

    public void setDc_title(String dc_title) {
        this.dc_title = dc_title;
    }

    public String getDtb_revision() {
        return dtb_revision;
    }

    public void setDtb_revision(String dtb_revision) {
        this.dtb_revision = dtb_revision;
    }
    
    public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getMessage_format() {
		return message_format;
	}

	public void setMessage_format(String message_format) {
		this.message_format = message_format;
	}

	public String getTarget_audience() {
		return target_audience;
	}

	public void setTarget_audience(String target_audience) {
		this.target_audience = target_audience;
	}

	public String getDate_recorded() {
		return date_recorded;
	}

	public void setDate_recorded(String date_recorded) {
		this.date_recorded = date_recorded;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getTiming() {
		return timing;
	}

	public void setTiming(String timing) {
		this.timing = timing;
	}

	public String getPrimary_speaker() {
		return primary_speaker;
	}

	public void setPrimary_speaker(String primary_speaker) {
		this.primary_speaker = primary_speaker;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(String goal) {
		this.goal = goal;
	}

	public String getEnglish_transcription() {
		return english_transcription;
	}

	public void setEnglish_transcription(String english_transcription) {
		this.english_transcription = english_transcription;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public void setBeneficiary(String beneficiary) {
		this.beneficiary = beneficiary;
	}
	
	public String getBeneficiary() {
		return this.beneficiary;
	}
	
	public void setNoLongerUsed(int noLongerUsed) {
		this.no_longer_used = noLongerUsed;
	}
	
	public Integer getNoLongerUsed() {
		return no_longer_used;
	}
    
    public void setStatistic(MetadataStatisticsField statisticsField, String deviceId, int bootCycleNumber, Integer count) {
        // look for existing statistics
    	boolean found = false;
    	for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	if (statistic.getDeviceID().equals(deviceId)) {
        		// only update if the new bootCycleNumber is higher
        		if (bootCycleNumber >= statistic.getBootCycleNumber()) {
        			statistic.setStatistic(statisticsField, count);
        			statistic.setBootCycleNumber(bootCycleNumber);
        		}
        		found = true;
        		break;
        	}
        }
    	if (!found) {
    		PersistentAudioItemStatistic statistic = new PersistentAudioItemStatistic(deviceId, bootCycleNumber);
			statistic.setStatistic(statisticsField, count);
			addPersistentAudioItemStatistic(statistic);
    	}
    }

    public Integer getStatistic(MetadataStatisticsField statisticsField) {
        Integer sum = 0;
        for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	sum += statistic.getStatistic(statisticsField);
        }
        return sum;    	
    }
        
    public void removeStatistic(MetadataStatisticsField statisticsField, String deviceId) {
    	for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	if (statistic.getDeviceID().equals(deviceId)) {
        		statistic.setStatistic(statisticsField, 0);
        		statistic.commit();
        		return;
        	}
        }
    }    
            
    public PersistentLocale getPersistentLocale() {
        return persistentLocale;
    }

    public void setPersistentLocale(PersistentLocale persistentLocale) {
        this.persistentLocale = persistentLocale;
    }
    
    public static PersistentMetadata getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentMetadata.class, id);
    }      
    
}
