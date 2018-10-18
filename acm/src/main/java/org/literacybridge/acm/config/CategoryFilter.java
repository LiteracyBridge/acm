package org.literacybridge.acm.config;

import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.Whitelister;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class CategoryFilter extends Whitelister {

    public static final String WHITELIST_FILENAME = "category.whitelist";

    private static File categoryWhitelistFile(File projectDir) {
        // If allCategories is set, we just ignore the whitelist anyway.
        if (ACMConfiguration.getInstance().isAllCategories()) {
            return null;
        }
        return new File(projectDir, WHITELIST_FILENAME);
    }

    /**
     * Construct the CategoryFilter with the project directory. The whitelist file, if it exists,
     * will be in that directory.
     * @param projectDir The project directory.
     */
    public CategoryFilter(File projectDir) {
        super(categoryWhitelistFile(projectDir));
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
        } else if (blacklistedItems != null && blacklistedItems.contains(cat.getId())) {
            // If there's a blacklist, and this id is in it, it is not visible.
            cat.setVisible(false);
        } else if (whitelistedItems != null) {
            // If a whitelist exists, this is visible if in the whitelist, or if
            // parent is visible.
            Category parent = cat.getParent();
            boolean inheritVisible = parent!=null && parent.isVisible();
            cat.setVisible(whitelistedItems.contains(cat.getId()) || inheritVisible);
        } else {
            // Not non-assignable, not blacklisted, no whitelist exists. Inherit.
            // If there is no parent, since there is no whitelist, make visible.
            Category parent = cat.getParent();
            cat.setVisible(parent==null || parent.isVisible());
        }
    }

}
