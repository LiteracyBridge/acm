package org.literacybridge.acm.db;

public interface Persistable {
    /**
     * Saves the current state of the runtime object to the database.
     *
     * @return an instance to the persistent object.
     */
    <T> T commit();

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
