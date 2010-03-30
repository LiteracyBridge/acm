package org.literacybridge.acm.db;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentLocalizedAudioItem.findAll", query = "select o from PersistentLocalizedAudioItem o")
})
@Table(name = "t_localized_audioitem")
public class PersistentLocalizedAudioItem extends PersistentObject implements Serializable {

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
    @JoinColumn(name = "manifest")
    private PersistentManifest persistentManifest;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "metadata")
    private PersistentMetadata persistentMetadata;

    @OneToOne(cascade = {CascadeType.ALL})
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

    public PersistentManifest getPersistentManifest() {
        return persistentManifest;
    }

    public void setPersistentManifest(PersistentManifest persistentManifest) {
        this.persistentManifest = persistentManifest;
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
    
}
