package org.literacybridge.acm.db;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentLocale.findAll", query = "select o from PersistentLocale o")
})
@Table(name = "t_locale")
public class PersistentLocale extends PersistentObject implements Serializable {

    private static final String COLUMN_VALUE = "gen_locale";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;

    @Column(name="country")
    private String country;
    
    @Column(name="description")
    private String description;

    @Column(name="language")
    private String language;
    
    @OneToMany(mappedBy = "persistentLocale")
    private List<PersistentMetadata> persistentMetadataList = new LinkedList<PersistentMetadata>();
    
    @OneToMany(mappedBy = "persistentLocale")
    private List<PersistentLocalizedAudioItem> persistentLocalizedAudioItemList = new LinkedList<PersistentLocalizedAudioItem>();

    @OneToMany(mappedBy = "persistentLocale")
    private List<PersistentLocalizedString> persistentLanguageStringList = new LinkedList<PersistentLocalizedString>();

    public PersistentLocale() {
    }

    public Integer getId() {
        return id;
    }  

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<PersistentMetadata> getPersistentMetadataList() {
        return persistentMetadataList;
    }

    private void setPersistentMetadataList(List<PersistentMetadata> persistentMetadataList) {
        this.persistentMetadataList = persistentMetadataList;
    }

    public List<PersistentLocalizedAudioItem> getPersistentLocalizedAudioItemList() {
        return persistentLocalizedAudioItemList;
    }

    private void setPersistentLocalizedAudioItemList(List<PersistentLocalizedAudioItem> persistentLocalizedAudioItemList) {
        this.persistentLocalizedAudioItemList = persistentLocalizedAudioItemList;
    }
    
    public List<PersistentLocalizedString> getPersistentLanguageStringList() {
        return persistentLanguageStringList;
    }

    private void setPersistentLanguageStringList(List<PersistentLocalizedString> persistentLanguageStringList) {
        this.persistentLanguageStringList = persistentLanguageStringList;
    }    
}
