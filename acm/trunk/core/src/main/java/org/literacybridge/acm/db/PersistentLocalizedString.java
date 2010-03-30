package org.literacybridge.acm.db;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;

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
  @NamedQuery(name = "PersistentLocalizedString.findAll", query = "select o from PersistentLocalizedString o")
})
@Table(name = "t_localized_string")
public class PersistentLocalizedString extends PersistentObject implements Serializable {

    private static final String COLUMN_VALUE = "gen_localized_string";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id; 

    @Column(name="translation_string")
    private String translation;

    @ManyToOne
    @JoinColumn(name = "string")
    private PersistentString persistentString;

    @ManyToOne
    @JoinColumn(name = "locale")
    private PersistentLocale persistentLocale;

    public PersistentLocalizedString() {
    }

    public Integer getId() {
        return id;
    }  

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String str) {
        this.translation = str;
    }


    public PersistentLocale getPersistentLocale() {
        return persistentLocale;
    }

    private void setPersistentLocale(PersistentLocale locale) {
        this.persistentLocale = locale;
    }
    
    public PersistentString getPersistentString() {
        return persistentString;
    }

    void setPersistentString(PersistentString persistentString) {
        this.persistentString = persistentString;
    }  
    
    @Override
    public String toString() {
            return getTranslation();
    }
}
