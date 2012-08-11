package org.literacybridge.acm.db;

import java.io.Serializable;

import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;


@MappedSuperclass
public abstract class PersistentObject implements Serializable {
    @Transient
    private Logger mLogger = Logger.getLogger(getClass().getName());
    
    private static final long serialVersionUID = 1L;
    protected static final int INITIAL_VALUE = 0;
    protected static final int ALLOCATION_SIZE = 1;
    protected static final String SEQUENCE_TABLE_NAME = "t_sequence";
    protected static final String SEQUENCE_KEY = "seq_name";
    protected static final String SEQUENCE_VALUE = "seq_count";
    
    public abstract Integer getId();
    
    @SuppressWarnings("unchecked")
	public synchronized <T> T commit() {
        mLogger.finest("Committing object " + toString());
        
        T persistentObj = null;
        EntityManager em = null;
        
        try {
            em = Persistence.getEntityManager();
            EntityTransaction t = null;
            try {
                t = em.getTransaction();
                t.begin();
                
                persistentObj = (T) em.merge(this);
                
                t.commit();
            } finally {
                if (t.isActive()) {
                    t.rollback();
                }
            }
        } finally {
            em.close();
        }
        
        return persistentObj;
    }
    
    
    public synchronized void destroy() {
        mLogger.finest("Destroying object " + toString());
        
        EntityManager em = null;
        
        try {
            em = Persistence.getEntityManager();
            EntityTransaction t = null;
            try {
                t = em.getTransaction();
                t.begin();
                
                PersistentObject managedObj = em.merge(this);
                em.remove(managedObj);
                
                t.commit();
            } finally {
                if (t.isActive()) {
                    t.rollback();
                }
            }
        } finally {
            em.close();        
        }
    }
    
    
    @SuppressWarnings("unchecked")
	public <T> T refresh() {
        if (getId() == null) {
            throw new IllegalStateException("Illegal to call refresh on an uncommitted instance.");
        }
        mLogger.finest("Refreshing object " + toString());
        T newInstance = (T) PersistentQueries.getPersistentObject(this.getClass(), getId());
        return newInstance;
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
