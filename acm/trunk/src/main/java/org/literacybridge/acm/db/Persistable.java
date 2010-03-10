package org.literacybridge.acm.db;

public interface Persistable {

    /** Persist a new object in the database **/
    <T> int save(T object);
    
    /** Edit an existing object **/    
    <T> boolean edit(T object);
    
    /** Delete an object from the database **/    
    <T> boolean delete(T object);
    
    /** Delete an object from the database by passing it's ID **/    
    boolean delete(int id);
    
    /** Retrieves all objects for the passed class from the database. **/    
    <T> T collect();
    
    /** Retrieve a specific object from the database **/    
    <T> T collect(int id);
}
