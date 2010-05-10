package org.literacybridge.acm.db;

import java.sql.Timestamp;
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
    
    @Column(name="dc_contributor")
    private String dc_contributor;
    
    @Column(name="dc_coverage")
    private String dc_coverage;
    
    @Column(name="dc_creator")
    private String dc_creator;
    
    @Column(name="dc_date")
    private Timestamp dc_date;
    
    @Column(name="dc_description")
    private String dc_description;
    
    @Column(name="dc_format")
    private String dc_format;
    
    @Column(name="dc_identifier")
    private String dc_identifier;
    
    @Column(name="dc_publisher")
    private String dc_publisher;
    
    @Column(name="dc_relation")
    private String dc_relation;
    
    @Column(name="dc_rights")
    private String dc_rights;
    
    @Column(name="dc_source")
    private String dc_source;
    
    @Column(name="dc_subject")
    private String dc_subject;
    
    @Column(name="dc_title")
    private String dc_title;
    
    @Column(name="dc_type")
    private String dc_type;
    
    @Column(name="dtb_revision")
    private String dtb_revision;
    
    @Column(name="dtb_revision_date")
    private Timestamp dtb_revision_date;
    
    @Column(name="dtb_revision_description")
    private String dtb_revision_description;
        
    @Column(name="lb_rating")
    private Short lb_rating;
    
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "dc_language")
    private PersistentLocale persistentLocale;

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
    
    public String getDc_contributor() {
        return dc_contributor;
    }

    public void setDc_contributor(String dc_contributor) {
        this.dc_contributor = dc_contributor;
    }

    public String getDc_coverage() {
        return dc_coverage;
    }

    public void setDc_coverage(String dc_coverage) {
        this.dc_coverage = dc_coverage;
    }

    public String getDc_creator() {
        return dc_creator;
    }

    public void setDc_creator(String dc_creator) {
        this.dc_creator = dc_creator;
    }

    public Timestamp getDc_date() {
        return dc_date;
    }

    public void setDc_date(Timestamp dc_date) {
        this.dc_date = dc_date;
    }

    public String getDc_description() {
        return dc_description;
    }

    public void setDc_description(String dc_description) {
        this.dc_description = dc_description;
    }

    public String getDc_format() {
        return dc_format;
    }

    public void setDc_format(String dc_format) {
        this.dc_format = dc_format;
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

    public String getDc_rights() {
        return dc_rights;
    }

    public void setDc_rights(String dc_rights) {
        this.dc_rights = dc_rights;
    }

    public String getDc_source() {
        return dc_source;
    }

    public void setDc_source(String dc_source) {
        this.dc_source = dc_source;
    }

    public String getDc_subject() {
        return dc_subject;
    }

    public void setDc_subject(String dc_subject) {
        this.dc_subject = dc_subject;
    }

    public String getDc_title() {
        return dc_title;
    }

    public void setDc_title(String dc_title) {
        this.dc_title = dc_title;
    }

    public String getDc_type() {
        return dc_type;
    }

    public void setDc_type(String dc_type) {
        this.dc_type = dc_type;
    }

    public String getDtb_revision() {
        return dtb_revision;
    }

    public void setDtb_revision(String dtb_revision) {
        this.dtb_revision = dtb_revision;
    }

    public Timestamp getDtb_revision_date() {
        return dtb_revision_date;
    }

    public void setDtb_revision_date(Timestamp dtb_revision_date) {
        this.dtb_revision_date = dtb_revision_date;
    }

    public String getDtb_revision_description() {
        return dtb_revision_description;
    }

    public void setDtb_revision_description(String dtb_revision_description) {
        this.dtb_revision_description = dtb_revision_description;
    }

    public Integer getLb_copy_count() {
        Integer sum = 0;
        for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	sum += statistic.getCopyCount();
        }
        return sum;
    }

    public void setLb_copy_count(String deviceId, Integer copyCount) {
        // look for existing statistics
    	for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	if (statistic.getDeviceID().equals(deviceId)) {
        		statistic.setCopyCount(copyCount);
        		return;
        	}
        }
        // otherwise create a new one
    	addPersistentAudioItemStatistic(new PersistentAudioItemStatistic(deviceId, copyCount, 0));
    }

    public void removeLb_copy_count(String deviceId) {
    	for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	if (statistic.getDeviceID().equals(deviceId)) {
        		statistic.setCopyCount(0);
        		return;
        	}
        }
    }    
    
    public Integer getLb_play_count() {
        Integer sum = 0;
        for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	sum += statistic.getPlayCount();
        }
        return sum;
    }

    public void setLb_play_count(String deviceId, Integer playCount) {
        // look for existing statistics
    	for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	if (statistic.getDeviceID().equals(deviceId)) {
        		statistic.setPlayCount(playCount);
        		return;
        	}
        }
        // otherwise create a new one
    	addPersistentAudioItemStatistic(new PersistentAudioItemStatistic(deviceId, 0, playCount));    	
    }
    
    public void removeLb_play_count(String deviceId) {
    	for (PersistentAudioItemStatistic statistic : getPersistentAudioItemStatistics()) {
        	if (statistic.getDeviceID().equals(deviceId)) {
        		statistic.setPlayCount(0);
        		return;
        	}
        }
    }     
    
    public Short getLb_rating() {
        return lb_rating;
    }

    public void setLb_rating(Short lb_rating) {
        this.lb_rating = lb_rating;
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
