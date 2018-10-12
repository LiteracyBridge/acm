package org.literacybridge.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This is a virtual HierarchicalList in which levels that don't have any choices are removed
 * from the hierarchy. Note that there may be many values in the level, but each parent only
 * has a single child.
 * @param <T> Hierarchical List of What?
 */
public class DelayeredHierarchicalList<T extends IHierarchicalRecord> extends HierarchicalList<T> {
    // We don't compute the layers until we need to. We assume that no items are added after the
    // first call to read data.
    private boolean needInit = true;
    // Map into the original levels, from the virtual levels. Used to look up names for levels.
    private int[] levelMap;
    // True if the corresponding level is omitted.
    private boolean[] omitted;
    // The virtual MAX_LEVEL.
    private int delayeredMaxLevel;


    public DelayeredHierarchicalList(HierarchyInfo<T> info) {
        super(info);
    }
    public DelayeredHierarchicalList(Collection<T> initialValues, HierarchyInfo<T> info) {
        super(initialValues, info);
        findOmittables();
    }

    /**
     * Find the levels that can be omitted.
     */
    private void findOmittables() {
        int levelsKept = 0;
        levelMap = new int[super.getLevels()];
        // The ones with multiple choices are the ones we need to keep. The others are omitted.
        delayeredMaxLevel = super.getMaxLevel();
        omitted = findLevelsWithMultipleChoices();
        for (int ix=0; ix<omitted.length; ix++) {
            // Make the value of omitted[ix] really be "omitted"
            omitted[ix] = !omitted[ix];
            if (omitted[ix]) {
                // Every omitted level reduces the count of levels.
                delayeredMaxLevel--;
            } else {
                // Kept levels are presented at a different level; this builds the mapping.
                levelMap[levelsKept++] = ix;
            }
        }
    }

    /**
     * Given a path based on the delayered view of the hierarchy, recreate the full path.
     * @param path a delayered path.
     * @return the same path, re-layered.
     */
    private List<String> reLayerPath(List<String> path) {
        List<String> result = new ArrayList<>();
        Iterator<String> it = path.iterator();

        // Build a list of up to LEVELS values (sometimes we just have a partial path).
        for (int ix=0; ix<super.getLevels(); ix++) {
            // If this level was omitted, that means there are no choices. Get the one-and-only
            // value at the level and plug it in.
            if (omitted[ix]) {
                List<String> omittedValue = new ArrayList<>(super.getChildrenOfPath(result));
                if (omittedValue.size() != 1) {
                    throw new IllegalStateException("Unexpected children found re-layering path");
                }
                result.add(omittedValue.get(0));
            } else {
                // This is not an omitted level. If there is a next from the supplied path,
                // use it. Otherwise, we've accumulated as much as we can of the path.
                if (it.hasNext()) {
                    result.add(it.next());
                } else {
                    break;
                }
            }
        }
        return result;
    }

    protected List<String> deLayerPath(List<String> path) {
        List<String> result = new ArrayList<>();
        for (int ix=0; ix<path.size(); ix++) {
            if (!omitted[ix]) {
                result.add(path.get(ix));
            }
        }
        return result;
    }

    /**
     * Lazy initialization of the omittables.
     */
    private void checkInit() {
        if (needInit) {
            findOmittables();
            needInit = false;
        }
    }

    @Override
    public Collection<String> getChildrenOfPath(List<String> parentKeys) {
        checkInit();
        List<String> delayeredKeys = reLayerPath(parentKeys);
        return super.getChildrenOfPath(delayeredKeys);
    }

    @Override
    public T getItemAtPath(List<String> path) {
        checkInit();
        List<String> delayeredPath = reLayerPath(path);
        return super.getItemAtPath(delayeredPath);
    }

    @Override
    public List<T> getItemsAtPath(List<String> path) {
        checkInit();
        List<String> delayeredPath = reLayerPath(path);
        return super.getItemsAtPath(delayeredPath);
    }

    @Override
    public List<String> getPathForItem(T item) {
        checkInit();
        List<String> path = super.getPathForItem(item);
        return deLayerPath(path);
    }

    @Override
    public int getLevels() {
        checkInit();
        return delayeredMaxLevel+1;
    }

    @Override
    public int getMaxLevel() {
        checkInit();
        return delayeredMaxLevel;
    }

    @Override
    public String getNameOfLevel(int level) {
        checkInit();
        return super.getNameOfLevel(levelMap[level]);
    }

    @Override
    public String getPluralOfLevel(int level) {
        checkInit();
        return super.getPluralOfLevel(levelMap[level]);
    }

}
