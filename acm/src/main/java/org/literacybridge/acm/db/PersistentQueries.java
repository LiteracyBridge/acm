package org.literacybridge.acm.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Playlist;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
class PersistentQueries {

    static <T> T getPersistentObject(Class<T> objectClass, Object id) {
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
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
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
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
    public static List<PersistentAudioItem> searchForAudioItems(String filter, List<Category> categories, List<Locale> locales) {
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
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
                    query.append(" AND (lower(t2.keywords) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_identifier) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_source) LIKE lower('%" + tokens[i] + "%')"
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

    @SuppressWarnings("unchecked")
    public static List<PersistentAudioItem> searchForAudioItems(String filter, Playlist selectedPlaylist) {
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
        List<PersistentAudioItem> searchResults = new LinkedList<PersistentAudioItem>();
        try {
            // queries all audioitems
            StringBuilder query =
                    new StringBuilder("SELECT DISTINCT t0.id AS \"id\", t0.uuid AS \"uuid\" "
                            + "FROM t_localized_audioitem t1, t_metadata t2, t_locale t3, (t_audioitem t0 LEFT OUTER JOIN t_audioitem_has_tag tc "
                            + "ON t0.id=tc.audioitem) "
                            + "WHERE t0.id=t1.audioitem AND t1.metadata=t2.id AND t1.language=t3.id ");

            // queries all audioitems matching a certain string
            if (filter != null && !filter.isEmpty()) {
                String[] tokens = filter.split(" ");
                for (int i=0; i < tokens.length; i++) {
                    query.append(" AND (lower(t2.keywords) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_identifier) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_source) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t3.language) LIKE lower('%" + tokens[i] + "%'))");
                }
            }

            // queries all audioitems matching a certain category
            if (selectedPlaylist != null) {
                query.append(" AND (");
                appendTagClause(query, selectedPlaylist);
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


    private static void appendCategoryClause(StringBuilder whereClause, List<Category> categories) {
        if (categories.isEmpty()) {
            return;
        }

        Iterator<Category> it = categories.iterator();
        Category category = it.next();
        whereClause.append("tc.category = " + PersistentCategory.uidToId(category.getUuid()));

        while (it.hasNext()) {
            category = it.next();
            whereClause.append(" OR ");
            whereClause.append("tc.category = " + PersistentCategory.uidToId(category.getUuid()));
        }
    }

    private static void appendTagClause(StringBuilder whereClause, Playlist selectedPlaylist) {
        if (selectedPlaylist == null) {
            return;
        }

        whereClause.append("tc.tag = " + PersistentTag.uidToId(selectedPlaylist.getUuid()));
    }

    private static void appendLocalesClause(StringBuilder whereClause, List<Locale> locales) {
        if (locales.isEmpty()) {
            return;
        }

        Iterator<Locale> it = locales.iterator();
        Locale locale = it.next();
        whereClause.append("t3.language = '" + locale.getLanguage() + "'");

        while (it.hasNext()) {
            locale = it.next();
            whereClause.append(" OR ");
            whereClause.append("t3.language = '" + locale.getLanguage() + "'");
        }
    }


    @SuppressWarnings("unchecked")
    static Map<String, Integer> getCategoryFacetCounts(String filter, List<Category> categories, List<Locale> locales) {
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
        Map<String, Integer> results = new HashMap<String, Integer>();
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
                    query.append("     (lower(t2.keywords) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_identifier) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_source) LIKE lower('%" + tokens[i] + "%')"
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
                    query.append(PersistentCategory.uidToId(categories.get(i).getUuid()));
                }
                query.append(")");
            }


            //System.out.println(query.toString());
            Query facetCount = em.createNativeQuery(query.toString());
            List<Object[]> counts = facetCount.getResultList();
            for (Object[] count : counts) {
                //System.out.println(count[0] + " -> " + count[1]);
                results.put(PersistentCategory.idToUid((Integer) count[0]),
                        (Integer) count[1]);
            }
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return results;
    }


    @SuppressWarnings("unchecked")
    static Map<String, Integer> getLanguageFacetCounts(String filter, List<Category> categories, List<Locale> locales) {
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
        Map<String, Integer> results = new HashMap<String, Integer>();
        try {
            StringBuilder query = new StringBuilder("SELECT a.\"lang_id\" AS \"id\", COUNT(a.\"lang_id\") AS \"count\" FROM ("
                    + "SELECT DISTINCT t3.language AS \"lang_id\", t0.id AS \"audioitem_id\" "
                    + "FROM t_audioitem t0 JOIN t_localized_audioitem t1 ON t0.id=t1.audioitem "
                    + "JOIN t_metadata t2 ON t1.metadata=t2.id "
                    + "JOIN t_locale t3 ON t1.language=t3.id "
                    + "JOIN t_audioitem_has_category tc ON t0.id=tc.audioitem "
                    + "JOIN t_category t5 ON tc.category=t5.id ");

            // search string conditions
            if (filter != null && filter.length() > 0) {
                String[] tokens = filter.split(" ");
                for (int i=0; i < tokens.length; i++) {
                    if (i == 0) {
                        query.append(" WHERE ");
                    } else {
                        query.append(" AND ");
                    }
                    query.append("     (lower(t2.keywords) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_title) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_identifier) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t2.dc_source) LIKE lower('%" + tokens[i] + "%')"
                            + "  OR lower(t3.language) LIKE lower('%" + tokens[i] + "%'))");
                }
            }

            // category filter
            if (categories != null && !categories.isEmpty()) {
                query.append(" AND (");
                appendCategoryClause(query, categories);
                query.append(")");
            }

            // language filter
            if (locales != null && !locales.isEmpty()) {
                query.append(" AND (");
                appendLocalesClause(query, locales);
                query.append(")");
            }

            // grouping
            query.append(") a GROUP BY a.\"lang_id\"");

            Query facetCount = em.createNativeQuery(query.toString());
            List<Object[]> counts = facetCount.getResultList();
            for (Object[] count : counts) {
                results.put((String) count[0],
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
