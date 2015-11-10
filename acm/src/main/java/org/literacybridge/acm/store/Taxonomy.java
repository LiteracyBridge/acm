package org.literacybridge.acm.store;

import java.io.File;
import java.util.Map;

import com.google.common.collect.Maps;

public class Taxonomy {

    private static String rootUUID = DefaultLiteracyBridgeTaxonomy.LB_TAXONOMY_UID;

    private final Category mRootCategory;

    private final Map<String, Category> categories;

    private Taxonomy(Category root) {
        categories = Maps.newHashMap();
        this.mRootCategory = root;
        categories.put(root.getUuid(), root);
    }

    public static Taxonomy createTaxonomy(File acmDirectory) {
        return getTaxonomy(DefaultLiteracyBridgeTaxonomy.LB_TAXONOMY_UID, acmDirectory);
    }

    private static Taxonomy getTaxonomy(String uuid, File acmDirectory) {
        DefaultLiteracyBridgeTaxonomy.TaxonomyRevision latestRevision = DefaultLiteracyBridgeTaxonomy.loadLatestTaxonomy(acmDirectory);

        Category root = new Category(uuid);
        root.setName("root");
        return createNewTaxonomy(latestRevision, null);
    }

    private static Taxonomy createNewTaxonomy(DefaultLiteracyBridgeTaxonomy.TaxonomyRevision revision, final Map<String, Category> existingCategories) {
        Category root = new Category(rootUUID);
        root.setName("root");
        root.setOrder(0);

        Taxonomy taxonomy = new Taxonomy(root);

        revision.createTaxonomy(taxonomy, existingCategories);
        return taxonomy;
    }

    public Category getRootCategory() {
        return mRootCategory;
    }

    public Category getCategory(String uuid) {
        return categories.get(uuid);
    }

    public Iterable<Category> getCategoryList() {
        return mRootCategory.getChildren();
    }

    public boolean isRoot(Category category) {
        return category == this.mRootCategory;
    }

    public boolean addChild(Category parent, Category newChild) {
        parent.addChild(newChild);
        categories.put(newChild.getUuid(), newChild);
        return true;
    }
}
