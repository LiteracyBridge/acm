package org.literacybridge.acm.db;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.literacybridge.acm.store.MetadataStore.Transaction;

public class DBTransaction extends Transaction {
    private final EntityManager em;
    private final EntityTransaction transaction;

    public DBTransaction(EntityManager em) {
        this.em = em;
        this.transaction = em.getTransaction();
    }

    EntityManager getEntityManager() {
        return em;
    }

    public void begin() {
        transaction.begin();
    }

    @Override
    public void doCommit() {
        transaction.commit();
        em.close();
    }

    @Override
    public void doRollback() {
        if (transaction.isActive()) {
            transaction.rollback();
        }
        em.close();
    }
}
