package org.literacybridge.core.utils;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public interface IHierarchicalList<T extends IHierarchicalRecord> {

    /**
     * Gets the names of the child nodes of the node at the given path.
     * @param parentKeys Path of the parent.
     * @return a Collection<String> of the names of the child nodes.
     */
    public Collection<String> getChildrenOfPath(List<String> parentKeys);

    /**
     * Gets the item with the given path. If less than a full path is given, the result
     * will be null. If no item exists at that path, the result will be null.
     * @param path of the desired item.
     * @return the item, or null if no item at that path, or not a full path.
     */
    public T getItemAtPath(List<String> path);

    /**
     * Gets all of the items within the given path. If a full path is given, this will
     * consist of a single item. If a partial path, it will consist of all items with that
     * partial path as the prefix to their path.
     * @param path of the desired item or items.
     * @return A list of the item or items.
     */
    public List<T> getItemsAtPath(List<String> path);

    /**
     * Given an item, return the path to the item.
     * @param item for which path is desired.
     * @return the path.
     */
    public List<String> getPathForItem(T item);

    /**
     * Gets the number of levels in the hierarchy.
     * @return the number.
     */
    public int getLevels();

    /**
     * Gets the highest level in the hierearchy (the most leaf-ward level).
     * @return the highest level.
     */
    public int getMaxLevel();

    /**
     * Gets the name of the indicated level.
     * @param level for which the name is desired.
     * @return the name.
     */
    public String getNameOfLevel(int level);
    public String getPluralOfLevel(int level);
}
