package org.literacybridge.acm.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@NamedQueries({
  @NamedQuery(name = "PersistentReferencedFile.findAll", query = "select o from PersistentReferencedFile o")
})
@Table(name = "t_referenced_file")
public class PersistentReferencedFile extends PersistentObject {

	private static final long serialVersionUID = 2256652930931429681L;

	private static final String COLUMN_VALUE = "gen_referenced_file";

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
    @ManyToOne
    @JoinColumn(name = "manifest")
    private PersistentManifest persistentManifest;

    public PersistentReferencedFile() {
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


    public PersistentManifest getPersistentManifest() {
        return persistentManifest;
    }

    void setPersistentManifest(PersistentManifest persistentManifest) {
        this.persistentManifest = persistentManifest;
    }
    
    public static PersistentReferencedFile getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentReferencedFile.class, id);
    }  
}
