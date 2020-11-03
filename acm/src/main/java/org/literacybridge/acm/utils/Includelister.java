package org.literacybridge.acm.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class to implement an "includelist" based on lines in a file. Strings can then be tested as to
 * whether they are included in the includelist, ie, are they "included".
 * 
 * Optionally, some lines may be prefixed with a negation character, in which case they become
 * part of an "excludelist".
 * 
 * If there is an excludelist, anything appearing in it is never "included".
 * If there is an includelist, only things appearing in it are "included".
 * 
 * Thus, if an item is in both the excludelist and the includelist, it is excluded.
 */
public class Includelister {
    public enum OPTIONS {
        regex,          // treat values as regular expressions
        caseSensitive,  // Values are case sensitive. Default case-insensitive.
        emptyImpliesAll //
    };

    /**
     * Simple helper class to ensure case insensitivity.
     * @param <T> Can only be String.
     */
    protected static class CaseInsensitiveSet<T extends String> extends TreeSet<String> {
        CaseInsensitiveSet() {
            super(String.CASE_INSENSITIVE_ORDER);
        }
    }

    protected final Set<String> includedItems;
    protected final Set<String> excludedItems;
    protected final Set<OPTIONS> options;

    /**
     * Construct the Includelister with the given file.
     * @param includelistFile The file containing include- and/or exclude-list items.
     */
    public Includelister(File includelistFile, OPTIONS... options) {
        this.options = new HashSet<OPTIONS>(Arrays.asList(options));
        Set<String> wl = null;
        Set<String> bl = null;
        if (includelistFile != null && includelistFile.exists()) {
            Set<String> idStrings = new HashSet<>();
            try {
                IOUtils.readLines(includelistFile, idStrings);
                for (String item : idStrings) {
                    // if the item string starts with '!', '~', or '-', exclude the item.
                    if ("!~-".indexOf(item.charAt(0))>=0) {
                        bl = addToList(bl, item.substring(1).trim());
                    } else {
                        wl = addToList(wl, item);
                    }
                }
                // If there were no include lines and no exclude lines:
                //   if the option 'emptyImpliesAll' is set then
                //      behave as if there is no includelist file, and include everything
                //   otherwise
                //      treat this as an empty includelist. That is, nothing will be included.
                if (wl == null && bl == null && !this.options.contains(OPTIONS.emptyImpliesAll)) {
                    wl = new TreeSet<>();
                }
            } catch (IOException ex) {
                // If we can't read the includelist file, include everything, exclude nothing.
                wl = null;
                bl = null;
            }
        }
        this.includedItems = wl;
        this.excludedItems = bl;
    }

    /**
     * Is the includelist possibly going to do anything?
     * @return true if it *might* perform filtering.
     */
    public boolean hasFilters() {
        return includedItems != null || excludedItems != null;
    }

    private Set<String> addToList(Set<String> items, String item) {
        if (items == null) {
            if (options.contains(OPTIONS.caseSensitive)) {
                items = new TreeSet<>();
            } else {
                items = new CaseInsensitiveSet<>();
            }
        }
        items.add(item);
        return items;
    }

    /**
     * Determine whether an item is included, based on a possible excludelist and a possible
     * includelist. The determination is made as follows:
     * - If there is an excludelist, and the item is in the excludelist, it is excluded.
     * - If there is an includelist, and the item is in the includelist, it is included.
     * - If there is an includelist, and the item is not in the includelist, it is excluded.
     * - Otherwise the item is not excluded, and there is no includelist, so it is included.
     * Notice that the excludelist takes priority over the includelist.
     * @param item The item of the category.
     * @return true if the category should be included.
     */
    public boolean isIncluded(String item) {
        if (excludedItems != null && isMatch(excludedItems, item)) {
            return false;
        }
        return includedItems == null || isMatch(includedItems, item);
    }

    private boolean isMatch(Set<String> items, String candidate) {
        if (options.contains(OPTIONS.regex)) {
            // Treat the list as regular expressions, and see if any match. This lets
            // us write "([a-z]*-)?201[3-5]-.*" to match 2013-, 14-, 15-, with or
            // without a prefix.
            for (String item : items) {
                String regex = (options.contains(OPTIONS.caseSensitive) ? "" : "(?i)") + item;
                // Make the test case insensitive
                if (candidate.matches(regex)) {
                    return true;
                }
            }
            return false;
        } else {
            return items.contains(candidate);
        }
    }

}
