package org.literacybridge.acm.db;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentManifest.findAll", query = "select o from PersistentManifest o")
})
@Table(name = "t_manifest")
public class PersistentManifest extends PersistentObject implements Serializable {

    private static final String COLUMN_VALUE = "gen_manifest";

    @TableGenerator(name = COLUMN_VALUE,
    table = PersistentObject.SEQUENCE_TABLE_NAME,
    pkColumnName = PersistentObject.SEQUENCE_KEY,
    valueColumnName = PersistentObject.SEQUENCE_VALUE,
    pkColumnValue = COLUMN_VALUE,
    allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id; 

    @OneToMany(mappedBy = "persistentManifest", cascade = CascadeType.ALL)
    private List<PersistentReferencedFile> persistentReferencedFileList = new LinkedList<PersistentReferencedFile>();
    
    @OneToOne(mappedBy = "persistentManifest")
    private PersistentLocalizedAudioItem persistentLocalizedAudioItem;

    public PersistentManifest() {
    }

    public Integer getId() {
        return id;
    }  

    public List<PersistentReferencedFile> getPersistentReferencedFileList() {
        return persistentReferencedFileList;
    }

    private void setPersistentReferencedFileList(List<PersistentReferencedFile> persistentReferencedFileList) {
        this.persistentReferencedFileList = persistentReferencedFileList;
    }

    public PersistentReferencedFile addPersistentReferencedFile(PersistentReferencedFile persistentReferencedFile) {
        getPersistentReferencedFileList().add(persistentReferencedFile);
        persistentReferencedFile.setPersistentManifest(this);
        return persistentReferencedFile;
    }

    public PersistentReferencedFile removePersistentLocalizedAudioItem(PersistentReferencedFile persistentReferencedFile) {
        getPersistentReferencedFileList().remove(persistentReferencedFile);
        persistentReferencedFile.setPersistentManifest(null);
        return persistentReferencedFile;
    }
    

    public PersistentLocalizedAudioItem getPersistentLocalizedAudioItem() {
        return persistentLocalizedAudioItem;
    }

    private void setPersistentLocalizedAudioItem(PersistentLocalizedAudioItem persistentLocalizedAudioItem) {
        this.persistentLocalizedAudioItem = persistentLocalizedAudioItem;
    }
}
