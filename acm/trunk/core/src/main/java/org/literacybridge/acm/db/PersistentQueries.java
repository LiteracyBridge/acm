package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class PersistentQueries {
    
    private static Logger mLogger = Logger.getLogger(PersistentQueries.class.getName());
    
    @SuppressWarnings("unchecked")
	static List<PersistentAudioItem> searchForAudioItems(String filter) {
    	StringBuffer whereClause = new StringBuffer();
    	String[] tokens = filter.split(" ");
    	for (int i=0; i < tokens.length; i++) {
    		whereClause.append(" AND (lower(t2.dc_creator) LIKE lower('%" + tokens[i] + "%')" 
                             + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')" 
                             + "  OR lower(t3.language) LIKE lower('%" + tokens[i] + "%'))");
    	}
        EntityManager em = Persistence.getEntityManager();
        List<PersistentAudioItem> searchResults = new LinkedList<PersistentAudioItem>();
        try {
        	StringBuffer query = 
        		new StringBuffer("SELECT t0.id AS \"id\", t0.uuid AS \"uuid\" FROM t_audioitem t0, t_localized_audioitem t1, t_metadata t2, t_locale t3 " + 
            		             "WHERE t0.id=t1.audioitem AND t1.metadata=t2.id AND t1.language=t3.id " + 
            		             whereClause);
            Query foundAudioItems = em.createNativeQuery(query.toString(), PersistentAudioItem.class);
            searchResults = foundAudioItems.getResultList();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return searchResults;	
    }
}
