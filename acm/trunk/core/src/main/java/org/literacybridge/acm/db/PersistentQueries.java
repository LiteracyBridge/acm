package org.literacybridge.acm.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

class PersistentQueries {
    
	static <T> T getPersistentObject(Class<T> objectClass, int id) {
        EntityManager em = Persistence.getEntityManager();
        T persistentObject = null;
        try {
            persistentObject = em.find(objectClass, id);
        } finally {
            em.close();        
        }
        return persistentObject;
    }
    
    @SuppressWarnings("unchecked")
	static <T> List<T> getPersistentObjects(Class<T> objectClass) {
        EntityManager em = Persistence.getEntityManager();
        List<T> results = null;
        try {
            Query findObjects = em.createQuery("SELECT o FROM " + objectClass.getSimpleName() + " o");
            results = findObjects.getResultList();
            results.size();
        } finally {
            em.close();
        }
        return results;
    }    
    
    @SuppressWarnings("unchecked")
	static List<PersistentAudioItem> searchForAudioItems(String filter, List<PersistentCategory> categories, List<PersistentLocale> locales) {    	
        EntityManager em = Persistence.getEntityManager();
        List<PersistentAudioItem> searchResults = new LinkedList<PersistentAudioItem>();
        try {
        	// queries all audioitems
        	StringBuilder query = 
        		new StringBuilder("SELECT DISTINCT t0.id AS \"id\", t0.uuid AS \"uuid\" " 
        				       + "FROM t_localized_audioitem t1, t_metadata t2, t_locale t3, (t_audioitem t0 LEFT OUTER JOIN t_audioitem_has_category tc " 
        				       + "ON t0.id=tc.audioitem) "
        				       + "WHERE t0.id=t1.audioitem AND t1.metadata=t2.id AND t1.language=t3.id ");
        	
        	// queries all audioitems matching a certain string
        	if (filter != null && !filter.isEmpty()) {
	        	String[] tokens = filter.split(" ");
	        	for (int i=0; i < tokens.length; i++) {
		    		query.append(" AND (lower(t2.dc_creator) LIKE lower('%" + tokens[i] + "%')" 
		                             + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')" 
		                             + "  OR lower(t3.language) LIKE lower('%" + tokens[i] + "%'))");
	        	}
        	}
        	
        	// queries all audioitems matching a certain category
        	if (categories != null && !categories.isEmpty()) {
        		query.append(" AND (");
        		appendCategoryClause(query, categories);
        		query.append(")");
        	}
        	
        	// queries all audioitems matching a certain language
        	if (locales != null && !locales.isEmpty()) {
        		query.append(" AND (");
        		appendLocalesClause(query, locales);
        		query.append(")");
        	}
        	
        	//System.out.println("Filter=" + filter + ", query=" + query.toString());
            Query foundAudioItems = em.createNativeQuery(query.toString(), PersistentAudioItem.class);
            searchResults = foundAudioItems.getResultList();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return searchResults;	

    }
    
    private static void appendCategoryClause(StringBuilder whereClause, List<PersistentCategory> categories) {
    	if (categories.isEmpty()) {
    		return;
    	}
    	
    	Iterator<PersistentCategory> it = categories.iterator();
    	PersistentCategory category = it.next();
    	whereClause.append("tc.category = " + category.getId());
    	
    	while (it.hasNext()) {
			category = it.next();
			whereClause.append(" OR ");
			whereClause.append("tc.category = " + category.getId());
    	}
    }
    
    private static void appendLocalesClause(StringBuilder whereClause, List<PersistentLocale> locales) {
    	if (locales.isEmpty()) {
    		return;
    	}
    	
    	Iterator<PersistentLocale> it = locales.iterator();
    	PersistentLocale locale = it.next();
    	whereClause.append("t3.language = '" + locale.getLanguage() + "'");
    	
    	while (it.hasNext()) {
			locale = it.next();
			whereClause.append(" OR ");
			whereClause.append("t3.language = '" + locale.getLanguage() + "'");
    	}
    }    
    
    @SuppressWarnings("unchecked")
	static Map<Integer, Integer> getFacetCounts(String filter, List<PersistentCategory> categories, List<PersistentLocale> locales) {
        EntityManager em = Persistence.getEntityManager();
        Map<Integer, Integer> results = new HashMap<Integer, Integer>();
        try {
        	StringBuilder query = new StringBuilder("SELECT DISTINCT t5.id AS \"id\", COUNT(t4.category) AS \"count\" " 
        			                              + "FROM t_audioitem t0 JOIN t_localized_audioitem t1 ON t0.id=t1.audioitem " 
        			                              + "JOIN t_metadata t2 ON t1.metadata=t2.id " 
        			                              + "JOIN t_locale t3 ON t1.language=t3.id " 
        			                              + "JOIN t_audioitem_has_category t4 ON t0.id=t4.audioitem " 
        			                              + "RIGHT OUTER JOIN t_category t5 ON t4.category=t5.id "); 
        	
        	// search string conditions
        	if (filter != null && filter.length() > 0) {
	        	String[] tokens = filter.split(" ");
	        	for (int i=0; i < tokens.length; i++) {
	        		if (i == 0) {
	        			query.append(" WHERE ");
	        		} else {
	        			query.append(" AND ");
	        		}
		    		query.append("     (lower(t2.dc_creator) LIKE lower('%" + tokens[i] + "%')" 
		                        + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')" 
		                        + "  OR lower(t3.language) LIKE lower('%" + tokens[i] + "%'))");
	        	}
        	}
        	
        	// language filter
        	if (locales != null && !locales.isEmpty()) {
        		query.append(" AND (");
        		appendLocalesClause(query, locales);
        		query.append(")");
        	}

        	// grouping
        	query.append(" GROUP BY t5.id, t4.category ");
        	
        	// having clause (category filter)
        	if (categories != null && categories.size() > 0) {
        		query.append(" HAVING t5.id IN (");        	
        		for (int i=0; i < categories.size(); i++) {
        			if (i > 0) {
        				query.append(",");
        			}
        			query.append(categories.get(i).getId());
        		}
        		query.append(")");
        	}


        	//System.out.println(query.toString());
        	Query facetCount = em.createNativeQuery(query.toString());
            List<Object[]> counts = facetCount.getResultList();
            for (Object[] count : counts) {
            	//System.out.println(count[0] + " -> " + count[1]);
            	results.put((Integer) count[0], 
            			(Integer) count[1]);
            }
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return results;	    	
    }    
}
