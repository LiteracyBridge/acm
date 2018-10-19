package org.literacybridge.acm.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class to implement a "whitelist" based on lines in a file. Strings can then be tested as to
 * whether they are included in the whitelist, ie, are they "included".
 * 
 * Optionally, some lines may be prefixed with a negation character, in which case they become
 * part of a "blacklist". 
 * 
 * If there is a blacklist, anything appearing in it is never "included".
 * If there is a whitelist, only things appearing in it are "included".
 * 
 * Thus, if an item is in both the blacklist and the whitelist, it is excluded.
 */
public class Whitelister {
    public enum OPTIONS {
        regex,          // treat values as regular expressions
        caseSensitive   // Values are case sensitive. Default case-insensitive.
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

    protected final Set<String> whitelistedItems;
    protected final Set<String> blacklistedItems;
    protected final Set<OPTIONS> options;

    /**
     * Construct the Whitelister with the given file.
     * @param whitelistFile The file containing white- and/or black-list items.
     */
    public Whitelister(File whitelistFile, OPTIONS... options) {
        this.options = new HashSet<OPTIONS>(Arrays.asList(options));
        Set<String> wl = null;
        Set<String> bl = null;
        if (whitelistFile != null && whitelistFile.exists()) {
            Set<String> idStrings = new HashSet<>();
            try {
                IOUtils.readLines(whitelistFile, idStrings);
                for (String item : idStrings) {
                    // if the item string starts with '!', '~', or '-', blacklist the item.
                    if ("!~-".indexOf(item.charAt(0))>=0) {
                        bl = addToList(bl, item.substring(1).trim());
                    } else {
                        wl = addToList(wl, item);
                    }
                }
                // If there were no whitelist lines and no blacklist lines, treat this as an
                // empty whitelist. That is, nothing will be included.
                if (wl == null && bl == null) {
                    wl = new TreeSet<>();
                }
            } catch (IOException ex) {
                // If we can't read the whitelist file, include everything, exclude nothing.
                wl = null;
                bl = null;
            }
        }
        this.whitelistedItems = wl;
        this.blacklistedItems = bl;
    }

    /**
     * Is the whitelist possibly going to do anything?
     * @return true if it *might* perform filtering.
     */
    public boolean hasFilters() {
        return whitelistedItems != null || blacklistedItems != null;
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
     * Determine whether an item is included, based on a possible blacklist and a possible
     * whitelist. The determination is made as follows:
     * - If there is a blacklist, and the item is in the blacklist, it is excluded.
     * - If there is a whitelist, and the item is in the whitelist, it is included.
     * - If there is a whitelist, and teh item is not in the whitelist, it is excluded.
     * - Otherwise the item is not blacklisted, and there is no whitelist, so it is included.
     * Note that the blacklist takes priority over the whitelist.
     * @param item The item of the category.
     * @return true if the category should be included.
     */
    public boolean isIncluded(String item) {
        if (blacklistedItems != null && isMatch(blacklistedItems, item)) {
            return false;
        }
        return whitelistedItems == null || isMatch(whitelistedItems, item);
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
