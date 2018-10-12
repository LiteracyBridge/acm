package org.literacybridge.core.utils;

/**
 * Objects with properties that are levels in a hierarchy (eg, country/state/county) can
 * implement this and then a list of those objects can be exposed as a HierarchicalList.
 */
public interface IHierarchicalRecord {
    /**
     * For the given level of the hierarchy, return this object's value at that level.
     * @param level in the hierarchy.
     * @return the value at the level.
     */
    String getValue(int level);
}
