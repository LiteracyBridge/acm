package org.literacybridge.acm.store;

import javax.persistence.EntityManager;

public interface Persistable {
    /**
     * Saves the current state of the runtime object to the database.
     *
     * @return an instance to the persistent object.
     */
    <T> T commit();

    <T> T commit(EntityManager em);

    /**
     * Deletes the persistent object from the database.
     */
    void destroy();

    /**
     * Overrides the state of the runtime object with the current state in the database.
     *
     * @return an instance to the refreshed persistent object
     */
    <T> T refresh();

}
