package org.literacybridge.acm.store;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

public class Category {
    private String name;
    private String uuid;
    private int order;
    private Category parent;
    private final List<Category> children;

    public Category(String uuid) {
        this.uuid = uuid;
        this.children = Lists.newLinkedList();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public void addChild(Category childCategory) {
        children.add(childCategory);
    }

    public void clearChildren() {
        children.clear();
    }

    public Iterable<Category> getChildren() {
        return children;
    }

    public Iterable<Category> getSortedChildren() {
        List<Category> sorted = Lists.newArrayList(children);
        Collections.sort(sorted, new Comparator<Category>() {
            @Override public int compare(Category c1, Category c2) {
                return c1.getOrder() - c2.getOrder();
            }
        });

        return sorted;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return this.name;
    }
}