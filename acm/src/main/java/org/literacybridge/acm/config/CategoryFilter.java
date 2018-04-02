package org.literacybridge.acm.config;

import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class CategoryFilter {

    /**
     * Simple helper class to ensure case insensitivity.
     * @param <T> Can only be String.
     */
    private static class CaseInsensitiveSet<T extends String> extends TreeSet<String> {
        CaseInsensitiveSet() {
            super(String.CASE_INSENSITIVE_ORDER);
        }
    }

    public static final String WHITELIST_FILENAME = "category.whitelist";

    private CaseInsensitiveSet<String> whitelistedIds = null;
    private CaseInsensitiveSet<String> blacklistedIds = null;

    /**
     * Construct the CategoryFilter with the project directory. The whitelist file, if it exists,
     * will be in that directory.
     * @param projectDir The project directory.
     */
    public CategoryFilter(File projectDir) {
        // If allCategories is set, we just ignore the whitelist anyway.
        if (ACMConfiguration.getInstance().isAllCategories()) {
            return;
        }
        File whitelistFile = new File(projectDir, WHITELIST_FILENAME);
        if (whitelistFile.exists()) {
            Set<String> idStrings = new HashSet<>();
            try {
                IOUtils.readLines(whitelistFile, idStrings);
                for (String id : idStrings) {
                    // if the id string starts with '!', '~', or '-', blacklist the id.
                    if ("!~-".indexOf(id.charAt(0))>=0) {
                        addToBlackList(id.substring(1).trim());
                    } else {
                        addToWhiteList(id);
                    }
                }
            } catch (IOException ex) {
                // If we can't read the whitelist file, include everything, exclude nothing.
                whitelistedIds = null;
                blacklistedIds = null;
            }
        }
    }
    
    private void addToWhiteList(String id) {
        if (whitelistedIds == null) {
            whitelistedIds = new CaseInsensitiveSet<>();
        }
        whitelistedIds.add(id);
    }

    private void addToBlackList(String id) {
        if (blacklistedIds == null) {
            blacklistedIds = new CaseInsensitiveSet<>();
        }
        blacklistedIds.add(id);
    }

    /**
     * Determine whether a category id is included, based on a possible blacklist and a possible
     * whitelist. The determination is made as follows:
     * - If there is a blacklist, and the id is in the blacklist, it is excluded.
     * - If there is a whitelist, and the id is in the whitelist, it is included.
     * - If there is a whitelist, and teh id is not in the whitelist, it is excluded.
     * - Otherwise the id is not blacklisted, and there is no whitelist, so it is included.
     * Note that the blacklist takes priority over the whitelist.
     * @param id The id of the category.
     * @return true if the category should be included.
     */
    public boolean isIncluded(String id) {
        if (blacklistedIds != null && blacklistedIds.contains(id)) {
            return false;
        }
        return whitelistedIds == null || whitelistedIds.contains(id);
    }

    /**
     * Sets the visibility for the given category. Determines visibility as follows:
     * - If the category is 'nonAssignable', it is not visible.
     * - If the category's id is blacklisted, it is not visible.
     * - If there is a whitelist, then:
     * --  If the category's id is in the whitelist, it is visible.
     * --  If the category has a parent, and the parent is visible, it is visible (by inheritance).
     * --  Otherwise the category is not visible.
     * - Otherwise there is no whitelist, and:
     * --  If the category has a parent, it inherits the parent's visibility.
     * --  Otherwise this is the root category, and it is visible.
     * @param cat The category for which to set the visibility.
     *
     * SIDE EFFECT: updates the 'visible' property of the given category.
     */
    public void setVisibilityFor(Category cat) {
        if (cat.isNonAssignable()) {
            // If the category is "nonAssignable" in the taxonomy, it is not visible.
            cat.setVisible(false);
        } else if (blacklistedIds != null && blacklistedIds.contains(cat.getId())) {
            // If there's a blacklist, and this id is in it, it is not visible.
            cat.setVisible(false);
        } else if (whitelistedIds != null) {
            // If a whitelist exists, this is visible if in the whitelist, or if
            // parent is visible.
            Category parent = cat.getParent();
            boolean inheritVisible = parent!=null && parent.isVisible();
            cat.setVisible(whitelistedIds.contains(cat.getId()) || inheritVisible);
        } else {
            // Not non-assignable, not blacklisted, no whitelist exists. Inherit.
            // If there is no parent, since there is no whitelist, make visible.
            Category parent = cat.getParent();
            cat.setVisible(parent==null || parent.isVisible());
        }
    }

}
