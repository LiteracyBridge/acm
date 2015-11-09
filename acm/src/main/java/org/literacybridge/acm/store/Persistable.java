package org.literacybridge.acm.store;

import javax.persistence.EntityManager;

public interface Persistable {
    /**
     * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
     *              for storing and searching all metadata.
     */
    @Deprecated
    <T> T commit(EntityManager em);

    //<T extends Transaction> void commitTransaction(T t);

    /**
     * Deletes the persistent object from the database.
     */
    @Deprecated
    void destroy();

    /**
     * Overrides the state of the runtime object with the current state in the database.
     *
     * @return an instance to the refreshed persistent object
     */
    @Deprecated
    <T> T refresh();
}
