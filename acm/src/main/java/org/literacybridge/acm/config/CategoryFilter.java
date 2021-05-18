package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Taxonomy;
import org.literacybridge.acm.utils.Includelister;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CategoryFilter extends Includelister {
    private static final Logger LOG = Logger.getLogger(CategoryFilter.class.getName());

    private static File categoryIncludelistFile(File includelistFile) {
        // If allCategories is set, we just ignore the includelist anyway.
        if (ACMConfiguration.getInstance().isAllCategories()) {
            return null;
        }
        return includelistFile;
    }

    /**
     * Construct the CategoryFilter with the project directory. The includelist file, if it exists,
     * will be in that directory.
     *
     * @param includelistFile The project directory.
     */
    public CategoryFilter(File includelistFile) {
        super(categoryIncludelistFile(includelistFile), OPTIONS.emptyImpliesAll);
    }

    /**
     * Writes a new category.includelist from the visibilities of the given taxonomy.
     *
     * @param newFile    File into which to write the new list.
     * @param taxonomy   The taxonomy from which to get the visibilities.
     */
    static void writeCategoryFilter(File newFile, Taxonomy taxonomy) {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(newFile));
             InputStream is = CategoryFilter.class.getClassLoader()
                .getResourceAsStream(Constants.CATEGORY_INCLUDELIST_FILENAME + ".txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.write("\n");
            }
            List<String> newData = buildIncludelistData(taxonomy);
            for (String d : newData) {
                bw.write(d);
                bw.write("\n");
            }
        } catch (Exception e) {
            LOG.warning("Unable to create new category.includelist file");
        }
    }

    /**
     * Determines the visibility of the given category. Determines visibility as follows:
     * - If the category is 'nonAssignable', it is not visible.
     * - If the category's id is excluded, it is not visible.
     * - If there is an includelist, then:
     * --  If the category's id is in the includelist, it is visible.
     * --  If the category has a parent, and the parent is visible, it is visible (by inheritance).
     * --  Otherwise the category is not visible.
     * - Otherwise there is no includelist, and:
     * --  If the category has a parent, it inherits the parent's visibility.
     * --  Otherwise this is the root category, and it is visible.
     *
     * @param catId    The category id for which to get the visibility.
     * @param parent The parent, if any, of the category.
     * @return true if the category should be visible to the user.
     */
    public boolean getVisibilityFor(String catId, Category parent) {
        boolean isVisible;
        if (excludedItems != null && excludedItems.contains(catId)) {
            // If there's an excludelist, and this id is in it, it is not visible.
            isVisible = false;
        } else if (includedItems != null) {
            // If an includelist exists, this is visible if in the includelist, or if
            // parent is visible.
            boolean inheritVisible = parent != null && parent.isVisible();
            isVisible = includedItems.contains(catId) || inheritVisible;
        } else {
            // Not non-assignable, not excluded, no includelist exists. Inherit.
            // If there is no parent, since there is no includelist, make visible.
            isVisible = parent == null || parent.isVisible();
        }
        return isVisible;
    }
    public boolean getVisibilityFor(Category cat, Category parent) {
        boolean isVisible;
        if (cat.isNonAssignable()) {
            isVisible = false;
        } else {
            isVisible = getVisibilityFor(cat.getId(), parent);
        }
        return isVisible;
    }

        /**
         * Determines if the given category, or any of its children, requires includelist semantics. This
         * really means "is there a visible category below a non-visible category?".
         *
         * @param taxonomy to be checked.
         * @return true if it requires includelist semantics.
         */
    public static boolean requiresIncludelistSemantics(Taxonomy taxonomy) {
        Category root = taxonomy.getRootCategory();
        return requiresIncludelistSemantics(root, false);
    }

    /**
     * Determines if the given category, or any of its children, requires includelist semantics. This
     * really means "is there a visible category below a non-visible category?".
     *
     * @param cat                 To be tested.
     * @param hasNonVisibleParent True if the category has a non-visible parent.
     * @return true if this category, or any of its descendents, requires includelist semantics.
     */
    private static boolean requiresIncludelistSemantics(Category cat, boolean hasNonVisibleParent) {
        if (cat.isVisible() && hasNonVisibleParent) {
            return true;
        }
        hasNonVisibleParent |= !cat.isVisible();
        for (Category child : cat.getChildren()) {
            if (requiresIncludelistSemantics(child, hasNonVisibleParent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the includelist (and/or excludelist) data for a Taxonomy.
     *
     * @param taxonomy for which the includelist data is desired.
     * @return a List<String> of the data.
     */
    public static List<String> buildIncludelistData(Taxonomy taxonomy) {
        // If there is an includelist, the root "inherits" non-visible.
        // If there is only an excludelist, the root "inherits" visible.
        boolean visibleParent = !requiresIncludelistSemantics(taxonomy);
        return buildIncludelistWorker(taxonomy.getRootCategory(), visibleParent, true);
    }

    /**
     * Helper to do the actual work of building the includelist data. Builds an includelist or
     * excludelist line for a category, if the category has different visibility from its parent.
     *
     * @param cat           The category for which to (possibly) generate a line.
     * @param visibleParent whether the parent is visible or not.
     * @param includeHeaders true if headers should be generated at this level.
     */
    private static List<String> buildIncludelistWorker(
        Category cat,
        boolean visibleParent,
        boolean includeHeaders)
    {
        List<String> result = new ArrayList<>();

        // if the (assignable) category's visibility is different than its parent's...
        if (!cat.isNonAssignable() && (cat.isVisible() != visibleParent)) {
            // If it's now NOT visible, make an excludelist line.
            StringBuilder sb = new StringBuilder();
            if (!cat.isVisible()) {
                sb.append("~ ");
            }
            // The included / excluded id
            sb.append(cat.getId());
            // Pad out to 10, then add the category name as a comment.
            while (sb.length() < 10) sb.append(' ');
            sb.append(" # ");
            sb.append(cat.getCategoryName());
            result.add(sb.toString());
        }
        visibleParent = cat.isVisible();
        for (Category child : cat.getSortedChildren()) {
            List<String> children = buildIncludelistWorker(child, visibleParent, false);
            // Add a header before the actual contents, if desired (ie, only on the top-level categories).
            if (includeHeaders && children.size()>0) {
                result.add("# "+child.getCategoryName());
            }
            result.addAll(children);
        }
        return result;
    }

}
