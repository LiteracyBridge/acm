package org.literacybridge.acm.config;

import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Taxonomy;
import org.literacybridge.acm.utils.Whitelister;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CategoryFilter extends Whitelister {
    private static final Logger LOG = Logger.getLogger(CategoryFilter.class.getName());

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
     *
     * @param projectDir The project directory.
     */
    public CategoryFilter(File projectDir) {
        super(categoryWhitelistFile(projectDir));
    }

    /**
     * Writes a new category.whitelist from the visibilities of the given taxonomy.
     *
     * @param projectDir Directory into which to write the file.
     * @param taxonomy   The taxonomy from which to get the visibilities.
     * @return True if the whitelist file was successfully written; false otherwise. (Check log
     * for reason.)
     */
    static boolean writeCategoryFilter(File projectDir, Taxonomy taxonomy) {
        File curFile = new File(projectDir, WHITELIST_FILENAME);
        File newFile = new File(projectDir, WHITELIST_FILENAME + ".new");
        File bakFile = new File(projectDir, WHITELIST_FILENAME + ".bak");

        boolean ok = true;
        // It's fine if there's no existing backup.
        if (bakFile.exists()) {
            ok = bakFile.delete();
            if (!ok) LOG.warning("Unable to delete old category.whitelist backup file");
        }
        if (ok) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(newFile));
                InputStream is = CategoryFilter.class.getClassLoader()
                    .getResourceAsStream(WHITELIST_FILENAME + ".txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.write("\n");
                }
                List<String> newData = buildWhitelistData(taxonomy);
                for (String d : newData) {
                    bw.write(d);
                    bw.write("\n");
                }
            } catch (Exception e) {
                ok = false;
                LOG.warning("Unable to create new category.whitelist file");
            }
        }
        if (ok) {
            // It's fine if there is no existing file.
            if (curFile.exists()) ok = curFile.renameTo(bakFile);
            if (!ok) LOG.warning("Unable to rename old category.whitelist file");
        }
        if (ok) {
            ok = newFile.renameTo(curFile);
            if (!ok) LOG.warning("Unable to rename new category.whitelist file");
        }
        return ok;
    }

    /**
     * Determines the visibility of the given category. Determines visibility as follows:
     * - If the category is 'nonAssignable', it is not visible.
     * - If the category's id is blacklisted, it is not visible.
     * - If there is a whitelist, then:
     * --  If the category's id is in the whitelist, it is visible.
     * --  If the category has a parent, and the parent is visible, it is visible (by inheritance).
     * --  Otherwise the category is not visible.
     * - Otherwise there is no whitelist, and:
     * --  If the category has a parent, it inherits the parent's visibility.
     * --  Otherwise this is the root category, and it is visible.
     *
     * @param cat    The category for which to get the visibility.
     * @param parent The parent, if any, of the category.
     * @return true if the category should be visible to the user.
     */
    public boolean getVisibilityFor(Category cat, Category parent) {
        boolean isVisible;
        if (cat.isNonAssignable()) {
            // If the category is "nonAssignable" in the taxonomy, it is not visible.
            isVisible = false;
        } else if (blacklistedItems != null && blacklistedItems.contains(cat.getId())) {
            // If there's a blacklist, and this id is in it, it is not visible.
            isVisible = false;
        } else if (whitelistedItems != null) {
            // If a whitelist exists, this is visible if in the whitelist, or if
            // parent is visible.
            boolean inheritVisible = parent != null && parent.isVisible();
            isVisible = whitelistedItems.contains(cat.getId()) || inheritVisible;
        } else {
            // Not non-assignable, not blacklisted, no whitelist exists. Inherit.
            // If there is no parent, since there is no whitelist, make visible.
            isVisible = parent == null || parent.isVisible();
        }
        return isVisible;
    }

    /**
     * Determines if the given category, or any of its children, requires whitelist semantics. This
     * really means "is there a visible category below a non-visible category?".
     *
     * @param taxonomy to be checked.
     * @return true if it requires whitelist semantics.
     */
    public static boolean requiresWhitelistSemantics(Taxonomy taxonomy) {
        Category root = taxonomy.getRootCategory();
        return requiresWhitelistSemantics(root, false);
    }

    /**
     * Determines if the given category, or any of its children, requires whitelist semantics. This
     * really means "is there a visible category below a non-visible category?".
     *
     * @param cat                 To be tested.
     * @param hasNonVisibleParent True if the category has a non-visible parent.
     * @return true if this category, or any of its descendents, requires whitelist semantics.
     */
    private static boolean requiresWhitelistSemantics(Category cat, boolean hasNonVisibleParent) {
        if (cat.isVisible() && hasNonVisibleParent) {
            return true;
        }
        hasNonVisibleParent |= !cat.isVisible();
        for (Category child : cat.getChildren()) {
            if (requiresWhitelistSemantics(child, hasNonVisibleParent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the whitelist (and/or blacklist) data for a Taxonomy.
     *
     * @param taxonomy for which the whitelist data is desired.
     * @return a List<String> of the data.
     */
    public static List<String> buildWhitelistData(Taxonomy taxonomy) {
        // If there is a whitelist, the root "inherits" non-visible.
        // If there is only a blacklist, the root "inherits" visible.
        boolean visibleParent = !requiresWhitelistSemantics(taxonomy);
        return buildWhitelistWorker(taxonomy.getRootCategory(), visibleParent, true);
    }

    /**
     * Helper to do the actual work of building the whitelist data. Builds a whitelist or
     * blacklist line for a category, if the category has different visibility from its parent.
     *
     * @param cat           The category for which to (possibly) generate a line.
     * @param visibleParent whether the parent is visible or not.
     * @param includeHeaders true if headers should be generated at this level.
     */
    private static List<String> buildWhitelistWorker(
        Category cat,
        boolean visibleParent,
        boolean includeHeaders)
    {
        List<String> result = new ArrayList<>();

        // if the (assignable) category's visibility is different than its parent's...
        if (!cat.isNonAssignable() && (cat.isVisible() != visibleParent)) {
            // If it's now NOT visible, make a blacklist line.
            StringBuilder sb = new StringBuilder();
            if (!cat.isVisible()) {
                sb.append("~ ");
            }
            // The whitelisted / blacklisted id
            sb.append(cat.getId());
            // Pad out to 10, then add the category name as a comment.
            while (sb.length() < 10) sb.append(' ');
            sb.append(" # ");
            sb.append(cat.getCategoryName());
            result.add(sb.toString());
        }
        visibleParent = cat.isVisible();
        for (Category child : cat.getSortedChildren()) {
            List<String> children = buildWhitelistWorker(child, visibleParent, false);
            // Add a header before the actual contents, if desired (ie, only on the top-level categories).
            if (includeHeaders && children.size()>0) {
                result.add("# "+child.getCategoryName());
            }
            result.addAll(children);
        }
        return result;
    }

}
