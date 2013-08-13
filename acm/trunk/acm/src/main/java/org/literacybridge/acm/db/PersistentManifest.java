package org.literacybridge.acm.db;

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
public class PersistentManifest extends PersistentObject {

	private static final long serialVersionUID = -3571849650554199446L;

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
    
    public static PersistentManifest getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentManifest.class, id);
    }     
}
