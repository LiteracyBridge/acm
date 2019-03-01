package org.literacybridge.core.spec;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StringFilter implements Predicate<String> {
    private boolean acceptsAll = false;
    private boolean isWhitelist = true;
    private Set<String> filteredItems;

    public StringFilter(String filter) {
        if (StringUtils.isAllBlank(filter)) {
            acceptsAll = true;
            return;
        }
        filter = filter.trim();
        if (filter.charAt(0) == '!' || filter.charAt(0) == '~') {
            filter = filter.substring(1).trim();
            isWhitelist = false;
        }
        String[] items = filter.split(",");
        filteredItems = Arrays.stream(items).map(String::trim).collect(Collectors.toSet());
    }

    @Override
    public boolean test(String s) {
        // If the item is present && it's a whitelist, or if the item isn't present and it isn't a whitelist.
        return acceptsAll || filteredItems.contains(s.trim()) == isWhitelist;
    }
}
