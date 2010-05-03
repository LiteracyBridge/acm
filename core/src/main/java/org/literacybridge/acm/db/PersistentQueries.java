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
	static List<PersistentAudioItem> searchForAudioItems(String filter, List<PersistentCategory> categories) {
    	if (filter == null || filter.isEmpty()) {
    		if (categories == null || categories.isEmpty()) {
    			return getPersistentObjects(PersistentAudioItem.class);
    		} else {
    			return searchForAudioItems(categories);
    		}
    	} else if (categories == null || categories.isEmpty()) {
    		return searchForAudioItems(filter);
    	}
    	
        EntityManager em = Persistence.getEntityManager();
        List<PersistentAudioItem> searchResults = new LinkedList<PersistentAudioItem>();
        try {
        	StringBuilder query = 
        		new StringBuilder("SELECT DISTINCT t0.id AS \"id\", t0.uuid AS \"uuid\" " 
        				       + "FROM t_localized_audioitem t1, t_metadata t2, t_locale t3, (t_audioitem t0 LEFT OUTER JOIN t_audioitem_has_category tc " 
        				       + "ON t0.id=tc.audioitem) "
        				       + "WHERE t0.id=t1.audioitem AND t1.metadata=t2.id AND t1.language=t3.id ");
        	String[] tokens = filter.split(" ");
        	for (int i=0; i < tokens.length; i++) {
	    		query.append(" AND (lower(t2.dc_creator) LIKE lower('%" + tokens[i] + "%')" 
	                             + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')" 
	                             + "  OR lower(t3.language) LIKE lower('%" + tokens[i] + "%'))");
        	}
        	if (categories != null && !categories.isEmpty()) {
        		query.append(" AND (");
        		appendCategoryClause(query, categories);
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
    
    @SuppressWarnings("unchecked")
	static List<PersistentAudioItem> searchForAudioItems(String filter) {
        EntityManager em = Persistence.getEntityManager();
        List<PersistentAudioItem> searchResults = new LinkedList<PersistentAudioItem>();
        try {
        	StringBuilder query = 
        		new StringBuilder("SELECT DISTINCT t0.id AS \"id\", t0.uuid AS \"uuid\" " 
        				       + "FROM t_audioitem t0, t_localized_audioitem t1, t_metadata t2, t_locale t3 "
        				       + "WHERE t0.id=t1.audioitem AND t1.metadata=t2.id AND t1.language=t3.id ");
        	String[] tokens = filter.split(" ");
        	for (int i=0; i < tokens.length; i++) {
	    		query.append(" AND (lower(t2.dc_creator) LIKE lower('%" + tokens[i] + "%')" 
	                             + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')" 
	                             + "  OR lower(t3.language) LIKE lower('%" + tokens[i] + "%'))");
        	}

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
    
    @SuppressWarnings("unchecked")
	static List<PersistentAudioItem> searchForAudioItems(List<PersistentCategory> categories) {
        EntityManager em = Persistence.getEntityManager();
        List<PersistentAudioItem> searchResults = new LinkedList<PersistentAudioItem>();
        try {
        	StringBuilder query = 
        		new StringBuilder("SELECT DISTINCT t0.id AS \"id\", t0.uuid AS \"uuid\" " 
        				       + "FROM t_audioitem t0 LEFT OUTER JOIN t_audioitem_has_category tc " 
        				       + "ON t0.id=tc.audioitem "
        				       + "WHERE ");
        	appendCategoryClause(query, categories);
            Query foundAudioItems = em.createNativeQuery(query.toString(), PersistentAudioItem.class);
            searchResults = foundAudioItems.getResultList();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return searchResults;	
    }    
    
    @SuppressWarnings("unchecked")
	static Map<Integer, Integer> getFacetCount() {
        EntityManager em = Persistence.getEntityManager();
        Map<Integer, Integer> results = new HashMap<Integer, Integer>();
        try {
        	String query = "SELECT t1.id AS \"id\", COUNT(t0.category) AS \"count\" " 
        			     + "FROM t_audioitem_has_category t0 RIGHT OUTER JOIN t_category t1 " 
        			     + "ON t0.category=t1.id GROUP BY t1.id, t0.category";
            Query facetCount = em.createNativeQuery(query);
            List<Object[]> counts = facetCount.getResultList();
            for (Object[] count : counts) {
            	results.put(Integer.parseInt(count[0].toString()), 
            			Integer.parseInt(count[1].toString()));
            }
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return results;	    	
    }
}
