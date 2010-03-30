package org.literacybridge.acm.db;

import java.io.Serializable;

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
  @NamedQuery(name = "PersistentString.findAll", query = "select o from PersistentString o")
})
@Table(name = "t_string")
public class PersistentString extends PersistentObject implements Serializable {

    private static final String COLUMN_VALUE = "gen_string";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id; 

    @Column(name="context")
    private String string;

    @OneToMany(mappedBy = "persistentString", cascade = {CascadeType.ALL})
    private List<PersistentLocalizedString> persistentLocalizedStringList = new LinkedList<PersistentLocalizedString>();

    @OneToMany(mappedBy = "persistentTitleString")
    private List<PersistentCategory> persistentCategoryTitleList = new LinkedList<PersistentCategory>();
    
    @OneToMany(mappedBy = "persistentDescriptionString")
    private List<PersistentCategory> persistentCategoryDescList = new LinkedList<PersistentCategory>();



    public PersistentString() {
    }
    
    public PersistentString(String str) {
        string = str;
    }

    public Integer getId() {
        return id;
    }  

    public String getString() {
        return string;
    }

    public void setString(String str) {
        this.string = str;
    }

    public List<PersistentLocalizedString> getPersistentLocalizedStringList() {
        return persistentLocalizedStringList;
    }

    private void setPersistentLocalizedStringList(List<PersistentLocalizedString> persistentLocalizedStringList) {
        this.persistentLocalizedStringList = persistentLocalizedStringList;
    }

    public PersistentLocalizedString addPersistentLocalizedString(PersistentLocalizedString persistentLocalizedString) {
        getPersistentLocalizedStringList().add(persistentLocalizedString);
        persistentLocalizedString.setPersistentString(this);
        return persistentLocalizedString;
    }

    public PersistentLocalizedString removePersistentLocalizedString(PersistentLocalizedString persistentLocalizedString) {
        getPersistentLocalizedStringList().remove(persistentLocalizedString);
        persistentLocalizedString.setPersistentString(null);
        return persistentLocalizedString;
    }
    
    @Override
    public String toString() {
            return getString();
    }
}
