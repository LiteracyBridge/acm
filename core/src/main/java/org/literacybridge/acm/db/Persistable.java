package org.literacybridge.acm.db;

public interface Persistable {

    Integer getId();
    
    <T> T commit();
    
    void destroy();
    
    <T> T refresh();

}
