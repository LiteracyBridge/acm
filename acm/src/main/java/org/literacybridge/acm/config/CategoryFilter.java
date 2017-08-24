package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CategoryFilter {

    private static final String WHITELIST_FILENAME = "category.whitelist";

    private Set<String> whitelistedIds = null;

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
            whitelistedIds = new HashSet<>();
            try {
                IOUtils.readLines(whitelistFile, whitelistedIds);
            } catch (IOException ex) {
                // If we can't read the whitelist file, include everything.
                whitelistedIds = null;
            }
        }
    }

    /**
     * A category is included if its id is in the whitelist, or if there is no whitelist at all.
     * @param id The id of the category.
     * @return true if the category should be included.
     */
    public boolean isIncluded(String id) {
        return whitelistedIds == null || whitelistedIds.contains(id);
    }
}
