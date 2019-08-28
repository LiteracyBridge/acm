package org.literacybridge.acm.store;

import org.junit.Test;
import org.literacybridge.acm.store.Category.CategoryBuilder;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CategoryTest {

    private int count;

    @Test
    public void testIterator() {
        Category tree = buildCatTree();
        Iterable<Category> catIter = tree.breadthFirstIterator();
        count = 0;
        Set<Category> result = new LinkedHashSet<>();
        catIter.forEach(cat -> {result.add(cat); count++;});

        assertEquals("Should have count entries.", result.size(), count);

        Set<Category> result2 = new LinkedHashSet<>();
        tree.breadthFirstIterator().stream().forEach(cat -> {result2.add(cat); count++;});

        assertEquals("Stream should be in order", result, result2);
    }

    @Test
    public void testFind() {
        Category tree = buildCatTree();

        Category c010 = tree.findChildWithName("0-1-0");
        assertNotNull(c010);

        Category c0041 = tree.findChildWithName("0-0-4-1");
        assertNotNull(c0041);

        Category c0041a = tree.findChildWithName("0-0:0-0-4:0-0-4-1");
        assertSame("Should return same object", c0041, c0041a);

    }

    private Category buildCatTree() {
        Category root = new CategoryBuilder("0").withName("0").build();

        addChildren(root, 0, 2);

        return root;
    }

    private void addChildren(Category root, int level, int maxLevel) {
        String baseId = root.getId();
        for (int i=0; i<5; i++) {
            String newId = String.format("%s-%d", baseId, i);
            Category newCat = new CategoryBuilder(newId).withName(newId).build();
            root.addChild(newCat);
            if (level < maxLevel) addChildren(newCat, level+1, maxLevel);
        }
    }
}
