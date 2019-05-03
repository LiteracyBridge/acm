package org.literacybridge.core.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class HierarchyInfo <T extends IHierarchicalRecord> {
    public final int LEVELS;
    public final int MAX_LEVEL;
    public final String[] NAMES;
    public final String[] PLURALS;
    public final boolean[] NON_OMITTABLE;

    public HierarchyInfo(String[] names) {
        this(names, names);
    }
    public HierarchyInfo(String[] names, String[] plurals) {
        this(names, plurals, new String[0]);
    }
    public HierarchyInfo(String[] names, String[] plurals, String[] requiredAlways) {
        List<String> requiredList = Arrays.asList(requiredAlways);
        if (names.length != plurals.length) {
            throw new IllegalArgumentException("names and plurals must be the same size");
        }
        NON_OMITTABLE = new boolean[names.length];
        IntStream.range(0, names.length).forEach(i->NON_OMITTABLE[i] = requiredList.contains(names[i]));
        LEVELS = names.length;
        MAX_LEVEL = LEVELS-1;
        NAMES = names;
        PLURALS = plurals;
    }

    public String name(int level) {
        if (level < 0 || level >= LEVELS) throw new IndexOutOfBoundsException();
        return NAMES[level];
    }
    public String plural(int level) {
        if (level < 0 || level >= LEVELS) throw new IndexOutOfBoundsException();
        return PLURALS[level];
    }
    public boolean isOmittable(int level) {
        if (level < 0 || level >= LEVELS) throw new IndexOutOfBoundsException();
        return !NON_OMITTABLE[level];
    }
    public int levels() {
        return LEVELS;
    }
}
