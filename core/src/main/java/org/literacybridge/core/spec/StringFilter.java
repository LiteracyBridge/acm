package org.literacybridge.core.spec;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StringFilter implements Predicate<String> {
    private static final Set<String> emptySet = new HashSet<>();
    private boolean acceptsAll = false;
    private boolean isIncludelist = true;
    private Set<String> filteredItems;

    public StringFilter(String filter) {
        if (StringUtils.isAllBlank(filter)) {
            acceptsAll = true;
            return;
        }
        filter = filter.trim().toLowerCase();
        if (filter.charAt(0) == '!' || filter.charAt(0) == '~') {
            filter = filter.substring(1).trim();
            isIncludelist = false;
        }
        String[] items = filter.split(",");
        filteredItems = Arrays.stream(items).map(String::trim).collect(Collectors.toSet());
    }

    @Override
    public boolean test(String s) {
        // If the item is present && it's an includelist, or if the item isn't present and it isn't an includelist.
        return acceptsAll || filteredItems.contains(s.trim().toLowerCase()) == isIncludelist;
    }

    public Collection<String> items() {
        return acceptsAll ? emptySet : new HashSet<>(this.filteredItems);
    }
}
