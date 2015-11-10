package org.literacybridge.acm.db;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.literacybridge.acm.store.MetadataStore.Transaction;
import org.literacybridge.acm.store.Persistable;


@MappedSuperclass
/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
abstract class PersistentObject implements Serializable, Persistable {
    @Transient
    private Logger mLogger = Logger.getLogger(getClass().getName());

    private static final long serialVersionUID = 1L;
    protected static final int INITIAL_VALUE = 0;
    protected static final int ALLOCATION_SIZE = 1;
    protected static final String SEQUENCE_TABLE_NAME = "t_sequence";
    protected static final String SEQUENCE_KEY = "seq_name";
    protected static final String SEQUENCE_VALUE = "seq_count";

    public abstract Object getId();

    @Override
    public void commitTransaction(Transaction t) {
        throw new UnsupportedOperationException("Writing to Derby DB is not supported anymore.");
    }

    @Override
    public int hashCode() {
        return (getId() != null) ? getId().hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        } else {
            if (this.getClass().getName().equals(object.getClass().getName())) {
                PersistentObject other = (PersistentObject) object;
                if ((this.getId() != null && other.getId() != null)
                        && (this.getId().equals(other.getId()))) {
                    return true;
                }
            }
        }
        return super.equals(object);
    }

    @Override
    public String toString() {
        return "[id=" + getId() + "]" + this.getClass().toString();
    }
}
