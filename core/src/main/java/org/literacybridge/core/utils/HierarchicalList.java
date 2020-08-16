package org.literacybridge.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class HierarchicalList<T extends IHierarchicalRecord> extends ArrayList<T>
    implements IHierarchicalList<T> {
    private final HierarchyInfo<T> info;
    private Node tree;

    public HierarchicalList(HierarchyInfo<T> info) {
        this.info = info;
    }
    public HierarchicalList(Collection<T> initialValues, HierarchyInfo<T> info) {
        super(initialValues);
        this.info = info;
    }

    /**
     * The Hierarchy consists of a tree of nodes; each node is a Map<String, Node>
     * where the keys are the values of the fields at that level. In the leaf nodes,
     * there is an item object, which an actual record of data.
     */
    private class Node extends HashMap<String, Node> {
        public final T item;
        Node() {this.item = null;}
        Node(T item) {this.item = item;}

        /**
         * Given a path, relative to this node, find the corresponding child node.
         * @param path to be found.
         * @return the node at that path, or null if no such node.
         */
        private Node childNodeAtPath(List<String> path) {
            // If there is no path to follow, we're done.
            if (path.size() == 0) return this;
            // See if the first part of the path has any members.
            String key = path.get(0);
            Node child = this.get(key);
            if (child == null) return null;  // no such child.
            // Follow the path.
            return child.childNodeAtPath(path.subList(1, path.size()));
        }

        /**
         * Gets the items within this node of the hierarchy.
         * @return a List of the items.
         */
        private List<T> items() {
            List<T> result = new ArrayList<>();
            if (item != null) {
                // This node has an item, so it has no children. The item is the result.
                result.add(item);
            } else {
                // This node has children. They might be leaf nodes, or might not. If they
                // are, the item check lets us return them all with a single ArrayList allocation.
                for (Node h : this.values()) {
                    if (h.item != null) {
                        result.add(h.item);
                    } else {
                        result.addAll(h.items());
                    }
                }
            }
            return result;
        }

        @Override
        public Node put(String key, Node value) {
            if (item != null) throw new IllegalStateException("Attempt to add child to leaf node");
            return super.put(key, value);
        }
    }

    /**
     * Given a list of records implementing IHierarchicalRecord, and such that the records
     * are all unique, construct a tree from the list, using the values returned by
     * IHierarchicalRecord.getValue(level) as the keys at each level.
     *
     * The leaf nodes store the record whose values compise the path to the node.
     *
     * This wastes an object reference in all of the non-leaf nodes, but the alternative
     * is an awful type like:
     *   class H extends Map<String, Map<String, Map<String, Map<String, T>>>>
     * If you, dear reader, think of a better way, please implement it.
     * @return the tree.
     */
    private Node asTree() {
        Node tree = new Node();
        // For every T item in the List<T>...
        for (T item : this) {
            // Starting with the root.
            Node container = tree;
            // For each of the successively deeper non-leaf levels in the hierarchy...
            for (int il=0; il<info.MAX_LEVEL; il++) {
                // Get the value of the current record at that level.
                String key = item.getValue(il);
                // Get the node for the value, creating it if needed.
                Node nested = container.get(key);
                if (nested == null) container.put(key, nested=new Node());
                container = nested;
            }
            // Finally, we've navigated to the level that contains leaf nodes.
            String key = item.getValue(info.MAX_LEVEL);
            Node leaf = container.get(key);
            if (leaf != null) throw new IllegalStateException("Duplicates found in list");
            container.put(key, new Node(item));
        }
        return tree;
    }

    /**
     * Internal worker for finding levels with multiple choices.
     * @param node Sub-tree to check for multiples.
     * @param curLevel Level currently under examination.
     * @param hasMultiples boolean[] to be filled in with 'true' at levels with multiple choices.
     */
    private void findLevelsWithMultipleChoices(Node node, int curLevel, boolean[] hasMultiples) {
        hasMultiples[curLevel] = hasMultiples[curLevel] || (node.size() > 1);
        if (curLevel < info.MAX_LEVEL) {
            for (Node n : node.values()) {
                findLevelsWithMultipleChoices(n, curLevel + 1, hasMultiples);
            }
        }
    }

    /**
     * Determine which levels have multiple choices. In this case "multiple-choices" means that
     * at least one node at some given level has multiple children. If every node at the level has
     * only one child, then that level does not have multiple choices. Note that whether the
     * children are the same or different is irrelevant (in general, they'll be different, anyway).
     * @return an array of booleans, with a value of true for every level with multiple choices.
     */
    boolean[] findLevelsWithMultipleChoices() {
        boolean[] result = new boolean[info.LEVELS];
        findLevelsWithMultipleChoices(getTree(), 0, result);
        return result;
    }

    /**
     * Lazy creator for the tree.
     * @return the root node of the tree.
     */
    synchronized private Node getTree() {
        if (tree == null) {
            tree = this.asTree();
        }
        return tree;
    }

    private Node nodeAtPath(List<String> path) {
        return getTree().childNodeAtPath(path);
    }

    @Override
    public Collection<String> getChildrenOfPath(List<String> parentKeys) {
        Node node = nodeAtPath(parentKeys);
        return node==null ? null : new TreeSet<>(node.keySet());
    }

    @Override
    public T getItemAtPath(List<String> path) {
        Node node = nodeAtPath(path);
        return node!=null ? node.item : null;
    }

    @Override
    public List<T> getItemsAtPath(List<String> path) {
        Node node = nodeAtPath(path);
        return node.items();
    }

    @Override
    public List<String> getPathForItem(T item) {
        List<String> path = new ArrayList<>();
        for (int level=0; level<6; level++) {
            path.add(item.getValue(level));
        }
        return path;
    }

    @Override
    public int getLevels() { return info.LEVELS; }
    @Override
    public int getMaxLevel() {
        return info.MAX_LEVEL;
    }

    @Override
    public String getNameOfLevel(int level) {
        return info.NAMES[level];
    }

    @Override
    public String getPluralOfLevel(int level) {
        return info.PLURALS[level];
    }

    public boolean isOmittable(int level) {
        return info.isOmittable(level);
    }
}
