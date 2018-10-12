package org.literacybridge.core.utils;

import java.util.Collection;

public class HierarchyInfo <T extends IHierarchicalRecord> {
    public final int LEVELS;
    public final int MAX_LEVEL;
    public final String[] NAMES;
    public final String[] PLURALS;

    public HierarchyInfo(String[] names) {
        this(names, names);
    }
    public HierarchyInfo(String[] names, String[] plurals) {
        if (names.length != plurals.length) {
            throw new IllegalArgumentException("names and plurals must be the same size");
        }
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
    public int levels() {
        return LEVELS;
    }
}
