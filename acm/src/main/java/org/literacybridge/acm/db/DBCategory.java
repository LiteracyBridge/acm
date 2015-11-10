package org.literacybridge.acm.db;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
abstract class DBCategory {
    public static String idToUid(int id) {
        PersistentCategory category = PersistentCategory.getFromDatabase(id);
        if (category == null) {
            return null;
        }
        return category.getUuid();
    }

    public static Integer uidToId(String uuid) {
        PersistentCategory category = PersistentCategory.getFromDatabase(uuid);
        if (category == null) {
            return null;
        }
        return category.getId();
    }

}
